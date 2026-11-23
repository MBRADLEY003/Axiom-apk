package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.domain.model.AuditLogRecord
import com.example.domain.model.AxiomRole
import com.example.domain.model.Company
import com.example.domain.model.Driver
import com.example.domain.model.DriverStatus
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.ExpenseRecord
import com.example.domain.model.FuelTransaction
import com.example.domain.model.FuelType
import com.example.domain.model.MaintenanceRecord
import com.example.domain.model.MaintenanceStatus
import com.example.domain.model.MaintenanceType
import com.example.domain.model.NotificationRecord
import com.example.domain.model.RiskAlertRecord
import com.example.domain.model.RiskResolutionStatus
import com.example.domain.model.RiskSeverity
import com.example.domain.model.ShipmentRecord
import com.example.domain.model.ShipmentStatus
import com.example.domain.model.TeamMember
import com.example.domain.model.TripRecord
import com.example.domain.model.TripStatus
import com.example.domain.model.UserProfile
import com.example.domain.model.Vehicle
import com.example.domain.model.VehicleCategory
import com.example.domain.model.VehicleStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AxiomRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    private inline fun <reified T> observeSubcollection(
        uid: String,
        collectionName: String,
    ): Flow<List<T>> = flow {
        if (uid.isBlank() || auth.currentUser?.uid != uid) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$uid/$collectionName"
        emitAll(
            userDoc(uid).collection(collectionName)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        T::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeProfile(uid: String): Flow<UserProfile?> = flow {
        if (uid.isBlank() || auth.currentUser?.uid != uid) {
            emit(null)
            return@flow
        }
        val path = "users/$uid"
        emitAll(
            userDoc(uid).snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeCompanies(uid: String) = observeSubcollection<Company>(uid, "companies")
    fun observeTeamMembers(uid: String) = observeSubcollection<TeamMember>(uid, "teamMembers")
    fun observeVehicles(uid: String) = observeSubcollection<Vehicle>(uid, "vehicles")
    fun observeDrivers(uid: String) = observeSubcollection<Driver>(uid, "drivers")
    fun observeFuelTransactions(uid: String) = observeSubcollection<FuelTransaction>(uid, "fuelTransactions")
    fun observeTrips(uid: String) = observeSubcollection<TripRecord>(uid, "trips")
    fun observeMaintenance(uid: String) = observeSubcollection<MaintenanceRecord>(uid, "maintenance")
    fun observeExpenses(uid: String) = observeSubcollection<ExpenseRecord>(uid, "expenses")
    fun observeShipments(uid: String) = observeSubcollection<ShipmentRecord>(uid, "shipments")
    fun observeRiskAlerts(uid: String) = observeSubcollection<RiskAlertRecord>(uid, "riskAlerts")
    fun observeNotifications(uid: String) = observeSubcollection<NotificationRecord>(uid, "notifications")
    fun observeAuditLogs(uid: String) = observeSubcollection<AuditLogRecord>(uid, "auditLogs")

    suspend fun ensureUserInitialized(
        uid: String,
        displayName: String,
        email: String,
    ): Result<Unit> = runCatchingFirestore(OperationType.WRITE, "users/$uid") {
        val currentUid = requireUserId()
        check(currentUid == uid)
        val profileSnap = userDoc(uid).get().await()
        if (!profileSnap.exists()) {
            val defaultCompanyId = "comp_apex"
            val profile = UserProfile(
                userId = uid,
                displayName = displayName.ifBlank { "AXIOM Executive" },
                email = email.ifBlank { "operator@axiom.io" },
                activeCompanyId = defaultCompanyId,
                activeRole = AxiomRole.SUPER_ADMIN.name,
                sessionTimeoutMinutes = 60L,
                emailAlertsEnabled = true,
                smsAlertsEnabled = false,
            )
            userDoc(uid).set(profile.toCreateMap()).await()

            val defaultCompany = Company(
                companyId = defaultCompanyId,
                name = "Apex Freight & Logistics",
                industry = "Long-Haul & Cold Chain",
                currency = "USD",
                demoData = false,
            )
            userDoc(uid).collection("companies").document(defaultCompanyId)
                .set(defaultCompany.toCreateMap()).await()

            val selfMember = TeamMember(
                companyId = defaultCompanyId,
                fullName = profile.displayName,
                email = profile.email,
                role = AxiomRole.SUPER_ADMIN.name,
                department = "Executive Command",
                activeStatus = "ACTIVE",
            )
            userDoc(uid).collection("teamMembers").document("member_owner")
                .set(selfMember.toCreateMap()).await()

            logAuditInternal(
                uid = uid,
                companyId = defaultCompanyId,
                email = profile.email,
                role = AxiomRole.SUPER_ADMIN.name,
                action = "WORKSPACE_INITIALIZED",
                target = defaultCompany.name,
                details = "Created AXIOM primary tenant and Super Administrator profile."
            )
        }
    }

    suspend fun updateProfileSettings(
        activeCompanyId: String? = null,
        activeRole: AxiomRole? = null,
        displayName: String? = null,
        pinHash: String? = null,
        sessionTimeoutMinutes: Long? = null,
        emailAlertsEnabled: Boolean? = null,
        smsAlertsEnabled: Boolean? = null,
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        return runCatchingFirestore(OperationType.UPDATE, "users/$uid") {
            requireUserId()
            val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            if (!activeCompanyId.isNullOrBlank()) updates["activeCompanyId"] = activeCompanyId.take(128)
            if (activeRole != null) updates["activeRole"] = activeRole.name
            if (!displayName.isNullOrBlank()) updates["displayName"] = displayName.take(100)
            if (pinHash != null) updates["pinHash"] = pinHash.take(128)
            if (sessionTimeoutMinutes != null) updates["sessionTimeoutMinutes"] = sessionTimeoutMinutes.coerceIn(5L, 480L)
            if (emailAlertsEnabled != null) updates["emailAlertsEnabled"] = emailAlertsEnabled
            if (smsAlertsEnabled != null) updates["smsAlertsEnabled"] = smsAlertsEnabled
            userDoc(uid).update(updates).await()
        }
    }

    suspend fun createCompany(company: Company): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val safeId = company.companyId.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(64).ifBlank {
            "comp_${System.currentTimeMillis()}"
        }
        return runCatchingFirestore(OperationType.CREATE, "users/$uid/companies/$safeId") {
            requireUserId()
            userDoc(uid).collection("companies").document(safeId)
                .set(company.copy(companyId = safeId).toCreateMap()).await()
            userDoc(uid).update(
                mapOf(
                    "activeCompanyId" to safeId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun addTeamMember(member: TeamMember): Result<Unit> =
        createDoc("teamMembers", member.toCreateMap())

    suspend fun updateTeamMemberRole(memberId: String, role: AxiomRole, activeStatus: String): Result<Unit> =
        updateDoc("teamMembers", memberId, mapOf("role" to role.name, "activeStatus" to activeStatus))

    suspend fun addVehicle(vehicle: Vehicle): Result<Unit> =
        createDoc("vehicles", vehicle.toCreateMap())

    suspend fun updateVehicle(vehicleId: String, updates: Map<String, Any>): Result<Unit> =
        updateDoc("vehicles", vehicleId, updates)

    suspend fun deleteVehicle(vehicleId: String): Result<Unit> =
        deleteDoc("vehicles", vehicleId)

    suspend fun addDriver(driver: Driver): Result<Unit> =
        createDoc("drivers", driver.toCreateMap())

    suspend fun addFuelTransaction(tx: FuelTransaction): Result<Unit> =
        createDoc("fuelTransactions", tx.toCreateMap())

    suspend fun addTrip(trip: TripRecord): Result<Unit> =
        createDoc("trips", trip.toCreateMap())

    suspend fun addMaintenance(record: MaintenanceRecord): Result<Unit> =
        createDoc("maintenance", record.toCreateMap())

    suspend fun updateMaintenanceStatus(recordId: String, status: MaintenanceStatus): Result<Unit> =
        updateDoc("maintenance", recordId, mapOf("status" to status.name))

    suspend fun addExpense(expense: ExpenseRecord): Result<Unit> =
        createDoc("expenses", expense.toCreateMap())

    suspend fun addShipment(shipment: ShipmentRecord): Result<Unit> =
        createDoc("shipments", shipment.toCreateMap())

    suspend fun updateShipmentStatus(shipmentId: String, status: ShipmentStatus): Result<Unit> =
        updateDoc("shipments", shipmentId, mapOf("status" to status.name))

    suspend fun addRiskAlert(alert: RiskAlertRecord): Result<Unit> =
        createDoc("riskAlerts", alert.toCreateMap())

    suspend fun resolveRiskAlert(
        alertId: String,
        status: RiskResolutionStatus,
        notes: String,
    ): Result<Unit> = updateDoc(
        "riskAlerts",
        alertId,
        mapOf(
            "resolutionStatus" to status.name,
            "resolutionNotes" to notes.take(500)
        )
    )

    suspend fun addNotification(notification: NotificationRecord): Result<Unit> =
        createDoc("notifications", notification.toCreateMap())

    suspend fun markNotificationRead(notificationId: String, read: Boolean): Result<Unit> =
        updateDoc("notifications", notificationId, mapOf("readStatus" to read))

    suspend fun logAudit(
        companyId: String,
        actorEmail: String,
        actorRole: String,
        action: String,
        targetEntity: String,
        details: String,
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        return runCatchingFirestore(OperationType.CREATE, "users/$uid/auditLogs") {
            requireUserId()
            logAuditInternal(uid, companyId, actorEmail, actorRole, action, targetEntity, details)
        }
    }

    private suspend fun logAuditInternal(
        uid: String,
        companyId: String,
        email: String,
        role: String,
        action: String,
        target: String,
        details: String,
    ) {
        val record = AuditLogRecord(
            companyId = companyId,
            actorEmail = email,
            actorRole = role,
            action = action,
            targetEntity = target,
            details = details,
        )
        userDoc(uid).collection("auditLogs").document().set(record.toCreateMap()).await()
    }

    private suspend fun createDoc(collection: String, payload: Map<String, Any>): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val docRef = userDoc(uid).collection(collection).document()
        return runCatchingFirestore(OperationType.CREATE, docRef.path) {
            requireUserId()
            docRef.set(payload).await()
        }
    }

    private suspend fun updateDoc(
        collection: String,
        docId: String,
        updates: Map<String, Any>,
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val docRef = userDoc(uid).collection(collection).document(docId)
        return runCatchingFirestore(OperationType.UPDATE, docRef.path) {
            requireUserId()
            require(docId.isNotBlank()) { "Document ID cannot be blank" }
            val merged = updates.toMutableMap()
            merged["updatedAt"] = FieldValue.serverTimestamp()
            docRef.update(merged).await()
        }
    }

    private suspend fun deleteDoc(collection: String, docId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val docRef = userDoc(uid).collection(collection).document(docId)
        return runCatchingFirestore(OperationType.DELETE, docRef.path) {
            requireUserId()
            require(docId.isNotBlank()) { "Document ID cannot be blank" }
            docRef.delete().await()
        }
    }

    /**
     * Loads realistic demonstration fleet, fuel (with intentional statistical anomaly signals),
     * GPS telemetry, maintenance, expenses, shipments, and risk alerts—strictly tagged demoData = true.
     */
    suspend fun loadDemonstrationFleetData(
        companyId: String,
        actorEmail: String,
        actorRole: String,
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        return runCatchingFirestore(OperationType.WRITE, "users/$uid/demoSeed") {
            requireUserId()
            val batch = db.batch()
            val now = System.currentTimeMillis()
            val dayMs = 86_400_000L

            val v1Ref = userDoc(uid).collection("vehicles").document("demo_v1")
            val v2Ref = userDoc(uid).collection("vehicles").document("demo_v2")
            val v3Ref = userDoc(uid).collection("vehicles").document("demo_v3")
            val v4Ref = userDoc(uid).collection("vehicles").document("demo_v4")
            val v5Ref = userDoc(uid).collection("vehicles").document("demo_v5")

            val d1Ref = userDoc(uid).collection("drivers").document("demo_d1")
            val d2Ref = userDoc(uid).collection("drivers").document("demo_d2")
            val d3Ref = userDoc(uid).collection("drivers").document("demo_d3")
            val d4Ref = userDoc(uid).collection("drivers").document("demo_d4")

            val vehicles = listOf(
                v1Ref to Vehicle(
                    companyId = companyId, registrationNumber = "AXM-104", vin = "YV2RT40A8LB104821",
                    makeModel = "Volvo FH16 750", category = VehicleCategory.HEAVY_TRUCK.name,
                    assignedDriverId = "demo_d1", assignedDriverName = "Marcus Vance",
                    fuelType = FuelType.DIESEL.name, tankCapacityLiters = 600.0, currentMileageKm = 128450.0,
                    status = VehicleStatus.ACTIVE.name, purchaseDate = "2023-04-12",
                    insuranceExpiry = "2027-04-12", licenseExpiry = "2026-12-20",
                    baselineKmPerLiter = 3.2, operationalCostTotal = 48200.0, demoData = true
                ),
                v2Ref to Vehicle(
                    companyId = companyId, registrationNumber = "AXM-209", vin = "YS2R6X20005520911",
                    makeModel = "Scania R500 V8", category = VehicleCategory.REFRIGERATED.name,
                    assignedDriverId = "demo_d2", assignedDriverName = "Elena Rostova",
                    fuelType = FuelType.DIESEL.name, tankCapacityLiters = 520.0, currentMileageKm = 94210.0,
                    status = VehicleStatus.ACTIVE.name, purchaseDate = "2024-01-19",
                    insuranceExpiry = "2027-01-19", licenseExpiry = "2027-02-28",
                    baselineKmPerLiter = 3.4, operationalCostTotal = 34900.0, demoData = true
                ),
                v3Ref to Vehicle(
                    companyId = companyId, registrationNumber = "AXM-317", vin = "WDB9634031L317402",
                    makeModel = "Mercedes Actros 2651", category = VehicleCategory.TANKER.name,
                    assignedDriverId = "demo_d3", assignedDriverName = "David Okafor",
                    fuelType = FuelType.DIESEL.name, tankCapacityLiters = 580.0, currentMileageKm = 164800.0,
                    status = VehicleStatus.MAINTENANCE.name, purchaseDate = "2022-09-05",
                    insuranceExpiry = "2026-12-10", licenseExpiry = "2027-01-15",
                    baselineKmPerLiter = 2.9, operationalCostTotal = 61450.0, demoData = true
                ),
                v4Ref to Vehicle(
                    companyId = companyId, registrationNumber = "AXM-412", vin = "1FTBW2XM8PKA41209",
                    makeModel = "Ford Transit 350 HD", category = VehicleCategory.LIGHT_VAN.name,
                    assignedDriverId = "demo_d4", assignedDriverName = "Liam Chen",
                    fuelType = FuelType.PETROL.name, tankCapacityLiters = 95.0, currentMileageKm = 41200.0,
                    status = VehicleStatus.ACTIVE.name, purchaseDate = "2025-02-14",
                    insuranceExpiry = "2027-08-14", licenseExpiry = "2027-08-14",
                    baselineKmPerLiter = 7.8, operationalCostTotal = 12300.0, demoData = true
                ),
                v5Ref to Vehicle(
                    companyId = companyId, registrationNumber = "AXM-508", vin = "XLRTE47MS0G508332",
                    makeModel = "DAF XF 530", category = VehicleCategory.FLATBED.name,
                    assignedDriverId = "", assignedDriverName = "",
                    fuelType = FuelType.DIESEL.name, tankCapacityLiters = 550.0, currentMileageKm = 73900.0,
                    status = VehicleStatus.INACTIVE.name, purchaseDate = "2024-06-22",
                    insuranceExpiry = "2027-06-22", licenseExpiry = "2027-07-01",
                    baselineKmPerLiter = 3.1, operationalCostTotal = 22800.0, demoData = true
                ),
            )
            vehicles.forEach { (ref, v) -> batch.set(ref, v.toCreateMap()) }

            val drivers = listOf(
                d1Ref to Driver(
                    companyId = companyId, driverCode = "DRV-101", fullName = "Marcus Vance",
                    licenseNumber = "CDL-A-88412", assignedVehicleId = "demo_v1", assignedVehicleReg = "AXM-104",
                    distanceTravelledKm = 18420.0, fuelUsedLiters = 6450.0, drivingHours = 264.0,
                    maintenanceIncidents = 1L, safetyEvents = 1L, onTimeDeliveryPct = 96.4,
                    status = DriverStatus.ON_DUTY.name, demoData = true
                ),
                d2Ref to Driver(
                    companyId = companyId, driverCode = "DRV-102", fullName = "Elena Rostova",
                    licenseNumber = "CDL-A-91204", assignedVehicleId = "demo_v2", assignedVehicleReg = "AXM-209",
                    distanceTravelledKm = 16890.0, fuelUsedLiters = 4810.0, drivingHours = 238.0,
                    maintenanceIncidents = 0L, safetyEvents = 0L, onTimeDeliveryPct = 99.1,
                    status = DriverStatus.ON_DUTY.name, demoData = true
                ),
                d3Ref to Driver(
                    companyId = companyId, driverCode = "DRV-103", fullName = "David Okafor",
                    licenseNumber = "CDL-A-77129", assignedVehicleId = "demo_v3", assignedVehicleReg = "AXM-317",
                    distanceTravelledKm = 14250.0, fuelUsedLiters = 5390.0, drivingHours = 219.0,
                    maintenanceIncidents = 3L, safetyEvents = 2L, onTimeDeliveryPct = 88.5,
                    status = DriverStatus.OFF_DUTY.name, demoData = true
                ),
                d4Ref to Driver(
                    companyId = companyId, driverCode = "DRV-104", fullName = "Liam Chen",
                    licenseNumber = "CDL-B-65421", assignedVehicleId = "demo_v4", assignedVehicleReg = "AXM-412",
                    distanceTravelledKm = 9480.0, fuelUsedLiters = 1230.0, drivingHours = 172.0,
                    maintenanceIncidents = 0L, safetyEvents = 0L, onTimeDeliveryPct = 97.8,
                    status = DriverStatus.ON_DUTY.name, demoData = true
                ),
            )
            drivers.forEach { (ref, d) -> batch.set(ref, d.toCreateMap()) }

            // Fuel Transactions with clear baseline + statistical Z-score/CUSUM & capacity anomalies
            val fuelSeeds = listOf(
                "demo_f1" to FuelTransaction(
                    companyId = companyId, vehicleId = "demo_v1", vehicleReg = "AXM-104",
                    driverId = "demo_d1", driverName = "Marcus Vance", routeName = "Northern Corridor A1",
                    liters = 310.0, costAmount = 511.5, odometerKm = 124200.0, distanceSinceLastKm = 992.0,
                    fuelType = FuelType.DIESEL.name, stationName = "Apex Hub Terminal",
                    transactionRef = "AXF-9001", txDateLabel = "2026-11-02", txTimestampMillis = now - 20 * dayMs, demoData = true
                ),
                "demo_f2" to FuelTransaction(
                    companyId = companyId, vehicleId = "demo_v1", vehicleReg = "AXM-104",
                    driverId = "demo_d1", driverName = "Marcus Vance", routeName = "Northern Corridor A1",
                    liters = 325.0, costAmount = 536.25, odometerKm = 125220.0, distanceSinceLastKm = 1020.0,
                    fuelType = FuelType.DIESEL.name, stationName = "Shell Fleetway M4",
                    transactionRef = "AXF-9014", txDateLabel = "2026-11-07", txTimestampMillis = now - 15 * dayMs, demoData = true
                ),
                "demo_f3" to FuelTransaction(
                    companyId = companyId, vehicleId = "demo_v1", vehicleReg = "AXM-104",
                    driverId = "demo_d1", driverName = "Marcus Vance", routeName = "Northern Corridor A1",
                    liters = 340.0, costAmount = 561.0, odometerKm = 126280.0, distanceSinceLastKm = 1060.0,
                    fuelType = FuelType.DIESEL.name, stationName = "TotalEnergies West",
                    transactionRef = "AXF-9029", txDateLabel = "2026-11-12", txTimestampMillis = now - 10 * dayMs, demoData = true
                ),
                "demo_f4" to FuelTransaction(
                    companyId = companyId, vehicleId = "demo_v1", vehicleReg = "AXM-104",
                    driverId = "demo_d1", driverName = "Marcus Vance", routeName = "Northern Corridor A1",
                    liters = 580.0, costAmount = 968.6, odometerKm = 127220.0, distanceSinceLastKm = 940.0,
                    fuelType = FuelType.DIESEL.name, stationName = "Highway 9 Truckstop",
                    transactionRef = "AXF-9045", txDateLabel = "2026-11-17", txTimestampMillis = now - 5 * dayMs, demoData = true
                ),
                "demo_f5" to FuelTransaction(
                    companyId = companyId, vehicleId = "demo_v1", vehicleReg = "AXM-104",
                    driverId = "demo_d1", driverName = "Marcus Vance", routeName = "Northern Corridor A1",
                    liters = 640.0, costAmount = 1068.8, odometerKm = 128450.0, distanceSinceLastKm = 1230.0,
                    fuelType = FuelType.DIESEL.name, stationName = "Highway 9 Truckstop",
                    transactionRef = "AXF-9061", txDateLabel = "2026-11-21", txTimestampMillis = now - 1 * dayMs, demoData = true
                ),
                "demo_f6" to FuelTransaction(
                    companyId = companyId, vehicleId = "demo_v2", vehicleReg = "AXM-209",
                    driverId = "demo_d2", driverName = "Elena Rostova", routeName = "Cold Chain East",
                    liters = 260.0, costAmount = 429.0, odometerKm = 93310.0, distanceSinceLastKm = 910.0,
                    fuelType = FuelType.DIESEL.name, stationName = "BP Logistics Yard",
                    transactionRef = "AXF-9068", txDateLabel = "2026-11-18", txTimestampMillis = now - 4 * dayMs, demoData = true
                ),
                "demo_f7" to FuelTransaction(
                    companyId = companyId, vehicleId = "demo_v2", vehicleReg = "AXM-209",
                    driverId = "demo_d2", driverName = "Elena Rostova", routeName = "Cold Chain East",
                    liters = 258.0, costAmount = 425.7, odometerKm = 94210.0, distanceSinceLastKm = 900.0,
                    fuelType = FuelType.DIESEL.name, stationName = "BP Logistics Yard",
                    transactionRef = "AXF-9075", txDateLabel = "2026-11-22", txTimestampMillis = now - 12 * 3_600_000L, demoData = true
                ),
                "demo_f8" to FuelTransaction(
                    companyId = companyId, vehicleId = "demo_v3", vehicleReg = "AXM-317",
                    driverId = "demo_d3", driverName = "David Okafor", routeName = "Refinery Spine R4",
                    liters = 490.0, costAmount = 808.5, odometerKm = 164800.0, distanceSinceLastKm = 820.0,
                    fuelType = FuelType.DIESEL.name, stationName = "Metro Fuel Depot",
                    transactionRef = "AXF-9080", txDateLabel = "2026-11-20", txTimestampMillis = now - 2 * dayMs, demoData = true
                ),
            )
            fuelSeeds.forEach { (id, f) ->
                batch.set(userDoc(uid).collection("fuelTransactions").document(id), f.toCreateMap())
            }

            // Trips
            val tripSeeds = listOf(
                "demo_t1" to TripRecord(
                    companyId = companyId, vehicleId = "demo_v1", vehicleReg = "AXM-104",
                    driverId = "demo_d1", driverName = "Marcus Vance", routeName = "Northern Corridor A1",
                    originName = "Chicago Hub", destinationName = "Minneapolis Depot",
                    plannedDistanceKm = 655.0, actualDistanceKm = 688.0, durationMinutes = 510L,
                    idleMinutes = 42L, stopCount = 4L, routeDeviationKm = 33.0,
                    geofenceBreached = true, simulatedTelemetry = true,
                    startLat = 41.8781, startLng = -87.6298, endLat = 44.9778, endLng = -93.2650,
                    status = TripStatus.DEVIATED.name, startedAtMillis = now - 2 * dayMs, demoData = true
                ),
                "demo_t2" to TripRecord(
                    companyId = companyId, vehicleId = "demo_v2", vehicleReg = "AXM-209",
                    driverId = "demo_d2", driverName = "Elena Rostova", routeName = "Cold Chain East",
                    originName = "Detroit Terminal", destinationName = "Cleveland Port",
                    plannedDistanceKm = 272.0, actualDistanceKm = 274.0, durationMinutes = 205L,
                    idleMinutes = 11L, stopCount = 1L, routeDeviationKm = 2.0,
                    geofenceBreached = false, simulatedTelemetry = true,
                    startLat = 42.3314, startLng = -83.0458, endLat = 41.4993, endLng = -81.6944,
                    status = TripStatus.COMPLETED.name, startedAtMillis = now - dayMs, demoData = true
                ),
                "demo_t3" to TripRecord(
                    companyId = companyId, vehicleId = "demo_v4", vehicleReg = "AXM-412",
                    driverId = "demo_d4", driverName = "Liam Chen", routeName = "Metro Express Loop",
                    originName = "Milwaukee Yard", destinationName = "Madison Hub",
                    plannedDistanceKm = 128.0, actualDistanceKm = 131.0, durationMinutes = 98L,
                    idleMinutes = 9L, stopCount = 3L, routeDeviationKm = 3.0,
                    geofenceBreached = false, simulatedTelemetry = true,
                    startLat = 43.0389, startLng = -87.9065, endLat = 43.0731, endLng = -89.4012,
                    status = TripStatus.IN_PROGRESS.name, startedAtMillis = now - 4 * 3_600_000L, demoData = true
                ),
            )
            tripSeeds.forEach { (id, t) ->
                batch.set(userDoc(uid).collection("trips").document(id), t.toCreateMap())
            }

            // Maintenance
            val maintSeeds = listOf(
                "demo_m1" to MaintenanceRecord(
                    companyId = companyId, vehicleId = "demo_v3", vehicleReg = "AXM-317",
                    maintenanceType = MaintenanceType.BREAKDOWN.name,
                    title = "High-Pressure Fuel Rail & Injector Overhaul",
                    description = "Unscheduled derate on Refinery Spine R4; injector seal wear suspected.",
                    serviceDate = "2026-11-20", mileageAtServiceKm = 164800.0, nextDueMileageKm = 180000.0,
                    costAmount = 4280.0, spareParts = "Bosch Common Rail Kit, Fuel Pressure Sensor",
                    downtimeHours = 36.0, status = MaintenanceStatus.IN_PROGRESS.name, demoData = true
                ),
                "demo_m2" to MaintenanceRecord(
                    companyId = companyId, vehicleId = "demo_v1", vehicleReg = "AXM-104",
                    maintenanceType = MaintenanceType.PREVENTIVE.name,
                    title = "130,000 km Major B-Service & Brake Calibration",
                    description = "Scheduled oil, DPF cleaning, and axle alignment.",
                    serviceDate = "2026-11-28", mileageAtServiceKm = 128450.0, nextDueMileageKm = 130000.0,
                    costAmount = 1450.0, spareParts = "Synthetic 10W-40 38L, DPF Gasket",
                    downtimeHours = 6.0, status = MaintenanceStatus.SCHEDULED.name, demoData = true
                ),
                "demo_m3" to MaintenanceRecord(
                    companyId = companyId, vehicleId = "demo_v2", vehicleReg = "AXM-209",
                    maintenanceType = MaintenanceType.PREVENTIVE.name,
                    title = "Reefer Compressor & Coolant Service",
                    description = "Completed cold-chain thermal certification.",
                    serviceDate = "2026-11-10", mileageAtServiceKm = 92000.0, nextDueMileageKm = 107000.0,
                    costAmount = 920.0, spareParts = "Refrigerant R452A, Belt Tensioner",
                    downtimeHours = 4.0, status = MaintenanceStatus.COMPLETED.name, demoData = true
                ),
            )
            maintSeeds.forEach { (id, m) ->
                batch.set(userDoc(uid).collection("maintenance").document(id), m.toCreateMap())
            }

            // Expenses
            val expSeeds = listOf(
                "demo_e1" to ExpenseRecord(
                    companyId = companyId, vehicleId = "demo_v1", vehicleReg = "AXM-104",
                    driverId = "demo_d1", category = ExpenseCategory.FUEL.name,
                    amount = 3646.15, budgetAllocated = 3100.0,
                    description = "November diesel card settlements", monthKey = "2026-11", demoData = true
                ),
                "demo_e2" to ExpenseRecord(
                    companyId = companyId, vehicleId = "demo_v3", vehicleReg = "AXM-317",
                    driverId = "demo_d3", category = ExpenseCategory.MAINTENANCE.name,
                    amount = 4280.0, budgetAllocated = 2800.0,
                    description = "Injector & rail corrective overhaul", monthKey = "2026-11", demoData = true
                ),
                "demo_e3" to ExpenseRecord(
                    companyId = companyId, vehicleId = "", vehicleReg = "FLEET",
                    driverId = "", category = ExpenseCategory.DRIVER.name,
                    amount = 14800.0, budgetAllocated = 15500.0,
                    description = "Driver payroll, per-diem & compliance", monthKey = "2026-11", demoData = true
                ),
                "demo_e4" to ExpenseRecord(
                    companyId = companyId, vehicleId = "", vehicleReg = "FLEET",
                    driverId = "", category = ExpenseCategory.INSURANCE.name,
                    amount = 4200.0, budgetAllocated = 4200.0,
                    description = "Commercial cargo & fleet liability", monthKey = "2026-11", demoData = true
                ),
                "demo_e5" to ExpenseRecord(
                    companyId = companyId, vehicleId = "", vehicleReg = "FLEET",
                    driverId = "", category = ExpenseCategory.LICENSING.name,
                    amount = 1150.0, budgetAllocated = 1200.0,
                    description = "IFTA & heavy haulage corridor permits", monthKey = "2026-11", demoData = true
                ),
            )
            expSeeds.forEach { (id, e) ->
                batch.set(userDoc(uid).collection("expenses").document(id), e.toCreateMap())
            }

            // Shipments
            val shipSeeds = listOf(
                "demo_s1" to ShipmentRecord(
                    companyId = companyId, trackingCode = "SHP-8821", customerName = "Nordic Pharma Cold",
                    customerContact = "dispatch@nordicpharma.com", cargoDescription = "Temperature-controlled biologics (2-8°C)",
                    weightKg = 14200.0, capacityKg = 18000.0, pickupLocation = "Detroit Terminal",
                    deliveryLocation = "Cleveland Port", vehicleId = "demo_v2", vehicleReg = "AXM-209",
                    driverId = "demo_d2", driverName = "Elena Rostova", revenueAmount = 4850.0,
                    tripCostEstimate = 1640.0, status = ShipmentStatus.DELIVERED.name,
                    scheduledDate = "2026-11-22", demoData = true
                ),
                "demo_s2" to ShipmentRecord(
                    companyId = companyId, trackingCode = "SHP-8834", customerName = "Great Lakes Steelworks",
                    customerContact = "logistics@glsteel.com", cargoDescription = "Structural rolled steel coils",
                    weightKg = 21500.0, capacityKg = 24000.0, pickupLocation = "Chicago Hub",
                    deliveryLocation = "Minneapolis Depot", vehicleId = "demo_v1", vehicleReg = "AXM-104",
                    driverId = "demo_d1", driverName = "Marcus Vance", revenueAmount = 6100.0,
                    tripCostEstimate = 2420.0, status = ShipmentStatus.IN_TRANSIT.name,
                    scheduledDate = "2026-11-23", demoData = true
                ),
                "demo_s3" to ShipmentRecord(
                    companyId = companyId, trackingCode = "SHP-8840", customerName = "Midwest Medical Supply",
                    customerContact = "ops@midwestmed.org", cargoDescription = "Sterile surgical consumables",
                    weightKg = 2450.0, capacityKg = 3500.0, pickupLocation = "Milwaukee Yard",
                    deliveryLocation = "Madison Hub", vehicleId = "demo_v4", vehicleReg = "AXM-412",
                    driverId = "demo_d4", driverName = "Liam Chen", revenueAmount = 1480.0,
                    tripCostEstimate = 410.0, status = ShipmentStatus.DISPATCHED.name,
                    scheduledDate = "2026-11-23", demoData = true
                ),
            )
            shipSeeds.forEach { (id, s) ->
                batch.set(userDoc(uid).collection("shipments").document(id), s.toCreateMap())
            }

            // Risk Alerts
            val riskSeeds = listOf(
                "demo_r1" to RiskAlertRecord(
                    companyId = companyId, category = "Fuel Investigation Signal",
                    severity = RiskSeverity.CRITICAL.name, entityType = "VEHICLE",
                    entityId = "demo_v1", entityLabel = "AXM-104 (Marcus Vance)",
                    supportingData = "Fill Ref AXF-9061 logged 640.0 L vs 600.0 L tank capacity (+40 L) after prior Z=-2.4 efficiency drop.",
                    detectionMethod = "Tank Capacity Validation + Z-Score & Tabular CUSUM",
                    historicalComparison = "Baseline 3.20 km/L vs recent 1.62–1.92 km/L (-40% efficiency).",
                    recommendedInvestigation = "Audit Highway 9 Truckstop POS receipt, verify odometer reading, and inspect fuel tank/return lines.",
                    confidenceScore = 0.94, resolutionStatus = RiskResolutionStatus.OPEN.name,
                    resolutionNotes = "", demoData = true
                ),
                "demo_r2" to RiskAlertRecord(
                    companyId = companyId, category = "Route Geofence Deviation",
                    severity = RiskSeverity.HIGH.name, entityType = "ROUTE",
                    entityId = "demo_v1", entityLabel = "AXM-104 • Northern Corridor A1",
                    supportingData = "33.0 km off-corridor deviation and 42 min unscheduled idle near corridor exit 14.",
                    detectionMethod = "Geofence Corridor Boundary & Distance Variance Check",
                    historicalComparison = "Typical corridor variance < 5 km; current trip +33 km.",
                    recommendedInvestigation = "Review driver dispatch notes for road closure or unapproved detour.",
                    confidenceScore = 0.88, resolutionStatus = RiskResolutionStatus.INVESTIGATING.name,
                    resolutionNotes = "Awaiting corridor dispatch log confirmation.", demoData = true
                ),
                "demo_r3" to RiskAlertRecord(
                    companyId = companyId, category = "Maintenance & Downtime Risk",
                    severity = RiskSeverity.HIGH.name, entityType = "VEHICLE",
                    entityId = "demo_v3", entityLabel = "AXM-317 (Mercedes Actros)",
                    supportingData = "36h downtime on fuel rail overhaul; fuel efficiency dropped to 1.67 km/L prior to derate.",
                    detectionMethod = "Mileage Interval & Breakdown Frequency Model",
                    historicalComparison = "2nd corrective fuel-system event in 90 days.",
                    recommendedInvestigation = "Evaluate fuel filtration quality at Metro Fuel Depot and inspect tank sediment.",
                    confidenceScore = 0.86, resolutionStatus = RiskResolutionStatus.OPEN.name,
                    resolutionNotes = "", demoData = true
                ),
            )
            riskSeeds.forEach { (id, r) ->
                batch.set(userDoc(uid).collection("riskAlerts").document(id), r.toCreateMap())
            }

            // Notifications
            val notifSeeds = listOf(
                "demo_n1" to NotificationRecord(
                    companyId = companyId,
                    title = "Fuel Investigation Signal: AXM-104",
                    message = "640.0 L transaction AXF-9061 exceeded 600 L tank rating. Review evidence before action.",
                    category = "Fuel Intelligence", severity = RiskSeverity.CRITICAL.name,
                    readStatus = false, channel = "IN_APP"
                ),
                "demo_n2" to NotificationRecord(
                    companyId = companyId,
                    title = "Upcoming Service Due: AXM-104",
                    message = "1,550 km remaining until 130,000 km Major B-Service.",
                    category = "Maintenance", severity = RiskSeverity.MEDIUM.name,
                    readStatus = false, channel = "EMAIL_READY"
                ),
                "demo_n3" to NotificationRecord(
                    companyId = companyId,
                    title = "Insurance Renewal Window: AXM-317",
                    message = "Policy expires 2026-12-10. Schedule broker renewal.",
                    category = "Compliance", severity = RiskSeverity.HIGH.name,
                    readStatus = false, channel = "IN_APP"
                ),
            )
            notifSeeds.forEach { (id, n) ->
                batch.set(userDoc(uid).collection("notifications").document(id), n.toCreateMap())
            }

            batch.commit().await()
            logAuditInternal(
                uid = uid,
                companyId = companyId,
                email = actorEmail,
                role = actorRole,
                action = "DEMO_FLEET_LOADED",
                target = companyId,
                details = "Loaded labelled demonstration fleet dataset (5 vehicles, 4 drivers, 8 fuel logs, 3 trips, 3 maintenance, 5 expenses, 3 shipments)."
            )
        }
    }

    private suspend inline fun runCatchingFirestore(
        operationType: OperationType,
        path: String,
        crossinline block: suspend () -> Unit,
    ): Result<Unit> {
        return try {
            block()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AxiomRepo", "Firestore failure at $path", e)
            handleFirestoreError(e, operationType, path)
            Result.failure(e)
        }
    }
}
