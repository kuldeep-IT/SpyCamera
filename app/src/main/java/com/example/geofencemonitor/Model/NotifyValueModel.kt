package com.example.geofencemonitor.Model

data class NotifyValueModel(
    val `data`: Data,
    val error: Error,
    val success: Int
) {
    data class Data(
        val notify_value: Boolean
    )

    class Error
}