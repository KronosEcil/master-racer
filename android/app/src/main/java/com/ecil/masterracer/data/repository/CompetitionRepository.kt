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
            ),
            Competition(
                id = "fr-2027-01-22-meeting-miramas-metropole",
                title = "Meeting Miramas Métropole (Masters & Elite Indoor)",
                country = "FR",
                country_name = "Fransa",
                city = "Miramas (Stadium Miramas Métropole)",
                start_date = "2027-01-22",
                end_date = "2027-01-23",
                venue_type = "indoor",
                categories = listOf("Sprint", "Engelli", "Atlamalar", "Uzun Mesafe"),
                scope = "regional",
                federation = "FFA",
                regulations_url = "https://www.athle.fr/reglements/meeting-miramas-2027.pdf",
                website_url = "https://www.athle.fr",
                status = "scheduled"
            ),
            Competition(
                id = "de-2027-01-23-landeshallenmeisterschaften-masters-winterwurf",
                title = "Landeshallenmeisterschaften Masters + Winterwurf 2027",
                country = "DE",
                country_name = "Almanya",
                city = "Chemnitz / Halle",
                start_date = "2027-01-23",
                end_date = "2027-01-24",
                venue_type = "indoor",
                categories = listOf("Atmalar", "Sprint", "Atlamalar", "Engelli"),
                scope = "regional",
                federation = "DLV",
                regulations_url = "https://www.leichtathletik.de/ausschreibungen/landeshallenmeisterschaften-masters-winterwurf-2027.pdf",
                website_url = "https://www.leichtathletik.de",
                status = "scheduled"
            ),
            Competition(
                id = "fr-2027-01-30-meeting-europeen-masters-en-salle-ema-league",
                title = "Meeting européen Masters en salle / EMA League",
                country = "EU",
                country_name = "Avrupa / Fransa",
                city = "Lyon / Miramas",
                start_date = "2027-01-30",
                end_date = "2027-01-31",
                venue_type = "indoor",
                categories = listOf("Sprint", "Engelli", "Atlamalar", "Atmalar"),
                scope = "europe",
                federation = "EMA",
                regulations_url = "https://european-masters-athletics.org/regulations/ema-league-indoor-2027.pdf",
                website_url = "https://european-masters-athletics.org",
                status = "scheduled"
            ),
            Competition(
                id = "de-2027-02-06-28-internationales-hallenmeeting",
                title = "28. Internationales Leichtathletik-Hallenmeeting (Masters & Aktive)",
                country = "DE",
                country_name = "Almanya",
                city = "Erfurt / Chemnitz",
                start_date = "2027-02-06",
                end_date = "2027-02-06",
                venue_type = "indoor",
                categories = listOf("Sprint", "Engelli", "Atlamalar", "Uzun Mesafe"),
                scope = "regional",
                federation = "DLV",
                regulations_url = "https://www.leichtathletik.de/ausschreibungen/28-hallenmeeting-2027.pdf",
                website_url = "https://www.leichtathletik.de",
                status = "scheduled"
            ),
            Competition(
                id = "fr-2027-02-19-fransa-masters-salon-sampiyonasi-2027",
                title = "Fransa Masters Salon Şampiyonası (Championnats de France Masters en Salle 2027)",
                country = "FR",
                country_name = "Fransa",
                city = "Nantes (Pierre-Quinon)",
                start_date = "2027-02-19",
                end_date = "2027-02-21",
                venue_type = "indoor",
                categories = listOf("Sprint", "Engelli", "Atlamalar", "Atmalar", "Uzun Mesafe"),
                scope = "regional",
                federation = "FFA",
                regulations_url = "https://www.athle.fr/reglements/masters-indoor-2027.pdf",
                website_url = "https://www.athle.fr",
                status = "scheduled"
            ),
            Competition(
                id = "de-2027-02-27-almanya-masters-salon-sampiyonasi-2027",
                title = "Almanya Masters Salon Şampiyonası (Deutsche Hallenmeisterschaften Masters 2027)",
                country = "DE",
                country_name = "Almanya",
                city = "Dortmund (Helmut-Körnig-Halle)",
                start_date = "2027-02-27",
                end_date = "2027-02-28",
                venue_type = "indoor",
                categories = listOf("Sprint", "Engelli", "Atlamalar", "Atmalar", "Uzun Mesafe"),
                scope = "regional",
                federation = "DLV",
                regulations_url = "https://www.leichtathletik.de/ausschreibungen/dm-masters-halle-2027.pdf",
                website_url = "https://www.leichtathletik.de",
                status = "scheduled"
            ),
            Competition(
                id = "wma-2027-05-14-world-masters-games-2027",
                title = "World Masters Games 2027 (Dünya Masterlar Oyunları)",
                country = "INT",
                country_name = "Uluslararası / Kansai",
                city = "Kansai / Taipei",
                start_date = "2027-05-14",
                end_date = "2027-05-23",
                venue_type = "outdoor",
                categories = listOf("Sprint", "Engelli", "Atlamalar", "Atmalar", "Uzun Mesafe"),
                scope = "world",
                federation = "IMGA / WMA",
                regulations_url = "https://wmg2027.jp/en/regulations.pdf",
                website_url = "https://wmg2027.jp",
                status = "scheduled"
            )
        )
    }
}

