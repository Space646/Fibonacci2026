package com.fibonacci.fibohealth.service

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.fibonacci.fibohealth.MainActivity
import com.fibonacci.fibohealth.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val CHANNEL_ID  = "ble_service"
private const val NOTIF_ID    = 1

@AndroidEntryPoint
class BleService : Service() {

    @Inject lateinit var bleClient: BleClient

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    inner class LocalBinder : Binder() { fun client() = bleClient }

    override fun onBind(intent: Intent): IBinder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("Scanning for Pi…"))
        bleClient.startScan()
        scope.launch {
            bleClient.isConnected.collectLatest { connected ->
                val text = if (connected) "Connected to AntiDonut Pi" else "Scanning for Pi…"
                val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIF_ID, buildNotification(text))
            }
        }
    }

    override fun onDestroy() { scope.cancel(); bleClient.disconnect(); super.onDestroy() }

    private fun buildNotification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val disconnectIntent = PendingIntent.getService(
            this, 1,
            Intent(this, BleService::class.java).setAction("DISCONNECT"),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AntiDonut")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_bluetooth)
            .setContentIntent(openIntent)
            .apply {
                if (bleClient.isConnected.value)
                    addAction(0, "Disconnect", disconnectIntent)
            }
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "DISCONNECT") bleClient.disconnect()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "Bluetooth", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
    }
}
