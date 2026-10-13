package com.ecil.masterracer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.messaging.FirebaseMessaging

class MasterRacerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        try {
            // Firebase Başlatma
            FirebaseApp.initializeApp(this)

            // Firestore Çevrimdışı Önbellekleme (Offline Persistence) Yapılandırması
            val firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            firestore.firestoreSettings = settings

            // Genel Yarışma Bildirim Kanalına Otomatik Abone Olma (0 TL Push Notification)
            FirebaseMessaging.getInstance().subscribeToTopic("competitions_all")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("MasterRacerApp", "FCM 'competitions_all' konusuna abone olundu.")
                    }
                }
        } catch (e: Exception) {
            Log.e("MasterRacerApp", "Firebase ilklendirme hatası (Geliştirme modu devrede): ${e.message}")
        }

        // Android 8.0+ Bildirim Kanalını Oluştur
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        lateinit var instance: MasterRacerApp
            private set
        const val NOTIFICATION_CHANNEL_ID = "competitions_channel"
    }
}

