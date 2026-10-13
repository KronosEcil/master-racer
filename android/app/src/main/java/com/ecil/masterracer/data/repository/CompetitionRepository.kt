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
                trySend(cleanAndDeduplicate(competitions))
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
                val competitions = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Competition::class.java)?.apply {
                        if (this.id.isBlank()) this.id = doc.id
                    }
                }
                cleanAndDeduplicate(competitions)
            } else {
                getFallbackCompetitions()
            }
        } catch (e: Exception) {
            Log.e("CompetitionRepo", "Yenileme hatası: ${e.message}")
            getFallbackCompetitions()
        }
    }

    /**
     * Gelen yarışma listesini sahte doküman başlıklarından (Technical Manual, Timetable vb.)
     * ve hatalı tarihlerden arındırır, aynı yarışmanın birden fazla kaydını tekilleştirir.
     */
    private fun cleanAndDeduplicate(list: List<Competition>): List<Competition> {
        val invalidKeywords = listOf(
            "technical manual", "timetable", "schedule", "registration",
            "registrations", "preliminary schedule", "hotel", "hotels",
            "accommodation", "manual", "record", "records", "results",
            "bulletin", "circular"
        )

        return list
            .filter { comp ->
                val titleLower = comp.title.lowercase().trim()
                val isInvalidTitle = invalidKeywords.any { kw ->
                    titleLower == kw || titleLower.startsWith("$kw ") || titleLower.contains("technical manual") || titleLower.contains("registrations")
                } || titleLower.contains("home »") || titleLower.contains("»")

                // 2006 veya geçersiz/eksik tarihleri filtrele
                val isBadDate = comp.start_date.startsWith("2006") || comp.start_date.length < 8
                val startYear = comp.start_date.take(4)

                // 1. BAMACS Craiova 2026 yılındadır; 2027 tarihli hatalı kopyayı filtrele
                val isWrongBamacs2027 = (titleLower.contains("craiova") || titleLower.contains("bamacs")) && startYear != "2026"

                // 2. Lucerne / EMAMRC 2027 yılındadır (31 Ekim 2027); 2026 veya 2028 kopyalarını filtrele
                val isWrongLucerne = (titleLower.contains("lucerne") || titleLower.contains("emamrc")) && startYear != "2027"

                // 3. Galway / EMACNS 2028 yılındadır (18-21 Mayıs 2028); 2027 kopyasını filtrele
                val isWrongGalway = (titleLower.contains("galway") || titleLower.contains("emacns")) && startYear != "2028"

                // 4. Jyväskylä / EMACS 2027 yılındadır (11-22 Ağustos 2027); 2028 kopyasını filtrele
                val isWrongJyvaskyla = (titleLower.contains("jyväskylä") || titleLower.contains("jyvaskyla") || titleLower.contains("emacs")) && startYear != "2027"

                // 5. Ojós / EMMTRC 2027 yılındadır (15-18 Nisan 2027); 2028 kopyasını filtrele
                val isWrongOjos = (titleLower.contains("ojós") || titleLower.contains("ojos") || titleLower.contains("emmtr")) && startYear != "2027"

                !isInvalidTitle && !isBadDate && !isWrongBamacs2027 && !isWrongLucerne && !isWrongGalway && !isWrongJyvaskyla && !isWrongOjos
            }
            .distinctBy { comp ->
                val titleLower = comp.title.lowercase()
                val normCity = comp.city.lowercase().replace(Regex("[^a-z0-9]"), "")
                val datePart = comp.start_date.take(10)
                val countryPart = comp.country.lowercase()

                when {
                    titleLower.contains("craiova") || normCity.contains("craiova") -> "ro_craiova_bamacs"
                    titleLower.contains("novo mesto") || normCity.contains("novomesto") || titleLower.contains("bamaci") -> "si_novomesto_bamaci"
                    titleLower.contains("zagreb") || (titleLower.contains("marathon") && countryPart == "hr") -> "hr_zagreb_marathon"
                    normCity.contains("bernhausen") || titleLower.contains("bernhausen") -> "de_bernhausen"

                    // EMA Resmi Şampiyonaları tekilleştirme:
                    titleLower.contains("emmtr") || normCity.contains("ojos") -> "ema_2027_emmtrc_ojos"
                    titleLower.contains("emacs") || normCity.contains("jyvaskyla") -> "ema_2027_emacs_jyvaskyla"
                    titleLower.contains("emamrc") || normCity.contains("lucerne") -> "ema_2027_emamrc_lucerne"
                    titleLower.contains("emaci") || normCity.contains("glasgow") -> "ema_2028_emaci_glasgow"
                    titleLower.contains("emacns") || normCity.contains("galway") -> "ema_2028_emacns_galway"

                    else -> {
                        val normTitleKey = titleLower
                            .replace("european masters", "em")
                            .replace("championships", "")
                            .replace("championship", "")
                            .replace(Regex("[^a-z0-9]"), "")
                            .take(15)
                        "${datePart}_${countryPart}_${if (normCity.isNotEmpty()) normCity else normTitleKey}"
                    }
                }
            }
    }

    /**
     * Yerel önbellekte veya ilk açılışta statik veri tutulmaz;
     * tüm veriler yalnızca canlı Firestore veritabanından çekilir.
     */
    fun getFallbackCompetitions(): List<Competition> {
        return emptyList()
    }
}

