package com.bongpal.servicetest

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyService : LifecycleService() {
    private var _count = MutableLiveData<Int>(0)
    val count: LiveData<Int> get() = _count
    var job: Job? = null

    private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }
    val notificationBuilder by lazy { createBuilder() }

    inner class MyBinder: Binder() {
        fun getService(): MyService {
            return this@MyService
        }
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)

        return MyBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(1, createNotification())
        job = GlobalScope.launch {
            while (true) {
                val cnt = _count.value ?: 0
                Log.i(TAG, "count: ${cnt}")

                delay(1000L)
                withContext(Dispatchers.Main) {
                    _count.value = cnt + 1
                }
            }
        }
        initObserver()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        stopSelf()
        _count.value = 0
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        notificationBuilder.apply {
            setSmallIcon(R.mipmap.ic_launcher_round)
            setWhen(System.currentTimeMillis())
            setContentTitle("백그라운드 동작중")
            setContentText("백그라운드에서 실행중입니다.")
            addAction(R.mipmap.ic_launcher_round, "Action", pendingIntent)
        }
        return notificationBuilder.build()
    }

    private fun createBuilder(): NotificationCompat.Builder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = createNotiChannel(CHANNEL_ID_01, CHANNEL_NAME_01, "notification")

            notificationManager.createNotificationChannel(channel)
            NotificationCompat.Builder(this, CHANNEL_ID_01)
        } else {
            NotificationCompat.Builder(this)
        }
    }

    private fun createNotiChannel(id: String, name: String, descript: String): NotificationChannel {
        return NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = descript
            setShowBadge(true)
        }
    }

    private fun initObserver() {
        count.observe(this) {
            val update = notificationBuilder
                .setSound(null)
                .setContentText("count: $it")
                .build()

            notificationManager.notify(1, update)
        }
    }
}

const val CHANNEL_ID_01 = "01"
const val CHANNEL_NAME_01 = "one"