package com.example

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.example.data.DataPortabilityManager
import com.example.data.ImportTargetType
import com.example.domain.model.Vehicle
import com.example.ui.components.AxiomBrandLogo
import com.example.ui.components.DataSourceBanner
import com.example.ui.theme.AxiomTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `app name matches AXIOM branding`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AXIOM", appName)
    }

    @Test
    fun `brand logo and data source banner render cleanly in AXIOM theme`() {
        composeRule.setContent {
            AxiomTheme {
                AxiomBrandLogo(
                    sizeDp = 44,
                    showWordmark = true,
                    subtitle = "Intelligent Fleet & Logistics Intelligence",
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("AXIOM").assertIsDisplayed()
        composeRule.onNodeWithText("Intelligent Fleet & Logistics Intelligence").assertIsDisplayed()
    }

    @Test
    fun `demo vs live data banner labels data provenance explicitly`() {
        composeRule.setContent {
            AxiomTheme {
                DataSourceBanner(hasDemoData = true)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(
            "DEMONSTRATION DATASET ACTIVE • Clearly separated from production records"
        ).assertIsDisplayed()
    }

    @Test
    fun `csv import parser validates rows and detects duplicate registrations`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempFile = File(context.cacheDir, "test_vehicles.csv")
        tempFile.writeText(
            """
            registrationNumber,vin,makeModel,category,fuelType,tankCapacityLiters,currentMileageKm,status,baselineKmPerLiter
            AXM-101,YV2RT40A8LB101101,Volvo FH16,HEAVY_TRUCK,DIESEL,600,45000,ACTIVE,3.2
            AXM-202,YV2RT40A8LB202202,Scania R500,HEAVY_TRUCK,DIESEL,550,21000,ACTIVE,3.4
            BADROW,12,Incomplete,HEAVY_TRUCK,DIESEL,abc,100,ACTIVE,3.0
            """.trimIndent()
        )
        val result = DataPortabilityManager.parseCsvImport(
            context = context,
            uri = Uri.fromFile(tempFile),
            targetType = ImportTargetType.VEHICLES,
            companyId = "comp_test",
            existingVehicles = listOf(Vehicle(id = "v1", registrationNumber = "AXM-101")),
            existingDrivers = emptyList(),
            existingFuel = emptyList(),
        )
        assertEquals(3, result.rows.size)
        assertTrue(result.rows[0].isDuplicate)
        assertTrue(result.rows[1].isValid && !result.rows[1].isDuplicate)
        assertFalse(result.rows[2].isValid)
        assertEquals(1, result.parsedVehicles.size)
        assertEquals("AXM-202", result.parsedVehicles.first().registrationNumber)
        tempFile.delete()
    }
}
