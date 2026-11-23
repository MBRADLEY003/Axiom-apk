package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.Driver
import com.example.domain.model.MaintenanceRecord
import com.example.domain.model.MaintenanceStatus
import com.example.domain.model.MaintenanceType
import com.example.domain.model.ShipmentRecord
import com.example.domain.model.ShipmentStatus
import com.example.domain.rbac.AxiomPermission
import com.example.domain.rbac.RbacPermissions
import com.example.ui.components.AxiomBarChart
import com.example.ui.components.AxiomEmptyState
import com.example.ui.components.AxiomPillBadge
import com.example.ui.components.AxiomSectionCard
import com.example.ui.theme.AxiomAmber
import com.example.ui.theme.AxiomAmberBg
import com.example.ui.theme.AxiomCobalt
import com.example.ui.theme.AxiomCobaltBg
import com.example.ui.theme.AxiomCrimson
import com.example.ui.theme.AxiomCrimsonBg
import com.example.ui.theme.AxiomEmerald
import com.example.ui.theme.AxiomEmeraldBg
import com.example.ui.theme.AxiomGold
import com.example.ui.theme.AxiomIvory
import com.example.ui.theme.AxiomObsidian
import com.example.ui.theme.AxiomSilver
import com.example.ui.viewmodel.AxiomWorkspaceUiState
import java.util.Locale

@Composable
fun DriversScreen(
    state: AxiomWorkspaceUiState.Ready,
    onAddDriver: (Driver) -> Unit,
    onInspectDriver: (String) -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    val canManage = RbacPermissions.hasPermission(state.profile.roleEnum(), AxiomPermission.MANAGE_FLEET_VEHICLES)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                AxiomSectionCard(
                    title = "Objective Driver Performance Benchmark",
                    subtitle = "Composite Index (Fuel efficiency vs fleet mean, On-time %, Safety & Maint incidents)",
                    badge = "${state.driverScores.size} DRIVERS",
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

            if (state.driverScores.isEmpty()) {
                item {
                    AxiomEmptyState(
                        title = "No Drivers Registered",
                        description = "Add driver profiles or load demonstration fleet data.",
                        icon = Icons.Outlined.Person,
                    )
                }
            } else {
                items(state.driverScores, key = { it.driverId.ifBlank { it.driverCode } }) { score ->
                    val (fg, bg) = when {
                        score.compositeIndex >= 85 -> AxiomEmerald to AxiomEmeraldBg
                        score.compositeIndex >= 65 -> AxiomAmber to AxiomAmberBg
                        else -> AxiomCrimson to AxiomCrimsonBg
                    }
                    AxiomSectionCard(
                        title = "#${score.rank} ${score.driverName} (${score.driverCode})",
                        subtitle = "Assigned Vehicle: ${score.assignedVehicleReg.ifBlank { "Unassigned" }}",
                        badge = "INDEX ${"%.1f".format(Locale.US, score.compositeIndex)}",
                        onClick = { onInspectDriver(score.driverId) },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Distance: ${score.distanceKm.toInt()} km • Efficiency: ${"%.2f".format(Locale.US, score.kmPerLiter)} km/L • Hours: ${score.drivingHours.toInt()}h",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AxiomIvory,
                                )
                                Text(
                                    text = "On-Time: ${"%.1f".format(Locale.US, score.onTimePct)}% • Safety Events: ${score.safetyEvents} • Maint Incidents: ${score.maintenanceIncidents}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AxiomSilver,
                                )
                            }
                            AxiomPillBadge(
                                text = if (score.compositeIndex >= 85) "ELITE" else if (score.compositeIndex >= 65) "NOMINAL" else "COACHING",
                                fgColor = fg,
                                bgColor = bg,
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }

        if (canManage) {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = AxiomGold,
                contentColor = AxiomObsidian,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("add_driver_fab"),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Driver")
            }
        }
    }

    if (showAdd) {
        AddDriverDialog(
            onDismiss = { showAdd = false },
            onSave = { d ->
                onAddDriver(d)
                showAdd = false
            }
        )
    }
}

@Composable
fun DriverDetailScreen(
    driver: Driver?,
    state: AxiomWorkspaceUiState.Ready,
    onBack: () -> Unit,
) {
    if (driver == null) {
        AxiomEmptyState("Driver Not Found", "Driver record unavailable.")
        return
    }
    val score = state.driverScores.firstOrNull { it.driverId == driver.id }
    val trips = state.trips.filter { it.driverId == driver.id || it.driverName == driver.fullName }
    val fuel = state.fuelTransactions.filter { it.driverId == driver.id || it.driverName == driver.fullName }

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
                Text("${driver.fullName} (${driver.driverCode})", style = MaterialTheme.typography.headlineSmall, color = AxiomIvory)
            }
        }
        item {
            AxiomSectionCard(
                title = "Driver Operational Profile",
                subtitle = "License: ${driver.licenseNumber} • Vehicle: ${driver.assignedVehicleReg.ifBlank { "Unassigned" }}",
                badge = "RANK #${score?.rank ?: "-"}",
            ) {
                Text("Distance Travelled: ${driver.distanceTravelledKm.toInt()} km", style = MaterialTheme.typography.bodyMedium, color = AxiomIvory)
                Text("Fuel Consumed: ${driver.fuelUsedLiters.toInt()} L (${"%.2f".format(Locale.US, driver.kmPerLiter)} km/L)", style = MaterialTheme.typography.bodyMedium, color = AxiomSilver)
                Text("Driving Hours: ${driver.drivingHours} hrs • On-Time Delivery: ${driver.onTimeDeliveryPct}%", style = MaterialTheme.typography.bodyMedium, color = AxiomSilver)
                Text("Safety Events: ${driver.safetyEvents} • Maintenance Incidents: ${driver.maintenanceIncidents}", style = MaterialTheme.typography.bodySmall, color = AxiomGold)
            }
        }
        item {
            AxiomSectionCard(title = "Journey History (${trips.size})") {
                if (trips.isEmpty()) Text("No trips recorded.", style = MaterialTheme.typography.bodySmall, color = AxiomSilver)
                trips.forEach { t ->
                    Text("${t.routeName} • ${t.actualDistanceKm} km • Idle ${t.idleMinutes} min • ${t.status}", style = MaterialTheme.typography.bodySmall, color = AxiomIvory)
                }
            }
        }
        item {
            AxiomSectionCard(title = "Fuel Transactions (${fuel.size})") {
                if (fuel.isEmpty()) Text("No fuel transactions.", style = MaterialTheme.typography.bodySmall, color = AxiomSilver)
                fuel.forEach { f ->
                    Text("${f.txDateLabel} • ${f.vehicleReg} • ${f.liters} L (${"%.2f".format(Locale.US, f.kmPerLiter)} km/L)", style = MaterialTheme.typography.bodySmall, color = AxiomIvory)
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun MaintenanceScreen(
    state: AxiomWorkspaceUiState.Ready,
    onAddMaintenance: (MaintenanceRecord) -> Unit,
    onUpdateStatus: (String, MaintenanceStatus) -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    val canManage = RbacPermissions.hasPermission(state.profile.roleEnum(), AxiomPermission.MANAGE_MAINTENANCE)
    val cur = state.activeCompany.currency

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                AxiomSectionCard(
                    title = "Predictive Service Intervals",
                    subtitle = "Estimated from current odometer, service intervals & breakdown history",
                    badge = "${state.maintenancePredictions.size} ASSETS",
                ) {
                    if (state.maintenancePredictions.isEmpty()) {
                        Text("Add vehicles to compute predictive maintenance windows.", style = MaterialTheme.typography.bodySmall, color = AxiomSilver)
                    } else {
                        state.maintenancePredictions.forEach { pred ->
                            val color = when {
                                pred.urgencyScore >= 70 -> AxiomCrimson
                                pred.urgencyScore >= 40 -> AxiomAmber
                                else -> AxiomEmerald
                            }
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("${pred.vehicleReg} (${pred.makeModel})", style = MaterialTheme.typography.titleSmall, color = AxiomIvory)
                                    Text("Urgency ${pred.urgencyScore}/100", style = MaterialTheme.typography.labelMedium, color = color)
                                }
                                LinearProgressIndicator(
                                    progress = { (pred.urgencyScore / 100f).coerceIn(0f, 1f) },
                                    color = color,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                )
                                Text(pred.statusNote, style = MaterialTheme.typography.bodySmall, color = AxiomSilver)
                            }
                        }
                    }
                }
            }

            if (state.maintenance.isEmpty()) {
                item {
                    AxiomEmptyState(
                        title = "No Maintenance Records",
                        description = "Schedule preventive maintenance or log corrective/breakdown events.",
                        icon = Icons.Outlined.Build,
                    )
                }
            } else {
                items(state.maintenance, key = { it.id.ifBlank { it.title + it.serviceDate } }) { rec ->
                    val (fg, bg) = when (rec.statusEnum()) {
                        MaintenanceStatus.COMPLETED -> AxiomEmerald to AxiomEmeraldBg
                        MaintenanceStatus.OVERDUE -> AxiomCrimson to AxiomCrimsonBg
                        MaintenanceStatus.IN_PROGRESS -> AxiomAmber to AxiomAmberBg
                        MaintenanceStatus.SCHEDULED -> AxiomCobalt to AxiomCobaltBg
                    }
                    AxiomSectionCard(
                        title = "${rec.vehicleReg} • ${rec.title}",
                        subtitle = "${rec.typeEnum().label} • Service Date: ${rec.serviceDate} • Cost: $cur ${"%.2f".format(Locale.US, rec.costAmount)}",
                        badge = "${rec.downtimeHours}h DOWNTIME",
                    ) {
                        AxiomPillBadge(rec.statusEnum().label.uppercase(), fg, bg)
                        Spacer(Modifier.height(6.dp))
                        Text(rec.description.ifBlank { "Standard inspection" }, style = MaterialTheme.typography.bodySmall, color = AxiomIvory)
                        if (rec.spareParts.isNotBlank()) {
                            Text("Spare Parts: ${rec.spareParts}", style = MaterialTheme.typography.labelSmall, color = AxiomSilver)
                        }
                        Text("Service Odometer: ${rec.mileageAtServiceKm.toInt()} km → Next Due: ${rec.nextDueMileageKm.toInt()} km", style = MaterialTheme.typography.labelSmall, color = AxiomGold)
                        if (canManage && rec.statusEnum() != MaintenanceStatus.COMPLETED) {
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (rec.statusEnum() == MaintenanceStatus.SCHEDULED) {
                                    OutlinedButton(onClick = { onUpdateStatus(rec.id, MaintenanceStatus.IN_PROGRESS) }) {
                                        Text("Start Work")
                                    }
                                }
                                Button(onClick = { onUpdateStatus(rec.id, MaintenanceStatus.COMPLETED) }) {
                                    Text("Mark Completed")
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
                onClick = { showAdd = true },
                containerColor = AxiomGold,
                contentColor = AxiomObsidian,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("add_maint_fab"),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Schedule Maintenance")
            }
        }
    }

    if (showAdd) {
        AddMaintenanceDialog(
            state = state,
            onDismiss = { showAdd = false },
            onSave = { m ->
                onAddMaintenance(m)
                showAdd = false
            }
        )
    }
}

@Composable
fun LogisticsScreen(
    state: AxiomWorkspaceUiState.Ready,
    onAddShipment: (ShipmentRecord) -> Unit,
    onUpdateStatus: (String, ShipmentStatus) -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    val canManage = RbacPermissions.hasPermission(state.profile.roleEnum(), AxiomPermission.MANAGE_SHIPMENTS)
    val canConfirm = RbacPermissions.hasPermission(state.profile.roleEnum(), AxiomPermission.CONFIRM_DELIVERY)
    val cur = state.activeCompany.currency

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }
            if (state.shipments.isEmpty()) {
                item {
                    AxiomEmptyState(
                        title = "No Shipments Dispatched",
                        description = "Register customer cargo, assign vehicle & driver, and track trip profitability.",
                        icon = Icons.Outlined.LocalShipping,
                    )
                }
            } else {
                items(state.shipments, key = { it.id.ifBlank { it.trackingCode } }) { sh ->
                    val (fg, bg) = when (sh.statusEnum()) {
                        ShipmentStatus.DELIVERED -> AxiomEmerald to AxiomEmeraldBg
                        ShipmentStatus.DELAYED -> AxiomCrimson to AxiomCrimsonBg
                        ShipmentStatus.IN_TRANSIT, ShipmentStatus.DISPATCHED -> AxiomAmber to AxiomAmberBg
                        ShipmentStatus.REGISTERED -> AxiomCobalt to AxiomCobaltBg
                    }
                    AxiomSectionCard(
                        title = "${sh.trackingCode} • ${sh.customerName}",
                        subtitle = "${sh.pickupLocation} → ${sh.deliveryLocation} • Scheduled ${sh.scheduledDate}",
                        badge = "LOAD ${"%.0f".format(Locale.US, sh.loadFactorPct)}%",
                    ) {
                        AxiomPillBadge(sh.statusEnum().label.uppercase(), fg, bg)
                        Spacer(Modifier.height(8.dp))
                        Text("Cargo: ${sh.cargoDescription} (${sh.weightKg.toInt()} / ${sh.capacityKg.toInt()} kg)", style = MaterialTheme.typography.bodySmall, color = AxiomIvory)
                        Text("Assigned: ${sh.vehicleReg.ifBlank { "Unassigned" }} (${sh.driverName.ifBlank { "Unassigned" }})", style = MaterialTheme.typography.bodySmall, color = AxiomSilver)
                        Text(
                            text = "Revenue: $cur ${"%.2f".format(Locale.US, sh.revenueAmount)} • Est Cost: $cur ${"%.2f".format(Locale.US, sh.tripCostEstimate)} • Margin: $cur ${"%.2f".format(Locale.US, sh.estimatedMargin)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (sh.estimatedMargin >= 0) AxiomEmerald else AxiomCrimson,
                        )
                        if (sh.statusEnum() != ShipmentStatus.DELIVERED && (canManage || canConfirm)) {
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (canManage && sh.statusEnum() == ShipmentStatus.REGISTERED) {
                                    OutlinedButton(onClick = { onUpdateStatus(sh.id, ShipmentStatus.DISPATCHED) }) {
                                        Text("Dispatch")
                                    }
                                }
                                if (canManage && sh.statusEnum() == ShipmentStatus.DISPATCHED) {
                                    OutlinedButton(onClick = { onUpdateStatus(sh.id, ShipmentStatus.IN_TRANSIT) }) {
                                        Text("In Transit")
                                    }
                                }
                                if (canConfirm) {
                                    Button(onClick = { onUpdateStatus(sh.id, ShipmentStatus.DELIVERED) }) {
                                        Text("Confirm Delivery")
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
                onClick = { showAdd = true },
                containerColor = AxiomGold,
                contentColor = AxiomObsidian,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("add_shipment_fab"),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Register Shipment")
            }
        }
    }

    if (showAdd) {
        AddShipmentDialog(
            state = state,
            onDismiss = { showAdd = false },
            onSave = { sh ->
                onAddShipment(sh)
                showAdd = false
            }
        )
    }
}

@Composable
private fun AddDriverDialog(onDismiss: () -> Unit, onSave: (Driver) -> Unit) {
    var code by remember { mutableStateOf("DRV-${System.currentTimeMillis() % 1000}") }
    var name by remember { mutableStateOf("") }
    var license by remember { mutableStateOf("") }
    var vehicleReg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register Driver") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(code, { code = it }, label = { Text("Driver Code") }, singleLine = true)
                OutlinedTextField(name, { name = it }, label = { Text("Full Name") }, singleLine = true)
                OutlinedTextField(license, { license = it }, label = { Text("License Number") }, singleLine = true)
                OutlinedTextField(vehicleReg, { vehicleReg = it }, label = { Text("Assigned Vehicle Reg") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (code.isNotBlank() && name.isNotBlank() && license.isNotBlank()) {
                    onSave(
                        Driver(
                            driverCode = code.trim().uppercase(),
                            fullName = name.trim(),
                            licenseNumber = license.trim().uppercase(),
                            assignedVehicleReg = vehicleReg.trim().uppercase(),
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddMaintenanceDialog(
    state: AxiomWorkspaceUiState.Ready,
    onDismiss: () -> Unit,
    onSave: (MaintenanceRecord) -> Unit,
) {
    val first = state.vehicles.firstOrNull()
    var vReg by remember { mutableStateOf(first?.registrationNumber ?: "AXM-104") }
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("650") }
    var nextDue by remember { mutableStateOf(((first?.currentMileageKm ?: 10000.0) + 15000.0).toInt().toString()) }
    var type by remember { mutableStateOf(MaintenanceType.PREVENTIVE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Maintenance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(vReg, { vReg = it }, label = { Text("Vehicle Registration") }, singleLine = true)
                OutlinedTextField(title, { title = it }, label = { Text("Service Title") }, singleLine = true)
                OutlinedTextField(desc, { desc = it }, label = { Text("Notes / Scope") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(cost, { cost = it }, label = { Text("Est Cost") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(nextDue, { nextDue = it }, label = { Text("Next Due km") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MaintenanceType.entries.forEach { mt ->
                        FilterChip(selected = type == mt, onClick = { type = mt }, label = { Text(mt.label) })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (vReg.isNotBlank() && title.isNotBlank()) {
                    val v = state.vehicles.firstOrNull { it.registrationNumber.equals(vReg.trim(), true) }
                    onSave(
                        MaintenanceRecord(
                            vehicleId = v?.id?.ifBlank { vReg.trim() } ?: vReg.trim(),
                            vehicleReg = vReg.trim().uppercase(),
                            maintenanceType = type.name,
                            title = title.trim(),
                            description = desc.trim(),
                            serviceDate = "2026-11-23",
                            mileageAtServiceKm = v?.currentMileageKm ?: 0.0,
                            nextDueMileageKm = nextDue.toDoubleOrNull() ?: 15000.0,
                            costAmount = cost.toDoubleOrNull() ?: 0.0,
                            downtimeHours = 4.0,
                            status = MaintenanceStatus.SCHEDULED.name,
                        )
                    )
                }
            }) { Text("Schedule") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddShipmentDialog(
    state: AxiomWorkspaceUiState.Ready,
    onDismiss: () -> Unit,
    onSave: (ShipmentRecord) -> Unit,
) {
    val first = state.vehicles.firstOrNull()
    var code by remember { mutableStateOf("SHP-${System.currentTimeMillis() % 10000}") }
    var customer by remember { mutableStateOf("") }
    var cargo by remember { mutableStateOf("") }
    var pickup by remember { mutableStateOf("Chicago Hub") }
    var drop by remember { mutableStateOf("Detroit Terminal") }
    var weight by remember { mutableStateOf("12000") }
    var revenue by remember { mutableStateOf("3500") }
    var cost by remember { mutableStateOf("1250") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register Shipment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(code, { code = it }, label = { Text("Tracking Code") }, singleLine = true)
                OutlinedTextField(customer, { customer = it }, label = { Text("Customer Name") }, singleLine = true)
                OutlinedTextField(cargo, { cargo = it }, label = { Text("Cargo Description") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(pickup, { pickup = it }, label = { Text("Pickup") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(drop, { drop = it }, label = { Text("Delivery") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(weight, { weight = it }, label = { Text("Weight kg") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(revenue, { revenue = it }, label = { Text("Revenue") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(cost, { cost = it }, label = { Text("Est Cost") }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (code.isNotBlank() && customer.isNotBlank() && cargo.isNotBlank()) {
                    onSave(
                        ShipmentRecord(
                            trackingCode = code.trim().uppercase(),
                            customerName = customer.trim(),
                            cargoDescription = cargo.trim(),
                            weightKg = weight.toDoubleOrNull() ?: 1000.0,
                            capacityKg = 24000.0,
                            pickupLocation = pickup.trim(),
                            deliveryLocation = drop.trim(),
                            vehicleId = first?.id.orEmpty(),
                            vehicleReg = first?.registrationNumber.orEmpty(),
                            driverId = first?.assignedDriverId.orEmpty(),
                            driverName = first?.assignedDriverName.orEmpty(),
                            revenueAmount = revenue.toDoubleOrNull() ?: 0.0,
                            tripCostEstimate = cost.toDoubleOrNull() ?: 0.0,
                            status = ShipmentStatus.REGISTERED.name,
                            scheduledDate = "2026-11-24",
                        )
                    )
                }
            }) { Text("Create Shipment") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
