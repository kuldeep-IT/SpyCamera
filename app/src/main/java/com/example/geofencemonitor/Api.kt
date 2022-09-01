package com.example.geofencemonitor

import com.example.geofencemonitor.Model.GeofenceModel
import com.example.geofencemonitor.Model.ImageConfigurationModel
import com.example.geofencemonitor.Model.NotifyValueModel
import retrofit2.Call
import retrofit2.http.GET

interface Api {
    @GET("getGeofenceSettings")
    fun getGeofence(): Call<GeofenceModel>

    @GET("getCameraSettings")
    fun getImageConfiguration(): Call<ImageConfigurationModel>

    @GET("getNotifValue")
    fun getNotifyValue(): Call<NotifyValueModel>

    companion object {
        const val BASE_URL = "https://s7wofearnbacee653spizygt9p82.requestly.me/"
    }
}