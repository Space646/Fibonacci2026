package com.fibonacci.fibohealth

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import com.fibonacci.fibohealth.service.BleService
import com.fibonacci.fibohealth.ui.navigation.FiboHealthNavigation
import com.fibonacci.fibohealth.ui.theme.FiboHealthTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {}
        override fun onServiceDisconnected(name: ComponentName) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Start BleService as foreground
        ContextCompat.startForegroundService(this, Intent(this, BleService::class.java))
        bindService(Intent(this, BleService::class.java), serviceConnection, BIND_AUTO_CREATE)

        setContent {
            FiboHealthTheme {
                FiboHealthNavigation()
            }
        }
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        super.onDestroy()
    }
}
