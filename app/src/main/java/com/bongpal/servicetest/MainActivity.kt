package com.bongpal.servicetest

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.bongpal.servicetest.databinding.ActivityMainBinding

const val TAG = "service_test"
const val IS_BIND = "isBind"

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private var isStart = false
    private val serviceIntent by lazy { Intent(this, MyService::class.java) }
    private val pref by lazy { getSharedPreferences(IS_BIND, 0) }
    var myService: MyService? = null
    private val connection: ServiceConnection by lazy { makeServiceConnection() }

    override fun onStart() {
        super.onStart()

        bindService(serviceIntent, connection, BIND_AUTO_CREATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        isStart = pref.getBoolean(IS_BIND, false)
        initServiceButton()
    }

    private fun initServiceButton() {
        with(binding) {
            btnServiceStart.setOnClickListener {
                startService()
                pref.putIsStart(isStart)
            }

            btnServiceStop.setOnClickListener {
                stopService()
                pref.putIsStart(isStart)
            }
        }
    }

    private fun SharedPreferences.putIsStart(isStart: Boolean) {
        edit().run {
            putBoolean(IS_BIND, isStart)
            apply()
        }
    }

    private fun startService() {
        if (isStart.not()) {
            startForegroundService(serviceIntent)
            bindService(serviceIntent, connection, BIND_AUTO_CREATE)
            isStart = true
            return
        }
    }

    private fun stopService() {
        if (isStart) {
            stopService(serviceIntent)
            unbindService(connection)
            isStart = false
            return
        }
    }


    private fun makeServiceConnection(): ServiceConnection {
        return object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as MyService.MyBinder

                myService = binder.getService()
                myService?.run {
                    count.observe(this@MainActivity) {
                        binding.tvCountText.text = "count: $it"
                    }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                isStart = false
            }
        }
    }
}