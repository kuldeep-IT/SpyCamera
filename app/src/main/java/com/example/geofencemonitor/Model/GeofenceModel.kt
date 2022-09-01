package com.example.geofencemonitor.Model

data class GeofenceModel(
    val `data`: Data,
    val error: Error,
    val success: Int
) {
    data class Data(
        val latitude: Double,
        val longitude: Double,
        val radius: Int
    )

    class Error
}