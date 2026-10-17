package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class AuditSeverity {
    INFO,
    WARNING,
    ALERT,
    SECURITY
}

enum class AuditCategory {
    SYSTEM,
    PERMISSION,
    TERMINAL,
    TASK_EXECUTION,
    DIAGNOSTICS,
    SELF_REPAIR,
    VOICE,
    AI_QUERY
}

data class AuditEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val category: AuditCategory,
    val severity: AuditSeverity,
    val action: String,
    val details: String,
    val outcome: String
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
}
