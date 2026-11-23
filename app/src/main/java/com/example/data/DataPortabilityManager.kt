package com.example.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.domain.analytics.FleetExecutiveSummary
import com.example.domain.analytics.FuelAnomalySignal
import com.example.domain.model.Driver
import com.example.domain.model.ExpenseRecord
import com.example.domain.model.FuelTransaction
import com.example.domain.model.FuelType
import com.example.domain.model.MaintenanceRecord
import com.example.domain.model.RiskAlertRecord
import com.example.domain.model.TripRecord
import com.example.domain.model.Vehicle
import com.example.domain.model.VehicleCategory
import com.example.domain.model.VehicleStatus
import java.io.OutputStream
import java.util.Locale

enum class ReportType(val label: String) {
    DAILY_FLEET("Daily Fleet Report"),
    WEEKLY_FLEET("Weekly Fleet Report"),
    MONTHLY_FLEET("Monthly Fleet Report"),
    FUEL_CONSUMPTION("Fuel Consumption & Anomalies"),
    DRIVER_PERFORMANCE("Driver Performance Report"),
    MAINTENANCE("Maintenance & Downtime Report"),
    FINANCIAL("Financial Expenditure Report"),
    RISK("Risk Intelligence Report"),
    ROUTE_EFFICIENCY("Route Efficiency Report"),
}

enum class ImportTargetType(val label: String, val headers: List<String>) {
    VEHICLES(
        "Vehicles",
        listOf("registrationNumber", "vin", "makeModel", "category", "fuelType", "tankCapacityLiters", "currentMileageKm", "status", "baselineKmPerLiter")
    ),
    DRIVERS(
        "Drivers",
        listOf("driverCode", "fullName", "licenseNumber", "assignedVehicleReg", "distanceTravelledKm", "fuelUsedLiters", "onTimeDeliveryPct")
    ),
    FUEL(
        "Fuel Records",
        listOf("vehicleReg", "driverName", "routeName", "liters", "costAmount", "odometerKm", "distanceSinceLastKm", "stationName", "transactionRef", "txDateLabel")
    ),
    TRIPS(
        "Trips",
        listOf("vehicleReg", "driverName", "routeName", "originName", "destinationName", "plannedDistanceKm", "actualDistanceKm", "durationMinutes", "idleMinutes")
    )
}

data class ImportPreviewRow(
    val rowNumber: Int,
    val rawColumns: List<String>,
    val isValid: Boolean,
    val isDuplicate: Boolean,
    val message: String,
)

data class ImportParseResult(
    val targetType: ImportTargetType,
    val rows: List<ImportPreviewRow>,
    val parsedVehicles: List<Vehicle> = emptyList(),
    val parsedDrivers: List<Driver> = emptyList(),
    val parsedFuel: List<FuelTransaction> = emptyList(),
    val parsedTrips: List<TripRecord> = emptyList(),
)

object DataPortabilityManager {

    fun generateImportTemplateCsv(targetType: ImportTargetType): String = buildString {
        appendLine(targetType.headers.joinToString(","))
        when (targetType) {
            ImportTargetType.VEHICLES ->
                appendLine("AXM-901,YV2RT40A8LB901122,Volvo FH 540,HEAVY_TRUCK,DIESEL,550,24500,ACTIVE,3.3")
            ImportTargetType.DRIVERS ->
                appendLine("DRV-205,Sofia Martinez,CDL-A-55201,AXM-901,6200,1880,98.2")
            ImportTargetType.FUEL ->
                appendLine("AXM-104,Marcus Vance,Northern Corridor A1,280.5,462.8,128730,895,Shell Hub 4,AXF-9921,2026-11-23")
            ImportTargetType.TRIPS ->
                appendLine("AXM-104,Marcus Vance,Northern Corridor A1,Chicago Hub,Minneapolis Depot,655,660,490,15")
        }
    }

    fun generateReportCsv(
        reportType: ReportType,
        companyName: String,
        currency: String,
        summary: FleetExecutiveSummary,
        vehicles: List<Vehicle>,
        drivers: List<Driver>,
        fuel: List<FuelTransaction>,
        signals: List<FuelAnomalySignal>,
        trips: List<TripRecord>,
        maintenance: List<MaintenanceRecord>,
        expenses: List<ExpenseRecord>,
        risks: List<RiskAlertRecord>,
    ): String = buildString {
        appendLine("AXIOM Enterprise Report,${escapeCsv(reportType.label)},Company,${escapeCsv(companyName)}")
        appendLine("Fleet Size,${summary.totalFleet},Active,${summary.activeVehicles},Total Distance (km),${"%.1f".format(Locale.US, summary.totalDistanceKm)},Total Opex ($currency),${"%.2f".format(Locale.US, summary.totalOpex)}")
        appendLine()
        when (reportType) {
            ReportType.DAILY_FLEET, ReportType.WEEKLY_FLEET, ReportType.MONTHLY_FLEET -> {
                appendLine("Registration,VIN,MakeModel,Category,Status,MileageKm,TankL,BaselineKmPerL,OpCost")
                vehicles.forEach { v ->
                    appendLine("${escapeCsv(v.registrationNumber)},${escapeCsv(v.vin)},${escapeCsv(v.makeModel)},${v.category},${v.status},${v.currentMileageKm},${v.tankCapacityLiters},${v.baselineKmPerLiter},${v.operationalCostTotal}")
                }
            }
            ReportType.FUEL_CONSUMPTION -> {
                appendLine("Date,Ref,Vehicle,Driver,Route,Liters,Cost,DistanceKm,KmPerL,Station")
                fuel.forEach { f ->
                    appendLine("${f.txDateLabel},${escapeCsv(f.transactionRef)},${escapeCsv(f.vehicleReg)},${escapeCsv(f.driverName)},${escapeCsv(f.routeName)},${f.liters},${f.costAmount},${f.distanceSinceLastKm},${"%.2f".format(Locale.US, f.kmPerLiter)},${escapeCsv(f.stationName)}")
                }
                appendLine()
                appendLine("AnomalySignal,Vehicle,Severity,ZScore,CUSUM-,Confidence,Evidence")
                signals.forEach { s ->
                    appendLine("${escapeCsv(s.signalCategory)},${escapeCsv(s.vehicleReg)},${s.severity},${"%.2f".format(Locale.US, s.zScore)},${"%.2f".format(Locale.US, s.cusumNegative)},${"%.2f".format(Locale.US, s.confidenceScore)},${escapeCsv(s.evidenceSummary)}")
                }
            }
            ReportType.DRIVER_PERFORMANCE -> {
                appendLine("Code,Driver,Vehicle,DistanceKm,FuelLiters,KmPerL,Hours,OnTimePct,SafetyEvents,MaintIncidents,Status")
                drivers.forEach { d ->
                    appendLine("${escapeCsv(d.driverCode)},${escapeCsv(d.fullName)},${escapeCsv(d.assignedVehicleReg)},${d.distanceTravelledKm},${d.fuelUsedLiters},${"%.2f".format(Locale.US, d.kmPerLiter)},${d.drivingHours},${d.onTimeDeliveryPct},${d.safetyEvents},${d.maintenanceIncidents},${d.status}")
                }
            }
            ReportType.MAINTENANCE -> {
                appendLine("Vehicle,Type,Title,ServiceDate,ServiceKm,NextDueKm,Cost,DowntimeHrs,Status")
                maintenance.forEach { m ->
                    appendLine("${escapeCsv(m.vehicleReg)},${m.maintenanceType},${escapeCsv(m.title)},${m.serviceDate},${m.mileageAtServiceKm},${m.nextDueMileageKm},${m.costAmount},${m.downtimeHours},${m.status}")
                }
            }
            ReportType.FINANCIAL -> {
                appendLine("Month,Category,Vehicle,Amount,Budget,Variance,Description")
                expenses.forEach { e ->
                    appendLine("${e.monthKey},${e.category},${escapeCsv(e.vehicleReg)},${e.amount},${e.budgetAllocated},${e.budgetAllocated - e.amount},${escapeCsv(e.description)}")
                }
            }
            ReportType.RISK -> {
                appendLine("Severity,Category,Entity,Method,Confidence,Status,Evidence,Recommendation")
                risks.forEach { r ->
                    appendLine("${r.severity},${escapeCsv(r.category)},${escapeCsv(r.entityLabel)},${escapeCsv(r.detectionMethod)},${r.confidenceScore},${r.resolutionStatus},${escapeCsv(r.supportingData)},${escapeCsv(r.recommendedInvestigation)}")
                }
            }
            ReportType.ROUTE_EFFICIENCY -> {
                appendLine("Route,Vehicle,Driver,PlannedKm,ActualKm,DeviationKm,IdleMin,EfficiencyPct,GeofenceBreach,Status")
                trips.forEach { t ->
                    appendLine("${escapeCsv(t.routeName)},${escapeCsv(t.vehicleReg)},${escapeCsv(t.driverName)},${t.plannedDistanceKm},${t.actualDistanceKm},${t.routeDeviationKm},${t.idleMinutes},${"%.1f".format(Locale.US, t.efficiencyPct)},${t.geofenceBreached},${t.status}")
                }
            }
        }
    }

    /**
     * Generates Excel-compatible XML Spreadsheet 2003 (.xls) that opens natively in Microsoft Excel & LibreOffice.
     */
    fun generateExcelXml(csvContent: String, sheetName: String): String {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        val safeSheet = sheetName.replace(Regex("[\\\\/?*\\[\\]]"), "").take(30).ifBlank { "AXIOM_Report" }
        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<?mso-application progid="Excel.Sheet"?>""")
            appendLine("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">""")
            appendLine("""  <Worksheet ss:Name="${escapeXml(safeSheet)}">""")
            appendLine("""    <Table>""")
            lines.forEach { line ->
                appendLine("      <Row>")
                parseCsvRow(line).forEach { cell ->
                    val num = cell.toDoubleOrNull()
                    if (num != null) {
                        appendLine("""        <Cell><Data ss:Type="Number">$num</Data></Cell>""")
                    } else {
                        appendLine("""        <Cell><Data ss:Type="String">${escapeXml(cell)}</Data></Cell>""")
                    }
                }
                appendLine("      </Row>")
            }
            appendLine("""    </Table>""")
            appendLine("""  </Worksheet>""")
            appendLine("""</Workbook>""")
        }
    }

    fun writeReportPdf(
        outputStream: OutputStream,
        reportType: ReportType,
        companyName: String,
        currency: String,
        summary: FleetExecutiveSummary,
        csvContent: String,
    ) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val lines = csvContent.lines().filter { it.isNotBlank() }

        val titlePaint = Paint().apply {
            color = Color.rgb(212, 175, 55)
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subtitlePaint = Paint().apply {
            color = Color.rgb(230, 230, 230)
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(35, 38, 45)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        val headerBgPaint = Paint().apply {
            color = Color.rgb(15, 18, 24)
        }

        var pageNumber = 1
        var lineIndex = 0

        while (lineIndex < lines.size || pageNumber == 1) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 96f, headerBgPaint)
            canvas.drawText("AXIOM • ${reportType.label}", 32f, 40f, titlePaint)
            canvas.drawText(
                "$companyName | Fleet: ${summary.totalFleet} | Opex: $currency ${"%.2f".format(Locale.US, summary.totalOpex)} | Page $pageNumber",
                32f,
                68f,
                subtitlePaint
            )

            var y = 128f
            while (lineIndex < lines.size && y < pageHeight - 48f) {
                val text = lines[lineIndex].take(92)
                canvas.drawText(text, 32f, y, bodyPaint)
                y += 16f
                lineIndex++
            }
            document.finishPage(page)
            pageNumber++
            if (lineIndex >= lines.size) break
        }

        document.writeTo(outputStream)
        document.close()
    }

    fun parseCsvImport(
        context: Context,
        uri: Uri,
        targetType: ImportTargetType,
        companyId: String,
        existingVehicles: List<Vehicle>,
        existingDrivers: List<Driver>,
        existingFuel: List<FuelTransaction>,
    ): ImportParseResult {
        val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: return ImportParseResult(targetType, emptyList())
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size <= 1) return ImportParseResult(targetType, emptyList())

        val previewRows = mutableListOf<ImportPreviewRow>()
        val vehicles = mutableListOf<Vehicle>()
        val drivers = mutableListOf<Driver>()
        val fuels = mutableListOf<FuelTransaction>()
        val trips = mutableListOf<TripRecord>()

        val existingRegs = existingVehicles.map { it.registrationNumber.uppercase(Locale.US) }.toSet()
        val existingDriverCodes = existingDrivers.map { it.driverCode.uppercase(Locale.US) }.toSet()
        val existingTxRefs = existingFuel.map { it.transactionRef.uppercase(Locale.US) }.toSet()

        lines.drop(1).forEachIndexed { idx, line ->
            val cols = parseCsvRow(line)
            val rowNum = idx + 2
            when (targetType) {
                ImportTargetType.VEHICLES -> {
                    val reg = cols.getOrNull(0).orEmpty().trim()
                    val vin = cols.getOrNull(1).orEmpty().trim()
                    val make = cols.getOrNull(2).orEmpty().trim()
                    val tank = cols.getOrNull(5)?.toDoubleOrNull()
                    val mileage = cols.getOrNull(6)?.toDoubleOrNull()
                    val baseline = cols.getOrNull(8)?.toDoubleOrNull() ?: 3.2
                    val dup = reg.uppercase(Locale.US) in existingRegs || vehicles.any { it.registrationNumber.equals(reg, true) }
                    val valid = reg.isNotBlank() && vin.length >= 5 && make.isNotBlank() && tank != null && mileage != null
                    previewRows += ImportPreviewRow(
                        rowNumber = rowNum,
                        rawColumns = cols,
                        isValid = valid,
                        isDuplicate = dup,
                        message = when {
                            !valid -> "Missing or invalid registration/VIN/tank/mileage"
                            dup -> "Duplicate registration $reg"
                            else -> "Ready ($reg • $make)"
                        }
                    )
                    if (valid && !dup) {
                        vehicles += Vehicle(
                            companyId = companyId,
                            registrationNumber = reg,
                            vin = vin,
                            makeModel = make,
                            category = VehicleCategory.fromRaw(cols.getOrNull(3)).name,
                            fuelType = FuelType.fromRaw(cols.getOrNull(4)).name,
                            tankCapacityLiters = tank,
                            currentMileageKm = mileage,
                            status = VehicleStatus.fromRaw(cols.getOrNull(7)).name,
                            baselineKmPerLiter = baseline,
                            demoData = false,
                        )
                    }
                }
                ImportTargetType.DRIVERS -> {
                    val code = cols.getOrNull(0).orEmpty().trim()
                    val name = cols.getOrNull(1).orEmpty().trim()
                    val license = cols.getOrNull(2).orEmpty().trim()
                    val dup = code.uppercase(Locale.US) in existingDriverCodes || drivers.any { it.driverCode.equals(code, true) }
                    val valid = code.isNotBlank() && name.isNotBlank() && license.isNotBlank()
                    previewRows += ImportPreviewRow(
                        rowNumber = rowNum, rawColumns = cols, isValid = valid, isDuplicate = dup,
                        message = if (!valid) "Invalid driver code/name/license" else if (dup) "Duplicate driver $code" else "Ready ($name)"
                    )
                    if (valid && !dup) {
                        drivers += Driver(
                            companyId = companyId,
                            driverCode = code,
                            fullName = name,
                            licenseNumber = license,
                            assignedVehicleReg = cols.getOrNull(3).orEmpty().trim(),
                            distanceTravelledKm = cols.getOrNull(4)?.toDoubleOrNull() ?: 0.0,
                            fuelUsedLiters = cols.getOrNull(5)?.toDoubleOrNull() ?: 0.0,
                            onTimeDeliveryPct = cols.getOrNull(6)?.toDoubleOrNull() ?: 95.0,
                            demoData = false,
                        )
                    }
                }
                ImportTargetType.FUEL -> {
                    val vReg = cols.getOrNull(0).orEmpty().trim()
                    val dName = cols.getOrNull(1).orEmpty().trim()
                    val route = cols.getOrNull(2).orEmpty().trim()
                    val liters = cols.getOrNull(3)?.toDoubleOrNull()
                    val cost = cols.getOrNull(4)?.toDoubleOrNull()
                    val odo = cols.getOrNull(5)?.toDoubleOrNull()
                    val dist = cols.getOrNull(6)?.toDoubleOrNull()
                    val station = cols.getOrNull(7).orEmpty().trim()
                    val ref = cols.getOrNull(8).orEmpty().trim()
                    val date = cols.getOrNull(9).orEmpty().trim().ifBlank { "2026-11-23" }
                    val dup = ref.uppercase(Locale.US) in existingTxRefs || fuels.any { it.transactionRef.equals(ref, true) }
                    val valid = vReg.isNotBlank() && liters != null && liters > 0 && cost != null && odo != null && dist != null && ref.isNotBlank()
                    previewRows += ImportPreviewRow(
                        rowNumber = rowNum, rawColumns = cols, isValid = valid, isDuplicate = dup,
                        message = if (!valid) "Invalid vehicle/liters/cost/odometer/ref" else if (dup) "Duplicate ref $ref" else "Ready ($vReg • ${liters}L)"
                    )
                    if (valid && !dup) {
                        val matchedVehicle = existingVehicles.firstOrNull { it.registrationNumber.equals(vReg, true) }
                        fuels += FuelTransaction(
                            companyId = companyId,
                            vehicleId = matchedVehicle?.id?.ifBlank { vReg } ?: vReg,
                            vehicleReg = vReg,
                            driverId = matchedVehicle?.assignedDriverId.orEmpty(),
                            driverName = dName,
                            routeName = route.ifBlank { "Imported Route" },
                            liters = liters,
                            costAmount = cost,
                            odometerKm = odo,
                            distanceSinceLastKm = dist,
                            fuelType = matchedVehicle?.fuelType ?: FuelType.DIESEL.name,
                            stationName = station.ifBlank { "Commercial Station" },
                            transactionRef = ref,
                            txDateLabel = date,
                            txTimestampMillis = System.currentTimeMillis(),
                            demoData = false,
                        )
                    }
                }
                ImportTargetType.TRIPS -> {
                    val vReg = cols.getOrNull(0).orEmpty().trim()
                    val dName = cols.getOrNull(1).orEmpty().trim()
                    val route = cols.getOrNull(2).orEmpty().trim()
                    val origin = cols.getOrNull(3).orEmpty().trim()
                    val dest = cols.getOrNull(4).orEmpty().trim()
                    val planned = cols.getOrNull(5)?.toDoubleOrNull()
                    val actual = cols.getOrNull(6)?.toDoubleOrNull()
                    val dur = cols.getOrNull(7)?.toLongOrNull() ?: 60L
                    val idle = cols.getOrNull(8)?.toLongOrNull() ?: 0L
                    val valid = vReg.isNotBlank() && route.isNotBlank() && planned != null && actual != null
                    previewRows += ImportPreviewRow(
                        rowNumber = rowNum, rawColumns = cols, isValid = valid, isDuplicate = false,
                        message = if (!valid) "Invalid vehicle/route/distance" else "Ready ($route)"
                    )
                    if (valid) {
                        val matchedVehicle = existingVehicles.firstOrNull { it.registrationNumber.equals(vReg, true) }
                        trips += TripRecord(
                            companyId = companyId,
                            vehicleId = matchedVehicle?.id?.ifBlank { vReg } ?: vReg,
                            vehicleReg = vReg,
                            driverId = matchedVehicle?.assignedDriverId.orEmpty(),
                            driverName = dName,
                            routeName = route,
                            originName = origin.ifBlank { "Origin" },
                            destinationName = dest.ifBlank { "Destination" },
                            plannedDistanceKm = planned,
                            actualDistanceKm = actual,
                            durationMinutes = dur,
                            idleMinutes = idle,
                            routeDeviationKm = kotlin.math.abs(actual - planned),
                            geofenceBreached = kotlin.math.abs(actual - planned) > 20.0,
                            simulatedTelemetry = false,
                            startedAtMillis = System.currentTimeMillis(),
                            demoData = false,
                        )
                    }
                }
            }
        }
        return ImportParseResult(targetType, previewRows, vehicles, drivers, fuels, trips)
    }

    fun parseCsvRow(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result += sb.toString().trim()
                    sb.clear()
                }
                else -> sb.append(ch)
            }
        }
        result += sb.toString().trim()
        return result
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else value
    }

    private fun escapeXml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
