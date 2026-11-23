package com.example.domain.analytics

import com.example.domain.model.Driver
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.ExpenseRecord
import com.example.domain.model.FuelTransaction
import com.example.domain.model.MaintenanceRecord
import com.example.domain.model.MaintenanceStatus
import com.example.domain.model.RiskAlertRecord
import com.example.domain.model.RiskResolutionStatus
import com.example.domain.model.RiskSeverity
import com.example.domain.model.ShipmentRecord
import com.example.domain.model.ShipmentStatus
import com.example.domain.model.TripRecord
import com.example.domain.model.TripStatus
import com.example.domain.model.Vehicle
import com.example.domain.model.VehicleStatus
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

data class FuelAnomalySignal(
    val transactionId: String,
    val vehicleId: String,
    val vehicleReg: String,
    val driverName: String,
    val signalCategory: String,
    val severity: RiskSeverity,
    val zScore: Double,
    val cusumNegative: Double,
    val cusumPositive: Double,
    val observedKmPerL: Double,
    val expectedKmPerL: Double,
    val liters: Double,
    val confidenceScore: Double,
    val evidenceSummary: String,
    val recommendedInvestigation: String,
)

data class CusumPoint(
    val label: String,
    val vehicleReg: String,
    val actualKmPerL: Double,
    val baselineKmPerL: Double,
    val cusumNeg: Double,
    val cusumPos: Double,
    val threshold: Double,
)

data class ForecastSeriesResult(
    val historicalLabels: List<String>,
    val historicalValues: List<Double>,
    val forecastLabels: List<String>,
    val forecastValues: List<Double>,
    val lowerBounds: List<Double>,
    val upperBounds: List<Double>,
    val slopePerPeriod: Double,
    val rSquared: Double,
    val mae: Double,
    val modelDescription: String,
)

data class MaintenancePrediction(
    val vehicleId: String,
    val vehicleReg: String,
    val makeModel: String,
    val currentMileageKm: Double,
    val nextDueMileageKm: Double,
    val remainingKm: Double,
    val urgencyScore: Int,
    val statusNote: String,
)

data class DriverPerformanceScore(
    val driverId: String,
    val driverName: String,
    val driverCode: String,
    val assignedVehicleReg: String,
    val distanceKm: Double,
    val kmPerLiter: Double,
    val drivingHours: Double,
    val onTimePct: Double,
    val safetyEvents: Long,
    val maintenanceIncidents: Long,
    val compositeIndex: Double,
    val rank: Int,
)

data class VehicleRiskScore(
    val vehicleId: String,
    val vehicleReg: String,
    val makeModel: String,
    val riskScore: Int,
    val riskTier: RiskSeverity,
    val observedFacts: String,
    val statisticalEstimate: String,
    val recommendedAction: String,
)

data class FleetExecutiveSummary(
    val totalFleet: Int,
    val activeVehicles: Int,
    val inactiveVehicles: Int,
    val maintenanceVehicles: Int,
    val totalDistanceKm: Double,
    val totalFuelLiters: Double,
    val avgKmPerLiter: Double,
    val avgLitersPer100Km: Double,
    val totalFuelSpend: Double,
    val totalMaintenanceSpend: Double,
    val totalOpex: Double,
    val costPerKm: Double,
    val fleetUtilisationPct: Double,
    val outstandingMaintenanceCount: Int,
    val openCriticalAlertsCount: Int,
    val activeShipmentsCount: Int,
    val deliveredShipmentsCount: Int,
)

object AxiomAnalyticsEngine {

    fun computeExecutiveSummary(
        vehicles: List<Vehicle>,
        drivers: List<Driver>,
        fuelTransactions: List<FuelTransaction>,
        trips: List<TripRecord>,
        maintenance: List<MaintenanceRecord>,
        expenses: List<ExpenseRecord>,
        shipments: List<ShipmentRecord>,
        riskAlerts: List<RiskAlertRecord>,
    ): FleetExecutiveSummary {
        val totalFleet = vehicles.size
        val activeVehicles = vehicles.count { it.statusEnum() == VehicleStatus.ACTIVE }
        val inactiveVehicles = vehicles.count { it.statusEnum() == VehicleStatus.INACTIVE }
        val maintenanceVehicles = vehicles.count { it.statusEnum() == VehicleStatus.MAINTENANCE }

        val tripDistance = trips.sumOf { it.actualDistanceKm }
        val fuelDistance = fuelTransactions.sumOf { it.distanceSinceLastKm }
        val totalDistance = max(tripDistance, fuelDistance)

        val totalFuelLiters = fuelTransactions.sumOf { it.liters }
        val avgKmPerL = if (totalFuelLiters > 0.0 && fuelDistance > 0.0) {
            fuelDistance / totalFuelLiters
        } else if (vehicles.isNotEmpty()) {
            vehicles.map { it.baselineKmPerLiter }.average()
        } else 0.0

        val avgL100 = if (avgKmPerL > 0.0) 100.0 / avgKmPerL else 0.0
        val totalFuelSpend = fuelTransactions.sumOf { it.costAmount }
        val totalMaintSpend = maintenance.sumOf { it.costAmount }
        val expenseTotal = expenses.sumOf { it.amount }
        val totalOpex = if (expenseTotal > 0.0) expenseTotal else (totalFuelSpend + totalMaintSpend)
        val costPerKm = if (totalDistance > 0.0) totalOpex / totalDistance else 0.0

        val utilisationPct = if (totalFleet > 0) {
            (activeVehicles.toDouble() / totalFleet.toDouble()) * 100.0
        } else 0.0

        val outstandingMaint = maintenance.count {
            it.statusEnum() != MaintenanceStatus.COMPLETED
        }
        val criticalAlerts = riskAlerts.count {
            it.resolutionEnum() != RiskResolutionStatus.RESOLVED &&
                it.resolutionEnum() != RiskResolutionStatus.DISMISSED &&
                (it.severityEnum() == RiskSeverity.CRITICAL || it.severityEnum() == RiskSeverity.HIGH)
        }
        val activeShipments = shipments.count {
            val st = it.statusEnum()
            st == ShipmentStatus.DISPATCHED || st == ShipmentStatus.IN_TRANSIT || st == ShipmentStatus.DELAYED
        }
        val deliveredShipments = shipments.count { it.statusEnum() == ShipmentStatus.DELIVERED }

        return FleetExecutiveSummary(
            totalFleet = totalFleet,
            activeVehicles = activeVehicles,
            inactiveVehicles = inactiveVehicles,
            maintenanceVehicles = maintenanceVehicles,
            totalDistanceKm = totalDistance,
            totalFuelLiters = totalFuelLiters,
            avgKmPerLiter = avgKmPerL,
            avgLitersPer100Km = avgL100,
            totalFuelSpend = totalFuelSpend,
            totalMaintenanceSpend = totalMaintSpend,
            totalOpex = totalOpex,
            costPerKm = costPerKm,
            fleetUtilisationPct = utilisationPct,
            outstandingMaintenanceCount = outstandingMaint,
            openCriticalAlertsCount = criticalAlerts,
            activeShipmentsCount = activeShipments,
            deliveredShipmentsCount = deliveredShipments,
        )
    }

    /**
     * Statistical Fuel Anomaly Detection:
     * 1. Z-Score against vehicle historical mean/stdDev (or fleet baseline when N < 3).
     * 2. Tabular CUSUM (k = 0.5*sigma, h = 4.0*sigma) for sustained drift detection.
     * 3. Tank capacity validation (> 102% rated tank).
     * 4. Duplicate transaction detection (< 90 min same vehicle or duplicate reference).
     * 5. Potential leakage / sharp efficiency drop (> 25% below baseline).
     *
     * Never classifies as theft automatically — outputs Investigation Signals with evidence & confidence.
     */
    fun analyzeFuelIntelligence(
        vehicles: List<Vehicle>,
        transactions: List<FuelTransaction>,
    ): Pair<List<FuelAnomalySignal>, List<CusumPoint>> {
        if (transactions.isEmpty()) return emptyList<FuelAnomalySignal>() to emptyList()

        val vehicleMap = vehicles.associateBy { it.id }
        val sortedTx = transactions.sortedBy { it.txTimestampMillis }
        val signals = mutableListOf<FuelAnomalySignal>()
        val cusumPoints = mutableListOf<CusumPoint>()

        // Duplicate reference check across all transactions
        val refCounts = sortedTx.groupingBy { it.transactionRef.trim().uppercase(Locale.US) }.eachCount()

        val groupedByVehicle = sortedTx.groupBy { it.vehicleId }
        groupedByVehicle.forEach { (vehicleId, vTxs) ->
            val vehicle = vehicleMap[vehicleId]
            val validEfficiencies = vTxs.filter { it.distanceSinceLastKm > 0.0 && it.liters > 0.0 }
                .map { it.kmPerLiter }

            val meanEff = when {
                validEfficiencies.size >= 3 -> validEfficiencies.average()
                vehicle != null && vehicle.baselineKmPerLiter > 0.0 -> vehicle.baselineKmPerLiter
                validEfficiencies.isNotEmpty() -> validEfficiencies.average()
                else -> 3.0
            }

            val variance = if (validEfficiencies.size >= 2) {
                validEfficiencies.map { (it - meanEff).pow(2) }.sum() / (validEfficiencies.size - 1)
            } else {
                (meanEff * 0.12).pow(2)
            }
            val stdDev = max(sqrt(variance), 0.15)
            val k = 0.5 * stdDev
            val h = 4.0 * stdDev

            var cPos = 0.0
            var cNeg = 0.0

            vTxs.forEachIndexed { idx, tx ->
                val eff = if (tx.liters > 0.0 && tx.distanceSinceLastKm > 0.0) tx.kmPerLiter else meanEff
                val zScore = (eff - meanEff) / stdDev

                cPos = max(0.0, cPos + (eff - meanEff - k))
                cNeg = max(0.0, cNeg + (meanEff - k - eff))

                cusumPoints += CusumPoint(
                    label = tx.txDateLabel.ifBlank { "T${idx + 1}" },
                    vehicleReg = tx.vehicleReg,
                    actualKmPerL = eff,
                    baselineKmPerL = meanEff,
                    cusumNeg = cNeg,
                    cusumPos = cPos,
                    threshold = h,
                )

                // Check 1: Tank capacity validation
                if (vehicle != null && vehicle.tankCapacityLiters > 0.0 &&
                    tx.liters > vehicle.tankCapacityLiters * 1.02
                ) {
                    signals += FuelAnomalySignal(
                        transactionId = tx.id,
                        vehicleId = tx.vehicleId,
                        vehicleReg = tx.vehicleReg,
                        driverName = tx.driverName,
                        signalCategory = "Tank Capacity Exceeded",
                        severity = RiskSeverity.CRITICAL,
                        zScore = zScore,
                        cusumNegative = cNeg,
                        cusumPositive = cPos,
                        observedKmPerL = eff,
                        expectedKmPerL = meanEff,
                        liters = tx.liters,
                        confidenceScore = 0.96,
                        evidenceSummary = String.format(
                            Locale.US,
                            "Recorded %.1f L exceeds rated tank capacity of %.1f L (+%.1f L overage) at %s (Ref %s).",
                            tx.liters,
                            vehicle.tankCapacityLiters,
                            tx.liters - vehicle.tankCapacityLiters,
                            tx.stationName,
                            tx.transactionRef
                        ),
                        recommendedInvestigation = "Verify station pump receipt, check for split-fill billing or secondary container dispensing. Do not assume misconduct before odometer audit.",
                    )
                }

                // Check 2: Duplicate transaction detection
                val refKey = tx.transactionRef.trim().uppercase(Locale.US)
                val prevTx = if (idx > 0) vTxs[idx - 1] else null
                val nearTimeDuplicate = prevTx != null &&
                    abs(tx.txTimestampMillis - prevTx.txTimestampMillis) in 1L..5_400_000L &&
                    abs(tx.liters - prevTx.liters) < 2.0
                if ((refCounts[refKey] ?: 0) > 1 || nearTimeDuplicate) {
                    signals += FuelAnomalySignal(
                        transactionId = tx.id,
                        vehicleId = tx.vehicleId,
                        vehicleReg = tx.vehicleReg,
                        driverName = tx.driverName,
                        signalCategory = "Potential Duplicate Fuel Transaction",
                        severity = RiskSeverity.HIGH,
                        zScore = zScore,
                        cusumNegative = cNeg,
                        cusumPositive = cPos,
                        observedKmPerL = eff,
                        expectedKmPerL = meanEff,
                        liters = tx.liters,
                        confidenceScore = 0.91,
                        evidenceSummary = "Ref ${tx.transactionRef} (${tx.liters} L at ${tx.stationName}) matches a near-simultaneous or duplicate reference entry.",
                        recommendedInvestigation = "Cross-check fuel card batch settlement and POS timestamps to confirm whether two separate fills occurred or if an invoice was double-posted.",
                    )
                }

                // Check 3: Statistical Z-score & CUSUM / possible leakage indicator
                val dropPct = if (meanEff > 0.0) ((meanEff - eff) / meanEff) * 100.0 else 0.0
                if (zScore <= -1.9 || cNeg >= h || dropPct >= 25.0) {
                    val severity = when {
                        zScore <= -2.8 || dropPct >= 38.0 -> RiskSeverity.CRITICAL
                        zScore <= -2.2 || cNeg >= h -> RiskSeverity.HIGH
                        else -> RiskSeverity.MEDIUM
                    }
                    val confidence = (0.65 + minOf(0.32, abs(zScore) * 0.09)).coerceIn(0.65, 0.97)
                    val category = if (dropPct >= 30.0) {
                        "Abnormal Consumption / Possible Fuel Leakage Indicator"
                    } else {
                        "Statistical Fuel Efficiency Deviation (Z-Score & CUSUM)"
                    }
                    signals += FuelAnomalySignal(
                        transactionId = tx.id,
                        vehicleId = tx.vehicleId,
                        vehicleReg = tx.vehicleReg,
                        driverName = tx.driverName,
                        signalCategory = category,
                        severity = severity,
                        zScore = zScore,
                        cusumNegative = cNeg,
                        cusumPositive = cPos,
                        observedKmPerL = eff,
                        expectedKmPerL = meanEff,
                        liters = tx.liters,
                        confidenceScore = confidence,
                        evidenceSummary = String.format(
                            Locale.US,
                            "Observed %.2f km/L vs baseline %.2f km/L (Z=%.2f, CUSUM-=%.2f / h=%.2f, %.1f%% below baseline) on route %s.",
                            eff, meanEff, zScore, cNeg, h, dropPct, tx.routeName
                        ),
                        recommendedInvestigation = "Inspect fuel lines/tank seal for leakage, verify payload weight/grade on ${tx.routeName}, and reconcile odometer logs before escalating.",
                    )
                } else if (zScore >= 2.2) {
                    signals += FuelAnomalySignal(
                        transactionId = tx.id,
                        vehicleId = tx.vehicleId,
                        vehicleReg = tx.vehicleReg,
                        driverName = tx.driverName,
                        signalCategory = "Unusually High Efficiency / Odometer Check",
                        severity = RiskSeverity.LOW,
                        zScore = zScore,
                        cusumNegative = cNeg,
                        cusumPositive = cPos,
                        observedKmPerL = eff,
                        expectedKmPerL = meanEff,
                        liters = tx.liters,
                        confidenceScore = 0.74,
                        evidenceSummary = String.format(
                            Locale.US,
                            "Observed %.2f km/L vs baseline %.2f km/L (Z=+%.2f). Possible partial fill or unlogged prior refuel.",
                            eff, meanEff, zScore
                        ),
                        recommendedInvestigation = "Verify whether tank was topped up completely or if an external cash refuel was omitted.",
                    )
                }
            }
        }

        return signals.sortedByDescending { it.severity.ordinal * 10 + abs(it.zScore) } to cusumPoints
    }

    /**
     * Ordinary Least Squares Linear Trend + Residual Confidence Forecast.
     * Separates observed history from statistical projections and reports R^2 and MAE.
     */
    fun forecastSeries(
        labels: List<String>,
        values: List<Double>,
        horizonPeriods: Int = 3,
        futureLabels: List<String> = listOf("F+1", "F+2", "F+3"),
    ): ForecastSeriesResult {
        if (values.isEmpty()) {
            return ForecastSeriesResult(
                historicalLabels = emptyList(),
                historicalValues = emptyList(),
                forecastLabels = emptyList(),
                forecastValues = emptyList(),
                lowerBounds = emptyList(),
                upperBounds = emptyList(),
                slopePerPeriod = 0.0,
                rSquared = 0.0,
                mae = 0.0,
                modelDescription = "Insufficient historical data for regression forecast.",
            )
        }
        val n = values.size
        if (n == 1) {
            val v = values.first()
            return ForecastSeriesResult(
                historicalLabels = labels,
                historicalValues = values,
                forecastLabels = futureLabels.take(horizonPeriods),
                forecastValues = List(horizonPeriods) { v },
                lowerBounds = List(horizonPeriods) { v * 0.9 },
                upperBounds = List(horizonPeriods) { v * 1.1 },
                slopePerPeriod = 0.0,
                rSquared = 0.0,
                mae = 0.0,
                modelDescription = "Single-point persistence baseline (N=1; more observations required for OLS trend).",
            )
        }

        val xMean = (n - 1) / 2.0
        val yMean = values.average()
        var num = 0.0
        var den = 0.0
        for (i in 0 until n) {
            val dx = i - xMean
            num += dx * (values[i] - yMean)
            den += dx * dx
        }
        val slope = if (den > 0.0) num / den else 0.0
        val intercept = yMean - slope * xMean

        val fitted = (0 until n).map { i -> intercept + slope * i }
        val residuals = values.indices.map { i -> values[i] - fitted[i] }
        val ssRes = residuals.sumOf { it * it }
        val ssTot = values.sumOf { (it - yMean).pow(2) }
        val rSquared = if (ssTot > 1e-9) (1.0 - (ssRes / ssTot)).coerceIn(0.0, 1.0) else 1.0
        val mae = residuals.map { abs(it) }.average()
        val stdErr = if (n > 2) sqrt(ssRes / (n - 2)) else mae

        val forecastVals = mutableListOf<Double>()
        val lowers = mutableListOf<Double>()
        val uppers = mutableListOf<Double>()
        for (h in 1..horizonPeriods) {
            val x = (n - 1 + h).toDouble()
            val pred = max(0.0, intercept + slope * x)
            val margin = 1.96 * max(stdErr, pred * 0.05) * sqrt(1.0 + h * 0.15)
            forecastVals += pred
            lowers += max(0.0, pred - margin)
            uppers += pred + margin
        }

        return ForecastSeriesResult(
            historicalLabels = labels,
            historicalValues = values,
            forecastLabels = futureLabels.take(horizonPeriods),
            forecastValues = forecastVals,
            lowerBounds = lowers,
            upperBounds = uppers,
            slopePerPeriod = slope,
            rSquared = rSquared,
            mae = mae,
            modelDescription = String.format(
                Locale.US,
                "OLS Linear Trend (N=%d, slope=%+.1f/period, R²=%.2f, MAE=%.1f)",
                n, slope, rSquared, mae
            ),
        )
    }

    fun predictMaintenanceNeeds(
        vehicles: List<Vehicle>,
        maintenanceRecords: List<MaintenanceRecord>,
    ): List<MaintenancePrediction> {
        return vehicles.map { v ->
            val vehicleMaint = maintenanceRecords.filter { it.vehicleId == v.id }
            val nextDue = vehicleMaint
                .filter { it.nextDueMileageKm > 0.0 }
                .maxOfOrNull { it.nextDueMileageKm }
                ?: (((v.currentMileageKm / 15000.0).toInt() + 1) * 15000.0)
            val remaining = nextDue - v.currentMileageKm
            val breakdowns = vehicleMaint.count { it.typeEnum().name == "BREAKDOWN" }
            val overdueCount = vehicleMaint.count { it.statusEnum() == MaintenanceStatus.OVERDUE }

            val mileageUrgency = when {
                remaining <= 0 -> 55
                remaining <= 1500 -> 42
                remaining <= 3500 -> 26
                else -> 10
            }
            val score = (mileageUrgency + breakdowns * 18 + overdueCount * 20).coerceIn(5, 100)
            val note = when {
                remaining <= 0 -> "Overdue by ${abs(remaining).toInt()} km — immediate workshop slot recommended"
                remaining <= 1500 -> "Service imminent within ${remaining.toInt()} km"
                breakdowns > 0 -> "Elevated breakdown history ($breakdowns events); schedule diagnostic check"
                else -> "Nominal — ${remaining.toInt()} km until scheduled interval"
            }

            MaintenancePrediction(
                vehicleId = v.id,
                vehicleReg = v.registrationNumber,
                makeModel = v.makeModel,
                currentMileageKm = v.currentMileageKm,
                nextDueMileageKm = nextDue,
                remainingKm = remaining,
                urgencyScore = score,
                statusNote = note,
            )
        }.sortedByDescending { it.urgencyScore }
    }

    fun scoreDrivers(
        drivers: List<Driver>,
        fuelTransactions: List<FuelTransaction>,
    ): List<DriverPerformanceScore> {
        if (drivers.isEmpty()) return emptyList()
        val fleetAvgEff = drivers.mapNotNull {
            if (it.fuelUsedLiters > 0.0) it.kmPerLiter else null
        }.takeIf { it.isNotEmpty() }?.average() ?: 3.0

        val scored = drivers.map { d ->
            val driverTxs = fuelTransactions.filter { it.driverId == d.id }
            val actualKmPerL = if (driverTxs.isNotEmpty() && driverTxs.sumOf { it.liters } > 0.0) {
                driverTxs.sumOf { it.distanceSinceLastKm } / driverTxs.sumOf { it.liters }
            } else {
                d.kmPerLiter
            }
            val effRatio = if (fleetAvgEff > 0.0 && actualKmPerL > 0.0) {
                (actualKmPerL / fleetAvgEff).coerceIn(0.5, 1.3)
            } else 1.0

            val composite = (
                (d.onTimeDeliveryPct * 0.45) +
                    (effRatio * 35.0) -
                    (d.safetyEvents * 6.5) -
                    (d.maintenanceIncidents * 4.0) +
                    15.0
                ).coerceIn(0.0, 100.0)

            DriverPerformanceScore(
                driverId = d.id,
                driverName = d.fullName,
                driverCode = d.driverCode,
                assignedVehicleReg = d.assignedVehicleReg,
                distanceKm = d.distanceTravelledKm,
                kmPerLiter = actualKmPerL,
                drivingHours = d.drivingHours,
                onTimePct = d.onTimeDeliveryPct,
                safetyEvents = d.safetyEvents,
                maintenanceIncidents = d.maintenanceIncidents,
                compositeIndex = composite,
                rank = 0,
            )
        }.sortedByDescending { it.compositeIndex }

        return scored.mapIndexed { idx, item -> item.copy(rank = idx + 1) }
    }

    fun scoreOperationalVehicleRisks(
        vehicles: List<Vehicle>,
        fuelSignals: List<FuelAnomalySignal>,
        trips: List<TripRecord>,
        maintenancePredictions: List<MaintenancePrediction>,
        riskAlerts: List<RiskAlertRecord>,
    ): List<VehicleRiskScore> {
        val maintMap = maintenancePredictions.associateBy { it.vehicleId }
        return vehicles.map { v ->
            val vSignals = fuelSignals.filter { it.vehicleId == v.id }
            val vTrips = trips.filter { it.vehicleId == v.id }
            val vAlerts = riskAlerts.filter {
                it.entityId == v.id &&
                    it.resolutionEnum() != RiskResolutionStatus.RESOLVED &&
                    it.resolutionEnum() != RiskResolutionStatus.DISMISSED
            }
            val maintUrgency = maintMap[v.id]?.urgencyScore ?: 10

            val fuelPenalty = vSignals.sumOf {
                when (it.severity) {
                    RiskSeverity.CRITICAL -> 28
                    RiskSeverity.HIGH -> 18
                    RiskSeverity.MEDIUM -> 10
                    RiskSeverity.LOW -> 4
                }
            }.coerceAtMost(45)

            val routePenalty = vTrips.count {
                it.geofenceBreached || it.routeDeviationKm > 15.0 || it.statusEnum() == TripStatus.DEVIATED
            } * 14

            val alertPenalty = vAlerts.size * 10
            val totalRisk = (fuelPenalty + routePenalty.coerceAtMost(25) + (maintUrgency * 0.35).toInt() + alertPenalty.coerceAtMost(20))
                .coerceIn(5, 100)

            val tier = when {
                totalRisk >= 75 -> RiskSeverity.CRITICAL
                totalRisk >= 50 -> RiskSeverity.HIGH
                totalRisk >= 28 -> RiskSeverity.MEDIUM
                else -> RiskSeverity.LOW
            }

            val observed = "${vSignals.size} fuel signal(s), ${vTrips.count { it.routeDeviationKm > 10.0 }} route deviation(s), mileage ${v.currentMileageKm.toInt()} km."
            val estimate = "Composite Risk Index $totalRisk/100 (Maint urgency $maintUrgency/100; confidence 0.84)."
            val action = when (tier) {
                RiskSeverity.CRITICAL -> "Hold dispatch pending fuel line & odometer audit + workshop inspection."
                RiskSeverity.HIGH -> "Assign supervisor review on recent fuel cards and route deviations within 24h."
                RiskSeverity.MEDIUM -> "Schedule preventive diagnostic check before next long-haul assignment."
                RiskSeverity.LOW -> "Continue standard telematics monitoring."
            }

            VehicleRiskScore(
                vehicleId = v.id,
                vehicleReg = v.registrationNumber,
                makeModel = v.makeModel,
                riskScore = totalRisk,
                riskTier = tier,
                observedFacts = observed,
                statisticalEstimate = estimate,
                recommendedAction = action,
            )
        }.sortedByDescending { it.riskScore }
    }

    /**
     * Generates a strictly factual, mathematical snapshot for Ottilia AI so Gemini never invents operational figures.
     */
    fun buildGroundedTelemetryContext(
        companyName: String,
        currency: String,
        summary: FleetExecutiveSummary,
        vehicles: List<Vehicle>,
        drivers: List<Driver>,
        fuelTransactions: List<FuelTransaction>,
        fuelSignals: List<FuelAnomalySignal>,
        trips: List<TripRecord>,
        maintenancePredictions: List<MaintenancePrediction>,
        expenses: List<ExpenseRecord>,
        shipments: List<ShipmentRecord>,
        riskAlerts: List<RiskAlertRecord>,
    ): String = buildString {
        appendLine("=== AXIOM VERIFIED OPERATIONAL TELEMETRY ($companyName, Currency: $currency) ===")
        appendLine("Note: Only cite figures present below. If a figure is absent, state 'Insufficient operational data'.")
        appendLine()
        appendLine("[EXECUTIVE SUMMARY]")
        appendLine("- Total Fleet: ${summary.totalFleet} (Active: ${summary.activeVehicles}, Maintenance: ${summary.maintenanceVehicles}, Inactive: ${summary.inactiveVehicles})")
        appendLine("- Utilisation: ${"%.1f".format(Locale.US, summary.fleetUtilisationPct)}%")
        appendLine("- Total Distance: ${"%.1f".format(Locale.US, summary.totalDistanceKm)} km")
        appendLine("- Total Fuel: ${"%.1f".format(Locale.US, summary.totalFuelLiters)} L | Avg Efficiency: ${"%.2f".format(Locale.US, summary.avgKmPerLiter)} km/L (${"%.2f".format(Locale.US, summary.avgLitersPer100Km)} L/100km)")
        appendLine("- Total Fuel Spend: $currency ${"%.2f".format(Locale.US, summary.totalFuelSpend)} | Maintenance Spend: $currency ${"%.2f".format(Locale.US, summary.totalMaintenanceSpend)}")
        appendLine("- Total Operational Expenditure: $currency ${"%.2f".format(Locale.US, summary.totalOpex)} | Avg Cost/km: $currency ${"%.3f".format(Locale.US, summary.costPerKm)}")
        appendLine("- Open High/Critical Risk Alerts: ${summary.openCriticalAlertsCount} | Outstanding Maintenance: ${summary.outstandingMaintenanceCount}")
        appendLine()

        appendLine("[VEHICLES (${vehicles.size})]")
        vehicles.take(15).forEach { v ->
            val vFuel = fuelTransactions.filter { it.vehicleId == v.id }
            val liters = vFuel.sumOf { it.liters }
            val cost = vFuel.sumOf { it.costAmount }
            appendLine("- ${v.registrationNumber} (${v.makeModel}, ${v.status}): driver=${v.assignedDriverName.ifBlank { "Unassigned" }}, mileage=${v.currentMileageKm.toInt()} km, tank=${v.tankCapacityLiters}L, baseline=${v.baselineKmPerLiter} km/L, loggedFuel=${"%.1f".format(Locale.US, liters)}L ($currency ${"%.2f".format(Locale.US, cost)}), totalOpCost=$currency ${"%.2f".format(Locale.US, v.operationalCostTotal)}")
        }
        appendLine()

        appendLine("[DRIVERS (${drivers.size})]")
        drivers.take(15).forEach { d ->
            appendLine("- ${d.fullName} (${d.driverCode}, ${d.status}): vehicle=${d.assignedVehicleReg}, distance=${d.distanceTravelledKm.toInt()} km, fuel=${d.fuelUsedLiters.toInt()} L (${"%.2f".format(Locale.US, d.kmPerLiter)} km/L), hours=${d.drivingHours}, onTime=${d.onTimeDeliveryPct}%, safetyEvents=${d.safetyEvents}, maintIncidents=${d.maintenanceIncidents}")
        }
        appendLine()

        appendLine("[FUEL ANOMALY SIGNALS (${fuelSignals.size})]")
        fuelSignals.take(12).forEach { s ->
            appendLine("- [${s.severity.name}] ${s.vehicleReg} (${s.driverName}): ${s.signalCategory} | Z=${"%.2f".format(Locale.US, s.zScore)}, CUSUM-=${"%.2f".format(Locale.US, s.cusumNegative)}, observed=${"%.2f".format(Locale.US, s.observedKmPerL)} vs expected=${"%.2f".format(Locale.US, s.expectedKmPerL)} km/L, conf=${"%.0f".format(Locale.US, s.confidenceScore * 100)}% | Evidence: ${s.evidenceSummary}")
        }
        appendLine()

        appendLine("[ROUTES & TELEMETRY (${trips.size})]")
        trips.take(12).forEach { t ->
            appendLine("- ${t.routeName} (${t.vehicleReg}, ${t.status}): planned=${t.plannedDistanceKm}km, actual=${t.actualDistanceKm}km, deviation=${t.routeDeviationKm}km, idle=${t.idleMinutes}min, efficiency=${"%.1f".format(Locale.US, t.efficiencyPct)}%, geofenceBreach=${t.geofenceBreached}, simulated=${t.simulatedTelemetry}")
        }
        appendLine()

        appendLine("[MAINTENANCE PREDICTIONS]")
        maintenancePredictions.take(10).forEach { m ->
            appendLine("- ${m.vehicleReg}: urgency=${m.urgencyScore}/100, remaining=${m.remainingKm.toInt()} km -> ${m.statusNote}")
        }
        appendLine()

        appendLine("[EXPENSE BREAKDOWN]")
        ExpenseCategory.entries.forEach { cat ->
            val catRows = expenses.filter { it.categoryEnum() == cat }
            if (catRows.isNotEmpty()) {
                val spent = catRows.sumOf { it.amount }
                val budget = catRows.sumOf { it.budgetAllocated }
                appendLine("- ${cat.label}: spent=$currency ${"%.2f".format(Locale.US, spent)} vs budget=$currency ${"%.2f".format(Locale.US, budget)}")
            }
        }
        appendLine()

        appendLine("[LOGISTICS SHIPMENTS (${shipments.size})]")
        shipments.take(10).forEach { sh ->
            appendLine("- ${sh.trackingCode} (${sh.customerName}): ${sh.pickupLocation}->${sh.deliveryLocation}, status=${sh.status}, load=${"%.0f".format(Locale.US, sh.loadFactorPct)}%, margin=$currency ${"%.2f".format(Locale.US, sh.estimatedMargin)}")
        }
        appendLine()

        appendLine("[OPEN RISK ALERTS (${riskAlerts.size})]")
        riskAlerts.take(10).forEach { r ->
            appendLine("- [${r.severity}] ${r.entityLabel} (${r.category}, ${r.resolutionStatus}): ${r.supportingData} | Method: ${r.detectionMethod}")
        }
    }
}
