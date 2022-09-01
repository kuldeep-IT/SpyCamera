package com.example.geofencemonitor.ViewModel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.example.geofencemonitor.RetrofitClient
import com.example.geofencemonitor.Model.GeofenceModel
import com.example.geofencemonitor.Model.ImageConfigurationModel
import com.example.geofencemonitor.Model.NotifyValueModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainViewModel : ViewModel() {

    fun getGeofence(context: Context, handleResponse: (response: GeofenceModel) -> Unit) {
        val call: Call<GeofenceModel> = RetrofitClient.instance!!.myApi.getGeofence()
        call.enqueue(object : Callback<GeofenceModel> {
            override fun onResponse(
                call: Call<GeofenceModel>,
                response: Response<GeofenceModel>
            ) {
                handleResponse(response.body()!!)
            }

            override fun onFailure(call: Call<GeofenceModel>, t: Throwable?) {
                Toast.makeText(context, "An error has occured", Toast.LENGTH_LONG).show()
            }
        })
    }

    fun getImageConfigurations(
        context: Context,
        handleResponse: (response: ImageConfigurationModel) -> Unit
    ) {
        val call: Call<ImageConfigurationModel> =
            RetrofitClient.instance!!.myApi.getImageConfiguration()
        call.enqueue(object : Callback<ImageConfigurationModel> {
            override fun onResponse(
                call: Call<ImageConfigurationModel>,
                response: Response<ImageConfigurationModel>
            ) {
                handleResponse(response.body()!!)
            }

            override fun onFailure(call: Call<ImageConfigurationModel>, t: Throwable?) {
                Toast.makeText(context, "An error has occured", Toast.LENGTH_LONG).show()
            }
        })
    }

    fun getNotifyValue(
        colorsArray: ArrayList<String>,
        context: Context,
        handleResponse: (response: NotifyValueModel) -> Unit
    ) {
        val call: Call<NotifyValueModel> = RetrofitClient.instance!!.myApi.getNotifyValue()
        call.enqueue(object : Callback<NotifyValueModel> {
            override fun onResponse(
                call: Call<NotifyValueModel>,
                response: Response<NotifyValueModel>
            ) {
                handleResponse(response.body()!!)
            }

            override fun onFailure(call: Call<NotifyValueModel>, t: Throwable?) {
                Toast.makeText(context, "An error has occured", Toast.LENGTH_LONG).show()
            }
        })
    }
}