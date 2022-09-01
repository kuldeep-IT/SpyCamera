package com.example.geofencemonitor.Model

data class ImageConfigurationModel(
    val `data`: Data,
    val error: Error,
    val success: Int
){
    data class Data(
        val photo_mode: String,
        val zoom_value: Int
    )

    class Error
}