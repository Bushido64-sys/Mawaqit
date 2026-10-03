package com.mawaqit.app.util

/** Great-circle bearing from (userLat, userLon) to Mecca, degrees 0–360 clockwise from north. */
object QiblaCalculator {
    private const val MECCA_LAT = 21.4225
    private const val MECCA_LON = 39.8262

    fun calculateQiblaBearing(userLat: Double, userLon: Double): Float {
        val lat1 = Math.toRadians(userLat)
        val lat2 = Math.toRadians(MECCA_LAT)
        val dLon = Math.toRadians(MECCA_LON - userLon)
        val y = kotlin.math.sin(dLon) * kotlin.math.cos(lat2)
        val x = kotlin.math.cos(lat1) * kotlin.math.sin(lat2) -
            kotlin.math.sin(lat1) * kotlin.math.cos(lat2) * kotlin.math.cos(dLon)
        return ((Math.toDegrees(kotlin.math.atan2(y, x)).toFloat()) + 360f) % 360f
    }
}
