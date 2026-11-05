package com.rozkaam.app.data.model

/**
 * Task category data model corresponding to Firestore config/categories.
 * SPEC.md Section A2 & A6.
 */
data class TaskCategory(
    val id: String = "",
    val label: String = "",
    val icon: String = ""
) {
    companion object {
        fun fromMap(id: String, map: Map<String, Any?>?): TaskCategory {
            if (map == null) return TaskCategory(id = id)
            return TaskCategory(
                id = (map["id"] as? String)?.takeIf { it.isNotBlank() } ?: id,
                label = (map["label"] as? String) ?: (map["name"] as? String) ?: id,
                icon = (map["icon"] as? String).orEmpty()
            )
        }
    }
}

