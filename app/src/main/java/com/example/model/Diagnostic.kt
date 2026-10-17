package com.example.model

enum class CheckStatus {
    HEALTHY,
    WARNING,
    CRITICAL,
    NOT_AVAILABLE
}

data class DiagnosticCheck(
    val id: String,
    val name: String,
    val category: String,
    val status: CheckStatus,
    val summary: String,
    val details: String,
    val possibleCause: String? = null,
    val repairable: Boolean = false,
    val repairProcedureId: String? = null
)

data class RepairProcedure(
    val id: String,
    val title: String,
    val description: String,
    val targetCheckId: String,
    val requiresUserApproval: Boolean,
    val prerequisites: List<String>,
    val potentialSideEffects: String,
    val maxRetries: Int = 3
)

data class RepairOutcome(
    val procedureId: String,
    val success: Boolean,
    val stepResults: List<String>,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
