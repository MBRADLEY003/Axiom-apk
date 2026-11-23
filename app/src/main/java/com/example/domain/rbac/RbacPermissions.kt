package com.example.domain.rbac

import com.example.domain.model.AxiomRole
import java.security.MessageDigest

enum class AxiomPermission {
    MANAGE_COMPANY_AND_ROLES,
    MANAGE_FLEET_VEHICLES,
    DELETE_VEHICLES,
    LOG_FUEL_TRANSACTIONS,
    MANAGE_TRIPS_AND_GEOFENCES,
    MANAGE_MAINTENANCE,
    MANAGE_SHIPMENTS,
    CONFIRM_DELIVERY,
    MANAGE_FINANCE_BUDGETS,
    RESOLVE_RISK_ALERTS,
    EXPORT_REPORTS,
    IMPORT_DATA,
    QUERY_OTTILIA_AI,
    VIEW_AUDIT_LOGS
}

object RbacPermissions {
    private val rolePermissions: Map<AxiomRole, Set<AxiomPermission>> = mapOf(
        AxiomRole.SUPER_ADMIN to AxiomPermission.entries.toSet(),
        AxiomRole.FLEET_MANAGER to setOf(
            AxiomPermission.MANAGE_FLEET_VEHICLES,
            AxiomPermission.DELETE_VEHICLES,
            AxiomPermission.LOG_FUEL_TRANSACTIONS,
            AxiomPermission.MANAGE_TRIPS_AND_GEOFENCES,
            AxiomPermission.MANAGE_MAINTENANCE,
            AxiomPermission.RESOLVE_RISK_ALERTS,
            AxiomPermission.EXPORT_REPORTS,
            AxiomPermission.IMPORT_DATA,
            AxiomPermission.QUERY_OTTILIA_AI,
            AxiomPermission.VIEW_AUDIT_LOGS
        ),
        AxiomRole.OPERATIONS_MANAGER to setOf(
            AxiomPermission.MANAGE_TRIPS_AND_GEOFENCES,
            AxiomPermission.MANAGE_SHIPMENTS,
            AxiomPermission.CONFIRM_DELIVERY,
            AxiomPermission.LOG_FUEL_TRANSACTIONS,
            AxiomPermission.RESOLVE_RISK_ALERTS,
            AxiomPermission.EXPORT_REPORTS,
            AxiomPermission.QUERY_OTTILIA_AI
        ),
        AxiomRole.FINANCE_MANAGER to setOf(
            AxiomPermission.MANAGE_FINANCE_BUDGETS,
            AxiomPermission.EXPORT_REPORTS,
            AxiomPermission.IMPORT_DATA,
            AxiomPermission.QUERY_OTTILIA_AI,
            AxiomPermission.VIEW_AUDIT_LOGS
        ),
        AxiomRole.DATA_ANALYST to setOf(
            AxiomPermission.RESOLVE_RISK_ALERTS,
            AxiomPermission.EXPORT_REPORTS,
            AxiomPermission.IMPORT_DATA,
            AxiomPermission.QUERY_OTTILIA_AI,
            AxiomPermission.VIEW_AUDIT_LOGS
        ),
        AxiomRole.DRIVER to setOf(
            AxiomPermission.LOG_FUEL_TRANSACTIONS,
            AxiomPermission.CONFIRM_DELIVERY,
            AxiomPermission.QUERY_OTTILIA_AI
        ),
        AxiomRole.AUDITOR to setOf(
            AxiomPermission.EXPORT_REPORTS,
            AxiomPermission.QUERY_OTTILIA_AI,
            AxiomPermission.VIEW_AUDIT_LOGS
        )
    )

    fun hasPermission(role: AxiomRole, permission: AxiomPermission): Boolean {
        return rolePermissions[role]?.contains(permission) == true
    }

    fun permissionsFor(role: AxiomRole): Set<AxiomPermission> {
        return rolePermissions[role].orEmpty()
    }
}

object SecurityHashUtil {
    fun hashPinSha256(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(pin: String, salt: String, expectedHash: String?): Boolean {
        if (expectedHash.isNullOrBlank()) return true
        return hashPinSha256(pin, salt) == expectedHash
    }
}
