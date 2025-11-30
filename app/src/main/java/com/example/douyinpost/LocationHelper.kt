package com.example.douyinpost

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import java.util.Locale

/**
 * 简单的定位工具类
 * 负责：获取经纬度 -> 反向编码为城市名
 */
object LocationHelper {

    interface LocationCallback {
        fun onCityFound(city: String)
        fun onFailure(error: String)
    }
    @SuppressLint("MissingPermission")
    fun getCurrentCity(context: Context, callback: LocationCallback) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        //检查权限
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            callback.onFailure("没有定位权限")
            return
        }
        //获取最后一次已知位置
            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    decodeCity(context, location, callback)
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {
                    callback.onFailure("定位服务未开启")
                }
            }, null)

    }

    // 反向地理编码：经纬度 -> 城市名
    private fun decodeCity(context: Context, location: Location, callback: LocationCallback) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                // 优先取 locality (城市)，没有就取 subAdminArea (区)，再没有就取 adminArea (省)
                val city = address.locality ?: address.subAdminArea ?: address.adminArea ?: "未知城市"
                callback.onCityFound(city)
            } else {
                callback.onFailure("无法识别当前位置")
            }
        } catch (e: Exception) {
            Log.e("LocationHelper", "Geocoder failed", e)
            callback.onFailure("地理编码失败: ${e.message}")
        }
    }
}