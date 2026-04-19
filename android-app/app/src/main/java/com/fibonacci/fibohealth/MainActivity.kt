package com.fibonacci.fibohealth

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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

    private val blePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> startBleService() }   // start regardless — user may have pre-granted

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val needed = arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            ).filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needed.isEmpty()) startBleService() else blePermissionLauncher.launch(needed.toTypedArray())
        } else {
            startBleService()
        }

        setContent {
            FiboHealthTheme {
                FiboHealthNavigation()
            }
        }
    }

    private fun startBleService() {
        ContextCompat.startForegroundService(this, Intent(this, BleService::class.java))
        bindService(Intent(this, BleService::class.java), serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        super.onDestroy()
    }
}
