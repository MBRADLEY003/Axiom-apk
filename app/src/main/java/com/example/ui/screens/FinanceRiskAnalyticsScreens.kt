package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.ExpenseRecord
import com.example.domain.model.RiskAlertRecord
import com.example.domain.model.RiskResolutionStatus
import com.example.domain.rbac.AxiomPermission
import com.example.domain.rbac.RbacPermissions
import com.example.ui.components.AxiomBarChart
import com.example.ui.components.AxiomEmptyState
import com.example.ui.components.AxiomKpiTile
import com.example.ui.components.AxiomLineForecastChart
import com.example.ui.components.AxiomPillBadge
import com.example.ui.components.AxiomSectionCard
import com.example.ui.components.severityColors
import com.example.ui.theme.AxiomCobalt
import com.example.ui.theme.AxiomCrimson
import com.example.ui.theme.AxiomEmerald
import com.example.ui.theme.AxiomGold
import com.example.ui.theme.AxiomIvory
import com.example.ui.theme.AxiomObsidian
import com.example.ui.theme.AxiomSilver
import com.example.ui.viewmodel.AxiomWorkspaceUiState
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FinancialIntelligenceScreen(
    state: AxiomWorkspaceUiState.Ready,
    onAddExpense: (ExpenseRecord) -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    val canManage = RbacPermissions.hasPermission(state.profile.roleEnum(), AxiomPermission.MANAGE_FINANCE_BUDGETS)
    val cur = state.activeCompany.currency

    val totalBudget = state.expenses.sumOf { it.budgetAllocated }
    val totalSpend = state.expenses.sumOf { it.amount }
    val costPerVehicle = if (state.summary.totalFleet > 0) state.summary.totalOpex / state.summary.totalFleet else 0.0

    val categoryTotals = ExpenseCategory.entries.map { cat ->
        val rows = state.expenses.filter { it.categoryEnum() == cat }
        Triple(cat, rows.sumOf { it.amount }, rows.sumOf { it.budgetAllocated })
    }.filter { it.second > 0 || it.third > 0 }

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
                        label = "Total Opex",
                        value = "$cur ${"%.0f".format(Locale.US, state.summary.totalOpex)}",
                        deltaOrNote = "Budget: $cur ${"%.0f".format(Locale.US, totalBudget)}",
                        icon = Icons.Outlined.AccountBalanceWallet,
                        modifier = Modifier.weight(1f),
                    )
                    AxiomKpiTile(
                        label = "Cost / km",
                        value = "$cur ${"%.3f".format(Locale.US, state.summary.costPerKm)}",
                        deltaOrNote = "$cur ${"%.0f".format(Locale.US, costPerVehicle)} / vehicle",
                        icon = Icons.Outlined.Analytics,
                        accentColor = if (totalSpend <= totalBudget) AxiomEmerald else AxiomCrimson,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                AxiomSectionCard(
                    title = "Budget vs Actual by Category",
                    subtitle = "Gold: Actual Expenditure • Cobalt: Allocated Budget",
                    badge = state.activeCompany.currency,
                ) {
                    if (categoryTotals.isEmpty()) {
                        Text("Record expenses to view budget comparison.", style = MaterialTheme.typography.bodySmall, color = AxiomSilver)
                    } else {
                        AxiomBarChart(
                            labels = categoryTotals.map { it.first.label.take(6) },
                            values = categoryTotals.map { it.second },
                            secondaryValues = categoryTotals.map { it.third },
                            primaryColor = AxiomGold,
                            secondaryColor = AxiomCobalt,
                        )
                    }
                }
            }

            if (state.expenses.isEmpty()) {
                item {
                    AxiomEmptyState(
                        title = "No Expenditure Records",
                        description = "Add fuel, maintenance, insurance, or licensing expenses.",
                        icon = Icons.Outlined.AccountBalanceWallet,
                    )
                }
            } else {
                items(state.expenses, key = { it.id.ifBlank { it.description + it.amount } }) { exp ->
                    val variance = exp.budgetAllocated - exp.amount
                    AxiomSectionCard(
                        title = "${exp.categoryEnum().label} • $cur ${"%.2f".format(Locale.US, exp.amount)}",
                        subtitle = "${exp.monthKey} • Vehicle: ${exp.vehicleReg.ifBlank { "FLEET" }}",
                        badge = if (variance >= 0) "UNDER BUDGET" else "OVER BUDGET",
                    ) {
                        Text(exp.description, style = MaterialTheme.typography.bodySmall, color = AxiomIvory)
                        Text(
                            text = "Allocated: $cur ${"%.2f".format(Locale.US, exp.budgetAllocated)} • Variance: $cur ${"%+.2f".format(Locale.US, variance)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (variance >= 0) AxiomEmerald else AxiomCrimson,
                        )
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
                    .testTag("add_expense_fab"),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Expense")
            }
        }
    }

    if (showAdd) {
        AddExpenseDialog(
            onDismiss = { showAdd = false },
            onSave = { e ->
                onAddExpense(e)
                showAdd = false
            }
        )
    }
}

@Composable
fun RiskIntelligenceScreen(
    state: AxiomWorkspaceUiState.Ready,
    onInspectRisk: (String) -> Unit,
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
                title = "Centralised Risk Intelligence Engine",
                subtitle = "Every alert carries category, severity, evidence, detection method, historical comparison & investigation guidance.",
                badge = "${state.riskAlerts.size} ALERTS",
            ) {
                val open = state.riskAlerts.count { it.resolutionEnum() == RiskResolutionStatus.OPEN }
                val inv = state.riskAlerts.count { it.resolutionEnum() == RiskResolutionStatus.INVESTIGATING }
                val res = state.riskAlerts.count { it.resolutionEnum() == RiskResolutionStatus.RESOLVED }
                Text("Open: $open • Investigating: $inv • Resolved: $res", style = MaterialTheme.typography.labelMedium, color = AxiomGold)
            }
        }

        if (state.riskAlerts.isEmpty()) {
            item {
                AxiomEmptyState(
                    title = "Zero Active Risk Alerts",
                    description = "All fleet operations are operating within statistical baselines.",
                    icon = Icons.Outlined.Shield,
                )
            }
        } else {
            items(state.riskAlerts, key = { it.id.ifBlank { it.entityLabel + it.category } }) { alert ->
                val (fg, bg) = severityColors(alert.severityEnum())
                AxiomSectionCard(
                    title = "${alert.category} • ${alert.entityLabel}",
                    subtitle = "Method: ${alert.detectionMethod} • Confidence ${(alert.confidenceScore * 100).toInt()}%",
                    badge = alert.resolutionEnum().label.uppercase(),
                    onClick = { onInspectRisk(alert.id) },
                ) {
                    AxiomPillBadge(alert.severityEnum().label.uppercase(), fg, bg)
                    Spacer(Modifier.height(8.dp))
                    Text("Supporting Data: ${alert.supportingData}", style = MaterialTheme.typography.bodySmall, color = AxiomIvory)
                    Text("Historical Comparison: ${alert.historicalComparison}", style = MaterialTheme.typography.bodySmall, color = AxiomSilver)
                    Text("Recommended Investigation: ${alert.recommendedInvestigation}", style = MaterialTheme.typography.bodySmall, color = AxiomGold)
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun RiskInvestigationScreen(
    alert: RiskAlertRecord?,
    onBack: () -> Unit,
    onResolve: (String, RiskResolutionStatus, String) -> Unit,
    canResolve: Boolean,
) {
    if (alert == null) {
        AxiomEmptyState("Alert Not Found", "Risk alert unavailable.")
        return
    }
    var notes by remember(alert.id) { mutableStateOf(alert.resolutionNotes) }
    val (fg, bg) = severityColors(alert.severityEnum())

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
                Text("Risk Investigation", style = MaterialTheme.typography.headlineSmall, color = AxiomIvory)
            }
        }
        item {
            AxiomSectionCard(
                title = alert.entityLabel,
                subtitle = "${alert.category} • ${alert.entityType}",
                badge = alert.resolutionEnum().label.uppercase(),
            ) {
                AxiomPillBadge(
                    "${alert.severityEnum().label} • ${(alert.confidenceScore * 100).toInt()}% CONFIDENCE",
                    fg,
                    bg,
                )
                Spacer(Modifier.height(10.dp))
                Text("1. Supporting Data:\n${alert.supportingData}", style = MaterialTheme.typography.bodyMedium, color = AxiomIvory)
                Spacer(Modifier.height(8.dp))
                Text("2. Detection Method:\n${alert.detectionMethod}", style = MaterialTheme.typography.bodySmall, color = AxiomSilver)
                Spacer(Modifier.height(8.dp))
                Text("3. Historical Comparison:\n${alert.historicalComparison}", style = MaterialTheme.typography.bodySmall, color = AxiomSilver)
                Spacer(Modifier.height(8.dp))
                Text("4. Recommended Investigation:\n${alert.recommendedInvestigation}", style = MaterialTheme.typography.bodyMedium, color = AxiomGold)
            }
        }
        if (canResolve) {
            item {
                AxiomSectionCard(title = "Supervisor Resolution Workflow") {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Investigation Findings & Resolution Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onResolve(alert.id, RiskResolutionStatus.INVESTIGATING, notes) }) {
                            Text("Mark Investigating")
                        }
                        Button(onClick = { onResolve(alert.id, RiskResolutionStatus.RESOLVED, notes) }) {
                            Text("Resolve")
                        }
                        OutlinedButton(onClick = { onResolve(alert.id, RiskResolutionStatus.DISMISSED, notes) }) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun AnalyticsDataScienceScreen(
    state: AxiomWorkspaceUiState.Ready,
    onAskOttilia: () -> Unit,
) {
    val cur = state.activeCompany.currency

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            AxiomSectionCard(
                title = "Statistical & Predictive Analytics Engine",
                subtitle = "Strictly separates Observed Facts, Statistical Estimates (OLS + Z-Score + CUSUM), and Management Actions",
                badge = "MODEL EVAL",
            ) {
                Text(
                    text = "• Observed Facts: ${state.fuelTransactions.size} fuel logs, ${"%.1f".format(Locale.US, state.summary.totalFuelLiters)} L consumed, ${"%.2f".format(Locale.US, state.summary.avgKmPerLiter)} km/L mean.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AxiomIvory,
                )
                Text(
                    text = "• Statistical Estimate: ${state.fuelSpendForecast.modelDescription}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AxiomEmerald,
                )
                Text(
                    text = "• Confidence & Error: R² = ${"%.2f".format(Locale.US, state.fuelSpendForecast.rSquared)}, MAE = $cur ${"%.2f".format(Locale.US, state.fuelSpendForecast.mae)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AxiomGold,
                )
            }
        }

        item {
            AxiomSectionCard(
                title = "Fuel Volume Forecast (Liters)",
                subtitle = state.fuelLitersForecast.modelDescription,
                badge = "TIME SERIES",
            ) {
                AxiomLineForecastChart(
                    historical = state.fuelLitersForecast.historicalValues,
                    forecast = state.fuelLitersForecast.forecastValues,
                )
                if (state.fuelLitersForecast.forecastValues.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    state.fuelLitersForecast.forecastValues.forEachIndexed { idx, v ->
                        val lbl = state.fuelLitersForecast.forecastLabels.getOrElse(idx) { "F+${idx + 1}" }
                        val lo = state.fuelLitersForecast.lowerBounds.getOrElse(idx) { v }
                        val hi = state.fuelLitersForecast.upperBounds.getOrElse(idx) { v }
                        Text(
                            text = "$lbl: ${"%.1f".format(Locale.US, v)} L (95% CI: ${"%.1f".format(Locale.US, lo)} – ${"%.1f".format(Locale.US, hi)} L)",
                            style = MaterialTheme.typography.labelSmall,
                            color = AxiomSilver,
                        )
                    }
                }
            }
        }

        item {
            AxiomSectionCard(
                title = "Operational Vehicle Risk Scoring (0–100)",
                subtitle = "Weighted across Z-score/CUSUM fuel signals, route deviations & maintenance urgency",
                badge = "SCORING",
            ) {
                if (state.vehicleRisks.isEmpty()) {
                    Text("No vehicles scored.", style = MaterialTheme.typography.bodySmall, color = AxiomSilver)
                } else {
                    state.vehicleRisks.forEach { vr ->
                        val (fg, bg) = severityColors(vr.riskTier)
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("${vr.vehicleReg} • ${vr.makeModel}", style = MaterialTheme.typography.titleSmall, color = AxiomIvory)
                                AxiomPillBadge("${vr.riskScore}/100 ${vr.riskTier.label}", fg, bg)
                            }
                            Text("Facts: ${vr.observedFacts}", style = MaterialTheme.typography.bodySmall, color = AxiomSilver)
                            Text("Estimate: ${vr.statisticalEstimate}", style = MaterialTheme.typography.labelSmall, color = AxiomCobalt)
                            Text("Action: ${vr.recommendedAction}", style = MaterialTheme.typography.bodySmall, color = AxiomGold)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = onAskOttilia) {
                    Text("Interpret Findings with Ottilia AI")
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (ExpenseRecord) -> Unit,
) {
    var category by remember { mutableStateOf(ExpenseCategory.OPERATIONAL) }
    var vehicleReg by remember { mutableStateOf("FLEET") }
    var amount by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Financial Expenditure") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExpenseCategory.entries.take(3).forEach { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat.label) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExpenseCategory.entries.drop(3).forEach { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat.label) })
                    }
                }
                OutlinedTextField(vehicleReg, { vehicleReg = it }, label = { Text("Vehicle Reg or FLEET") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(amount, { amount = it }, label = { Text("Amount") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(budget, { budget = it }, label = { Text("Budget") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(desc, { desc = it }, label = { Text("Description") })
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toDoubleOrNull()
                if (amt != null && desc.isNotBlank()) {
                    onSave(
                        ExpenseRecord(
                            vehicleReg = vehicleReg.trim().uppercase().ifBlank { "FLEET" },
                            category = category.name,
                            amount = amt,
                            budgetAllocated = budget.toDoubleOrNull() ?: amt,
                            description = desc.trim(),
                            monthKey = "2026-11",
                        )
                    )
                }
            }) { Text("Save Expense") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
