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
                }
                // 2006 veya geçersiz/eksik tarihleri filtrele
                val isBadDate = comp.start_date.startsWith("2006") || comp.start_date.length < 8
                // BAMACS Craiova 2026 yılındadır; 2027 tarihli hatalı kopyayı filtrele
                val isWrongBamacs2027 = (titleLower.contains("craiova") || titleLower.contains("bamacs")) && comp.start_date.startsWith("2027")
                
                !isInvalidTitle && !isBadDate && !isWrongBamacs2027
            }
            .distinctBy { comp ->
                val titleLower = comp.title.lowercase()
                val normCity = comp.city.lowercase().replace(Regex("[^a-z0-9]"), "")
                val datePart = comp.start_date.take(10)
                val countryPart = comp.country.lowercase()

                when {
                    titleLower.contains("craiova") || normCity.contains("craiova") -> "ro_craiova_bamacs"
                    titleLower.contains("novo mesto") || normCity.contains("novomesto") || titleLower.contains("bamaci") -> "si_novomesto_bamaci"
                    titleLower.contains("zagreb") || (titleLower.contains("marathon") && countryPart == "hr") -> "hr_${datePart}_marathon"
                    normCity.contains("bernhausen") || titleLower.contains("bernhausen") -> "de_${datePart}_bernhausen"
                    titleLower.contains("emacs") -> "ema_${comp.start_date.take(4)}_emacs"
                    titleLower.contains("emaci") -> "ema_${comp.start_date.take(4)}_emaci"
                    titleLower.contains("emmtr") -> "ema_${comp.start_date.take(4)}_emmtr"
                    titleLower.contains("emamrc") -> "ema_${comp.start_date.take(4)}_emamrc"
                    titleLower.contains("emacns") -> "ema_${comp.start_date.take(4)}_emacns"
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

