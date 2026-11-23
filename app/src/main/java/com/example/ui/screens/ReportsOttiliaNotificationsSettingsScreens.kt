package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.DataPortabilityManager
import com.example.data.ImportParseResult
import com.example.data.ImportTargetType
import com.example.data.ReportType
import com.example.domain.model.AxiomRole
import com.example.domain.model.Company
import com.example.domain.rbac.AxiomPermission
import com.example.domain.rbac.RbacPermissions
import com.example.ui.components.AxiomEmptyState
import com.example.ui.components.AxiomPillBadge
import com.example.ui.components.AxiomSectionCard
import com.example.ui.components.severityColors
import com.example.ui.theme.AxiomCharcoal
import com.example.ui.theme.AxiomCrimson
import com.example.ui.theme.AxiomCrimsonBg
import com.example.ui.theme.AxiomEmerald
import com.example.ui.theme.AxiomEmeraldBg
import com.example.ui.theme.AxiomGold
import com.example.ui.theme.AxiomGoldSubtleContainer
import com.example.ui.theme.AxiomHairline
import com.example.ui.theme.AxiomIvory
import com.example.ui.theme.AxiomObsidian
import com.example.ui.theme.AxiomSilver
import com.example.ui.theme.AxiomSlate
import com.example.ui.viewmodel.AxiomWorkspaceUiState
import com.example.ui.viewmodel.BriefingUiState
import com.example.ui.viewmodel.OttiliaChatMessage

@Composable
fun ReportsScreen(
    state: AxiomWorkspaceUiState.Ready,
    onExportFeedback: (String) -> Unit,
) {
    val context = LocalContext.current
    var selectedReport by remember { mutableStateOf(ReportType.MONTHLY_FLEET) }
    val canExport = RbacPermissions.hasPermission(state.profile.roleEnum(), AxiomPermission.EXPORT_REPORTS)

    fun buildCsv(): String = DataPortabilityManager.generateReportCsv(
        reportType = selectedReport,
        companyName = state.activeCompany.name,
        currency = state.activeCompany.currency,
        summary = state.summary,
        vehicles = state.filteredVehicles,
        drivers = state.drivers,
        fuel = state.fuelTransactions,
        signals = state.fuelSignals,
        trips = state.trips,
        maintenance = state.maintenance,
        expenses = state.expenses,
        risks = state.riskAlerts,
    )

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(buildCsv().toByteArray(Charsets.UTF_8))
            }
            onExportFeedback("Exported ${selectedReport.label} as CSV.")
        }
    }

    val excelLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.ms-excel")
    ) { uri ->
        if (uri != null) {
            val xls = DataPortabilityManager.generateExcelXml(buildCsv(), selectedReport.label)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(xls.toByteArray(Charsets.UTF_8))
            }
            onExportFeedback("Exported ${selectedReport.label} as Excel (.xls).")
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                DataPortabilityManager.writeReportPdf(
                    outputStream = out,
                    reportType = selectedReport,
                    companyName = state.activeCompany.name,
                    currency = state.activeCompany.currency,
                    summary = state.summary,
                    csvContent = buildCsv(),
                )
            }
            onExportFeedback("Exported ${selectedReport.label} as PDF.")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            AxiomSectionCard(
                title = "Enterprise Report Generator",
                subtitle = "Filter-aware exports supporting PDF, Excel (.xls), and CSV",
                badge = state.dateFilter.label.uppercase(),
            ) {
                Text("Select Report Template:", style = MaterialTheme.typography.labelMedium, color = AxiomGold)
                Spacer(Modifier.height(8.dp))
                ReportType.entries.forEach { rt ->
                    FilterChip(
                        selected = selectedReport == rt,
                        onClick = { selectedReport = rt },
                        label = { Text(rt.label) },
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
            }
        }

        item {
            AxiomSectionCard(
                title = "Export Actions • ${selectedReport.label}",
                subtitle = "Includes active company (${state.activeCompany.name}) & current filters",
            ) {
                if (!canExport) {
                    Text("Your current role cannot export reports.", style = MaterialTheme.typography.bodySmall, color = AxiomCrimson)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { pdfLauncher.launch("AXIOM_${selectedReport.name}.pdf") },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("PDF")
                        }
                        Button(
                            onClick = { excelLauncher.launch("AXIOM_${selectedReport.name}.xls") },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.TableChart, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Excel")
                        }
                        OutlinedButton(
                            onClick = { csvLauncher.launch("AXIOM_${selectedReport.name}.csv") },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("CSV")
                        }
                    }
                }
            }
        }

        item {
            val previewLines = remember(selectedReport, state.vehicles, state.fuelTransactions) {
                buildCsv().lines().take(12).joinToString("\n")
            }
            AxiomSectionCard(title = "Data Preview (First 12 Rows)") {
                Text(previewLines, style = MaterialTheme.typography.labelSmall, color = AxiomSilver)
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun OttiliaAiScreen(
    messages: List<OttiliaChatMessage>,
    streamingText: String,
    isBusy: Boolean,
    errorText: String?,
    briefingState: BriefingUiState,
    onSendQuestion: (String) -> Unit,
    onGenerateBriefing: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val suggestedPrompts = listOf(
        "Which vehicle consumed the most fuel this month?",
        "Identify unusual diesel transactions",
        "Why was AXM-104 flagged?",
        "Which driver travelled the longest distance?",
        "What is our average cost per kilometre?",
        "Give me a weekly operational summary"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                AxiomSectionCard(
                    title = "OTTILIA • AI Decision Intelligence",
                    subtitle = "Grounded in live AXIOM telemetry via Firebase AI Logic (gemini-3.8-flash). Never fabricates figures.",
                    badge = "VERIFIED",
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_ottilia_avatar),
                            contentDescription = "Ottilia AI Emblem",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(1.dp, AxiomGold, CircleShape),
                        )
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = onGenerateBriefing,
                            enabled = briefingState !is BriefingUiState.Loading,
                            colors = ButtonDefaults.buttonColors(containerColor = AxiomGold, contentColor = AxiomObsidian),
                            modifier = Modifier.testTag("ottilia_briefing_btn"),
                        ) {
                            Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Generate Executive Briefing")
                        }
                    }
                }
            }

            when (briefingState) {
                is BriefingUiState.Loading -> item {
                    AxiomSectionCard(title = "Generating Structured Executive Briefing...") {
                        CircularProgressIndicator(color = AxiomGold, modifier = Modifier.size(24.dp))
                    }
                }
                is BriefingUiState.Error -> item {
                    Surface(
                        color = AxiomCrimsonBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AxiomCrimson.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(briefingState.message, style = MaterialTheme.typography.bodySmall, color = AxiomCrimson, modifier = Modifier.padding(12.dp))
                    }
                }
                is BriefingUiState.Success -> item {
                    val b = briefingState.briefing
                    AxiomSectionCard(
                        title = b.executiveHeadline,
                        subtitle = "Structured Decision-Support Briefing",
                        badge = "OTTILIA BRIEF",
                    ) {
                        Text("What Happened: ${b.whatHappened}", style = MaterialTheme.typography.bodySmall, color = AxiomIvory)
                        Spacer(Modifier.height(6.dp))
                        Text("Where & Why: ${b.whereAndWhy}", style = MaterialTheme.typography.bodySmall, color = AxiomSilver)
                        Spacer(Modifier.height(6.dp))
                        Text("Severity: ${b.severityAssessment}", style = MaterialTheme.typography.bodySmall, color = AxiomGold)
                        Spacer(Modifier.height(6.dp))
                        Text("Forecast / Next Risk: ${b.forecastAndNextRisk}", style = MaterialTheme.typography.bodySmall, color = AxiomSilver)
                        Spacer(Modifier.height(6.dp))
                        Text("Supporting Calculations: ${b.supportingCalculations}", style = MaterialTheme.typography.labelSmall, color = AxiomEmerald)
                        Spacer(Modifier.height(6.dp))
                        b.recommendedActions.forEachIndexed { idx, act ->
                            Text("${idx + 1}. $act", style = MaterialTheme.typography.bodySmall, color = AxiomIvory)
                        }
                    }
                }
                BriefingUiState.Idle -> {}
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestedPrompts) { q ->
                        FilterChip(
                            selected = false,
                            onClick = { onSendQuestion(q) },
                            label = { Text(q) },
                        )
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                Surface(
                    color = if (msg.fromUser) AxiomGoldSubtleContainer else AxiomCharcoal,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (msg.fromUser) AxiomGold.copy(alpha = 0.4f) else AxiomHairline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (msg.fromUser) "YOU" else "OTTILIA AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (msg.fromUser) AxiomGold else AxiomEmerald,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(msg.text, style = MaterialTheme.typography.bodyMedium, color = AxiomIvory)
                    }
                }
            }

            if (streamingText.isNotBlank()) {
                item {
                    Surface(
                        color = AxiomCharcoal,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AxiomGold.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("OTTILIA AI (STREAMING...)", style = MaterialTheme.typography.labelSmall, color = AxiomGold)
                            Spacer(Modifier.height(4.dp))
                            Text(streamingText, style = MaterialTheme.typography.bodyMedium, color = AxiomIvory)
                        }
                    }
                }
            }

            if (errorText != null) {
                item {
                    Surface(
                        color = AxiomCrimsonBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(errorText, style = MaterialTheme.typography.bodySmall, color = AxiomCrimson, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Ask Ottilia about fleet, fuel, risk, or cost/km...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ottilia_input"),
                singleLine = true,
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val q = input
                    input = ""
                    onSendQuestion(q)
                },
                enabled = !isBusy && input.isNotBlank(),
                modifier = Modifier.testTag("ottilia_send_btn"),
            ) {
                if (isBusy) {
                    CircularProgressIndicator(color = AxiomGold, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send to Ottilia", tint = AxiomGold)
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    state: AxiomWorkspaceUiState.Ready,
    onMarkRead: (String, Boolean) -> Unit,
    onUpdateChannels: (Boolean, Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            AxiomSectionCard(
                title = "Notification & Escalation Channels",
                subtitle = "In-app active • Email & SMS architecture integration preferences",
                badge = "${state.notifications.count { !it.readStatus }} UNREAD",
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Email Escalation Ready", style = MaterialTheme.typography.bodyMedium, color = AxiomIvory)
                    Switch(
                        checked = state.profile.emailAlertsEnabled,
                        onCheckedChange = { onUpdateChannels(it, state.profile.smsAlertsEnabled) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("SMS Critical Alert Ready", style = MaterialTheme.typography.bodyMedium, color = AxiomIvory)
                    Switch(
                        checked = state.profile.smsAlertsEnabled,
                        onCheckedChange = { onUpdateChannels(state.profile.emailAlertsEnabled, it) }
                    )
                }
            }
        }

        if (state.notifications.isEmpty()) {
            item {
                AxiomEmptyState(
                    title = "All Clear",
                    description = "No operational alerts or compliance reminders.",
                    icon = Icons.Outlined.Notifications,
                )
            }
        } else {
            items(state.notifications, key = { it.id.ifBlank { it.title } }) { n ->
                val (fg, bg) = severityColors(n.severityEnum())
                AxiomSectionCard(
                    title = n.title,
                    subtitle = "${n.category} • Channel: ${n.channel}",
                    badge = if (n.readStatus) "READ" else "NEW",
                ) {
                    AxiomPillBadge(n.severityEnum().label.uppercase(), fg, bg)
                    Spacer(Modifier.height(6.dp))
                    Text(n.message, style = MaterialTheme.typography.bodySmall, color = AxiomIvory)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { onMarkRead(n.id, !n.readStatus) }) {
                        Text(if (n.readStatus) "Mark Unread" else "Mark as Read", color = AxiomGold)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun SettingsAndAdminScreen(
    state: AxiomWorkspaceUiState.Ready,
    onSwitchCompany: (Company) -> Unit,
    onCreateCompany: (String, String, String) -> Unit,
    onSwitchRole: (AxiomRole) -> Unit,
    onAddTeamMember: (String, String, AxiomRole, String) -> Unit,
    onSetPin: (String) -> Unit,
    onLoadDemo: () -> Unit,
    onCommitImport: (ImportParseResult) -> Unit,
    onFeedback: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var showCreateCompany by remember { mutableStateOf(false) }
    var showAddMember by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var importType by remember { mutableStateOf(ImportTargetType.VEHICLES) }
    var parsedPreview by remember { mutableStateOf<ImportParseResult?>(null) }

    val templateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val csv = DataPortabilityManager.generateImportTemplateCsv(importType)
            context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
            onFeedback("Downloaded ${importType.label} CSV import template.")
        }
    }

    val importPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            parsedPreview = DataPortabilityManager.parseCsvImport(
                context = context,
                uri = uri,
                targetType = importType,
                companyId = state.activeCompany.companyId.ifBlank { state.activeCompany.id },
                existingVehicles = state.vehicles,
                existingDrivers = state.drivers,
                existingFuel = state.fuelTransactions,
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            AxiomSectionCard(
                title = "Operator Profile & Session",
                subtitle = "${state.profile.displayName} (${state.profile.email})",
                badge = state.profile.roleEnum().displayName.uppercase(),
            ) {
                Text(
                    text = "Authentication: Google Identity Services (Credential Manager) • Timeout: ${state.profile.sessionTimeoutMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = AxiomSilver,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = { Text("SHA-256 Security PIN") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = {
                        onSetPin(pinInput)
                        pinInput = ""
                    }) {
                        Text("Save PIN")
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { signOutAxiom(credentialManager, scope) },
                    border = BorderStroke(1.dp, AxiomCrimson),
                    modifier = Modifier.testTag("sign_out_button"),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, tint = AxiomCrimson, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Sign Out Safely", color = AxiomCrimson)
                }
            }
        }

        // Multi-Company Tenant Management
        item {
            AxiomSectionCard(
                title = "Multi-Company Tenant Management",
                subtitle = "Active Tenant: ${state.activeCompany.name} (${state.activeCompany.currency})",
                badge = "${state.companies.size} COMPANIES",
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.companies) { comp ->
                        val cid = comp.companyId.ifBlank { comp.id }
                        FilterChip(
                            selected = cid == state.activeCompany.companyId,
                            onClick = { onSwitchCompany(comp) },
                            label = { Text("${comp.name} (${comp.currency})") },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { showCreateCompany = true }) {
                    Text("Create New Company")
                }
            }
        }

        // 7-Role RBAC Matrix
        item {
            AxiomSectionCard(
                title = "Role-Based Access Control (7 Roles)",
                subtitle = state.profile.roleEnum().summary,
                badge = "RBAC ACTIVE",
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AxiomRole.entries) { role ->
                        FilterChip(
                            selected = state.profile.roleEnum() == role,
                            onClick = { onSwitchRole(role) },
                            label = { Text(role.displayName) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                val perms = RbacPermissions.permissionsFor(state.profile.roleEnum())
                Text(
                    text = "Active Permissions (${perms.size}/14): ${perms.joinToString { it.name }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AxiomGold,
                )
                Spacer(Modifier.height(10.dp))
                Text("Team Directory (${state.teamMembers.size}):", style = MaterialTheme.typography.titleSmall, color = AxiomIvory)
                state.teamMembers.forEach { tm ->
                    Text("• ${tm.fullName} (${tm.email}) — ${tm.roleEnum().displayName}", style = MaterialTheme.typography.bodySmall, color = AxiomSilver)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { showAddMember = true }) {
                    Text("Assign Team Member Role")
                }
            }
        }

        // Data Import & Templates
        item {
            AxiomSectionCard(
                title = "Data Import, Templates & Demonstration Seed",
                subtitle = "Validate CSV records, detect duplicates, preview rows, or load labelled demo data",
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ImportTargetType.entries) { t ->
                        FilterChip(selected = importType == t, onClick = { importType = t }, label = { Text(t.label) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { templateLauncher.launch("AXIOM_Template_${importType.name}.csv") },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Download, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Template")
                    }
                    Button(
                        onClick = { importPicker.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.UploadFile, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Import CSV")
                    }
                }
                parsedPreview?.let { preview ->
                    Spacer(Modifier.height(10.dp))
                    Text("Import Preview (${preview.rows.size} rows):", style = MaterialTheme.typography.titleSmall, color = AxiomGold)
                    preview.rows.take(6).forEach { r ->
                        val color = if (r.isValid && !r.isDuplicate) AxiomEmerald else AxiomCrimson
                        Text("Row ${r.rowNumber}: ${r.message}", style = MaterialTheme.typography.labelSmall, color = color)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        onCommitImport(preview)
                        parsedPreview = null
                    }) {
                        Text("Commit Valid Rows")
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onLoadDemo) {
                    Text("Load / Refresh Demonstration Fleet Data")
                }
            }
        }

        // Immutable Audit Trail
        item {
            AxiomSectionCard(
                title = "Immutable Security & Activity Audit Log",
                subtitle = "Write-once compliance trail enforced by Firestore Security Rules",
                badge = "${state.auditLogs.size} EVENTS",
            ) {
                if (state.auditLogs.isEmpty()) {
                    Text("No audit entries yet.", style = MaterialTheme.typography.bodySmall, color = AxiomSilver)
                } else {
                    state.auditLogs.take(15).forEach { log ->
                        Text(
                            text = "[${log.actorRole}] ${log.action} • ${log.targetEntity} — ${log.details}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AxiomIvory,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showCreateCompany) {
        var name by remember { mutableStateOf("") }
        var industry by remember { mutableStateOf("Freight & Cold Chain") }
        var currency by remember { mutableStateOf("USD") }
        AlertDialog(
            onDismissRequest = { showCreateCompany = false },
            title = { Text("Create New Company") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Company Name") }, singleLine = true)
                    OutlinedTextField(industry, { industry = it }, label = { Text("Industry") }, singleLine = true)
                    OutlinedTextField(currency, { currency = it }, label = { Text("Currency (e.g. USD, EUR, GBP)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onCreateCompany(name, industry, currency)
                    showCreateCompany = false
                }) { Text("Create") }
            },
            dismissButton = { TextButton({ showCreateCompany = false }) { Text("Cancel") } }
        )
    }

    if (showAddMember) {
        var mName by remember { mutableStateOf("") }
        var mEmail by remember { mutableStateOf("") }
        var mDept by remember { mutableStateOf("Fleet Operations") }
        var mRole by remember { mutableStateOf(AxiomRole.FLEET_MANAGER) }
        AlertDialog(
            onDismissRequest = { showAddMember = false },
            title = { Text("Assign Team Member Role") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(mName, { mName = it }, label = { Text("Full Name") }, singleLine = true)
                    OutlinedTextField(mEmail, { mEmail = it }, label = { Text("Email") }, singleLine = true)
                    OutlinedTextField(mDept, { mDept = it }, label = { Text("Department") }, singleLine = true)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(AxiomRole.entries) { r ->
                            FilterChip(selected = mRole == r, onClick = { mRole = r }, label = { Text(r.displayName) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAddTeamMember(mName, mEmail, mRole, mDept)
                    showAddMember = false
                }) { Text("Assign") }
            },
            dismissButton = { TextButton({ showAddMember = false }) { Text("Cancel") } }
        )
    }
}
