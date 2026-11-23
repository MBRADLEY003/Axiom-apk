package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.FuelType
import com.example.domain.model.Vehicle
import com.example.domain.model.VehicleCategory
import com.example.domain.model.VehicleStatus
import com.example.domain.rbac.AxiomPermission
import com.example.domain.rbac.RbacPermissions
import com.example.ui.components.AxiomBarChart
import com.example.ui.components.AxiomEmptyState
import com.example.ui.components.AxiomKpiTile
import com.example.ui.components.AxiomLineForecastChart
import com.example.ui.components.AxiomPillBadge
import com.example.ui.components.AxiomSectionCard
import com.example.ui.components.DataSourceBanner
import com.example.ui.components.severityColors
import com.example.ui.theme.AxiomAmber
import com.example.ui.theme.AxiomAmberBg
import com.example.ui.theme.AxiomCharcoal
import com.example.ui.theme.AxiomCrimson
import com.example.ui.theme.AxiomCrimsonBg
import com.example.ui.theme.AxiomEmerald
import com.example.ui.theme.AxiomEmeraldBg
import com.example.ui.theme.AxiomGold
import com.example.ui.theme.AxiomGoldSubtleContainer
import com.example.ui.theme.AxiomIvory
import com.example.ui.theme.AxiomObsidian
import com.example.ui.theme.AxiomSilver
import com.example.ui.theme.AxiomSlate
import com.example.ui.viewmodel.AxiomWorkspaceUiState
import com.example.ui.viewmodel.DateRangeFilter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OverviewScreen(
    state: AxiomWorkspaceUiState.Ready,
    onSetDateFilter: (DateRangeFilter) -> Unit,
    onSetVehicleFilter: (String?) -> Unit,
    onLoadDemoData: () -> Unit,
    onNavigateToOttilia: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToRisk: () -> Unit,
) {
    val summary = state.summary
    val cur = state.activeCompany.currency

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            DataSourceBanner(hasDemoData = state.hasDemoRecords)
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AxiomCharcoal)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_command_hero),
                    contentDescription = "AXIOM Command Telemetry Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .alpha(0.42f),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(AxiomObsidian.copy(alpha = 0.9f), Color.Transparent)
                            )
                        )
                )
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = state.activeCompany.name.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = AxiomGold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Executive Command Centre",
                        style = MaterialTheme.typography.headlineMedium,
                        color = AxiomIvory,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${state.profile.roleEnum().displayName} • DATA → ANALYSIS → RISK → INSIGHT → ACTION",
                        style = MaterialTheme.typography.bodySmall,
                        color = AxiomSilver,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onNavigateToOttilia,
                            colors = ButtonDefaults.buttonColors(containerColor = AxiomGold, contentColor = AxiomObsidian),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("overview_ottilia_btn"),
                        ) {
                            Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ask Ottilia AI")
                        }
                        OutlinedButton(
                            onClick = onNavigateToReports,
                            border = BorderStroke(1.dp, AxiomGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("overview_reports_btn"),
                        ) {
                            Icon(Icons.Filled.Download, null, tint = AxiomGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reports", color = AxiomGold)
                        }
                    }
                }
            }
        }

        if (state.vehicles.isEmpty()) {
            item {
                AxiomSectionCard(
                    title = "Empty Fleet Tenant",
                    subtitle = "No operational vehicles are registered in ${state.activeCompany.name} yet.",
                    badge = "ONBOARDING",
                ) {
                    Text(
                        text = "Add vehicles manually in Fleet, import CSV records in Settings, or load the clearly-labelled Demonstration Fleet dataset to explore Z-score, CUSUM, GPS telemetry, and Ottilia AI.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AxiomSilver,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onLoadDemoData,
                        colors = ButtonDefaults.buttonColors(containerColor = AxiomGold, contentColor = AxiomObsidian),
                        modifier = Modifier.testTag("load_demo_fleet_btn"),
                    ) {
                        Text("Load Demonstration Fleet Data")
                    }
                }
            }
        }

        // Date & Vehicle Filters
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DateRangeFilter.entries) { df ->
                        FilterChip(
                            selected = state.dateFilter == df,
                            onClick = { onSetDateFilter(df) },
                            label = { Text(df.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AxiomGoldSubtleContainer,
                                selectedLabelColor = AxiomGold,
                            ),
                        )
                    }
                }
                if (state.vehicles.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = state.selectedVehicleFilterId == null,
                                onClick = { onSetVehicleFilter(null) },
                                label = { Text("All Vehicles") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AxiomGoldSubtleContainer,
                                    selectedLabelColor = AxiomGold,
                                ),
                            )
                        }
                        items(state.vehicles) { v ->
                            FilterChip(
                                selected = state.selectedVehicleFilterId == v.id,
                                onClick = { onSetVehicleFilter(v.id) },
                                label = { Text(v.registrationNumber) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AxiomGoldSubtleContainer,
                                    selectedLabelColor = AxiomGold,
                                ),
                            )
                        }
                    }
                }
            }
        }

        // Executive KPI Grid
        item {
            FlowRow(
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AxiomKpiTile(
                    label = "Total Fleet",
                    value = "${summary.totalFleet}",
                    deltaOrNote = "${summary.activeVehicles} Active • ${summary.maintenanceVehicles} Maint • ${summary.inactiveVehicles} Idle",
                    icon = Icons.Outlined.DirectionsCar,
                    modifier = Modifier.weight(1f),
                )
                AxiomKpiTile(
                    label = "Utilisation",
                    value = "${"%.0f".format(Locale.US, summary.fleetUtilisationPct)}%",
                    deltaOrNote = "${summary.activeShipmentsCount} active shipments",
                    icon = Icons.Outlined.Speed,
                    accentColor = AxiomEmerald,
                    modifier = Modifier.weight(1f),
                )
                AxiomKpiTile(
                    label = "Total Distance",
                    value = "${"%.0f".format(Locale.US, summary.totalDistanceKm)} km",
                    deltaOrNote = "${"%.2f".format(Locale.US, summary.avgKmPerLiter)} km/L avg",
                    icon = Icons.Outlined.Route,
                    modifier = Modifier.weight(1f),
                )
                AxiomKpiTile(
                    label = "Fuel Consumed",
                    value = "${"%.0f".format(Locale.US, summary.totalFuelLiters)} L",
                    deltaOrNote = "$cur ${"%.0f".format(Locale.US, summary.totalFuelSpend)} fuel spend",
                    icon = Icons.Filled.LocalGasStation,
                    modifier = Modifier.weight(1f),
                )
                AxiomKpiTile(
                    label = "Total Opex",
                    value = "$cur ${"%.0f".format(Locale.US, summary.totalOpex)}",
                    deltaOrNote = "$cur ${"%.2f".format(Locale.US, summary.costPerKm)} / km",
                    icon = Icons.Outlined.AccountBalanceWallet,
                    modifier = Modifier.weight(1f),
                )
                AxiomKpiTile(
                    label = "Alerts & Maint",
                    value = "${summary.openCriticalAlertsCount} Risks",
                    deltaOrNote = "${summary.outstandingMaintenanceCount} outstanding maint",
                    icon = Icons.Outlined.WarningAmber,
                    accentColor = if (summary.openCriticalAlertsCount > 0) AxiomCrimson else AxiomEmerald,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Fuel Expenditure & OLS Forecast Chart
        item {
            AxiomSectionCard(
                title = "Fuel Expenditure Trend & OLS Forecast",
                subtitle = state.fuelSpendForecast.modelDescription,
                badge = "STATISTICAL",
            ) {
                AxiomLineForecastChart(
                    historical = state.fuelSpendForecast.historicalValues,
                    forecast = state.fuelSpendForecast.forecastValues,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Solid gold: observed fuel spend • Dashed emerald: OLS regression projection",
                    style = MaterialTheme.typography.labelSmall,
                    color = AxiomSilver,
                )
            }
        }

        // Driver Performance Comparison Chart
        item {
            AxiomSectionCard(
                title = "Driver Efficiency & Composite Index",
                subtitle = "Objective comparison across fuel km/L, on-time rate, and safety events",
                badge = "BENCHMARK",
            ) {
                if (state.driverScores.isEmpty()) {
                    Text("No drivers available for comparison.", style = MaterialTheme.typography.bodySmall, color = AxiomSilver)
                } else {
                    AxiomBarChart(
                        labels = state.driverScores.map { it.driverName.substringBefore(" ") },
                        values = state.driverScores.map { it.compositeIndex },
                    )
                }
            }
        }

        // Critical Operational Alerts
        item {
            AxiomSectionCard(
                title = "Critical Operational Alerts",
                subtitle = "Latest high-priority investigation signals",
                badge = "${state.fuelSignals.size} SIGNALS",
                onClick = onNavigateToRisk,
            ) {
                if (state.riskAlerts.isEmpty() && state.fuelSignals.isEmpty()) {
                    Text("No active risk alerts detected.", style = MaterialTheme.typography.bodySmall, color = AxiomEmerald)
                } else {
                    state.riskAlerts.take(3).forEach { alert ->
                        val (fg, bg) = severityColors(alert.severityEnum())
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(alert.entityLabel, style = MaterialTheme.typography.titleSmall, color = AxiomIvory)
                                Text(alert.supportingData, style = MaterialTheme.typography.bodySmall, color = AxiomSilver, maxLines = 2)
                            }
                            Spacer(Modifier.width(8.dp))
                            AxiomPillBadge(alert.severity, fg, bg)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun FleetScreen(
    state: AxiomWorkspaceUiState.Ready,
    onAddVehicle: (Vehicle) -> Unit,
    onUpdateVehicle: (String, String, Map<String, Any>) -> Unit,
    onDeleteVehicle: (Vehicle) -> Unit,
    onInspectVehicle: (String) -> Unit,
) {
    var search by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<VehicleStatus?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingVehicle by remember { mutableStateOf<Vehicle?>(null) }

    val canManage = RbacPermissions.hasPermission(state.profile.roleEnum(), AxiomPermission.MANAGE_FLEET_VEHICLES)
    val canDelete = RbacPermissions.hasPermission(state.profile.roleEnum(), AxiomPermission.DELETE_VEHICLES)

    val vehicles = state.vehicles.filter { v ->
        val matchesSearch = search.isBlank() ||
            v.registrationNumber.contains(search, true) ||
            v.makeModel.contains(search, true) ||
            v.vin.contains(search, true) ||
            v.assignedDriverName.contains(search, true)
        val matchesStatus = statusFilter == null || v.statusEnum() == statusFilter
        matchesSearch && matchesStatus
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Search registration, VIN, model, or driver") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fleet_search_input"),
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = statusFilter == null,
                            onClick = { statusFilter = null },
                            label = { Text("All (${state.vehicles.size})") },
                        )
                    }
                    items(VehicleStatus.entries) { st ->
                        FilterChip(
                            selected = statusFilter == st,
                            onClick = { statusFilter = st },
                            label = { Text("${st.label} (${state.vehicles.count { it.statusEnum() == st }})") },
                        )
                    }
                }
            }

            if (vehicles.isEmpty()) {
                item {
                    AxiomEmptyState(
                        title = "No matching vehicles",
                        description = "Register a new fleet asset or adjust your search filter.",
                        icon = Icons.Filled.DirectionsCar,
                    )
                }
            } else {
                items(vehicles, key = { it.id.ifBlank { it.registrationNumber } }) { v ->
                    val (sFg, sBg) = when (v.statusEnum()) {
                        VehicleStatus.ACTIVE -> AxiomEmerald to AxiomEmeraldBg
                        VehicleStatus.MAINTENANCE -> AxiomAmber to AxiomAmberBg
                        VehicleStatus.INACTIVE -> AxiomSilver to AxiomSlate
                    }
                    AxiomSectionCard(
                        title = "${v.registrationNumber} • ${v.makeModel}",
                        subtitle = "VIN: ${v.vin} • ${v.categoryEnum().label} • ${v.fuelTypeEnum().label}",
                        badge = v.statusEnum().label.uppercase(),
                        onClick = { onInspectVehicle(v.id) },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text("Driver: ${v.assignedDriverName.ifBlank { "Unassigned" }}", style = MaterialTheme.typography.bodySmall, color = AxiomIvory)
                                Text("Mileage: ${"%.0f".format(Locale.US, v.currentMileageKm)} km • Tank: ${v.tankCapacityLiters.toInt()} L", style = MaterialTheme.typography.bodySmall, color = AxiomSilver)
                                Text("Ins Exp: ${v.insuranceExpiry.ifBlank { "N/A" }} • Lic Exp: ${v.licenseExpiry.ifBlank { "N/A" }}", style = MaterialTheme.typography.labelSmall, color = AxiomSilver)
                            }
                            AxiomPillBadge(v.statusEnum().label, sFg, sBg)
                        }
                        if (canManage || canDelete) {
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onInspectVehicle(v.id) }) {
                                    Text("Inspect")
                                }
                                if (canManage) {
                                    OutlinedButton(onClick = { editingVehicle = v }) {
                                        Icon(Icons.Filled.Edit, null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Edit")
                                    }
                                }
                                if (canDelete) {
                                    IconButton(onClick = { onDeleteVehicle(v) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete ${v.registrationNumber}", tint = AxiomCrimson)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }

        if (canManage) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AxiomGold,
                contentColor = AxiomObsidian,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("add_vehicle_fab"),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Vehicle")
            }
        }
    }

    if (showAddDialog) {
        VehicleFormDialog(
            initial = null,
            onDismiss = { showAddDialog = false },
            onSave = { v ->
                onAddVehicle(v)
                showAddDialog = false
            }
        )
    }

    editingVehicle?.let { current ->
        VehicleFormDialog(
            initial = current,
            onDismiss = { editingVehicle = null },
            onSave = { updated ->
                onUpdateVehicle(
                    current.id,
                    updated.registrationNumber,
                    mapOf(
                        "registrationNumber" to updated.registrationNumber,
                        "makeModel" to updated.makeModel,
                        "assignedDriverName" to updated.assignedDriverName,
                        "currentMileageKm" to updated.currentMileageKm,
                        "tankCapacityLiters" to updated.tankCapacityLiters,
                        "status" to updated.status,
                        "baselineKmPerLiter" to updated.baselineKmPerLiter,
                    )
                )
                editingVehicle = null
            }
        )
    }
}

@Composable
fun VehicleDetailScreen(
    vehicle: Vehicle?,
    state: AxiomWorkspaceUiState.Ready,
    onBack: () -> Unit,
) {
    if (vehicle == null) {
        AxiomEmptyState("Vehicle Not Found", "This vehicle record may have been removed.")
        return
    }
    val cur = state.activeCompany.currency
    val vFuel = state.fuelTransactions.filter { it.vehicleId == vehicle.id }
    val vMaint = state.maintenance.filter { it.vehicleId == vehicle.id }
    val vTrips = state.trips.filter { it.vehicleId == vehicle.id }
    val risk = state.vehicleRisks.firstOrNull { it.vehicleId == vehicle.id }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AxiomGold)
                }
                Text("${vehicle.registrationNumber} • ${vehicle.makeModel}", style = MaterialTheme.typography.headlineSmall, color = AxiomIvory)
            }
        }
        item {
            AxiomSectionCard(
                title = "Asset Specifications",
                subtitle = "VIN: ${vehicle.vin}",
                badge = vehicle.statusEnum().label,
            ) {
                Text("Category: ${vehicle.categoryEnum().label} • Fuel: ${vehicle.fuelTypeEnum().label}", style = MaterialTheme.typography.bodyMedium, color = AxiomIvory)
                Text("Assigned Driver: ${vehicle.assignedDriverName.ifBlank { "Unassigned" }}", style = MaterialTheme.typography.bodyMedium, color = AxiomSilver)
                Text("Tank Capacity: ${vehicle.tankCapacityLiters} L • Current Mileage: ${"%.0f".format(Locale.US, vehicle.currentMileageKm)} km", style = MaterialTheme.typography.bodyMedium, color = AxiomSilver)
                Text("Baseline Efficiency: ${vehicle.baselineKmPerLiter} km/L • Op Cost: $cur ${"%.2f".format(Locale.US, vehicle.operationalCostTotal)}", style = MaterialTheme.typography.bodyMedium, color = AxiomGold)
                Text("Purchase: ${vehicle.purchaseDate.ifBlank { "N/A" }} • Insurance Exp: ${vehicle.insuranceExpiry.ifBlank { "N/A" }} • License Exp: ${vehicle.licenseExpiry.ifBlank { "N/A" }}", style = MaterialTheme.typography.labelSmall, color = AxiomSilver)
            }
        }
        if (risk != null) {
            item {
                val (fg, bg) = severityColors(risk.riskTier)
                AxiomSectionCard(title = "Operational Risk Assessment", badge = "SCORE ${risk.riskScore}/100") {
                    AxiomPillBadge(risk.riskTier.label, fg, bg)
                    Spacer(Modifier.height(6.dp))
                    Text("Observed: ${risk.observedFacts}", style = MaterialTheme.typography.bodySmall, color = AxiomIvory)
                    Text("Action: ${risk.recommendedAction}", style = MaterialTheme.typography.bodySmall, color = AxiomGold)
                }
            }
        }
        item {
            AxiomSectionCard(title = "Fuel History (${vFuel.size})", badge = "${"%.1f".format(Locale.US, vFuel.sumOf { it.liters })} L") {
                if (vFuel.isEmpty()) Text("No fuel records.", style = MaterialTheme.typography.bodySmall, color = AxiomSilver)
                vFuel.take(8).forEach { f ->
                    Text("${f.txDateLabel} • ${f.liters} L ($cur ${f.costAmount}) • ${"%.2f".format(Locale.US, f.kmPerLiter)} km/L • ${f.stationName}", style = MaterialTheme.typography.bodySmall, color = AxiomIvory)
                }
            }
        }
        item {
            AxiomSectionCard(title = "Maintenance History (${vMaint.size})") {
                if (vMaint.isEmpty()) Text("No maintenance records.", style = MaterialTheme.typography.bodySmall, color = AxiomSilver)
                vMaint.forEach { m ->
                    Text("${m.serviceDate} • ${m.title} (${m.status}) • $cur ${m.costAmount}", style = MaterialTheme.typography.bodySmall, color = AxiomIvory)
                }
            }
        }
        item {
            AxiomSectionCard(title = "Recent GPS Trips (${vTrips.size})") {
                if (vTrips.isEmpty()) Text("No trips logged.", style = MaterialTheme.typography.bodySmall, color = AxiomSilver)
                vTrips.forEach { t ->
                    Text("${t.routeName} • ${t.actualDistanceKm} km (dev ${t.routeDeviationKm} km) • ${t.status}", style = MaterialTheme.typography.bodySmall, color = AxiomIvory)
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun VehicleFormDialog(
    initial: Vehicle?,
    onDismiss: () -> Unit,
    onSave: (Vehicle) -> Unit,
) {
    var reg by remember { mutableStateOf(initial?.registrationNumber ?: "") }
    var vin by remember { mutableStateOf(initial?.vin ?: "") }
    var makeModel by remember { mutableStateOf(initial?.makeModel ?: "") }
    var driverName by remember { mutableStateOf(initial?.assignedDriverName ?: "") }
    var tank by remember { mutableStateOf(initial?.tankCapacityLiters?.toString() ?: "500") }
    var mileage by remember { mutableStateOf(initial?.currentMileageKm?.toString() ?: "0") }
    var baseline by remember { mutableStateOf(initial?.baselineKmPerLiter?.toString() ?: "3.2") }
    var status by remember { mutableStateOf(initial?.statusEnum() ?: VehicleStatus.ACTIVE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Register Fleet Vehicle" else "Edit ${initial.registrationNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(reg, { reg = it }, label = { Text("Registration Number") }, singleLine = true)
                OutlinedTextField(vin, { vin = it }, label = { Text("VIN (min 5 chars)") }, singleLine = true)
                OutlinedTextField(makeModel, { makeModel = it }, label = { Text("Make & Model") }, singleLine = true)
                OutlinedTextField(driverName, { driverName = it }, label = { Text("Assigned Driver") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(tank, { tank = it }, label = { Text("Tank (L)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(mileage, { mileage = it }, label = { Text("Mileage (km)") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(baseline, { baseline = it }, label = { Text("Baseline km/L") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VehicleStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st.label) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reg.isNotBlank() && vin.length >= 5 && makeModel.isNotBlank()) {
                        onSave(
                            (initial ?: Vehicle()).copy(
                                registrationNumber = reg.trim().uppercase(),
                                vin = vin.trim().uppercase(),
                                makeModel = makeModel.trim(),
                                assignedDriverName = driverName.trim(),
                                category = initial?.category ?: VehicleCategory.HEAVY_TRUCK.name,
                                fuelType = initial?.fuelType ?: FuelType.DIESEL.name,
                                tankCapacityLiters = tank.toDoubleOrNull() ?: 500.0,
                                currentMileageKm = mileage.toDoubleOrNull() ?: 0.0,
                                baselineKmPerLiter = baseline.toDoubleOrNull() ?: 3.2,
                                status = status.name,
                                purchaseDate = initial?.purchaseDate?.ifBlank { "2026-11-23" } ?: "2026-11-23",
                                insuranceExpiry = initial?.insuranceExpiry?.ifBlank { "2027-11-23" } ?: "2027-11-23",
                                licenseExpiry = initial?.licenseExpiry?.ifBlank { "2027-11-23" } ?: "2027-11-23",
                            )
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
