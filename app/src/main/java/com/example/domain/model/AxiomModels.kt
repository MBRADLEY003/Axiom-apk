package com.example.domain.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AxiomRole(val displayName: String, val summary: String) {
    SUPER_ADMIN("Super Administrator", "Full platform, company, RBAC, and operational control"),
    FLEET_MANAGER("Fleet Manager", "Vehicles, fuel intelligence, maintenance, and driver oversight"),
    OPERATIONS_MANAGER("Operations Manager", "Dispatch, GPS routes, geofences, and shipment lifecycle"),
    FINANCE_MANAGER("Finance Manager", "Budgets, expenditure forecasts, cost/km, and financial reports"),
    DATA_ANALYST("Data Analyst", "Statistical models, anomaly signals, Ottilia AI, and exports"),
    DRIVER("Driver", "Assigned vehicle trips, fuel logs, and delivery confirmations"),
    AUDITOR("Read-only Auditor", "Read-only compliance inspection across modules and audit logs");

    companion object {
        fun fromRaw(raw: String?): AxiomRole =
            entries.firstOrNull { it.name == raw } ?: SUPER_ADMIN
    }
}

enum class VehicleCategory(val label: String) {
    HEAVY_TRUCK("Heavy Truck"),
    LIGHT_VAN("Light Van"),
    REFRIGERATED("Refrigerated"),
    TANKER("Tanker"),
    FLATBED("Flatbed"),
    UTILITY("Utility");

    companion object {
        fun fromRaw(raw: String?): VehicleCategory =
            entries.firstOrNull { it.name == raw } ?: HEAVY_TRUCK
    }
}

enum class FuelType(val label: String) {
    DIESEL("Diesel"),
    PETROL("Petrol"),
    HYBRID("Hybrid"),
    ELECTRIC("Electric");

    companion object {
        fun fromRaw(raw: String?): FuelType =
            entries.firstOrNull { it.name == raw } ?: DIESEL
    }
}

enum class VehicleStatus(val label: String) {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    MAINTENANCE("Maintenance");

    companion object {
        fun fromRaw(raw: String?): VehicleStatus =
            entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}

enum class DriverStatus(val label: String) {
    ON_DUTY("On Duty"),
    OFF_DUTY("Off Duty"),
    SUSPENDED("Suspended");

    companion object {
        fun fromRaw(raw: String?): DriverStatus =
            entries.firstOrNull { it.name == raw } ?: ON_DUTY
    }
}

enum class TripStatus(val label: String) {
    SCHEDULED("Scheduled"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    DEVIATED("Deviated");

    companion object {
        fun fromRaw(raw: String?): TripStatus =
            entries.firstOrNull { it.name == raw } ?: COMPLETED
    }
}

enum class MaintenanceType(val label: String) {
    PREVENTIVE("Preventive"),
    CORRECTIVE("Corrective"),
    BREAKDOWN("Breakdown");

    companion object {
        fun fromRaw(raw: String?): MaintenanceType =
            entries.firstOrNull { it.name == raw } ?: PREVENTIVE
    }
}

enum class MaintenanceStatus(val label: String) {
    SCHEDULED("Scheduled"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    OVERDUE("Overdue");

    companion object {
        fun fromRaw(raw: String?): MaintenanceStatus =
            entries.firstOrNull { it.name == raw } ?: SCHEDULED
    }
}

enum class ExpenseCategory(val label: String) {
    FUEL("Fuel"),
    MAINTENANCE("Maintenance"),
    DRIVER("Driver"),
    INSURANCE("Insurance"),
    LICENSING("Licensing"),
    OPERATIONAL("Operational");

    companion object {
        fun fromRaw(raw: String?): ExpenseCategory =
            entries.firstOrNull { it.name == raw } ?: OPERATIONAL
    }
}

enum class ShipmentStatus(val label: String) {
    REGISTERED("Registered"),
    DISPATCHED("Dispatched"),
    IN_TRANSIT("In Transit"),
    DELIVERED("Delivered"),
    DELAYED("Delayed");

    companion object {
        fun fromRaw(raw: String?): ShipmentStatus =
            entries.firstOrNull { it.name == raw } ?: REGISTERED
    }
}

enum class RiskSeverity(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical");

    companion object {
        fun fromRaw(raw: String?): RiskSeverity =
            entries.firstOrNull { it.name == raw } ?: MEDIUM
    }
}

enum class RiskResolutionStatus(val label: String) {
    OPEN("Open"),
    INVESTIGATING("Investigating"),
    RESOLVED("Resolved"),
    DISMISSED("Dismissed");

    companion object {
        fun fromRaw(raw: String?): RiskResolutionStatus =
            entries.firstOrNull { it.name == raw } ?: OPEN
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val activeCompanyId: String = "comp_apex",
    val activeRole: String = AxiomRole.SUPER_ADMIN.name,
    val pinHash: String? = null,
    val sessionTimeoutMinutes: Long = 60L,
    val emailAlertsEnabled: Boolean = true,
    val smsAlertsEnabled: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun roleEnum(): AxiomRole = AxiomRole.fromRaw(activeRole)

    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("displayName", displayName.take(100).ifBlank { "AXIOM Operator" })
        put("email", email.take(150).ifBlank { "operator@axiom.io" })
        put("activeCompanyId", activeCompanyId.take(128).ifBlank { "comp_apex" })
        put("activeRole", roleEnum().name)
        if (!pinHash.isNullOrBlank()) put("pinHash", pinHash.take(128))
        put("sessionTimeoutMinutes", sessionTimeoutMinutes.coerceIn(5L, 480L))
        put("emailAlertsEnabled", emailAlertsEnabled)
        put("smsAlertsEnabled", smsAlertsEnabled)
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class Company(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val name: String = "",
    val industry: String = "Heavy Freight & Logistics",
    val currency: String = "USD",
    val demoData: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "companyId" to companyId.take(128),
        "name" to name.take(120),
        "industry" to industry.take(80),
        "currency" to currency.take(10),
        "demoData" to demoData,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class TeamMember(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = AxiomRole.FLEET_MANAGER.name,
    val department: String = "Operations",
    val activeStatus: String = "ACTIVE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun roleEnum(): AxiomRole = AxiomRole.fromRaw(role)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "companyId" to companyId.take(128),
        "fullName" to fullName.take(100),
        "email" to email.take(150),
        "role" to roleEnum().name,
        "department" to department.take(80),
        "activeStatus" to if (activeStatus == "SUSPENDED") "SUSPENDED" else "ACTIVE",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class Vehicle(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val registrationNumber: String = "",
    val vin: String = "",
    val makeModel: String = "",
    val category: String = VehicleCategory.HEAVY_TRUCK.name,
    val assignedDriverId: String = "",
    val assignedDriverName: String = "",
    val fuelType: String = FuelType.DIESEL.name,
    val tankCapacityLiters: Double = 500.0,
    val currentMileageKm: Double = 0.0,
    val status: String = VehicleStatus.ACTIVE.name,
    val purchaseDate: String = "",
    val insuranceExpiry: String = "",
    val licenseExpiry: String = "",
    val baselineKmPerLiter: Double = 3.2,
    val operationalCostTotal: Double = 0.0,
    val demoData: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun categoryEnum() = VehicleCategory.fromRaw(category)
    fun fuelTypeEnum() = FuelType.fromRaw(fuelType)
    fun statusEnum() = VehicleStatus.fromRaw(status)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "companyId" to companyId.take(128),
        "registrationNumber" to registrationNumber.take(30),
        "vin" to vin.take(32),
        "makeModel" to makeModel.take(100),
        "category" to categoryEnum().name,
        "assignedDriverId" to assignedDriverId.take(128),
        "assignedDriverName" to assignedDriverName.take(100),
        "fuelType" to fuelTypeEnum().name,
        "tankCapacityLiters" to tankCapacityLiters.coerceIn(0.0, 5000.0),
        "currentMileageKm" to currentMileageKm.coerceIn(0.0, 10000000.0),
        "status" to statusEnum().name,
        "purchaseDate" to purchaseDate.take(30),
        "insuranceExpiry" to insuranceExpiry.take(30),
        "licenseExpiry" to licenseExpiry.take(30),
        "baselineKmPerLiter" to baselineKmPerLiter.coerceIn(0.1, 100.0),
        "operationalCostTotal" to operationalCostTotal.coerceIn(0.0, 100000000.0),
        "demoData" to demoData,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class Driver(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val driverCode: String = "",
    val fullName: String = "",
    val licenseNumber: String = "",
    val assignedVehicleId: String = "",
    val assignedVehicleReg: String = "",
    val distanceTravelledKm: Double = 0.0,
    val fuelUsedLiters: Double = 0.0,
    val drivingHours: Double = 0.0,
    val maintenanceIncidents: Long = 0L,
    val safetyEvents: Long = 0L,
    val onTimeDeliveryPct: Double = 100.0,
    val status: String = DriverStatus.ON_DUTY.name,
    val demoData: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun statusEnum() = DriverStatus.fromRaw(status)
    val kmPerLiter: Double
        get() = if (fuelUsedLiters > 0.0) distanceTravelledKm / fuelUsedLiters else 0.0

    fun toCreateMap(): Map<String, Any> = mapOf(
        "companyId" to companyId.take(128),
        "driverCode" to driverCode.take(30),
        "fullName" to fullName.take(100),
        "licenseNumber" to licenseNumber.take(50),
        "assignedVehicleId" to assignedVehicleId.take(128),
        "assignedVehicleReg" to assignedVehicleReg.take(30),
        "distanceTravelledKm" to distanceTravelledKm.coerceIn(0.0, 10000000.0),
        "fuelUsedLiters" to fuelUsedLiters.coerceIn(0.0, 5000000.0),
        "drivingHours" to drivingHours.coerceIn(0.0, 500000.0),
        "maintenanceIncidents" to maintenanceIncidents.coerceIn(0L, 10000L),
        "safetyEvents" to safetyEvents.coerceIn(0L, 10000L),
        "onTimeDeliveryPct" to onTimeDeliveryPct.coerceIn(0.0, 100.0),
        "status" to statusEnum().name,
        "demoData" to demoData,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class FuelTransaction(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val vehicleId: String = "",
    val vehicleReg: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val routeName: String = "",
    val liters: Double = 0.0,
    val costAmount: Double = 0.0,
    val odometerKm: Double = 0.0,
    val distanceSinceLastKm: Double = 0.0,
    val fuelType: String = FuelType.DIESEL.name,
    val stationName: String = "",
    val transactionRef: String = "",
    val txDateLabel: String = "",
    val txTimestampMillis: Long = 0L,
    val demoData: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val kmPerLiter: Double
        get() = if (liters > 0.0) distanceSinceLastKm / liters else 0.0
    val litersPer100Km: Double
        get() = if (distanceSinceLastKm > 0.0) (liters / distanceSinceLastKm) * 100.0 else 0.0
    val litersPerKm: Double
        get() = if (distanceSinceLastKm > 0.0) liters / distanceSinceLastKm else 0.0

    fun toCreateMap(): Map<String, Any> = mapOf(
        "companyId" to companyId.take(128),
        "vehicleId" to vehicleId.take(128),
        "vehicleReg" to vehicleReg.take(30),
        "driverId" to driverId.take(128),
        "driverName" to driverName.take(100),
        "routeName" to routeName.take(120),
        "liters" to liters.coerceIn(0.1, 10000.0),
        "costAmount" to costAmount.coerceIn(0.0, 1000000.0),
        "odometerKm" to odometerKm.coerceIn(0.0, 10000000.0),
        "distanceSinceLastKm" to distanceSinceLastKm.coerceIn(0.0, 100000.0),
        "fuelType" to FuelType.fromRaw(fuelType).name,
        "stationName" to stationName.take(120),
        "transactionRef" to transactionRef.take(80),
        "txDateLabel" to txDateLabel.take(32),
        "txTimestampMillis" to txTimestampMillis.coerceAtLeast(0L),
        "demoData" to demoData,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class TripRecord(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val vehicleId: String = "",
    val vehicleReg: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val routeName: String = "",
    val originName: String = "",
    val destinationName: String = "",
    val plannedDistanceKm: Double = 0.0,
    val actualDistanceKm: Double = 0.0,
    val durationMinutes: Long = 0L,
    val idleMinutes: Long = 0L,
    val stopCount: Long = 0L,
    val routeDeviationKm: Double = 0.0,
    val geofenceBreached: Boolean = false,
    val simulatedTelemetry: Boolean = false,
    val startLat: Double = 0.0,
    val startLng: Double = 0.0,
    val endLat: Double = 0.0,
    val endLng: Double = 0.0,
    val status: String = TripStatus.COMPLETED.name,
    val startedAtMillis: Long = 0L,
    val demoData: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun statusEnum() = TripStatus.fromRaw(status)

    val efficiencyPct: Double
        get() = if (actualDistanceKm > 0.0 && plannedDistanceKm > 0.0) {
            ((plannedDistanceKm / actualDistanceKm) * 100.0).coerceIn(0.0, 100.0)
        } else 100.0

    fun toCreateMap(): Map<String, Any> = mapOf(
        "companyId" to companyId.take(128),
        "vehicleId" to vehicleId.take(128),
        "vehicleReg" to vehicleReg.take(30),
        "driverId" to driverId.take(128),
        "driverName" to driverName.take(100),
        "routeName" to routeName.take(120),
        "originName" to originName.take(100),
        "destinationName" to destinationName.take(100),
        "plannedDistanceKm" to plannedDistanceKm.coerceIn(0.0, 100000.0),
        "actualDistanceKm" to actualDistanceKm.coerceIn(0.0, 100000.0),
        "durationMinutes" to durationMinutes.coerceIn(0L, 100000L),
        "idleMinutes" to idleMinutes.coerceIn(0L, 100000L),
        "stopCount" to stopCount.coerceIn(0L, 1000L),
        "routeDeviationKm" to routeDeviationKm.coerceIn(0.0, 50000.0),
        "geofenceBreached" to geofenceBreached,
        "simulatedTelemetry" to simulatedTelemetry,
        "startLat" to startLat.coerceIn(-90.0, 90.0),
        "startLng" to startLng.coerceIn(-180.0, 180.0),
        "endLat" to endLat.coerceIn(-90.0, 90.0),
        "endLng" to endLng.coerceIn(-180.0, 180.0),
        "status" to statusEnum().name,
        "startedAtMillis" to startedAtMillis.coerceAtLeast(0L),
        "demoData" to demoData,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class MaintenanceRecord(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val vehicleId: String = "",
    val vehicleReg: String = "",
    val maintenanceType: String = MaintenanceType.PREVENTIVE.name,
    val title: String = "",
    val description: String = "",
    val serviceDate: String = "",
    val mileageAtServiceKm: Double = 0.0,
    val nextDueMileageKm: Double = 0.0,
    val costAmount: Double = 0.0,
    val spareParts: String = "",
    val downtimeHours: Double = 0.0,
    val status: String = MaintenanceStatus.SCHEDULED.name,
    val demoData: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun typeEnum() = MaintenanceType.fromRaw(maintenanceType)
    fun statusEnum() = MaintenanceStatus.fromRaw(status)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "companyId" to companyId.take(128),
        "vehicleId" to vehicleId.take(128),
        "vehicleReg" to vehicleReg.take(30),
        "maintenanceType" to typeEnum().name,
        "title" to title.take(140),
        "description" to description.take(500),
        "serviceDate" to serviceDate.take(32),
        "mileageAtServiceKm" to mileageAtServiceKm.coerceIn(0.0, 10000000.0),
        "nextDueMileageKm" to nextDueMileageKm.coerceIn(0.0, 10000000.0),
        "costAmount" to costAmount.coerceIn(0.0, 10000000.0),
        "spareParts" to spareParts.take(300),
        "downtimeHours" to downtimeHours.coerceIn(0.0, 10000.0),
        "status" to statusEnum().name,
        "demoData" to demoData,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class ExpenseRecord(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val vehicleId: String = "",
    val vehicleReg: String = "",
    val driverId: String = "",
    val category: String = ExpenseCategory.OPERATIONAL.name,
    val amount: Double = 0.0,
    val budgetAllocated: Double = 0.0,
    val description: String = "",
    val monthKey: String = "",
    val demoData: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun categoryEnum() = ExpenseCategory.fromRaw(category)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "companyId" to companyId.take(128),
        "vehicleId" to vehicleId.take(128),
        "vehicleReg" to vehicleReg.take(30),
        "driverId" to driverId.take(128),
        "category" to categoryEnum().name,
        "amount" to amount.coerceIn(0.0, 100000000.0),
        "budgetAllocated" to budgetAllocated.coerceIn(0.0, 100000000.0),
        "description" to description.take(300),
        "monthKey" to monthKey.take(20),
        "demoData" to demoData,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class ShipmentRecord(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val trackingCode: String = "",
    val customerName: String = "",
    val customerContact: String = "",
    val cargoDescription: String = "",
    val weightKg: Double = 0.0,
    val capacityKg: Double = 1000.0,
    val pickupLocation: String = "",
    val deliveryLocation: String = "",
    val vehicleId: String = "",
    val vehicleReg: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val revenueAmount: Double = 0.0,
    val tripCostEstimate: Double = 0.0,
    val status: String = ShipmentStatus.REGISTERED.name,
    val scheduledDate: String = "",
    val demoData: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun statusEnum() = ShipmentStatus.fromRaw(status)
    val loadFactorPct: Double
        get() = if (capacityKg > 0.0) ((weightKg / capacityKg) * 100.0).coerceIn(0.0, 100.0) else 0.0
    val estimatedMargin: Double
        get() = revenueAmount - tripCostEstimate

    fun toCreateMap(): Map<String, Any> = mapOf(
        "companyId" to companyId.take(128),
        "trackingCode" to trackingCode.take(50),
        "customerName" to customerName.take(120),
        "customerContact" to customerContact.take(100),
        "cargoDescription" to cargoDescription.take(250),
        "weightKg" to weightKg.coerceIn(0.0, 500000.0),
        "capacityKg" to capacityKg.coerceIn(1.0, 500000.0),
        "pickupLocation" to pickupLocation.take(120),
        "deliveryLocation" to deliveryLocation.take(120),
        "vehicleId" to vehicleId.take(128),
        "vehicleReg" to vehicleReg.take(30),
        "driverId" to driverId.take(128),
        "driverName" to driverName.take(100),
        "revenueAmount" to revenueAmount.coerceIn(0.0, 100000000.0),
        "tripCostEstimate" to tripCostEstimate.coerceIn(0.0, 100000000.0),
        "status" to statusEnum().name,
        "scheduledDate" to scheduledDate.take(32),
        "demoData" to demoData,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class RiskAlertRecord(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val category: String = "",
    val severity: String = RiskSeverity.MEDIUM.name,
    val entityType: String = "VEHICLE",
    val entityId: String = "",
    val entityLabel: String = "",
    val supportingData: String = "",
    val detectionMethod: String = "",
    val historicalComparison: String = "",
    val recommendedInvestigation: String = "",
    val confidenceScore: Double = 0.85,
    val resolutionStatus: String = RiskResolutionStatus.OPEN.name,
    val resolutionNotes: String = "",
    val demoData: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun severityEnum() = RiskSeverity.fromRaw(severity)
    fun resolutionEnum() = RiskResolutionStatus.fromRaw(resolutionStatus)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "companyId" to companyId.take(128),
        "category" to category.take(80),
        "severity" to severityEnum().name,
        "entityType" to entityType.take(30),
        "entityId" to entityId.take(128),
        "entityLabel" to entityLabel.take(120),
        "supportingData" to supportingData.take(600),
        "detectionMethod" to detectionMethod.take(160),
        "historicalComparison" to historicalComparison.take(400),
        "recommendedInvestigation" to recommendedInvestigation.take(500),
        "confidenceScore" to confidenceScore.coerceIn(0.0, 1.0),
        "resolutionStatus" to resolutionEnum().name,
        "resolutionNotes" to resolutionNotes.take(500),
        "demoData" to demoData,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class NotificationRecord(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val title: String = "",
    val message: String = "",
    val category: String = "",
    val severity: String = RiskSeverity.MEDIUM.name,
    val readStatus: Boolean = false,
    val channel: String = "IN_APP",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun severityEnum() = RiskSeverity.fromRaw(severity)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "companyId" to companyId.take(128),
        "title" to title.take(140),
        "message" to message.take(500),
        "category" to category.take(60),
        "severity" to severityEnum().name,
        "readStatus" to readStatus,
        "channel" to channel.take(30),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class AuditLogRecord(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val actorEmail: String = "",
    val actorRole: String = "",
    val action: String = "",
    val targetEntity: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "companyId" to companyId.take(128),
        "actorEmail" to actorEmail.take(150),
        "actorRole" to actorRole.take(50),
        "action" to action.take(120),
        "targetEntity" to targetEntity.take(120),
        "details" to details.take(500),
        "createdAt" to FieldValue.serverTimestamp(),
    )
}
