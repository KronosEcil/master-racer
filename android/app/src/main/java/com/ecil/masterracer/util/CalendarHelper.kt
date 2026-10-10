package com.ecil.masterracer.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import com.ecil.masterracer.data.model.Competition
import java.text.SimpleDateFormat
import java.util.Locale

object CalendarHelper {

    /**
     * Yarışmayı kullanıcının yerel Android / Google Takvimine tek tıkla ekler.
     * ACTION_INSERT intenti sayesinde kullanıcıdan takvim izni istemeye gerek kalmaz.
     */
    fun addEventToCalendar(context: Context, competition: Competition) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startDate = sdf.parse(competition.start_date)?.time ?: System.currentTimeMillis()
            val endDate = if (competition.end_date.isNotBlank()) {
                sdf.parse(competition.end_date)?.time ?: (startDate + 24 * 60 * 60 * 1000)
            } else {
                startDate + 24 * 60 * 60 * 1000
            }

            val description = buildString {
                append("Kapsam: ${competition.getScopeTitle()}\n")
                append("Tesis: ${competition.getVenueTitle()}\n")
                append("Düzenleyen: ${competition.federation}\n")
                if (!competition.website_url.isNullOrBlank()) {
                    append("Web: ${competition.website_url}\n")
                }
                if (!competition.regulations_url.isNullOrBlank()) {
                    append("Reglaman: ${competition.regulations_url}\n")
                }
            }

            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, "🏃 ${competition.title}")
                putExtra(CalendarContract.Events.EVENT_LOCATION, "${competition.city}, ${competition.country_name}")
                putExtra(CalendarContract.Events.DESCRIPTION, description)
                putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startDate)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endDate)
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Takvim uygulaması açılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Web bağlantısı veya PDF reglamanını harici tarayıcıda açar.
     */
    fun openUrl(context: Context, url: String?) {
        if (url.isNullOrBlank()) {
            Toast.makeText(context, "Bağlantı adresi bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Tarayıcı açılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

