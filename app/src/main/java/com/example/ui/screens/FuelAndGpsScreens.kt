package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.domain.model.FuelTransaction
import com.example.domain.model.FuelType
import com.example.domain.model.TripRecord
import com.example.domain.model.TripStatus
import com.example.domain.rbac.AxiomPermission
import com.example.domain.rbac.RbacPermissions
import com.example.ui.components.AxiomEmptyState
import com.example.ui.components.AxiomKpiTile
import com.example.ui.components.AxiomLineForecastChart
import com.example.ui.components.AxiomPillBadge
import com.example.ui.components.AxiomRouteTelemetryMap
import com.example.ui.components.AxiomSectionCard
import com.example.ui.components.severityColors
import com.example.ui.theme.AxiomAmber
import com.example.ui.theme.AxiomAmberBg
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FuelIntelligenceScreen(
    state: AxiomWorkspaceUiState.Ready,
    onLogFuel: (FuelTransaction) -> Unit,
) {
    var showLogDialog by remember { mutableStateOf(false) }
    val canLog = RbacPermissions.hasPermission(state.profile.roleEnum(), AxiomPermission.LOG_FUEL_TRANSACTIONS)
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
                FlowRow(
                    maxItemsInEachRow = 2,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AxiomKpiTile(
                        label = "Total Fuel Issued",
                        value = "${"%.1f".format(Locale.US, state.summary.totalFuelLiters)} L",
                        deltaOrNote = "$cur ${"%.0f".format(Locale.US, state.summary.totalFuelSpend)} total spend",
                        icon = Icons.Filled.LocalGasStation,
                        modifier = Modifier.weight(1f),
                    )
                    AxiomKpiTile(
                        label = "Fleet Efficiency",
                        value = "${"%.2f".format(Locale.US, state.summary.avgKmPerLiter)} km/L",
                        deltaOrNote = "${"%.2f".format(Locale.US, state.summary.avgLitersPer100Km)} L/100km",
                        icon = Icons.Outlined.Analytics,
                        accentColor = AxiomEmerald,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                AxiomSectionCard(
                    title = "CUSUM Drift Monitor (Negative Efficiency)",
                    subtitle = "Cumulative Sum control chart detecting sustained drops below baseline km/L",
                    badge = "${state.fuelSignals.size} SIGNALS",
                ) {
                    if (state.cusumPoints.isEmpty()) {
                        Text("Log fuel transactions to generate CUSUM drift series.", style = MaterialTheme.typography.bodySmall, color = AxiomSilver)
                    } else {
                        val threshold = state.cusumPoints.firstOrNull()?.threshold ?: 1.5
                        AxiomLineForecastChart(
                            historical = state.cusumPoints.map { it.cusumNeg },
                            forecast = emptyList(),
                            thresholdLine = threshold,
                            lineColor = AxiomGold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Red dashed line: CUSUM decision boundary (h = 4σ). Signals are flagged for investigation—never auto-classified as theft.",
                            style = MaterialTheme.typography.labelSmall,
                            color = AxiomSilver,
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Statistical Investigation Signals (${state.fuelSignals.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = AxiomIvory,
                )
            }

            if (state.fuelSignals.isEmpty()) {
                item {
                    AxiomEmptyState(
                        title = "No Fuel Anomalies Detected",
                        description = "All recorded fuel fills align within Z-score, CUSUM, and tank capacity bounds.",
                        icon = Icons.Outlined.WarningAmber,
                    )
                }
            } else {
                items(state.fuelSignals) { sig ->
                    val (fg, bg) = severityColors(sig.severity)
                    AxiomSectionCard(
                        title = "${sig.vehicleReg} • ${sig.signalCategory}",
                        subtitle = "Driver: ${sig.driverName.ifBlank { "Unassigned" }} • Confidence: ${(sig.confidenceScore * 100).toInt()}%",
                        badge = "Z = ${"%.2f".format(Locale.US, sig.zScore)}",
                    ) {
                        AxiomPillBadge(sig.severity.label.uppercase(), fg, bg)
                        Spacer(Modifier.height(8.dp))
                        Text("Evidence: ${sig.evidenceSummary}", style = MaterialTheme.typography.bodySmall, color = AxiomIvory)
                        Spacer(Modifier.height(4.dp))
                        Text("Recommended Investigation: ${sig.recommendedInvestigation}", style = MaterialTheme.typography.bodySmall, color = AxiomGold)
                    }
                }
            }

            item {
                Text(
                    text = "Fuel Ledger (${state.fuelTransactions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = AxiomIvory,
                )
            }

            items(state.fuelTransactions) { tx ->
                AxiomSectionCard(
                    title = "${tx.vehicleReg} • ${tx.liters} L ($cur ${"%.2f".format(Locale.US, tx.costAmount)})",
                    subtitle = "${tx.txDateLabel} • Ref ${tx.transactionRef} • ${tx.stationName}",
                    badge = "${"%.2f".format(Locale.US, tx.kmPerLiter)} km/L",
                ) {
                    Text(
                        text = "Route: ${tx.routeName} • Odometer: ${tx.odometerKm.toInt()} km (+${tx.distanceSinceLastKm.toInt()} km) • ${"%.3f".format(Locale.US, tx.litersPerKm)} L/km",
                        style = MaterialTheme.typography.bodySmall,
                        color = AxiomSilver,
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        if (canLog) {
            FloatingActionButton(
                onClick = { showLogDialog = true },
                containerColor = AxiomGold,
                contentColor = AxiomObsidian,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("log_fuel_fab"),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Log Fuel Transaction")
            }
        }
    }

    if (showLogDialog) {
        LogFuelDialog(
            state = state,
            onDismiss = { showLogDialog = false },
            onSave = { tx ->
                onLogFuel(tx)
                showLogDialog = false
            }
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
fun GpsRoutesScreen(
    state: AxiomWorkspaceUiState.Ready,
    onToggleSimMode: (Boolean) -> Unit,
    onAddTrip: (TripRecord) -> Unit,
) {
    val context = LocalContext.current
    var liveGpsReading by remember { mutableStateOf<String?>(null) }
    var showTripDialog by remember { mutableStateOf(false) }

    val canManage = RbacPermissions.hasPermission(state.profile.roleEnum(), AxiomPermission.MANAGE_TRIPS_AND_GEOFENCES)

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        liveGpsReading = if (granted) {
            readDeviceLastLocation(context)
        } else {
            "Location permission denied. Using telematics/simulation mode."
        }
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
                Surface(
                    color = if (state.simulatedModeActive) AxiomAmberBg else AxiomEmeraldBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        (if (state.simulatedModeActive) AxiomAmber else AxiomEmerald).copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (state.simulatedModeActive) "SIMULATED TELEMATICS MODE" else "LIVE HARDWARE / DEVICE GPS MODE",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (state.simulatedModeActive) AxiomAmber else AxiomEmerald,
                            )
                            Text(
                                text = if (state.simulatedModeActive) {
                                    "Live fleet GPS hardware is not connected; route paths below use clearly-labelled simulated telemetry."
                                } else {
                                    liveGpsReading ?: "Tap Sync Device GPS to read local Android LocationManager coordinates."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = AxiomIvory,
                            )
                        }
                        Switch(
                            checked = state.simulatedModeActive,
                            onCheckedChange = onToggleSimMode,
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            val fineGranted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (fineGranted) {
                                onToggleSimMode(false)
                                liveGpsReading = readDeviceLastLocation(context)
                            } else {
                                onToggleSimMode(false)
                                locationLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        border = BorderStroke(1.dp, AxiomGold),
                    ) {
                        Icon(Icons.Filled.MyLocation, null, tint = AxiomGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sync Device GPS", color = AxiomGold)
                    }
                }
            }

            item {
                AxiomSectionCard(
                    title = "Corridor & Geofence Telemetry Map",
                    subtitle = "Gold: On-corridor • Crimson: Route deviation / Geofence breach • Dashed circle: Geofence zone",
                    badge = "${state.trips.size} ROUTES",
                ) {
                    AxiomRouteTelemetryMap(routes = state.trips)
                }
            }

            if (state.trips.isEmpty()) {
                item {
                    AxiomEmptyState(
                        title = "No Route History",
                        description = "Log a trip or load demonstration data to view route efficiency and geofence telemetry.",
                        icon = Icons.Outlined.Route,
                    )
                }
            } else {
                items(state.trips) { trip ->
                    val (fg, bg) = if (trip.geofenceBreached || trip.statusEnum() == TripStatus.DEVIATED) {
                        AxiomCrimson to AxiomCrimsonBg
                    } else {
                        AxiomEmerald to AxiomEmeraldBg
                    }
                    AxiomSectionCard(
                        title = "${trip.routeName} • ${trip.vehicleReg}",
                        subtitle = "${trip.originName} → ${trip.destinationName} • Driver: ${trip.driverName}",
                        badge = "${"%.1f".format(Locale.US, trip.efficiencyPct)}% EFF",
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Planned: ${trip.plannedDistanceKm.toInt()} km • Actual: ${trip.actualDistanceKm.toInt()} km • Deviation: ${trip.routeDeviationKm} km",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AxiomIvory,
                                )
                                Text(
                                    text = "Duration: ${trip.durationMinutes} min • Idle: ${trip.idleMinutes} min • Stops: ${trip.stopCount} • ${if (trip.simulatedTelemetry) "SIMULATED" else "RECORDED"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AxiomSilver,
                                )
                            }
                            AxiomPillBadge(
                                text = if (trip.geofenceBreached) "GEOFENCE BREACH" else trip.statusEnum().label,
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
                onClick = { showTripDialog = true },
                containerColor = AxiomGold,
                contentColor = AxiomObsidian,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("add_trip_fab"),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Log Trip")
            }
        }
    }

    if (showTripDialog) {
        LogTripDialog(
            state = state,
            onDismiss = { showTripDialog = false },
            onSave = { trip ->
                onAddTrip(trip)
                showTripDialog = false
            }
        )
    }
}

@SuppressLint("MissingPermission")
private fun readDeviceLastLocation(context: Context): String {
    return try {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (loc != null) {
            "Device GPS: ${"%.5f".format(Locale.US, loc.latitude)}, ${"%.5f".format(Locale.US, loc.longitude)} (±${loc.accuracy.toInt()}m)"
        } else {
            "GPS provider active; awaiting first satellite fix in emulator."
        }
    } catch (e: Exception) {
        "Unable to query device location: ${e.localizedMessage}"
    }
}

@Composable
private fun LogFuelDialog(
    state: AxiomWorkspaceUiState.Ready,
    onDismiss: () -> Unit,
    onSave: (FuelTransaction) -> Unit,
) {
    val firstVehicle = state.vehicles.firstOrNull()
    var vehicleReg by remember { mutableStateOf(firstVehicle?.registrationNumber ?: "AXM-104") }
    var route by remember { mutableStateOf("Northern Corridor A1") }
    var liters by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var odo by remember { mutableStateOf(firstVehicle?.currentMileageKm?.toInt()?.toString() ?: "10000") }
    var distance by remember { mutableStateOf("850") }
    var station by remember { mutableStateOf("Shell Commercial Hub") }
    var ref by remember { mutableStateOf("AXF-${System.currentTimeMillis() % 100000}") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Fuel Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(vehicleReg, { vehicleReg = it }, label = { Text("Vehicle Registration") }, singleLine = true)
                OutlinedTextField(route, { route = it }, label = { Text("Route Name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(liters, { liters = it }, label = { Text("Liters") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(cost, { cost = it }, label = { Text("Cost") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(odo, { odo = it }, label = { Text("Odometer km") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(distance, { distance = it }, label = { Text("Trip km") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(station, { station = it }, label = { Text("Fuel Station") }, singleLine = true)
                OutlinedTextField(ref, { ref = it }, label = { Text("Transaction Ref") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val l = liters.toDoubleOrNull()
                    val c = cost.toDoubleOrNull()
                    val o = odo.toDoubleOrNull()
                    val d = distance.toDoubleOrNull()
                    if (vehicleReg.isNotBlank() && l != null && l > 0 && c != null && o != null && d != null) {
                        val v = state.vehicles.firstOrNull { it.registrationNumber.equals(vehicleReg.trim(), true) }
                        onSave(
                            FuelTransaction(
                                vehicleId = v?.id?.ifBlank { vehicleReg.trim() } ?: vehicleReg.trim(),
                                vehicleReg = vehicleReg.trim().uppercase(),
                                driverId = v?.assignedDriverId.orEmpty(),
                                driverName = v?.assignedDriverName.orEmpty(),
                                routeName = route.trim().ifBlank { "Corridor Route" },
                                liters = l,
                                costAmount = c,
                                odometerKm = o,
                                distanceSinceLastKm = d,
                                fuelType = v?.fuelType ?: FuelType.DIESEL.name,
                                stationName = station.trim().ifBlank { "Commercial Depot" },
                                transactionRef = ref.trim(),
                                txDateLabel = "2026-11-23",
                                txTimestampMillis = System.currentTimeMillis(),
                            )
                        )
                    }
                }
            ) { Text("Record Fuel") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun LogTripDialog(
    state: AxiomWorkspaceUiState.Ready,
    onDismiss: () -> Unit,
    onSave: (TripRecord) -> Unit,
) {
    val firstVehicle = state.vehicles.firstOrNull()
    var vehicleReg by remember { mutableStateOf(firstVehicle?.registrationNumber ?: "AXM-104") }
    var routeName by remember { mutableStateOf("Midwest Spine Corridor") }
    var origin by remember { mutableStateOf("Chicago Hub") }
    var dest by remember { mutableStateOf("Indianapolis Yard") }
    var planned by remember { mutableStateOf("295") }
    var actual by remember { mutableStateOf("302") }
    var idle by remember { mutableStateOf("18") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Route Trip") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(vehicleReg, { vehicleReg = it }, label = { Text("Vehicle Registration") }, singleLine = true)
                OutlinedTextField(routeName, { routeName = it }, label = { Text("Route Name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(origin, { origin = it }, label = { Text("Origin") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(dest, { dest = it }, label = { Text("Destination") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(planned, { planned = it }, label = { Text("Planned km") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(actual, { actual = it }, label = { Text("Actual km") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(idle, { idle = it }, label = { Text("Idle Minutes") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = planned.toDoubleOrNull() ?: 0.0
                    val a = actual.toDoubleOrNull() ?: 0.0
                    val dev = kotlin.math.abs(a - p)
                    val v = state.vehicles.firstOrNull { it.registrationNumber.equals(vehicleReg.trim(), true) }
                    onSave(
                        TripRecord(
                            vehicleId = v?.id?.ifBlank { vehicleReg.trim() } ?: vehicleReg.trim(),
                            vehicleReg = vehicleReg.trim().uppercase(),
                            driverId = v?.assignedDriverId.orEmpty(),
                            driverName = v?.assignedDriverName.orEmpty(),
                            routeName = routeName.trim(),
                            originName = origin.trim(),
                            destinationName = dest.trim(),
                            plannedDistanceKm = p,
                            actualDistanceKm = a,
                            durationMinutes = (a / 65.0 * 60).toLong().coerceAtLeast(30L),
                            idleMinutes = idle.toLongOrNull() ?: 0L,
                            stopCount = 2L,
                            routeDeviationKm = dev,
                            geofenceBreached = dev > 20.0,
                            simulatedTelemetry = state.simulatedModeActive,
                            startLat = 41.88,
                            startLng = -87.63,
                            endLat = 39.77,
                            endLng = -86.16,
                            status = if (dev > 20.0) TripStatus.DEVIATED.name else TripStatus.COMPLETED.name,
                            startedAtMillis = System.currentTimeMillis(),
                        )
                    )
                }
            ) { Text("Save Trip") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
