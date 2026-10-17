package com.example.audit

import com.example.model.AuditCategory
import com.example.model.AuditEntry
import com.example.model.AuditSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections
import java.util.LinkedList

object AuditLogger {
    private val maxEntries = 250
    private val lock = Any()
    private val entriesList = LinkedList<AuditEntry>()
    private val _auditFlow = MutableStateFlow<List<AuditEntry>>(emptyList())
    val auditFlow: StateFlow<List<AuditEntry>> = _auditFlow.asStateFlow()

    fun log(
        category: AuditCategory,
        severity: AuditSeverity,
        action: String,
        details: String,
        outcome: String
    ) {
        val sanitizedDetails = sanitize(details)
        val entry = AuditEntry(
            category = category,
            severity = severity,
            action = action,
            details = sanitizedDetails,
            outcome = outcome
        )
        synchronized(lock) {
            entriesList.addFirst(entry)
            if (entriesList.size > maxEntries) {
                entriesList.removeLast()
            }
            _auditFlow.value = ArrayList(entriesList)
        }
    }

    fun getRecent(limit: Int = 50): List<AuditEntry> {
        synchronized(lock) {
            return entriesList.take(limit)
        }
    }

    fun clear() {
        synchronized(lock) {
            entriesList.clear()
            _auditFlow.value = emptyList()
        }
    }

    private fun sanitize(input: String): String {
        // Redact any patterns resembling API keys or secret tokens
        return input
            .replace(Regex("(?i)AIza[0-9A-Za-z\\-_]{20,}"), "[REDACTED_API_KEY]")
            .replace(Regex("(?i)sk-[a-zA-Z0-9_-]{20,}"), "[REDACTED_API_KEY]")
            .replace(Regex("(?i)gsk_[a-zA-Z0-9_-]{20,}"), "[REDACTED_API_KEY]")
            .replace(Regex("(?i)key=[^&\\s]+"), "key=[REDACTED]")
            .replace(Regex("(?i)password=[^&\\s]+"), "password=[REDACTED]")
    }
}
