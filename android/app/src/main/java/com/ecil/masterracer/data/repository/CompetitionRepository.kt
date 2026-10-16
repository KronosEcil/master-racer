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
            .map { comp ->
                val titleLower = comp.title.lowercase().trim()
                val cityLower = comp.city.lowercase()
                val cNameLower = comp.country_name.lowercase()
                val cCountryUpper = comp.country.uppercase()

                val isKansai = titleLower.contains("kansai") || 
                               titleLower.contains("world masters games") || 
                               titleLower.contains("wmg") || 
                               cityLower.contains("kansai") || 
                               cNameLower.contains("kansai")

                val isAlbania = cCountryUpper == "AL" || cCountryUpper == "ALB" ||
                                cNameLower.contains("albania") || cNameLower.contains("arnavutluk") ||
                                titleLower.contains("albania") || titleLower.contains("arnavutluk") ||
                                cityLower.contains("shkod") || cityLower.contains("tiran")

                // Türkiye Masterlar Açık Saha Atletizm Şampiyonası (TAF ve EMA Uluslararası Eşleşmesi)
                val isTrOutdoor = (
                    (titleLower.contains("türkiye") || titleLower.contains("turkiye") || titleLower.contains("turkish") || titleLower.contains("turk")) &&
                    (titleLower.contains("açık") || titleLower.contains("acik") || titleLower.contains("outdoor")) &&
                    (titleLower.contains("master") || titleLower.contains("champs") || titleLower.contains("şampiyona") || titleLower.contains("sampiyona"))
                ) || (cityLower.contains("izmir") && (titleLower.contains("outdoor") || titleLower.contains("açık") || titleLower.contains("master") || titleLower.contains("turkish")))

                // Türkiye Masterlar Salon Atletizm Şampiyonası (TAF ve EMA Uluslararası Eşleşmesi)
                val isTrIndoor = (
                    (titleLower.contains("türkiye") || titleLower.contains("turkiye") || titleLower.contains("turkish") || titleLower.contains("turk")) &&
                    (titleLower.contains("salon") || titleLower.contains("indoor")) &&
                    (titleLower.contains("master") || titleLower.contains("champs") || titleLower.contains("şampiyona") || titleLower.contains("sampiyona"))
                ) || (cityLower.contains("istanbul") && (titleLower.contains("salon") || titleLower.contains("indoor")) && titleLower.contains("master"))

                when {
                    isTrOutdoor -> comp.copy(
                        title = "Türkiye Masterlar Açık Saha Atletizm Şampiyonası",
                        country = "TR",
                        country_name = "Türkiye",
                        city = "İzmir",
                        start_date = "2026-07-11",
                        end_date = "2026-07-12",
                        federation = "TAF",
                        venue_type = "outdoor",
                        scope = "turkey",
                        website_url = "https://taf.org.tr"
                    )
                    isTrIndoor -> comp.copy(
                        title = "Türkiye Masterlar Salon Atletizm Şampiyonası",
                        country = "TR",
                        country_name = "Türkiye",
                        city = "İstanbul",
                        start_date = "2026-02-21",
                        end_date = "2026-02-22",
                        federation = "TAF",
                        venue_type = "indoor",
                        scope = "turkey",
                        website_url = "https://taf.org.tr"
                    )
                    isKansai -> comp.copy(
                        country = "JP",
                        country_name = "Japonya",
                        city = "Kansai",
                        start_date = if (comp.start_date.isBlank() || comp.start_date.startsWith("2026")) "2027-05-14" else comp.start_date,
                        end_date = if (comp.end_date.isBlank() || comp.end_date.startsWith("2026")) "2027-05-23" else comp.end_date,
                        federation = if (comp.federation.isBlank()) "IMGA" else comp.federation,
                        website_url = "https://wmg2027.jp"
                    )
                    isAlbania -> comp.copy(
                        country = "AL",
                        country_name = "Arnavutluk",
                        city = if (comp.city.equals("Albania", ignoreCase = true) || comp.city.equals("Arnavutluk", ignoreCase = true)) "" else comp.city
                    )
                    else -> comp.copy(
                        country = comp.getDisplayCountryCode(),
                        country_name = comp.getDisplayCountryName()
                    )
                }
            }
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
                    // Kansai / World Masters Games
                    titleLower.contains("kansai") || normCity.contains("kansai") || titleLower.contains("world masters games") -> "jp_2027_wmg_kansai"

                    // Arnavutluk yarışmaları
                    comp.country == "AL" || titleLower.contains("arnavutluk") || titleLower.contains("albania") || normCity.contains("shkod") || normCity.contains("tiran") -> "al_${datePart}_${if (normCity.isNotEmpty()) normCity else "arnavutluk"}"

                    // Türkiye Masterlar Şampiyonaları tekilleştirme (TAF ve EMA)
                    titleLower.contains("masterlar açık") || titleLower.contains("masterlar acik") || titleLower.contains("turkish outdoor") -> "tr_${datePart.take(4)}_masterlar_acik_saha"
                    titleLower.contains("masterlar salon") || titleLower.contains("turkish indoor") -> "tr_${datePart.take(4)}_masterlar_salon"

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

