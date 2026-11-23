package com.example.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class OttiliaBriefing(
    val executiveHeadline: String,
    val whatHappened: String,
    val whereAndWhy: String,
    val severityAssessment: String,
    val forecastAndNextRisk: String,
    val recommendedActions: List<String>,
    val supportingCalculations: String,
)

class OttiliaAiRepository {

    private val streamingModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    You are OTTILIA, the executive operational intelligence AI inside AXIOM (Intelligent Fleet & Logistics Intelligence).
                    Philosophy: DATA -> ANALYSIS -> RISK -> INSIGHT -> ACTION.
                    STRICT INTEGRITY RULES:
                    1. NEVER invent, estimate without stating so, or fabricate operational figures, vehicle IDs, driver names, fuel liters, costs, or distances.
                    2. Base every factual claim strictly on the VERIFIED OPERATIONAL TELEMETRY context provided in the prompt.
                    3. If the requested data is missing or insufficient to answer accurately, state clearly: "Insufficient operational data in the current dataset to determine this."
                    4. Always show the underlying numbers and calculations (e.g., km / liters = km/L, Z-score, cost/km) behind your conclusions.
                    5. Never classify a fuel anomaly as confirmed theft; describe it as an "investigation signal" with supporting evidence and recommended verification steps.
                    6. Clearly separate: (a) Observed Facts, (b) Statistical Estimates/Signals, and (c) Recommended Management Actions.
                    Keep tone crisp, authoritative, concise, and enterprise-grade.
                    """.trimIndent()
                )
            }
        )
    }

    private val briefingSchema = Schema.obj(
        properties = mapOf(
            "executiveHeadline" to Schema.string(description = "One-line executive summary of operational status"),
            "whatHappened" to Schema.string(description = "Observed facts with exact figures from telemetry"),
            "whereAndWhy" to Schema.string(description = "Vehicles, drivers, routes, and statistical root causes"),
            "severityAssessment" to Schema.string(description = "Operational and financial severity with confidence"),
            "forecastAndNextRisk" to Schema.string(description = "What could happen next based on trends and maintenance intervals"),
            "recommendedActions" to Schema.array(
                items = Schema.string(),
                description = "Concrete prioritized management actions"
            ),
            "supportingCalculations" to Schema.string(description = "Exact mathematical figures and formulas backing the briefing")
        )
    )

    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = briefingSchema
            },
            systemInstruction = content {
                text(
                    "You are OTTILIA inside AXIOM. Answer only from verified telemetry. Never fabricate figures. State insufficient data if absent."
                )
            }
        )
    }

    fun streamOperationalAnswer(
        question: String,
        telemetryContext: String,
    ): Flow<String> = flow {
        val fullPrompt = """
            $telemetryContext

            [OPERATOR QUESTION]
            $question

            Respond clearly with:
            1. Direct Answer & Observed Figures
            2. Supporting Calculation / Statistical Basis
            3. Recommended Action (if applicable)
        """.trimIndent()

        val buffer = StringBuilder()
        withTimeout(60_000L) {
            streamingModel.generateContentStream(fullPrompt).collect { chunk ->
                val part = chunk.text.orEmpty()
                if (part.isNotEmpty()) {
                    buffer.append(part)
                    emit(buffer.toString())
                }
            }
        }
        if (buffer.isBlank()) {
            throw IllegalStateException("Ottilia returned an empty response.")
        }
    }.catch { error ->
        if (error is CancellationException && error !is TimeoutCancellationException) {
            throw error
        }
        Log.e(TAG, "Ottilia streaming error", error)
        throw error
    }

    suspend fun generateDecisionBriefing(telemetryContext: String): Result<OttiliaBriefing> {
        return try {
            val prompt = """
                $telemetryContext

                Produce an executive decision-support briefing answering:
                - What happened
                - Where it happened & why
                - How serious it is
                - What could happen next
                - What management should do
                Cite exact numbers and calculations from the verified telemetry above.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                structuredModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("Empty structured briefing")
            val json = JSONObject(rawJson)
            val actionsArray = json.getJSONArray("recommendedActions")
            val actions = (0 until actionsArray.length()).map { actionsArray.getString(it) }

            Result.success(
                OttiliaBriefing(
                    executiveHeadline = json.getString("executiveHeadline"),
                    whatHappened = json.getString("whatHappened"),
                    whereAndWhy = json.getString("whereAndWhy"),
                    severityAssessment = json.getString("severityAssessment"),
                    forecastAndNextRisk = json.getString("forecastAndNextRisk"),
                    recommendedActions = actions,
                    supportingCalculations = json.getString("supportingCalculations"),
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Ottilia briefing timed out", e)
            Result.failure(Exception("Ottilia briefing timed out after 60 seconds. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Ottilia briefing failed", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "OttiliaAiRepo"
    }
}
