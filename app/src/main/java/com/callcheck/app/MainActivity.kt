package com.callcheck.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        web = WebView(this)
        setContentView(web)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        web.addJavascriptInterface(Bridge(this), "Native")
        web.loadUrl("file:///android_asset/checklist.html")

        ensureNotificationPermission()
        askBatteryExemption()
        NtfyService.start(this)
    }

    override fun onResume() {
        super.onResume()
        if (this::web.isInitialized) {
            web.evaluateJavascript("window.__reload && window.__reload();", null)
        }
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS")
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf("android.permission.POST_NOTIFICATIONS"), 1
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        NtfyService.start(this)
    }

    private fun askBatteryExemption() {
        try {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val i = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
                startActivity(i)
            }
        } catch (_: Exception) {
        }
    }

    /** WebView 안의 JS 가 호출하는 저장소 브리지. 각 메서드는 변경 후 전체 목록(JSON)을 돌려준다. */
    class Bridge(private val ctx: Context) {
        @JavascriptInterface fun list(): String = Store.listJson(ctx)

        @JavascriptInterface fun add(t: String): String {
            Store.add(ctx, t.toLong()); return Store.listJson(ctx)
        }

        @JavascriptInterface fun toggle(id: String): String {
            Store.toggle(ctx, id); return Store.listJson(ctx)
        }

        @JavascriptInterface fun setNote(id: String, note: String): String {
            Store.setNote(ctx, id, note); return Store.listJson(ctx)
        }

        @JavascriptInterface fun remove(id: String): String {
            Store.remove(ctx, id); return Store.listJson(ctx)
        }
    }
}
