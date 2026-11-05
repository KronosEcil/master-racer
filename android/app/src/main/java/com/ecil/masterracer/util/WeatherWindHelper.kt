package com.ecil.masterracer.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class TrackWeatherForecast(
    val city: String,
    val temperatureC: Double,
    val feelsLikeC: Double,
    val minTempC: Double? = null,
    val maxTempC: Double? = null,
    val humidityPercent: Int = 50,
    val precipProbPercent: Int = 0,
    val windSpeedMs: Double,
    val windGustsMs: Double = windSpeedMs,
    val windDirectionDeg: Int = 0,
    val weatherCode: Int = 0,
    val isRaceDayForecast: Boolean = false,
    val forecastDateLabel: String = "Canlı Pist Havası"
) {
    /**
     * World Athletics / WMA Kuralı:
     * 100m, 200m, 80m/100m/110m Engelli, Uzun Atlama ve Üçadım Atlama branşlarında
     * +2.0 m/s üzerindeki arkadan esen rüzgarlarda kırılan rekorlar resmi olarak tescil edilmez.
     */
    val isWindWithinLegalLimit: Boolean
        get() = windSpeedMs <= 2.0

    fun getWindDirectionCardinal(): String {
        val dirs = arrayOf("K (N)", "KD (NE)", "D (E)", "GD (SE)", "G (S)", "GB (SW)", "B (W)", "KB (NW)")
        val idx = (((windDirectionDeg % 360) + 360) % 360 + 22) / 45 % 8
        return dirs[idx]
    }

    fun getWeatherEmojiAndText(): Pair<String, String> {
        return when (weatherCode) {
            0 -> "☀️" to "Açık & Güneşli"
            1 -> "🌤️" to "Çoğunlukla Açık"
            2 -> "⛅" to "Parçalı Bulutlu"
            3 -> "☁️" to "Bulutlu / Kapalı"
            45, 48 -> "🌫️" to "Sisli"
            51, 53, 55, 56, 57 -> "🌦️" to "Hafif Çisenti"
            61, 63, 65, 66, 67, 80, 81, 82 -> "🌧️" to "Yağmurlu"
            71, 73, 75, 77, 85, 86 -> "❄️" to "Karlı"
            95, 96, 99 -> "⛈️" to "Gök Gürültülü Fırtına"
            else -> "🌤️" to "Açık Hava"
        }
    }

    fun getWarmupRecommendation(): String {
        return when {
            temperatureC < 14.0 ->
                "🧥 Serin Pist (<14°C): Kas ısısını korumak için eşofmanla ısınmayı +8 dk uzatın, Çağrı Odası'ndan çıkana kadar üstünüzü çıkarmayın."
            temperatureC > 27.0 ->
                "💧 Sıcak Pist (>27°C): Jok süresini kısa tutup dinamik mobiliteye odaklanın; Çağrı Odası öncesi su/elektrolit alımını artırın."
            else ->
                "🏃 İdeal Pist Sıcaklığı (${String.format(Locale.US, "%.0f", temperatureC)}°C): Standart ısınma, dril ve artırma protokolü için optimum stadyum koşulları."
        }
    }
}

object WeatherWindHelper {

    private val forecastCache = ConcurrentHashMap<String, TrackWeatherForecast>()

    // Sık kullanılan Türkiye ve uluslararası Master Atletizm şehir koordinatları (Anında çözümleme için)
    private val KNOWN_CITY_COORDS: Map<String, Pair<Double, Double>> = mapOf(
        "izmir" to (38.4237 to 27.1428),
        "i̇zmir" to (38.4237 to 27.1428),
        "istanbul" to (40.9903 to 28.8505),
        "i̇stanbul" to (40.9903 to 28.8505),
        "ankara" to (39.9334 to 32.8597),
        "bursa" to (40.1885 to 29.0610),
        "antalya" to (36.8969 to 30.7133),
        "mersin" to (36.8121 to 34.6415),
        "adana" to (37.0000 to 35.3213),
        "konya" to (37.8746 to 32.4932),
        "eskişehir" to (39.7767 to 30.5206),
        "eskisehir" to (39.7767 to 30.5206),
        "samsun" to (41.2867 to 36.3300),
        "trabzon" to (41.0027 to 39.7168),
        "denizli" to (37.7765 to 29.0864),
        "aydın" to (37.8560 to 27.8416),
        "aydin" to (37.8560 to 27.8416),
        "kayseri" to (38.7312 to 35.4787),
        "gaziantep" to (37.0662 to 37.3833),
        "diyarbakır" to (37.9144 to 40.2306),
        "diyarbakir" to (37.9144 to 40.2306),
        "edirne" to (41.6771 to 26.5557),
        "kocaeli" to (40.7654 to 29.9408),
        "sakarya" to (40.7569 to 30.3783),
        "balıkesir" to (39.6484 to 27.8826),
        "muğla" to (37.2153 to 28.3636),
        "athens" to (37.9838 to 23.7275),
        "atina" to (37.9838 to 23.7275),
        "thessaloniki" to (40.6401 to 22.9444),
        "selanik" to (40.6401 to 22.9444),
        "sofia" to (42.6977 to 23.3219),
        "sofya" to (42.6977 to 23.3219),
        "stara zagora" to (42.4258 to 25.6345),
        "bucharest" to (44.4268 to 26.1025),
        "bükreş" to (44.4268 to 26.1025),
        "belgrade" to (44.7866 to 20.4489),
        "belgrad" to (44.7866 to 20.4489),
        "zagreb" to (45.8150 to 15.9819),
        "ljubljana" to (46.0569 to 14.5058),
        "bar" to (42.0931 to 19.1003),
        "korce" to (40.6186 to 20.7808),
        "korçë" to (40.6186 to 20.7808),
        "gothenburg" to (57.7089 to 11.9746),
        "göteborg" to (57.7089 to 11.9746),
        "torun" to (53.0138 to 18.5984),
        "toruń" to (53.0138 to 18.5984),
        "braga" to (41.5454 to -8.4265),
        "madeira" to (32.6669 to -16.9241),
        "funchal" to (32.6669 to -16.9241),
        "pescara" to (42.4618 to 14.2161),
        "catania" to (37.5079 to 15.0830),
        "tampere" to (61.4978 to 23.7610),
        "daegu" to (35.8714 to 128.6014),
        "gainesville" to (29.6516 to -82.3248),
        "valencia" to (39.4699 to -0.3763),
        "madrid" to (40.4168 to -3.7038),
        "berlin" to (52.5200 to 13.4050),
        "paris" to (48.8566 to 2.3522),
        "london" to (51.5072 to -0.1276),
        "londra" to (51.5072 to -0.1276),
        "roma" to (41.9028 to 12.4964),
        "rome" to (41.9028 to 12.4964)
    )

    private var cachedCurrentLocationWeather: TrackWeatherForecast? = null

    /**
     * Kullanıcının o an bulunduğu konumu (IP Geolocation üzerinden sıfır izinle) tespit eder
     * ve o koordinatların anlık hava durumu + rüzgar (m/s) verisini döndürür.
     */
    suspend fun fetchCurrentLocationWeather(): TrackWeatherForecast? = withContext(Dispatchers.IO) {
        cachedCurrentLocationWeather?.let { return@withContext it }

        try {
            val locInfo = resolveCurrentDeviceLocation()
            val lat = locInfo?.first ?: 41.0082
            val lon = locInfo?.second ?: 28.9784
            val detectedCity = locInfo?.third.orEmpty()

            val apiUrl = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,wind_direction_10m,wind_gusts_10m" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,wind_speed_10m_max" +
                "&wind_speed_unit=ms&timezone=auto&forecast_days=1"

            val jsonStr = httpGet(apiUrl) ?: return@withContext null
            val root = JSONObject(jsonStr)
            val currentObj = root.optJSONObject("current")
            val curTemp = currentObj?.optDouble("temperature_2m", 20.0) ?: 20.0
            val curFeels = currentObj?.optDouble("apparent_temperature", curTemp) ?: curTemp
            val curHumidity = currentObj?.optInt("relative_humidity_2m", 50) ?: 50
            val curCode = currentObj?.optInt("weather_code", 0) ?: 0
            val curWindMs = currentObj?.optDouble("wind_speed_10m", 1.2) ?: 1.2
            val curGustMs = currentObj?.optDouble("wind_gusts_10m", curWindMs) ?: curWindMs
            val curDir = currentObj?.optInt("wind_direction_10m", 0) ?: 0

            val dailyObj = root.optJSONObject("daily")
            val todayMax = dailyObj?.optJSONArray("temperature_2m_max")?.optDouble(0, curTemp)
            val todayMin = dailyObj?.optJSONArray("temperature_2m_min")?.optDouble(0, curTemp)

            val result = TrackWeatherForecast(
                city = detectedCity,
                temperatureC = curTemp,
                feelsLikeC = curFeels,
                minTempC = todayMin,
                maxTempC = todayMax,
                humidityPercent = curHumidity,
                precipProbPercent = 0,
                windSpeedMs = curWindMs,
                windGustsMs = curGustMs,
                windDirectionDeg = curDir,
                weatherCode = curCode,
                isRaceDayForecast = false,
                forecastDateLabel = "Konumum"
            )
            cachedCurrentLocationWeather = result
            result
        } catch (e: Exception) {
            null
        }
    }

    private fun resolveCurrentDeviceLocation(): Triple<Double, Double, String>? {
        // 1. Servis: ipwho.is
        try {
            val resp = httpGet("https://ipwho.is/?lang=tr")
            if (resp != null) {
                val obj = JSONObject(resp)
                if (obj.optBoolean("success", false)) {
                    val lat = obj.optDouble("latitude", Double.NaN)
                    val lon = obj.optDouble("longitude", Double.NaN)
                    val city = obj.optString("city", "")
                    if (!lat.isNaN() && !lon.isNaN()) {
                        return Triple(lat, lon, city)
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Yedek Servis: geojs.io
        try {
            val resp2 = httpGet("https://get.geojs.io/v1/ip/geo.json")
            if (resp2 != null) {
                val obj = JSONObject(resp2)
                val lat = obj.optString("latitude", "").toDoubleOrNull()
                val lon = obj.optString("longitude", "").toDoubleOrNull()
                val city = obj.optString("city", "")
                if (lat != null && lon != null) {
                    return Triple(lat, lon, city)
                }
            }
        } catch (_: Exception) {}

        return null
    }

    suspend fun fetchTrackWeather(
        rawCity: String,
        countryName: String = "",
        targetStartDate: String? = null,
        forceRefresh: Boolean = false
    ): TrackWeatherForecast? = withContext(Dispatchers.IO) {
        val cleanCity = rawCity
            .substringBefore(",")
            .substringBefore("/")
            .substringBefore("(")
            .trim()
            .ifBlank { countryName.ifBlank { "İzmir" } }

        val cacheKey = "${cleanCity.lowercase(Locale.ROOT)}_${targetStartDate.orEmpty()}"
        if (!forceRefresh) {
            forecastCache[cacheKey]?.let { return@withContext it }
        }

        try {
            val coords = resolveCoordinates(cleanCity, countryName) ?: return@withContext null
            val (lat, lon) = coords

            val apiUrl = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,wind_direction_10m,wind_gusts_10m" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,wind_speed_10m_max" +
                "&wind_speed_unit=ms&timezone=auto&forecast_days=16"

            val jsonStr = httpGet(apiUrl) ?: return@withContext null
            val root = JSONObject(jsonStr)

            val currentObj = root.optJSONObject("current")
            val curTemp = currentObj?.optDouble("temperature_2m", 20.0) ?: 20.0
            val curFeels = currentObj?.optDouble("apparent_temperature", curTemp) ?: curTemp
            val curHumidity = currentObj?.optInt("relative_humidity_2m", 50) ?: 50
            val curCode = currentObj?.optInt("weather_code", 0) ?: 0
            val curWindMs = currentObj?.optDouble("wind_speed_10m", 1.2) ?: 1.2
            val curGustMs = currentObj?.optDouble("wind_gusts_10m", curWindMs) ?: curWindMs
            val curDir = currentObj?.optInt("wind_direction_10m", 0) ?: 0

            val dailyObj = root.optJSONObject("daily")
            val timesArr = dailyObj?.optJSONArray("time")

            var matchedIndex = -1
            if (!targetStartDate.isNullOrBlank() && timesArr != null) {
                for (i in 0 until timesArr.length()) {
                    if (timesArr.optString(i) == targetStartDate) {
                        matchedIndex = i
                        break
                    }
                }
            }

            val result = if (matchedIndex >= 0 && dailyObj != null) {
                val maxT = dailyObj.optJSONArray("temperature_2m_max")?.optDouble(matchedIndex, curTemp) ?: curTemp
                val minT = dailyObj.optJSONArray("temperature_2m_min")?.optDouble(matchedIndex, curTemp) ?: curTemp
                val dCode = dailyObj.optJSONArray("weather_code")?.optInt(matchedIndex, curCode) ?: curCode
                val dPrecip = dailyObj.optJSONArray("precipitation_probability_max")?.optInt(matchedIndex, 0) ?: 0
                val dWindMax = dailyObj.optJSONArray("wind_speed_10m_max")?.optDouble(matchedIndex, curWindMs) ?: curWindMs
                val typicalTrackWind = if (matchedIndex == 0) curWindMs else (dWindMax * 0.72)

                TrackWeatherForecast(
                    city = cleanCity,
                    temperatureC = maxT,
                    feelsLikeC = maxT,
                    minTempC = minT,
                    maxTempC = maxT,
                    humidityPercent = curHumidity,
                    precipProbPercent = dPrecip,
                    windSpeedMs = typicalTrackWind,
                    windGustsMs = dWindMax,
                    windDirectionDeg = curDir,
                    weatherCode = dCode,
                    isRaceDayForecast = true,
                    forecastDateLabel = "Yarışma Günü Tahmini"
                )
            } else {
                // Yarışmaya 16 günden fazla varsa: Open-Meteo Arşiv servisinden tam o yarış gününün (Ay-Gün)
                // mevsimsel gündüz sıcaklığı ve rüzgar verisini çek
                val seasonalForecast = if (!targetStartDate.isNullOrBlank()) {
                    fetchSeasonalArchiveWeather(lat, lon, cleanCity, targetStartDate, curHumidity, curDir)
                } else null

                if (seasonalForecast != null) {
                    seasonalForecast
                } else {
                    val todayMax = dailyObj?.optJSONArray("temperature_2m_max")?.optDouble(0, curTemp) ?: curTemp
                    val todayMin = dailyObj?.optJSONArray("temperature_2m_min")?.optDouble(0, curTemp)
                    val todayPrecip = dailyObj?.optJSONArray("precipitation_probability_max")?.optInt(0, 0) ?: 0

                    TrackWeatherForecast(
                        city = cleanCity,
                        temperatureC = todayMax,
                        feelsLikeC = curFeels,
                        minTempC = todayMin,
                        maxTempC = todayMax,
                        humidityPercent = curHumidity,
                        precipProbPercent = todayPrecip,
                        windSpeedMs = curWindMs,
                        windGustsMs = curGustMs,
                        windDirectionDeg = curDir,
                        weatherCode = curCode,
                        isRaceDayForecast = false,
                        forecastDateLabel = "Yarışma Zamanı Tahmini"
                    )
                }
            }

            forecastCache[cacheKey] = result
            result
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 16 günden ileri tarihli yarışmalar için yarışmanın yapılacağı Ay-Gün (MM-DD) tarihinde
     * bir önceki yıl gerçekleşen gündüz en yüksek sıcaklığı, hava durumu kodunu ve rüzgar hızını (m/s) getirir.
     */
    private fun fetchSeasonalArchiveWeather(
        lat: Double,
        lon: Double,
        city: String,
        targetStartDate: String,
        defaultHumidity: Int,
        defaultDir: Int
    ): TrackWeatherForecast? {
        return try {
            val parts = targetStartDate.split("-")
            if (parts.size < 3) return null
            val month = parts[1].toIntOrNull() ?: return null
            val day = parts[2].toIntOrNull() ?: return null

            val safeDay = if (month == 2 && day == 29) 28 else day
            // Open-Meteo Archive 2024/2025 tam yıllarını eksiksiz barındırır
            val refYear = 2024
            val archiveDate = String.format(Locale.US, "%04d-%02d-%02d", refYear, month, safeDay)

            val archiveUrl = "https://archive-api.open-meteo.com/v1/archive" +
                "?latitude=$lat&longitude=$lon" +
                "&start_date=$archiveDate&end_date=$archiveDate" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,wind_speed_10m_max" +
                "&wind_speed_unit=ms&timezone=auto"

            val resp = httpGet(archiveUrl) ?: return null
            val daily = JSONObject(resp).optJSONObject("daily") ?: return null
            val maxT = daily.optJSONArray("temperature_2m_max")?.optDouble(0, Double.NaN) ?: Double.NaN
            if (maxT.isNaN()) return null
            val minT = daily.optJSONArray("temperature_2m_min")?.optDouble(0, maxT - 6.0) ?: (maxT - 6.0)
            val wCode = daily.optJSONArray("weather_code")?.optInt(0, 0) ?: 0
            val maxWind = daily.optJSONArray("wind_speed_10m_max")?.optDouble(0, 1.8) ?: 1.8

            TrackWeatherForecast(
                city = city,
                temperatureC = maxT,
                feelsLikeC = maxT,
                minTempC = minT,
                maxTempC = maxT,
                humidityPercent = defaultHumidity,
                precipProbPercent = 0,
                windSpeedMs = maxWind * 0.75,
                windGustsMs = maxWind,
                windDirectionDeg = defaultDir,
                weatherCode = wCode,
                isRaceDayForecast = true,
                forecastDateLabel = "Yarışma Zamanı Tahmini"
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveCoordinates(city: String, countryName: String): Pair<Double, Double>? {
        val lower = city.lowercase(Locale.forLanguageTag("tr-TR")).trim()
        KNOWN_CITY_COORDS[lower]?.let { return it }
        KNOWN_CITY_COORDS.entries.firstOrNull { lower.contains(it.key) || it.key.contains(lower) }?.let {
            return it.value
        }

        return try {
            val encoded = URLEncoder.encode(city, "UTF-8")
            val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=tr&format=json"
            val resp = httpGet(geoUrl) ?: return null
            val results = JSONObject(resp).optJSONArray("results")
            if (results != null && results.length() > 0) {
                val first = results.getJSONObject(0)
                first.getDouble("latitude") to first.getDouble("longitude")
            } else if (countryName.isNotBlank()) {
                val encodedCountry = URLEncoder.encode(countryName, "UTF-8")
                val fallbackResp = httpGet("https://geocoding-api.open-meteo.com/v1/search?name=$encodedCountry&count=1&language=tr&format=json") ?: return null
                val fbResults = JSONObject(fallbackResp).optJSONArray("results")
                if (fbResults != null && fbResults.length() > 0) {
                    val f = fbResults.getJSONObject(0)
                    f.getDouble("latitude") to f.getDouble("longitude")
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun httpGet(urlStr: String): String? {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6000
            readTimeout = 6000
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        } finally {
            conn.disconnect()
        }
    }
}
