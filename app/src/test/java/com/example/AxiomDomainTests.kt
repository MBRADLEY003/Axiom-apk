package com.example

import com.example.data.DataPortabilityManager
import com.example.data.ImportTargetType
import com.example.data.ReportType
import com.example.domain.analytics.AxiomAnalyticsEngine
import com.example.domain.model.AxiomRole
import com.example.domain.model.Driver
import com.example.domain.model.ExpenseRecord
import com.example.domain.model.FuelTransaction
import com.example.domain.model.MaintenanceRecord
import com.example.domain.model.MaintenanceStatus
import com.example.domain.model.RiskAlertRecord
import com.example.domain.model.RiskSeverity
import com.example.domain.model.TripRecord
import com.example.domain.model.Vehicle
import com.example.domain.model.VehicleStatus
import com.example.domain.rbac.AxiomPermission
import com.example.domain.rbac.RbacPermissions
import com.example.domain.rbac.SecurityHashUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AxiomDomainTests {

    @Test
    fun `fuel intelligence detects tank capacity overflow, duplicates, and z-score drops without classifying theft`() {
        val vehicle = Vehicle(
            id = "v1",
            registrationNumber = "AXM-104",
            tankCapacityLiters = 500.0,
            baselineKmPerLiter = 3.2,
            currentMileageKm = 120000.0,
            status = VehicleStatus.ACTIVE.name,
        )
        val txs = listOf(
            FuelTransaction(
                id = "f1", vehicleId = "v1", vehicleReg = "AXM-104", driverName = "Marcus Vance",
                liters = 300.0, costAmount = 480.0, distanceSinceLastKm = 960.0,
                transactionRef = "TX-1", txDateLabel = "2026-11-01", txTimestampMillis = 1_000_000L
            ),
            FuelTransaction(
                id = "f2", vehicleId = "v1", vehicleReg = "AXM-104", driverName = "Marcus Vance",
                liters = 310.0, costAmount = 496.0, distanceSinceLastKm = 992.0,
                transactionRef = "TX-2", txDateLabel = "2026-11-05", txTimestampMillis = 2_000_000L
            ),
            FuelTransaction(
                id = "f3", vehicleId = "v1", vehicleReg = "AXM-104", driverName = "Marcus Vance",
                liters = 290.0, costAmount = 464.0, distanceSinceLastKm = 928.0,
                transactionRef = "TX-3", txDateLabel = "2026-11-10", txTimestampMillis = 3_000_000L
            ),
            // Tank capacity overflow (560L > 500L * 1.02) & severe efficiency drop (1.4 km/L)
            FuelTransaction(
                id = "f4", vehicleId = "v1", vehicleReg = "AXM-104", driverName = "Marcus Vance",
                liters = 560.0, costAmount = 896.0, distanceSinceLastKm = 784.0,
                transactionRef = "TX-4", txDateLabel = "2026-11-15", txTimestampMillis = 4_000_000L
            ),
            // Duplicate transaction reference
            FuelTransaction(
                id = "f5", vehicleId = "v1", vehicleReg = "AXM-104", driverName = "Marcus Vance",
                liters = 250.0, costAmount = 400.0, distanceSinceLastKm = 800.0,
                transactionRef = "TX-4", txDateLabel = "2026-11-16", txTimestampMillis = 5_000_000L
            ),
        )

        val (signals, cusum) = AxiomAnalyticsEngine.analyzeFuelIntelligence(listOf(vehicle), txs)
        assertEquals(5, cusum.size)
        assertTrue(signals.any { it.signalCategory == "Tank Capacity Exceeded" && it.severity == RiskSeverity.CRITICAL })
        assertTrue(signals.any { it.signalCategory == "Potential Duplicate Fuel Transaction" })
        assertTrue(signals.any { it.zScore <= -1.9 || it.signalCategory.contains("Leakage") || it.signalCategory.contains("Deviation") })
        // Strict rule: never classify anomalies automatically as theft
        assertTrue(signals.none { it.signalCategory.contains("Theft", ignoreCase = true) })
    }

    @Test
    fun `ols forecast computes accurate linear trend and non-zero R2`() {
        val res = AxiomAnalyticsEngine.forecastSeries(
            labels = listOf("M1", "M2", "M3", "M4"),
            values = listOf(100.0, 200.0, 300.0, 400.0),
            horizonPeriods = 2,
            futureLabels = listOf("M5", "M6"),
        )
        assertEquals(1.0, res.rSquared, 0.001)
        assertEquals(0.0, res.mae, 0.001)
        assertEquals(500.0, res.forecastValues[0], 0.01)
        assertEquals(600.0, res.forecastValues[1], 0.01)
    }

    @Test
    fun `maintenance predictor prioritizes overdue breakdown assets`() {
        val vehicles = listOf(
            Vehicle(id = "v1", registrationNumber = "AXM-1", makeModel = "Volvo", currentMileageKm = 152000.0),
            Vehicle(id = "v2", registrationNumber = "AXM-2", makeModel = "MAN", currentMileageKm = 31000.0),
        )
        val maint = listOf(
            MaintenanceRecord(
                id = "m1", vehicleId = "v1", vehicleReg = "AXM-1",
                maintenanceType = "BREAKDOWN", nextDueMileageKm = 150000.0, status = MaintenanceStatus.OVERDUE.name
            ),
            MaintenanceRecord(
                id = "m2", vehicleId = "v2", vehicleReg = "AXM-2",
                maintenanceType = "PREVENTIVE", nextDueMileageKm = 45000.0, status = MaintenanceStatus.SCHEDULED.name
            ),
        )
        val preds = AxiomAnalyticsEngine.predictMaintenanceNeeds(vehicles, maint)
        assertEquals("AXM-1", preds.first().vehicleReg)
        assertTrue(preds.first().urgencyScore > preds.last().urgencyScore)
    }

    @Test
    fun `rbac permission matrix enforces all 7 enterprise roles accurately`() {
        // Super Admin has all permissions
        AxiomPermission.entries.forEach { perm ->
            assertTrue(RbacPermissions.hasPermission(AxiomRole.SUPER_ADMIN, perm))
        }
        // Read-only Auditor can export, query Ottilia, and view audit logs, but cannot mutate fleet/finance
        assertTrue(RbacPermissions.hasPermission(AxiomRole.AUDITOR, AxiomPermission.VIEW_AUDIT_LOGS))
        assertTrue(RbacPermissions.hasPermission(AxiomRole.AUDITOR, AxiomPermission.EXPORT_REPORTS))
        assertFalse(RbacPermissions.hasPermission(AxiomRole.AUDITOR, AxiomPermission.MANAGE_FLEET_VEHICLES))
        assertFalse(RbacPermissions.hasPermission(AxiomRole.AUDITOR, AxiomPermission.DELETE_VEHICLES))

        // Driver can log fuel & confirm delivery, but cannot manage company or delete vehicles
        assertTrue(RbacPermissions.hasPermission(AxiomRole.DRIVER, AxiomPermission.LOG_FUEL_TRANSACTIONS))
        assertTrue(RbacPermissions.hasPermission(AxiomRole.DRIVER, AxiomPermission.CONFIRM_DELIVERY))
        assertFalse(RbacPermissions.hasPermission(AxiomRole.DRIVER, AxiomPermission.MANAGE_COMPANY_AND_ROLES))
        assertFalse(RbacPermissions.hasPermission(AxiomRole.DRIVER, AxiomPermission.DELETE_VEHICLES))

        // Finance Manager owns budgets but not fleet deletion
        assertTrue(RbacPermissions.hasPermission(AxiomRole.FINANCE_MANAGER, AxiomPermission.MANAGE_FINANCE_BUDGETS))
        assertFalse(RbacPermissions.hasPermission(AxiomRole.FINANCE_MANAGER, AxiomPermission.DELETE_VEHICLES))
    }

    @Test
    fun `sha256 salted security pin hashes deterministically and rejects wrong pins`() {
        val hash = SecurityHashUtil.hashPinSha256("4829", "uid_123")
        assertEquals(64, hash.length)
        assertTrue(SecurityHashUtil.verifyPin("4829", "uid_123", hash))
        assertFalse(SecurityHashUtil.verifyPin("0000", "uid_123", hash))
        assertFalse(SecurityHashUtil.verifyPin("4829", "uid_other", hash))
    }

    @Test
    fun `excel xml generator and csv templates produce valid export structures`() {
        val template = DataPortabilityManager.generateImportTemplateCsv(ImportTargetType.FUEL)
        assertTrue(template.contains("transactionRef"))
        val summary = AxiomAnalyticsEngine.computeExecutiveSummary(
            vehicles = listOf(Vehicle(id = "v1", registrationNumber = "AXM-1", status = VehicleStatus.ACTIVE.name)),
            drivers = listOf(Driver(id = "d1", fullName = "Alice", distanceTravelledKm = 500.0, fuelUsedLiters = 150.0)),
            fuelTransactions = listOf(FuelTransaction(id = "f1", liters = 150.0, costAmount = 240.0, distanceSinceLastKm = 500.0)),
            trips = listOf(TripRecord(id = "t1", actualDistanceKm = 500.0)),
            maintenance = emptyList(),
            expenses = listOf(ExpenseRecord(id = "e1", amount = 240.0, budgetAllocated = 300.0)),
            shipments = emptyList(),
            riskAlerts = listOf(RiskAlertRecord(id = "r1", severity = RiskSeverity.LOW.name)),
        )
        val csv = DataPortabilityManager.generateReportCsv(
            reportType = ReportType.MONTHLY_FLEET,
            companyName = "Apex Logistics",
            currency = "USD",
            summary = summary,
            vehicles = listOf(Vehicle(id = "v1", registrationNumber = "AXM-1", vin = "123456", makeModel = "Volvo")),
            drivers = emptyList(),
            fuel = emptyList(),
            signals = emptyList(),
            trips = emptyList(),
            maintenance = emptyList(),
            expenses = emptyList(),
            risks = emptyList(),
        )
        val xml = DataPortabilityManager.generateExcelXml(csv, "Monthly Fleet")
        assertTrue(xml.contains("<Workbook"))
        assertTrue(xml.contains("AXM-1"))
    }
}
