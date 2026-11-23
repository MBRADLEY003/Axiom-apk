package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AxiomRepository
import com.example.data.ImportParseResult
import com.example.data.OttiliaAiRepository
import com.example.data.OttiliaBriefing
import com.example.domain.analytics.AxiomAnalyticsEngine
import com.example.domain.analytics.CusumPoint
import com.example.domain.analytics.DriverPerformanceScore
import com.example.domain.analytics.FleetExecutiveSummary
import com.example.domain.analytics.ForecastSeriesResult
import com.example.domain.analytics.FuelAnomalySignal
import com.example.domain.analytics.MaintenancePrediction
import com.example.domain.analytics.VehicleRiskScore
import com.example.domain.model.AuditLogRecord
import com.example.domain.model.AxiomRole
import com.example.domain.model.Company
import com.example.domain.model.Driver
import com.example.domain.model.ExpenseRecord
import com.example.domain.model.FuelTransaction
import com.example.domain.model.MaintenanceRecord
import com.example.domain.model.MaintenanceStatus
import com.example.domain.model.NotificationRecord
import com.example.domain.model.RiskAlertRecord
import com.example.domain.model.RiskResolutionStatus
import com.example.domain.model.RiskSeverity
import com.example.domain.model.ShipmentRecord
import com.example.domain.model.ShipmentStatus
import com.example.domain.model.TeamMember
import com.example.domain.model.TripRecord
import com.example.domain.model.UserProfile
import com.example.domain.model.Vehicle
import com.example.domain.rbac.AxiomPermission
import com.example.domain.rbac.RbacPermissions
import com.example.domain.rbac.SecurityHashUtil
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DateRangeFilter(val label: String, val days: Int?) {
    ALL("All Time", null),
    LAST_30("30 Days", 30),
    LAST_7("7 Days", 7),
}

data class OttiliaChatMessage(
    val id: String,
    val fromUser: Boolean,
    val text: String,
    val timestampLabel: String,
)

sealed interface AxiomWorkspaceUiState {
    data object Loading : AxiomWorkspaceUiState
    data class Error(val message: String) : AxiomWorkspaceUiState
    data class Ready(
        val profile: UserProfile,
        val activeCompany: Company,
        val companies: List<Company>,
        val teamMembers: List<TeamMember>,
        val vehicles: List<Vehicle>,
        val filteredVehicles: List<Vehicle>,
        val drivers: List<Driver>,
        val fuelTransactions: List<FuelTransaction>,
        val trips: List<TripRecord>,
        val maintenance: List<MaintenanceRecord>,
        val expenses: List<ExpenseRecord>,
        val shipments: List<ShipmentRecord>,
        val riskAlerts: List<RiskAlertRecord>,
        val notifications: List<NotificationRecord>,
        val auditLogs: List<AuditLogRecord>,
        val summary: FleetExecutiveSummary,
        val fuelSignals: List<FuelAnomalySignal>,
        val cusumPoints: List<CusumPoint>,
        val fuelSpendForecast: ForecastSeriesResult,
        val fuelLitersForecast: ForecastSeriesResult,
        val maintenancePredictions: List<MaintenancePrediction>,
        val driverScores: List<DriverPerformanceScore>,
        val vehicleRisks: List<VehicleRiskScore>,
        val dateFilter: DateRangeFilter,
        val selectedVehicleFilterId: String?,
        val simulatedModeActive: Boolean,
        val hasDemoRecords: Boolean,
    ) : AxiomWorkspaceUiState
}

sealed interface BriefingUiState {
    data object Idle : BriefingUiState
    data object Loading : BriefingUiState
    data class Success(val briefing: OttiliaBriefing) : BriefingUiState
    data class Error(val message: String) : BriefingUiState
}

class AxiomViewModel(
    private val repository: AxiomRepository,
    private val ottiliaRepo: OttiliaAiRepository,
    private val currentUserId: String,
    private val userEmail: String,
    private val userDisplayName: String,
) : ViewModel() {

    private val _dateFilter = MutableStateFlow(DateRangeFilter.ALL)
    private val _vehicleFilterId = MutableStateFlow<String?>(null)
    private val _simulatedMode = MutableStateFlow(true)

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _ottiliaMessages = MutableStateFlow(
        listOf(
            OttiliaChatMessage(
                id = "welcome",
                fromUser = false,
                text = "Ottilia online. Ask any operational question across Fleet, Fuel Z-Score/CUSUM signals, Routes, Maintenance, Finance, or Shipments. Every figure I cite is computed directly from your AXIOM dataset.",
                timestampLabel = "Ready"
            )
        )
    )
    val ottiliaMessages: StateFlow<List<OttiliaChatMessage>> = _ottiliaMessages.asStateFlow()

    private val _ottiliaStreaming = MutableStateFlow("")
    val ottiliaStreaming: StateFlow<String> = _ottiliaStreaming.asStateFlow()

    private val _ottiliaBusy = MutableStateFlow(false)
    val ottiliaBusy: StateFlow<Boolean> = _ottiliaBusy.asStateFlow()

    private val _ottiliaError = MutableStateFlow<String?>(null)
    val ottiliaError: StateFlow<String?> = _ottiliaError.asStateFlow()

    private val _briefingState = MutableStateFlow<BriefingUiState>(BriefingUiState.Idle)
    val briefingState: StateFlow<BriefingUiState> = _briefingState.asStateFlow()

    private var latestTelemetryContext: String = ""

    init {
        viewModelScope.launch {
            val initResult = repository.ensureUserInitialized(
                uid = currentUserId,
                displayName = userDisplayName,
                email = userEmail,
            )
            if (initResult.isFailure) {
                _snackbarMessage.value = initResult.exceptionOrNull()?.localizedMessage
                    ?: "Failed to initialize AXIOM workspace."
            }
        }
    }

    private data class CoreCollectionsA(
        val profile: UserProfile?,
        val companies: List<Company>,
        val team: List<TeamMember>,
        val vehicles: List<Vehicle>,
        val drivers: List<Driver>,
        val fuel: List<FuelTransaction>,
    )

    private data class CoreCollectionsB(
        val trips: List<TripRecord>,
        val maintenance: List<MaintenanceRecord>,
        val expenses: List<ExpenseRecord>,
        val shipments: List<ShipmentRecord>,
        val risks: List<RiskAlertRecord>,
        val notifications: List<NotificationRecord>,
        val audits: List<AuditLogRecord>,
    )

    private val flowA = combine(
        repository.observeProfile(currentUserId),
        repository.observeCompanies(currentUserId),
        repository.observeTeamMembers(currentUserId),
        repository.observeVehicles(currentUserId),
        repository.observeDrivers(currentUserId),
        repository.observeFuelTransactions(currentUserId),
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        CoreCollectionsA(
            profile = values[0] as UserProfile?,
            companies = values[1] as List<Company>,
            team = values[2] as List<TeamMember>,
            vehicles = values[3] as List<Vehicle>,
            drivers = values[4] as List<Driver>,
            fuel = values[5] as List<FuelTransaction>,
        )
    }

    private val flowB = combine(
        repository.observeTrips(currentUserId),
        repository.observeMaintenance(currentUserId),
        repository.observeExpenses(currentUserId),
        repository.observeShipments(currentUserId),
        repository.observeRiskAlerts(currentUserId),
        repository.observeNotifications(currentUserId),
        repository.observeAuditLogs(currentUserId),
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        CoreCollectionsB(
            trips = values[0] as List<TripRecord>,
            maintenance = values[1] as List<MaintenanceRecord>,
            expenses = values[2] as List<ExpenseRecord>,
            shipments = values[3] as List<ShipmentRecord>,
            risks = values[4] as List<RiskAlertRecord>,
            notifications = values[5] as List<NotificationRecord>,
            audits = values[6] as List<AuditLogRecord>,
        )
    }

    val uiState: StateFlow<AxiomWorkspaceUiState> = combine(
        flowA,
        flowB,
        _dateFilter,
        _vehicleFilterId,
        _simulatedMode,
    ) { a, b, dateFilter, vehicleFilterId, simMode ->
        val profile = a.profile ?: UserProfile(
            userId = currentUserId,
            displayName = userDisplayName.ifBlank { "AXIOM Operator" },
            email = userEmail,
            activeCompanyId = "comp_apex",
            activeRole = AxiomRole.SUPER_ADMIN.name,
        )
        val companies = a.companies.ifEmpty {
            listOf(
                Company(
                    id = "comp_apex",
                    companyId = "comp_apex",
                    name = "Apex Freight & Logistics",
                    industry = "Long-Haul & Cold Chain",
                    currency = "USD",
                    demoData = false
                )
            )
        }
        val activeCompany = companies.firstOrNull {
            it.companyId == profile.activeCompanyId || it.id == profile.activeCompanyId
        } ?: companies.first()
        val cid = activeCompany.companyId.ifBlank { activeCompany.id }

        val cTeam = a.team.filter { it.companyId == cid }
        val cVehicles = a.vehicles.filter { it.companyId == cid }.sortedBy { it.registrationNumber }
        val cDrivers = a.drivers.filter { it.companyId == cid }.sortedBy { it.fullName }

        val nowMs = System.currentTimeMillis()
        val cutoffMs = dateFilter.days?.let { nowMs - it.toLong() * 86_400_000L }

        val cFuel = a.fuel.filter { it.companyId == cid }
            .filter { cutoffMs == null || it.txTimestampMillis == 0L || it.txTimestampMillis >= cutoffMs }
            .filter { vehicleFilterId == null || it.vehicleId == vehicleFilterId }
            .sortedByDescending { it.txTimestampMillis }

        val cTrips = b.trips.filter { it.companyId == cid }
            .filter { cutoffMs == null || it.startedAtMillis == 0L || it.startedAtMillis >= cutoffMs }
            .filter { vehicleFilterId == null || it.vehicleId == vehicleFilterId }
            .sortedByDescending { it.startedAtMillis }

        val cMaint = b.maintenance.filter { it.companyId == cid }
            .filter { vehicleFilterId == null || it.vehicleId == vehicleFilterId }
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }

        val cExpenses = b.expenses.filter { it.companyId == cid }
            .filter { vehicleFilterId == null || it.vehicleId.isBlank() || it.vehicleId == vehicleFilterId }

        val cShipments = b.shipments.filter { it.companyId == cid }
            .filter { vehicleFilterId == null || it.vehicleId.isBlank() || it.vehicleId == vehicleFilterId }
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }

        val cRisks = b.risks.filter { it.companyId == cid }
            .filter { vehicleFilterId == null || it.entityId.isBlank() || it.entityId == vehicleFilterId }
            .sortedByDescending { it.severityEnum().ordinal }

        val cNotifs = b.notifications.filter { it.companyId == cid }
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }

        val cAudits = b.audits.filter { it.companyId == cid }
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }

        val filteredVehicles = if (vehicleFilterId == null) cVehicles else cVehicles.filter { it.id == vehicleFilterId }

        val summary = AxiomAnalyticsEngine.computeExecutiveSummary(
            vehicles = filteredVehicles,
            drivers = cDrivers,
            fuelTransactions = cFuel,
            trips = cTrips,
            maintenance = cMaint,
            expenses = cExpenses,
            shipments = cShipments,
            riskAlerts = cRisks,
        )

        val (fuelSignals, cusumPoints) = AxiomAnalyticsEngine.analyzeFuelIntelligence(cVehicles, cFuel)

        val chronologicalFuel = cFuel.sortedBy { it.txTimestampMillis }
        val spendForecast = AxiomAnalyticsEngine.forecastSeries(
            labels = chronologicalFuel.map { it.txDateLabel.takeLast(5) },
            values = chronologicalFuel.map { it.costAmount },
            horizonPeriods = 3,
            futureLabels = listOf("Next+1", "Next+2", "Next+3")
        )
        val litersForecast = AxiomAnalyticsEngine.forecastSeries(
            labels = chronologicalFuel.map { it.txDateLabel.takeLast(5) },
            values = chronologicalFuel.map { it.liters },
            horizonPeriods = 3,
            futureLabels = listOf("Next+1", "Next+2", "Next+3")
        )

        val maintPredictions = AxiomAnalyticsEngine.predictMaintenanceNeeds(filteredVehicles, cMaint)
        val driverScores = AxiomAnalyticsEngine.scoreDrivers(cDrivers, cFuel)
        val vehicleRisks = AxiomAnalyticsEngine.scoreOperationalVehicleRisks(
            vehicles = filteredVehicles,
            fuelSignals = fuelSignals,
            trips = cTrips,
            maintenancePredictions = maintPredictions,
            riskAlerts = cRisks,
        )

        latestTelemetryContext = AxiomAnalyticsEngine.buildGroundedTelemetryContext(
            companyName = activeCompany.name,
            currency = activeCompany.currency,
            summary = summary,
            vehicles = cVehicles,
            drivers = cDrivers,
            fuelTransactions = cFuel,
            fuelSignals = fuelSignals,
            trips = cTrips,
            maintenancePredictions = maintPredictions,
            expenses = cExpenses,
            shipments = cShipments,
            riskAlerts = cRisks,
        )

        val hasDemo = cVehicles.any { it.demoData } || cFuel.any { it.demoData }

        AxiomWorkspaceUiState.Ready(
            profile = profile,
            activeCompany = activeCompany,
            companies = companies,
            teamMembers = cTeam,
            vehicles = cVehicles,
            filteredVehicles = filteredVehicles,
            drivers = cDrivers,
            fuelTransactions = cFuel,
            trips = cTrips,
            maintenance = cMaint,
            expenses = cExpenses,
            shipments = cShipments,
            riskAlerts = cRisks,
            notifications = cNotifs,
            auditLogs = cAudits,
            summary = summary,
            fuelSignals = fuelSignals,
            cusumPoints = cusumPoints,
            fuelSpendForecast = spendForecast,
            fuelLitersForecast = litersForecast,
            maintenancePredictions = maintPredictions,
            driverScores = driverScores,
            vehicleRisks = vehicleRisks,
            dateFilter = dateFilter,
            selectedVehicleFilterId = vehicleFilterId,
            simulatedModeActive = simMode,
            hasDemoRecords = hasDemo,
        ) as AxiomWorkspaceUiState
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.e(TAG, "Workspace stream failure", rootError)
        emit(AxiomWorkspaceUiState.Error(rootError.localizedMessage ?: "Failed to load AXIOM workspace"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = AxiomWorkspaceUiState.Loading
    )

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun setDateFilter(filter: DateRangeFilter) {
        _dateFilter.value = filter
    }

    fun setVehicleFilter(vehicleId: String?) {
        _vehicleFilterId.value = vehicleId
    }

    fun toggleSimulatedMode(enabled: Boolean) {
        _simulatedMode.value = enabled
    }

    private fun currentReady(): AxiomWorkspaceUiState.Ready? =
        uiState.value as? AxiomWorkspaceUiState.Ready

    private fun requirePermission(permission: AxiomPermission): Boolean {
        val state = currentReady() ?: return false
        val role = state.profile.roleEnum()
        val allowed = RbacPermissions.hasPermission(role, permission)
        if (!allowed) {
            _snackbarMessage.value = "RBAC Denied: ${role.displayName} lacks ${permission.name}"
        }
        return allowed
    }

    fun loadSampleDemoFleet() {
        val state = currentReady() ?: return
        if (!requirePermission(AxiomPermission.MANAGE_FLEET_VEHICLES)) return
        viewModelScope.launch {
            val res = repository.loadDemonstrationFleetData(
                companyId = state.activeCompany.companyId.ifBlank { state.activeCompany.id },
                actorEmail = state.profile.email,
                actorRole = state.profile.activeRole,
            )
            _snackbarMessage.value = if (res.isSuccess) {
                "Loaded labelled Demonstration Fleet dataset."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to load demonstration data."
            }
        }
    }

    fun switchCompany(company: Company) {
        val state = currentReady() ?: return
        viewModelScope.launch {
            val cid = company.companyId.ifBlank { company.id }
            val res = repository.updateProfileSettings(activeCompanyId = cid)
            if (res.isSuccess) {
                _vehicleFilterId.value = null
                repository.logAudit(cid, state.profile.email, state.profile.activeRole, "COMPANY_SWITCHED", company.name, "Switched active company tenant")
                _snackbarMessage.value = "Active company switched to ${company.name}"
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to switch company"
            }
        }
    }

    fun createCompany(name: String, industry: String, currency: String) {
        val state = currentReady() ?: return
        if (!requirePermission(AxiomPermission.MANAGE_COMPANY_AND_ROLES)) return
        if (name.isBlank()) {
            _snackbarMessage.value = "Company name is required."
            return
        }
        viewModelScope.launch {
            val slug = "comp_${name.lowercase().replace(Regex("[^a-z0-9]"), "_").take(24)}_${System.currentTimeMillis() % 1000}"
            val res = repository.createCompany(
                Company(
                    companyId = slug,
                    name = name.trim(),
                    industry = industry.trim().ifBlank { "Fleet & Logistics" },
                    currency = currency.trim().ifBlank { "USD" }.uppercase(),
                    demoData = false
                )
            )
            if (res.isSuccess) {
                repository.logAudit(slug, state.profile.email, state.profile.activeRole, "COMPANY_CREATED", name, "Created new company tenant ($currency)")
                _snackbarMessage.value = "Company '$name' created and activated."
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to create company"
            }
        }
    }

    fun switchActiveRole(role: AxiomRole) {
        val state = currentReady() ?: return
        viewModelScope.launch {
            val res = repository.updateProfileSettings(activeRole = role)
            if (res.isSuccess) {
                val cid = state.activeCompany.companyId.ifBlank { state.activeCompany.id }
                repository.logAudit(cid, state.profile.email, role.name, "ROLE_CHANGED", state.profile.displayName, "Active RBAC role switched to ${role.displayName}")
                _snackbarMessage.value = "RBAC Role active: ${role.displayName}"
            }
        }
    }

    fun setAppSecurityPin(pin: String) {
        val state = currentReady() ?: return
        if (pin.length < 4) {
            _snackbarMessage.value = "PIN must be at least 4 characters."
            return
        }
        viewModelScope.launch {
            val hash = SecurityHashUtil.hashPinSha256(pin, currentUserId)
            val res = repository.updateProfileSettings(pinHash = hash)
            if (res.isSuccess) {
                val cid = state.activeCompany.companyId.ifBlank { state.activeCompany.id }
                repository.logAudit(cid, state.profile.email, state.profile.activeRole, "SECURITY_PIN_UPDATED", state.profile.email, "Updated SHA-256 salted security PIN")
                _snackbarMessage.value = "SHA-256 security PIN saved."
            }
        }
    }

    fun updateAlertChannels(emailEnabled: Boolean, smsEnabled: Boolean) {
        viewModelScope.launch {
            val res = repository.updateProfileSettings(
                emailAlertsEnabled = emailEnabled,
                smsAlertsEnabled = smsEnabled
            )
            if (res.isSuccess) {
                _snackbarMessage.value = "Alert routing preferences updated."
            }
        }
    }

    fun addTeamMember(fullName: String, email: String, role: AxiomRole, department: String) {
        val state = currentReady() ?: return
        if (!requirePermission(AxiomPermission.MANAGE_COMPANY_AND_ROLES)) return
        if (fullName.isBlank() || email.isBlank()) {
            _snackbarMessage.value = "Name and email are required."
            return
        }
        viewModelScope.launch {
            val cid = state.activeCompany.companyId.ifBlank { state.activeCompany.id }
            val res = repository.addTeamMember(
                TeamMember(
                    companyId = cid,
                    fullName = fullName.trim(),
                    email = email.trim(),
                    role = role.name,
                    department = department.trim().ifBlank { "Operations" },
                    activeStatus = "ACTIVE"
                )
            )
            if (res.isSuccess) {
                repository.logAudit(cid, state.profile.email, state.profile.activeRole, "TEAM_MEMBER_ADDED", email, "Assigned role ${role.displayName}")
                _snackbarMessage.value = "Added $fullName as ${role.displayName}"
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to add team member"
            }
        }
    }

    fun addVehicle(vehicle: Vehicle) {
        val state = currentReady() ?: return
        if (!requirePermission(AxiomPermission.MANAGE_FLEET_VEHICLES)) return
        viewModelScope.launch {
            val cid = state.activeCompany.companyId.ifBlank { state.activeCompany.id }
            val res = repository.addVehicle(vehicle.copy(companyId = cid, demoData = false))
            if (res.isSuccess) {
                repository.logAudit(cid, state.profile.email, state.profile.activeRole, "VEHICLE_CREATED", vehicle.registrationNumber, "Added ${vehicle.makeModel}")
                _snackbarMessage.value = "Vehicle ${vehicle.registrationNumber} registered."
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to register vehicle"
            }
        }
    }

    fun updateVehicle(vehicleId: String, registration: String, updates: Map<String, Any>) {
        val state = currentReady() ?: return
        if (!requirePermission(AxiomPermission.MANAGE_FLEET_VEHICLES)) return
        viewModelScope.launch {
            val cid = state.activeCompany.companyId.ifBlank { state.activeCompany.id }
            val res = repository.updateVehicle(vehicleId, updates)
            if (res.isSuccess) {
                repository.logAudit(cid, state.profile.email, state.profile.activeRole, "VEHICLE_UPDATED", registration, "Updated fields: ${updates.keys.joinToString()}")
                _snackbarMessage.value = "Updated $registration"
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update vehicle"
            }
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        val state = currentReady() ?: return
        if (!requirePermission(AxiomPermission.DELETE_VEHICLES)) return
        viewModelScope.launch {
            val cid = state.activeCompany.companyId.ifBlank { state.activeCompany.id }
            val res = repository.deleteVehicle(vehicle.id)
            if (res.isSuccess) {
                repository.logAudit(cid, state.profile.email, state.profile.activeRole, "VEHICLE_DELETED", vehicle.registrationNumber, "Removed vehicle ${vehicle.vin}")
                _snackbarMessage.value = "Deleted ${vehicle.registrationNumber}"
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete vehicle"
            }
        }
    }

    fun addDriver(driver: Driver) {
        val state = currentReady() ?: return
        if (!requirePermission(AxiomPermission.MANAGE_FLEET_VEHICLES)) return
        viewModelScope.launch {
            val cid = state.activeCompany.companyId.ifBlank { state.activeCompany.id }
            val res = repository.addDriver(driver.copy(companyId = cid, demoData = false))
            if (res.isSuccess) {
                repository.logAudit(cid, state.profile.email, state.profile.activeRole, "DRIVER_ADDED", driver.fullName, "Driver code ${driver.driverCode}")
                _snackbarMessage.value = "Driver ${driver.fullName} added."
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to add driver"
            }
        }
    }

    fun logFuelTransaction(tx: FuelTransaction) {
        val state = currentReady() ?: return
        if (!requirePermission(AxiomPermission.LOG_FUEL_TRANSACTIONS)) return
        viewModelScope.launch {
            val cid = state.activeCompany.companyId.ifBlank { state.activeCompany.id }
            val enriched = tx.copy(companyId = cid, demoData = false)
            val res = repository.addFuelTransaction(enriched)
            if (res.isSuccess) {
                // Immediately evaluate statistical anomaly against vehicle history
                val (signals, _) = AxiomAnalyticsEngine.analyzeFuelIntelligence(
                    state.vehicles,
                    state.fuelTransactions + enriched
                )
                val newSignal = signals.firstOrNull { it.transactionId == enriched.id || (it.vehicleReg == enriched.vehicleReg && it.liters == enriched.liters) }
                if (newSignal != null && (newSignal.severity == RiskSeverity.HIGH || newSignal.severity == RiskSeverity.CRITICAL)) {
                    repository.addRiskAlert(
                        RiskAlertRecord(
                            companyId = cid,
                            category = newSignal.signalCategory,
                            severity = newSignal.severity.name,
                            entityType = "VEHICLE",
                            entityId = enriched.vehicleId,
                            entityLabel = "${enriched.vehicleReg} (${enriched.driverName})",
                            supportingData = newSignal.evidenceSummary,
                            detectionMethod = "Real-time Z-Score, CUSUM & Tank Capacity Engine",
                            historicalComparison = "Expected ${"%.2f".format(newSignal.expectedKmPerL)} km/L vs observed ${"%.2f".format(newSignal.observedKmPerL)} km/L",
                            recommendedInvestigation = newSignal.recommendedInvestigation,
                            confidenceScore = newSignal.confidenceScore,
                            resolutionStatus = RiskResolutionStatus.OPEN.name,
                            demoData = false
                        )
                    )
                    repository.addNotification(
                        NotificationRecord(
                            companyId = cid,
                            title = "Fuel Investigation Signal: ${enriched.vehicleReg}",
                            message = newSignal.evidenceSummary.take(400),
                            category = "Fuel Intelligence",
                            severity = newSignal.severity.name,
                            readStatus = false,
                            channel = "IN_APP"
                        )
                    )
                }
                repository.logAudit(cid, state.profile.email, state.profile.activeRole, "FUEL_LOGGED", enriched.vehicleReg, "${enriched.liters}L ref ${enriched.transactionRef}")
                _snackbarMessage.value = if (newSignal != null) {
                    "Fuel logged — Investigation Signal triggered (${newSignal.signalCategory})"
                } else {
                    "Fuel transaction ${enriched.transactionRef} saved."
                }
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to log fuel"
            }
        }
    }

    fun addTrip(trip: TripRecord) {
        val state = currentReady() ?: return
        if (!requirePermission(AxiomPermission.MANAGE_TRIPS_AND_GEOFENCES)) return
        viewModelScope.launch {
            val cid = state.activeCompany.companyId.ifBlank { state.activeCompany.id }
            val res = repository.addTrip(trip.copy(companyId = cid, demoData = false))
            if (res.isSuccess) {
                repository.logAudit(cid, state.profile.email, state.profile.activeRole, "TRIP_RECORDED", trip.routeName, "${trip.vehicleReg} (${trip.actualDistanceKm} km)")
                _snackbarMessage.value = "Trip ${trip.routeName} saved."
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to log trip"
            }
        }
    }

    fun addMaintenance(record: MaintenanceRecord) {
        val state = currentReady() ?: return
        if (!requirePermission(AxiomPermission.MANAGE_MAINTENANCE)) return
        viewModelScope.launch {
            val cid = state.activeCompany.companyId.ifBlank { state.activeCompany.id }
            val res = repository.addMaintenance(record.copy(companyId = cid, demoData = false))
            if (res.isSuccess) {
                repository.logAudit(cid, state.profile.email, state.profile.activeRole, "MAINTENANCE_CREATED", record.vehicleReg, record.title)
                _snackbarMessage.value = "Maintenance record added for ${record.vehicleReg}."
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to add maintenance"
            }
        }
    }

    fun updateMaintenanceStatus(recordId: String, status: MaintenanceStatus) {
        val state = currentReady() ?: return
        if (!requirePermission(AxiomPermission.MANAGE_MAINTENANCE)) return
        viewModelScope.launch {
            val cid = state.activeCompany.companyId.ifBlank { state.activeCompany.id }
            val res = repository.updateMaintenanceStatus(recordId, status)
            if (res.isSuccess) {
                repository.logAudit(cid, state.profile.email, state.profile.activeRole, "MAINTENANCE_STATUS", recordId, "Status -> ${status.label}")
                _snackbarMessage.value = "Maintenance marked ${status.label}"
            }
        }
    }

    fun addExpense(expense: ExpenseRecord) {
        val state = currentReady() ?: return
        if (!requirePermission(AxiomPermission.MANAGE_FINANCE_BUDGETS)) return
        viewModelScope.launch {
            val cid = state.activeCompany.companyId.ifBlank { state.activeCompany.id }
            val res = repository.addExpense(expense.copy(companyId = cid, demoData = false))
            if (res.isSuccess) {
                repository.logAudit(cid, state.profile.email, state.profile.activeRole, "EXPENSE_RECORDED", expense.category, "${expense.amount} (${expense.description})")
                _snackbarMessage.value = "Expense recorded."
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save expense"
            }
        }
    }

    fun addShipment(shipment: ShipmentRecord) {
        val state = currentReady() ?: return
        if (!requirePermission(AxiomPermission.MANAGE_SHIPMENTS)) return
        viewModelScope.launch {
            val cid = state.activeCompany.companyId.ifBlank { state.activeCompany.id }
            val res = repository.addShipment(shipment.copy(companyId = cid, demoData = false))
            if (res.isSuccess) {
                repository.logAudit(cid, state.profile.email, state.profile.activeRole, "SHIPMENT_REGISTERED", shipment.trackingCode, "${shipment.customerName} -> ${shipment.deliveryLocation}")
                _snackbarMessage.value = "Shipment ${shipment.trackingCode} created."
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to create shipment"
            }
        }
    }

    fun updateShipmentStatus(shipmentId: String, status: ShipmentStatus) {
        val state = currentReady() ?: return
        val perm = if (status == ShipmentStatus.DELIVERED) AxiomPermission.CONFIRM_DELIVERY else AxiomPermission.MANAGE_SHIPMENTS
        if (!requirePermission(perm)) return
        viewModelScope.launch {
            val cid = state.activeCompany.companyId.ifBlank { state.activeCompany.id }
            val res = repository.updateShipmentStatus(shipmentId, status)
            if (res.isSuccess) {
                repository.logAudit(cid, state.profile.email, state.profile.activeRole, "SHIPMENT_STATUS", shipmentId, "Status -> ${status.label}")
                _snackbarMessage.value = "Shipment updated to ${status.label}"
            }
        }
    }

    fun resolveRiskAlert(alertId: String, status: RiskResolutionStatus, notes: String) {
        val state = currentReady() ?: return
        if (!requirePermission(AxiomPermission.RESOLVE_RISK_ALERTS)) return
        viewModelScope.launch {
            val cid = state.activeCompany.companyId.ifBlank { state.activeCompany.id }
            val res = repository.resolveRiskAlert(alertId, status, notes)
            if (res.isSuccess) {
                repository.logAudit(cid, state.profile.email, state.profile.activeRole, "RISK_UPDATED", alertId, "Resolution -> ${status.label}")
                _snackbarMessage.value = "Risk signal updated to ${status.label}."
            }
        }
    }

    fun markNotificationRead(notificationId: String, read: Boolean) {
        viewModelScope.launch {
            repository.markNotificationRead(notificationId, read)
        }
    }

    fun commitParsedImport(parseResult: ImportParseResult) {
        val state = currentReady() ?: return
        if (!requirePermission(AxiomPermission.IMPORT_DATA)) return
        viewModelScope.launch {
            val cid = state.activeCompany.companyId.ifBlank { state.activeCompany.id }
            var count = 0
            parseResult.parsedVehicles.forEach {
                if (repository.addVehicle(it.copy(companyId = cid)).isSuccess) count++
            }
            parseResult.parsedDrivers.forEach {
                if (repository.addDriver(it.copy(companyId = cid)).isSuccess) count++
            }
            parseResult.parsedFuel.forEach {
                if (repository.addFuelTransaction(it.copy(companyId = cid)).isSuccess) count++
            }
            parseResult.parsedTrips.forEach {
                if (repository.addTrip(it.copy(companyId = cid)).isSuccess) count++
            }
            repository.logAudit(cid, state.profile.email, state.profile.activeRole, "CSV_DATA_IMPORTED", parseResult.targetType.label, "Imported $count validated record(s)")
            _snackbarMessage.value = "Imported $count validated ${parseResult.targetType.label} record(s)."
        }
    }

    fun askOttilia(question: String) {
        if (!requirePermission(AxiomPermission.QUERY_OTTILIA_AI)) return
        val trimmed = question.trim()
        if (trimmed.isEmpty() || _ottiliaBusy.value) return

        val state = currentReady()
        val telemetry = latestTelemetryContext.ifBlank {
            "No operational records loaded yet."
        }
        val cid = state?.activeCompany?.companyId?.ifBlank { state.activeCompany.id } ?: "comp_apex"

        val userMsg = OttiliaChatMessage(
            id = "u_${System.currentTimeMillis()}",
            fromUser = true,
            text = trimmed,
            timestampLabel = "Just now"
        )
        _ottiliaMessages.value = _ottiliaMessages.value + userMsg
        _ottiliaError.value = null
        _ottiliaStreaming.value = ""
        _ottiliaBusy.value = true

        viewModelScope.launch {
            try {
                var finalAnswer = ""
                ottiliaRepo.streamOperationalAnswer(trimmed, telemetry).collect { partial ->
                    finalAnswer = partial
                    _ottiliaStreaming.value = partial
                }
                if (finalAnswer.isNotBlank()) {
                    _ottiliaMessages.value = _ottiliaMessages.value + OttiliaChatMessage(
                        id = "a_${System.currentTimeMillis()}",
                        fromUser = false,
                        text = finalAnswer,
                        timestampLabel = "Ottilia AI"
                    )
                    state?.let {
                        repository.logAudit(cid, it.profile.email, it.profile.activeRole, "OTTILIA_QUERY", "Ottilia AI", trimmed.take(120))
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Ottilia timed out", e)
                _ottiliaError.value = "Ottilia timed out after 60 seconds. Please retry."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Ottilia failed", e)
                _ottiliaError.value = e.localizedMessage ?: "Ottilia AI error."
            } finally {
                _ottiliaStreaming.value = ""
                _ottiliaBusy.value = false
            }
        }
    }

    fun generateExecutiveBriefing() {
        if (!requirePermission(AxiomPermission.QUERY_OTTILIA_AI)) return
        val telemetry = latestTelemetryContext.ifBlank {
            "No operational records loaded yet."
        }
        _briefingState.value = BriefingUiState.Loading
        viewModelScope.launch {
            val result = ottiliaRepo.generateDecisionBriefing(telemetry)
            _briefingState.value = result.fold(
                onSuccess = { BriefingUiState.Success(it) },
                onFailure = { BriefingUiState.Error(it.localizedMessage ?: "Failed to generate briefing") }
            )
        }
    }

    private companion object {
        const val TAG = "AxiomViewModel"
    }
}
