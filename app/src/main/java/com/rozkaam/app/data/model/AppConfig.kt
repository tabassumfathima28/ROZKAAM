package com.rozkaam.app.data.model

/**
 * Application configuration loaded dynamically from Firestore config/app
 * per SPEC.md Section A6.
 *
 * quickAmounts: Predefined top-up amounts in paise (e.g. ₹500, ₹1000, ₹2000, ₹5000)
 * minPrice: Minimum task/top-up amount in paise
 * maxPrice: Maximum task/top-up amount in paise
 */
data class AppConfig(
    val quickAmounts: List<Long> = listOf(50000L, 100000L, 200000L, 500000L), // ₹500, ₹1,000, ₹2,000, ₹5,000
    val minPrice: Long = 10000L,       // ₹100
    val maxPrice: Long = 1000000L,     // ₹10,000
    val arrivalRadiusMeters: Int = 100,
    val autoReleaseHours: Int = 24
) {
    val minPriceRupees: Long
        get() = minPrice / 100

    val maxPriceRupees: Long
        get() = maxPrice / 100

    companion object {
        fun fromMap(map: Map<String, Any?>?): AppConfig {
            if (map == null) return AppConfig()
            val rawQuick = map["quickAmounts"] as? List<*>
            val parsedQuick = rawQuick?.mapNotNull { (it as? Number)?.toLong() }?.filter { it > 0 }
            val quickAmounts = if (!parsedQuick.isNullOrEmpty()) parsedQuick else listOf(50000L, 100000L, 200000L, 500000L)

            val minPrice = (map["minPrice"] as? Number)?.toLong() ?: 10000L
            val maxPrice = (map["maxPrice"] as? Number)?.toLong() ?: 1000000L
            val arrivalRadiusMeters = (map["arrivalRadiusMeters"] as? Number)?.toInt() ?: 100
            val autoReleaseHours = (map["autoReleaseHours"] as? Number)?.toInt() ?: 24

            return AppConfig(
                quickAmounts = quickAmounts,
                minPrice = minPrice,
                maxPrice = maxPrice,
                arrivalRadiusMeters = arrivalRadiusMeters,
                autoReleaseHours = autoReleaseHours
            )
        }
    }
}

