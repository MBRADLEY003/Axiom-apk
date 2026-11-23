package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.RiskSeverity
import com.example.ui.theme.AxiomAmber
import com.example.ui.theme.AxiomAmberBg
import com.example.ui.theme.AxiomCharcoal
import com.example.ui.theme.AxiomCobalt
import com.example.ui.theme.AxiomCobaltBg
import com.example.ui.theme.AxiomCrimson
import com.example.ui.theme.AxiomCrimsonBg
import com.example.ui.theme.AxiomEmerald
import com.example.ui.theme.AxiomEmeraldBg
import com.example.ui.theme.AxiomGold
import com.example.ui.theme.AxiomGoldChampagne
import com.example.ui.theme.AxiomGoldSubtleContainer
import com.example.ui.theme.AxiomHairline
import com.example.ui.theme.AxiomIvory
import com.example.ui.theme.AxiomObsidian
import com.example.ui.theme.AxiomSilver
import com.example.ui.theme.AxiomSlate
import kotlin.math.max

@Composable
fun AxiomBrandLogo(
    sizeDp: Int = 36,
    showWordmark: Boolean = true,
    subtitle: String? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(id = R.drawable.axiom_logo_emblem),
            contentDescription = "AXIOM Brand Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, AxiomGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
        )
        if (showWordmark) {
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "AXIOM",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = AxiomGold
                    ),
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = AxiomSilver,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun AxiomSectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    badge: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cardMod = if (onClick != null) modifier.clickable { onClick() } else modifier
    Card(
        modifier = cardMod.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AxiomCharcoal),
        border = BorderStroke(1.dp, AxiomHairline),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!title.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = AxiomIvory,
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = AxiomSilver,
                            )
                        }
                    }
                    if (!badge.isNullOrBlank()) {
                        AxiomPillBadge(
                            text = badge,
                            fgColor = AxiomGoldChampagne,
                            bgColor = AxiomGoldSubtleContainer,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
    }
}

@Composable
fun AxiomKpiTile(
    label: String,
    value: String,
    deltaOrNote: String,
    icon: ImageVector,
    accentColor: Color = AxiomGold,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = AxiomSlate),
        border = BorderStroke(1.dp, AxiomHairline),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = AxiomSilver,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = AxiomIvory,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = deltaOrNote,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun AxiomPillBadge(
    text: String,
    fgColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, fgColor.copy(alpha = 0.35f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = fgColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1,
        )
    }
}

fun severityColors(severity: RiskSeverity): Pair<Color, Color> = when (severity) {
    RiskSeverity.CRITICAL -> AxiomCrimson to AxiomCrimsonBg
    RiskSeverity.HIGH -> AxiomAmber to AxiomAmberBg
    RiskSeverity.MEDIUM -> AxiomCobalt to AxiomCobaltBg
    RiskSeverity.LOW -> AxiomEmerald to AxiomEmeraldBg
}

@Composable
fun DataSourceBanner(hasDemoData: Boolean, modifier: Modifier = Modifier) {
    val label = if (hasDemoData) {
        "DEMONSTRATION DATASET ACTIVE • Clearly separated from production records"
    } else {
        "LIVE OPERATIONAL WORKSPACE • Cloud-synced via Firebase Firestore"
    }
    val fg = if (hasDemoData) AxiomAmber else AxiomEmerald
    val bg = if (hasDemoData) AxiomAmberBg else AxiomEmeraldBg
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = bg,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
            )
        }
    }
}

@Composable
fun AxiomEmptyState(
    title: String,
    description: String,
    icon: ImageVector = Icons.Outlined.Info,
) {
    AxiomSectionCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = AxiomGold,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = AxiomIvory)
            Spacer(Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = AxiomSilver,
            )
        }
    }
}

@Composable
fun AxiomBarChart(
    labels: List<String>,
    values: List<Double>,
    secondaryValues: List<Double>? = null,
    primaryColor: Color = AxiomGold,
    secondaryColor: Color = AxiomCobalt,
    modifier: Modifier = Modifier,
) {
    if (values.isEmpty()) {
        Text("Insufficient chart data", style = MaterialTheme.typography.bodySmall, color = AxiomSilver)
        return
    }
    val maxVal = max(
        values.maxOrNull() ?: 1.0,
        secondaryValues?.maxOrNull() ?: 0.0
    ).coerceAtLeast(1.0)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val count = values.size
            val slotW = size.width / count.coerceAtLeast(1)
            val barW = if (secondaryValues != null) slotW * 0.32f else slotW * 0.48f

            // Baseline gridlines
            for (g in 0..3) {
                val gy = size.height * (g / 3f)
                drawLine(
                    color = AxiomHairline,
                    start = Offset(0f, gy),
                    end = Offset(size.width, gy),
                    strokeWidth = 1.5f
                )
            }

            values.forEachIndexed { i, v ->
                val ratio = (v / maxVal).toFloat().coerceIn(0f, 1f)
                val barH = ratio * (size.height - 12f)
                val x = i * slotW + if (secondaryValues != null) slotW * 0.14f else (slotW - barW) / 2f
                drawRect(
                    color = primaryColor,
                    topLeft = Offset(x, size.height - barH),
                    size = androidx.compose.ui.geometry.Size(barW, barH)
                )
                secondaryValues?.getOrNull(i)?.let { sv ->
                    val sRatio = (sv / maxVal).toFloat().coerceIn(0f, 1f)
                    val sBarH = sRatio * (size.height - 12f)
                    drawRect(
                        color = secondaryColor,
                        topLeft = Offset(x + barW + 4f, size.height - sBarH),
                        size = androidx.compose.ui.geometry.Size(barW, sBarH)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            labels.take(6).forEach { lbl ->
                Text(
                    text = lbl.take(8),
                    style = MaterialTheme.typography.labelSmall,
                    color = AxiomSilver,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun AxiomLineForecastChart(
    historical: List<Double>,
    forecast: List<Double>,
    thresholdLine: Double? = null,
    lineColor: Color = AxiomGold,
    forecastColor: Color = AxiomEmerald,
    modifier: Modifier = Modifier,
) {
    val all = historical + forecast + listOfNotNull(thresholdLine)
    if (all.isEmpty()) return
    val maxV = (all.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    val minV = (all.minOrNull() ?: 0.0).coerceAtMost(maxV - 0.1)
    val range = (maxV - minV).coerceAtLeast(0.1)
    val totalPoints = (historical.size + forecast.size).coerceAtLeast(2)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(155.dp)
    ) {
        val stepX = size.width / (totalPoints - 1).coerceAtLeast(1)

        fun yFor(v: Double): Float {
            val norm = ((v - minV) / range).toFloat().coerceIn(0f, 1f)
            return size.height - 14f - norm * (size.height - 28f)
        }

        for (g in 0..3) {
            val gy = size.height * (g / 3f)
            drawLine(AxiomHairline, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
        }

        if (thresholdLine != null) {
            val ty = yFor(thresholdLine)
            drawLine(
                color = AxiomCrimson,
                start = Offset(0f, ty),
                end = Offset(size.width, ty),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            )
        }

        if (historical.size >= 2) {
            val path = Path()
            historical.forEachIndexed { idx, v ->
                val pt = Offset(idx * stepX, yFor(v))
                if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            drawPath(path, color = lineColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
        }
        historical.forEachIndexed { idx, v ->
            drawCircle(color = lineColor, radius = 5f, center = Offset(idx * stepX, yFor(v)))
        }

        if (forecast.isNotEmpty() && historical.isNotEmpty()) {
            val fPath = Path()
            val startIdx = historical.lastIndex
            fPath.moveTo(startIdx * stepX, yFor(historical.last()))
            forecast.forEachIndexed { fIdx, fv ->
                val x = (historical.size + fIdx) * stepX
                fPath.lineTo(x, yFor(fv))
            }
            drawPath(
                fPath,
                color = forecastColor,
                style = Stroke(
                    width = 4f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )
            )
            forecast.forEachIndexed { fIdx, fv ->
                val x = (historical.size + fIdx) * stepX
                drawCircle(color = forecastColor, radius = 5f, center = Offset(x, yFor(fv)))
            }
        }
    }
}

@Composable
fun AxiomRouteTelemetryMap(
    routes: List<com.example.domain.model.TripRecord>,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AxiomObsidian)
            .border(1.dp, AxiomHairline, RoundedCornerShape(12.dp))
    ) {
        // Precision coordinate grid
        val cols = 6
        val rows = 4
        for (c in 1 until cols) {
            val x = size.width * (c / cols.toFloat())
            drawLine(AxiomHairline.copy(alpha = 0.5f), Offset(x, 0f), Offset(x, size.height), 1f)
        }
        for (r in 1 until rows) {
            val y = size.height * (r / rows.toFloat())
            drawLine(AxiomHairline.copy(alpha = 0.5f), Offset(0f, y), Offset(size.width, y), 1f)
        }

        // Geofence corridor rings
        drawCircle(
            color = AxiomGold.copy(alpha = 0.14f),
            radius = size.minDimension * 0.36f,
            center = Offset(size.width * 0.5f, size.height * 0.5f),
            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)))
        )

        if (routes.isEmpty()) return@Canvas

        val lats = routes.flatMap { listOf(it.startLat, it.endLat) }.filter { it != 0.0 }
        val lngs = routes.flatMap { listOf(it.startLng, it.endLng) }.filter { it != 0.0 }
        val minLat = lats.minOrNull() ?: 40.0
        val maxLat = (lats.maxOrNull() ?: 46.0).coerceAtLeast(minLat + 0.5)
        val minLng = lngs.minOrNull() ?: -95.0
        val maxLng = (lngs.maxOrNull() ?: -80.0).coerceAtLeast(minLng + 0.5)

        fun project(lat: Double, lng: Double, fallbackIdx: Int): Offset {
            if (lat == 0.0 && lng == 0.0) {
                return Offset(
                    size.width * (0.2f + (fallbackIdx % 3) * 0.25f),
                    size.height * (0.3f + (fallbackIdx % 2) * 0.35f)
                )
            }
            val nx = ((lng - minLng) / (maxLng - minLng)).toFloat().coerceIn(0.08f, 0.92f)
            val ny = (1f - ((lat - minLat) / (maxLat - minLat)).toFloat()).coerceIn(0.12f, 0.88f)
            return Offset(nx * size.width, ny * size.height)
        }

        routes.forEachIndexed { idx, trip ->
            val p1 = project(trip.startLat, trip.startLng, idx)
            val p2 = project(trip.endLat, trip.endLng, idx + 1)
            val color = if (trip.geofenceBreached || trip.routeDeviationKm > 15.0) AxiomCrimson else AxiomGold

            // Planned straight corridor
            drawLine(
                color = AxiomSilver.copy(alpha = 0.35f),
                start = p1,
                end = p2,
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )

            // Actual telemetry arc
            val mid = Offset(
                (p1.x + p2.x) / 2f,
                ((p1.y + p2.y) / 2f) + if (trip.geofenceBreached) -28f else 14f
            )
            val path = Path().apply {
                moveTo(p1.x, p1.y)
                quadraticTo(mid.x, mid.y, p2.x, p2.y)
            }
            drawPath(path, color = color, style = Stroke(width = 4f, cap = StrokeCap.Round))
            drawCircle(color = AxiomEmerald, radius = 6f, center = p1)
            drawCircle(color = color, radius = 7f, center = p2)
        }
    }
}
