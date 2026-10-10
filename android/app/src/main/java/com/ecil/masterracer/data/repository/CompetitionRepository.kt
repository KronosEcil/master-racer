package com.ecil.masterracer.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ecil.masterracer.data.model.Competition
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CompetitionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Yarışmaları Firestore'dan gerçek zamanlı (realtime) dinler.
     * Çevrimdışı (offline) önbellek desteği devrededir.
     */
    fun getCompetitionsFlow(): Flow<List<Competition>> = callbackFlow {
        val collectionRef = firestore.collection("competitions")
            .orderBy("start_date", Query.Direction.ASCENDING)

        val listenerRegistration = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("CompetitionRepo", "Firestore dinleme hatası, yerel tohum verileri kullanılıyor: ${error.message}")
                trySend(getFallbackCompetitions())
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                val competitions = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Competition::class.java)?.apply {
                        if (this.id.isBlank()) this.id = doc.id
                    }
                }
                trySend(competitions)
            } else {
                Log.d("CompetitionRepo", "Firestore koleksiyonu boş veya henüz senkronize edilmedi.")
                trySend(getFallbackCompetitions())
            }
        }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    /**
     * Tek seferlik yenileme çağrısı (Pull-to-refresh).
     */
    suspend fun refreshCompetitions(): List<Competition> {
        return try {
            val snapshot = firestore.collection("competitions")
                .orderBy("start_date", Query.Direction.ASCENDING)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Competition::class.java)?.apply {
                        if (this.id.isBlank()) this.id = doc.id
                    }
                }
            } else {
                getFallbackCompetitions()
            }
        } catch (e: Exception) {
            Log.e("CompetitionRepo", "Yenileme hatası: ${e.message}")
            getFallbackCompetitions()
        }
    }

    /**
     * Firebase henüz kurulmamışken veya çevrimdışı ilk açılışta
     * kullanıcıya gösterilecek zengin başlangıç Masterlar yarışma verileri.
     */
    fun getFallbackCompetitions(): List<Competition> {
        return listOf(
            Competition(
                id = "tur-2026-02-21-turkiye-masterlar-salon-atletizm-sampiyonasi",
                title = "Türkiye Masterlar Salon Atletizm Şampiyonası",
                country = "TR",
                country_name = "Türkiye",
                city = "İstanbul (Ataköy)",
                start_date = "2026-02-21",
                end_date = "2026-02-22",
                venue_type = "indoor",
                categories = listOf("Sprint", "Engelli", "Atlamalar", "Atmalar", "Uzun Mesafe"),
                scope = "turkey",
                federation = "TAF",
                regulations_url = "https://taf.org.tr/reglamanlar/masterlar-salon-2026.pdf",
                website_url = "https://taf.org.tr",
                status = "scheduled"
            ),
            Competition(
                id = "rs-2026-03-07-balkan-masterlar-salon-sampiyonasi-bamaci",
                title = "Balkan Masterlar Salon Atletizm Şampiyonası (BAMACI)",
                country = "RS",
                country_name = "Sırbistan",
                city = "Belgrad",
                start_date = "2026-03-07",
                end_date = "2026-03-08",
                venue_type = "indoor",
                categories = listOf("Sprint", "Engelli", "Atlamalar", "Atmalar", "Uzun Mesafe"),
                scope = "balkan",
                federation = "BAMACS",
                regulations_url = "https://balkanmastersathletics.org/regulations/bamaci-2026.pdf",
                website_url = "https://balkanmastersathletics.org",
                status = "scheduled"
            ),
            Competition(
                id = "pl-2026-03-15-ema-european-masters-championships-indoor",
                title = "EMA Avrupa Masterlar Salon Atletizm Şampiyonası (EMACI)",
                country = "PL",
                country_name = "Polonya",
                city = "Toruń",
                start_date = "2026-03-15",
                end_date = "2026-03-22",
                venue_type = "indoor",
                categories = listOf("Sprint", "Engelli", "Atlamalar", "Atmalar", "Uzun Mesafe"),
                scope = "europe",
                federation = "EMA",
                regulations_url = "https://european-masters-athletics.org/regulations/emaci-2026.pdf",
                website_url = "https://torun2026.com",
                status = "scheduled"
            ),
            Competition(
                id = "tur-2026-07-04-turkiye-masterlar-acik-saha-sampiyonasi",
                title = "Türkiye Masterlar Açık Saha Atletizm Şampiyonası",
                country = "TR",
                country_name = "Türkiye",
                city = "İzmir (Seha Aksoy Pisti)",
                start_date = "2026-07-04",
                end_date = "2026-07-05",
                venue_type = "outdoor",
                categories = listOf("Sprint", "Engelli", "Atlamalar", "Atmalar", "Uzun Mesafe"),
                scope = "turkey",
                federation = "TAF",
                regulations_url = "https://taf.org.tr/reglamanlar/masterlar-pist-2026.pdf",
                website_url = "https://taf.org.tr",
                status = "scheduled"
            ),
            Competition(
                id = "kor-2026-08-22-wma-world-masters-athletics-championships",
                title = "WMA Dünya Masterlar Atletizm Şampiyonası",
                country = "KOR",
                country_name = "Güney Kore",
                city = "Daegu",
                start_date = "2026-08-22",
                end_date = "2026-09-03",
                venue_type = "outdoor",
                categories = listOf("Sprint", "Engelli", "Atlamalar", "Atmalar", "Uzun Mesafe"),
                scope = "world",
                federation = "WMA",
                regulations_url = "https://world-masters-athletics.org/regulations/wma-daegu-2026.pdf",
                website_url = "https://daegu2026wma.org",
                status = "scheduled"
            ),
            Competition(
                id = "gr-2026-09-18-balkan-masterlar-acik-saha-sampiyonasi-bamacs",
                title = "Balkan Masterlar Açık Saha Atletizm Şampiyonası (BAMACS)",
                country = "GR",
                country_name = "Yunanistan",
                city = "Selanik (Kaftanzoglio Stadyumu)",
                start_date = "2026-09-18",
                end_date = "2026-09-20",
                venue_type = "outdoor",
                categories = listOf("Sprint", "Engelli", "Atlamalar", "Atmalar", "Uzun Mesafe"),
                scope = "balkan",
                federation = "BAMACS",
                regulations_url = "https://balkanmastersathletics.org/regulations/bamacs-2026.pdf",
                website_url = "https://balkanmastersathletics.org",
                status = "scheduled"
            ),
            Competition(
                id = "tur-2026-10-18-ankara-masterlar-sonbahar-pisti",
                title = "Ankara Masterlar Sonbahar Pisti & Çoklu Branş Yarışması",
                country = "TR",
                country_name = "Türkiye",
                city = "Ankara (Naili Moran Pisti)",
                start_date = "2026-10-18",
                end_date = "2026-10-19",
                venue_type = "outdoor",
                categories = listOf("Sprint", "Atlamalar", "Atmalar"),
                scope = "regional",
                federation = "TAF",
                regulations_url = "https://taf.org.tr/reglamanlar/ankara-master-2026.pdf",
                website_url = "https://taf.org.tr",
                status = "scheduled"
            )
        )
    }
}

