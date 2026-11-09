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
        /**
         * Safe bundled fallback categories for development if Firestore config/categories
         * is temporarily unavailable or empty. Firestore remains the primary source.
         * SPEC.md Section A2 & A5.
         */
        val DEFAULT_CATEGORIES: List<TaskCategory> = listOf(
            TaskCategory(id = "cleaning", label = "Cleaning", icon = "cleaning"),
            TaskCategory(id = "delivery", label = "Delivery", icon = "delivery"),
            TaskCategory(id = "moving", label = "Moving", icon = "moving"),
            TaskCategory(id = "repair", label = "Repairs", icon = "repair"),
            TaskCategory(id = "gardening", label = "Gardening", icon = "gardening"),
            TaskCategory(id = "car_wash", label = "Car Wash", icon = "car_wash"),
            TaskCategory(id = "pet_care", label = "Pet Care", icon = "pet")
        )

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

