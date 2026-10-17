package com.example.model

import java.util.UUID

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

enum class StepStatus {
    PENDING,
    IN_PROGRESS,
    SUCCESS,
    FAILED,
    SKIPPED
}

data class PlanStep(
    val id: String = UUID.randomUUID().toString(),
    val index: Int,
    val title: String,
    val description: String,
    val tool: String,
    val actionCommand: String? = null,
    val requiredPermission: String? = null,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    var status: StepStatus = StepStatus.PENDING,
    var resultMessage: String? = null
)

data class TaskPlan(
    val id: String = UUID.randomUUID().toString(),
    val userInstruction: String,
    val intent: String,
    val requiredTools: List<String>,
    val requiredPermissions: List<String>,
    val expectedOutcome: String,
    val potentialRisks: RiskLevel,
    val steps: List<PlanStep>,
    val verificationMethod: String,
    val timestamp: Long = System.currentTimeMillis(),
    var isApprovedByHuman: Boolean = false,
    var isCompleted: Boolean = false
)
