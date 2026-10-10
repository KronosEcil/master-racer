import logging
from typing import Optional
from firebase_admin import messaging
from .config import Competition, ScraperConfig

logger = logging.getLogger(__name__)

class NotificationManager:
    """
    Firebase Cloud Messaging (FCM) Topic-Based bildirim göndericisi.
    0 TL Maliyet: Cihaz token'ı depolama veya veritabanı sorgusu maliyeti olmadan
    toplu (broadcast) veya ülke bazlı aboneliklere bildirim iletir.
    """
    def __init__(self, is_initialized: bool):
        self.is_initialized = is_initialized

    def send_competition_notification(self, comp: Competition, change_type: str) -> bool:
        if not self.is_initialized:
            logger.info(f"[SIMULASYON] FCM Bildirimi tetiklendi -> Tür: {change_type}, Başlık: {comp.title}")
            return True

        try:
            if change_type == "NEW":
                title_text = "🏃 Yeni Master Atletizm Yarışması Eklendi!"
                body_text = f"{comp.title} ({comp.city}, {comp.country_name}) takvime eklendi. Tarih: {comp.start_date}"
            elif change_type == "DATE_CHANGED":
                title_text = "⚠️ Yarışma Tarihi Güncellendi!"
                body_text = f"{comp.title} etkinliğinin tarihi {comp.start_date} olarak revize edildi."
            else:
                title_text = "📢 Yarışma Detayları Güncellendi"
                body_text = f"{comp.title} için reglaman veya detay bilgileri güncellendi."

            # Genel Topic Mesajı
            topic_all = ScraperConfig.FCM_TOPIC_ALL

            message = messaging.Message(
                notification=messaging.Notification(
                    title=title_text,
                    body=body_text
                ),
                data={
                    "competition_id": comp.id,
                    "title": comp.title,
                    "start_date": comp.start_date,
                    "country": comp.country,
                    "venue_type": comp.venue_type,
                    "change_type": change_type,
                    "click_action": "FLUTTER_NOTIFICATION_CLICK"
                },
                topic=topic_all
            )

            response = messaging.send(message)
            logger.info(f"FCM Bildirimi başarıyla gönderildi (ID: {response}) -> Topic: {topic_all}")

            # İsteğe bağlı olarak ülke bazlı topic'e de gönderilebilir
            country_topic = f"{ScraperConfig.FCM_TOPIC_PREFIX_COUNTRY}{comp.country.lower()}"
            country_message = messaging.Message(
                notification=messaging.Notification(
                    title=title_text,
                    body=body_text
                ),
                data={"competition_id": comp.id, "change_type": change_type},
                topic=country_topic
            )
            messaging.send(country_message)

            return True
        except Exception as e:
            logger.error(f"FCM Bildirimi gönderilirken hata oluştu: {e}")
            return False
