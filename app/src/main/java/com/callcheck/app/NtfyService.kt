package com.callcheck.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * ntfy 토픽에 상시 연결(WebSocket)해 콜 알림을 수신하는 포그라운드 서비스.
 * 메시지 수신 시: (1) 콜 기록을 Store 에 자동 추가, (2) 고중요도 알림 표시(워치 미러링).
 */
class NtfyService : Service() {

    private var client: OkHttpClient? = null
    private var ws: WebSocket? = null
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var stopped = false

    companion object {
        const val CH_FG = "callcheck_fg"
        const val CH_ALERT = "callcheck_alert"
        const val FG_ID = 1

        fun start(ctx: Context) {
            val i = Intent(ctx, NtfyService::class.java)
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannels()
        startForeground(FG_ID, foregroundNotification())
        client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
        connect()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun connect() {
        if (stopped) return
        val url = "wss://" + Config.SERVER_HOST + "/" + Config.TOPIC + "/ws"
        val req = Request.Builder().url(url).build()
        ws = client?.newWebSocket(req, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) = handleMessage(text)
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = scheduleReconnect()
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = scheduleReconnect()
        })
    }

    private fun scheduleReconnect() {
        if (stopped) return
        handler.postDelayed({ connect() }, 5000)
    }

    private fun handleMessage(text: String) {
        try {
            val o = JSONObject(text)
            // ntfy 이벤트: open / keepalive / message — 실제 알림만 처리
            if (o.optString("event") != "message") return
            val title = o.optString("title", "📞 콜 도착")
            val msg = o.optString("message", "")
            val tms = if (o.has("time")) o.optLong("time") * 1000L else System.currentTimeMillis()
            Store.add(this, tms)
            notifyCall(title, msg)
        } catch (_: Exception) {
        }
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            val fg = NotificationChannel(CH_FG, "백그라운드 연결", NotificationManager.IMPORTANCE_MIN)
            fg.description = "콜 알림 수신을 위해 연결을 유지합니다"
            nm.createNotificationChannel(fg)
            val alert = NotificationChannel(CH_ALERT, "콜 알림", NotificationManager.IMPORTANCE_HIGH)
            alert.enableVibration(true)
            nm.createNotificationChannel(alert)
        }
    }

    private fun foregroundNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CH_FG)
            .setContentTitle("콜 체크리스트 실행 중")
            .setContentText("콜 알림을 수신 대기 중입니다")
            .setSmallIcon(R.drawable.ic_stat)
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun notifyCall(title: String, msg: String) {
        val pi = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(this, CH_ALERT)
            .setContentTitle(title)
            .setContentText(msg)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        getSystemService(NotificationManager::class.java)
            .notify((System.currentTimeMillis() % 100000).toInt(), n)
    }

    override fun onDestroy() {
        stopped = true
        try { ws?.close(1000, "bye") } catch (_: Exception) {}
        // 서비스가 종료되면 다시 띄우도록 재시작 요청
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(Intent(this, NtfyService::class.java))
        } catch (_: Exception) {}
        super.onDestroy()
    }
}
