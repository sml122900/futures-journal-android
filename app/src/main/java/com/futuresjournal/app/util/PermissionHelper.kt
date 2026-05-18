package com.futuresjournal.app.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionHelper {

    const val REQUEST_OVERLAY = 1001
    const val REQUEST_NOTIFICATION = 1002

    fun hasOverlayPermission(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    fun requestOverlayPermission(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${activity.packageName}")
        )
        activity.startActivityForResult(intent, REQUEST_OVERLAY)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATION
            )
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestIgnoreBatteryOptimizations(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${activity.packageName}")
        )
        activity.startActivity(intent)
    }

    fun getManufacturerGuideMessage(): String? {
        return when (Build.MANUFACTURER.lowercase()) {
            "xiaomi", "redmi" ->
                "샤오미: 설정 > 앱 > FuturesJournal > 자동 시작 허용 및 백그라운드 실행 허용"
            "huawei", "honor" ->
                "화웨이: 설정 > 배터리 > 앱 시작 관리 > FuturesJournal 수동 관리 설정"
            "oppo", "realme", "oneplus" ->
                "OPPO/OnePlus: 설정 > 배터리 > 배터리 최적화 > FuturesJournal 최적화 안 함"
            else -> null
        }
    }
}
