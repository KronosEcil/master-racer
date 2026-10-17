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

    private fun normalizeText(text: String): String {
        return text.lowercase()
            .replace("ı", "i")
            .replace("i̇", "i")
            .replace("ş", "s")
            .replace("ç", "c")
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ö", "o")
            .replace(Regex("[^a-z0-9]"), "")
    }

    /**
     * Tüm veri doğrulama, tarih aralığı birleştirme, ülke/kategori tespiti ve tekilleştirme işlemleri
     * GitHub Actions üzerindeki otonom tarayıcıda (backend/scraper) merkezi olarak yapılır.
     * Uygulama katmanı yalnızca Firestore'dan gelen temiz veriyi okur ve genel biçim kontrolü uygular.
     */
    private fun cleanAndDeduplicate(list: List<Competition>): List<Competition> {
        return list
            .filter { comp ->
                comp.title.isNotBlank() && comp.start_date.length >= 8 && !comp.start_date.startsWith("2006")
            }
            .map { comp ->
                comp.copy(
                    country = comp.getDisplayCountryCode(),
                    country_name = comp.getDisplayCountryName(),
                    scope = comp.getNormalizedScope()
                )
            }
            .distinctBy { comp ->
                comp.id.ifBlank { "${comp.country}_${comp.start_date}_${normalizeText(comp.title).take(20)}" }
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

