package com.rozkaam.app.data.model

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppNotification(
    val id: String,
    val taskId: String,
    val title: String,
    val message: String,
    val createdAt: Long,
    val read: Boolean
) {
    val formattedDate: String
        get() = if (createdAt == 0L) {
            ""
        } else {
            SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(createdAt))
        }

    companion object {
        fun fromMap(id: String, data: Map<String, Any?>?): AppNotification {
            val createdAt = when (val value = data?.get("createdAt")) {
                is Timestamp -> value.toDate().time
                is Number -> value.toLong()
                else -> 0L
            }
            return AppNotification(
                id = id,
                taskId = data?.get("taskId") as? String ?: "",
                title = data?.get("title") as? String ?: "",
                message = data?.get("message") as? String ?: "",
                createdAt = createdAt,
                read = data?.get("read") as? Boolean ?: false
            )
        }
    }
}
