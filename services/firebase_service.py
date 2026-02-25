"""
FirebaseService — updated to match your actual Firestore structure

Collection: 'lostItems'  (not 'items')
Status:     "LOST" / "FOUND"  (uppercase)
Category:   "KEYS", "PHONE" … (uppercase)
Timestamp:  number (milliseconds since epoch)
Fields:     userId, userName, userEmail, imageURLs
"""

import logging
from datetime import datetime, timezone

import firebase_admin
from firebase_admin import credentials, firestore, messaging
from google.cloud.firestore_v1 import AsyncClient
from google.oauth2 import service_account

from config import settings
from models.item import FoundItem, MatchResult, ItemCategory, LostItemRequest

logger = logging.getLogger(__name__)

COLLECTION = "lostItems"   # ← your actual collection name


def _to_category(value: str) -> ItemCategory:
    """Convert any casing to a valid ItemCategory, fallback to OTHER."""
    try:
        return ItemCategory(value.upper())
    except ValueError:
        return ItemCategory.OTHER


def _to_ms_timestamp(value) -> int:
    """Normalise whatever Firestore gives us to milliseconds int."""
    if value is None:
        return int(datetime.now(tz=timezone.utc).timestamp() * 1000)
    if isinstance(value, (int, float)):
        # Already a number — could be seconds or ms
        # If it looks like seconds (< year 3000 in ms), convert
        if value < 9_999_999_999:
            return int(value * 1000)
        return int(value)
    if hasattr(value, "timestamp"):   # Firestore Timestamp object
        return int(value.timestamp() * 1000)
    return int(datetime.now(tz=timezone.utc).timestamp() * 1000)


class FirebaseService:

    def __init__(self):
        if not firebase_admin._apps:
            cred = credentials.Certificate(settings.FIREBASE_SERVICE_ACCOUNT_PATH)
            firebase_admin.initialize_app(
                cred, {"projectId": settings.FIREBASE_PROJECT_ID}
            )

        # Pass credentials explicitly — avoids DefaultCredentialsError on Windows
        google_creds = service_account.Credentials.from_service_account_file(
            settings.FIREBASE_SERVICE_ACCOUNT_PATH,
            scopes=["https://www.googleapis.com/auth/cloud-platform"],
        )
        self.db: AsyncClient = firestore.AsyncClient(
            project=settings.FIREBASE_PROJECT_ID,
            credentials=google_creds,
        )
        logger.info("✅ Firebase / Firestore ready")

    # ─────────────────────────────────────────────────────────────────
    # READ
    # ─────────────────────────────────────────────────────────────────

    async def get_active_found_items(
        self, exclude_user_id: str | None = None
    ) -> list[FoundItem]:
        """
        Returns all unresolved FOUND posts from 'lostItems', newest first.
        Skips posts made by the same user who posted the lost item.
        """
        # Two .where() + .order_by() requires a composite Firestore index.
        # We sort in Python instead to work immediately without any index setup.
        from google.cloud.firestore_v1.base_query import FieldFilter
        query = (
            self.db.collection(COLLECTION)
            .where(filter=FieldFilter("status",   "==", "FOUND"))
            .where(filter=FieldFilter("resolved", "==", False))
        )

        snapshot = await query.get()
        items: list[FoundItem] = []

        for doc in snapshot:
            data = doc.to_dict()

            # Skip posts by the same user
            if exclude_user_id and data.get("userId") == exclude_user_id:
                continue

            try:
                items.append(
                    FoundItem(
                        id          = data.get("id", doc.id),
                        userId      = data.get("userId",    ""),
                        userName    = data.get("userName",  "Unknown"),
                        userEmail   = data.get("userEmail", ""),
                        title       = data.get("title",       ""),
                        description = data.get("description", ""),
                        category    = _to_category(data.get("category", "OTHER")),
                        location    = data.get("location"),
                        timestamp   = _to_ms_timestamp(data.get("timestamp")),
                        imageURLs   = data.get("imageURLs", []),
                    )
                )
            except Exception as e:
                logger.warning(f"Skipping malformed found item {doc.id}: {e}")

        # Sort newest first in Python (avoids composite index on Firestore)
        items.sort(key=lambda x: x.timestamp, reverse=True)
        items = items[:settings.MAX_FOUND_ITEMS_PER_MATCH]

        logger.info(f"📦 Loaded {len(items)} active FOUND items from Firestore")
        return items

    async def get_all_active_lost_items(self) -> list[LostItemRequest]:
        """Returns all unresolved LOST posts. Used for batch re-matching."""
        from google.cloud.firestore_v1.base_query import FieldFilter
        query = (
            self.db.collection(COLLECTION)
            .where(filter=FieldFilter("status",   "==", "LOST"))
            .where(filter=FieldFilter("resolved", "==", False))
            # No .order_by() — sort in Python to avoid composite index
        )

        snapshot = await query.get()
        items: list[LostItemRequest] = []

        for doc in snapshot:
            data = doc.to_dict()
            try:
                items.append(
                    LostItemRequest(
                        id          = data.get("id", doc.id),
                        userId      = data.get("userId",    ""),
                        userName    = data.get("userName",  "Unknown"),
                        userEmail   = data.get("userEmail", ""),
                        title       = data.get("title",       ""),
                        description = data.get("description", ""),
                        category    = _to_category(data.get("category", "OTHER")),
                        location    = data.get("location"),
                        timestamp   = _to_ms_timestamp(data.get("timestamp")),
                        imageURLs   = data.get("imageURLs", []),
                    )
                )
            except Exception as e:
                logger.warning(f"Skipping malformed lost item {doc.id}: {e}")

        return items

    # ─────────────────────────────────────────────────────────────────
    # WRITE
    # ─────────────────────────────────────────────────────────────────

    async def save_match_results(
        self, lost_item_id: str, matches: list[MatchResult]
    ) -> None:
        """
        Writes match results to /matches/{lost_item_id}.
        Your Kotlin app listens to this document in real-time to update the UI.
        """
        doc_ref = self.db.collection("matches").document(lost_item_id)

        payload = {
            "lostItemId": lost_item_id,
            "matchedAt":  firestore.SERVER_TIMESTAMP,
            "results": [
                {
                    "id":              m.id,
                    "userId":          m.userId,
                    "userName":        m.userName,
                    "userEmail":       m.userEmail,
                    "title":           m.title,
                    "similarityScore": m.similarity_score,
                    "aiExplanation":   m.ai_explanation,
                    "scoreBreakdown": {
                        "text":     m.score_breakdown.text_score,
                        "location": m.score_breakdown.location_score,
                        "time":     m.score_breakdown.time_score,
                        "image":    m.score_breakdown.image_score,
                    },
                }
                for m in matches
            ],
        }

        await doc_ref.set(payload)
        logger.info(f"💾 Saved {len(matches)} match(es) → /matches/{lost_item_id}")

    # ─────────────────────────────────────────────────────────────────
    # FCM NOTIFICATIONS
    # ─────────────────────────────────────────────────────────────────

    async def send_match_notification(
        self,
        user_uid:        str,
        lost_item_title: str,
        match_count:     int,
        top_match_id:    str,
    ) -> None:
        """
        Sends an FCM push notification to the user who posted the lost item.
        Only fires when top match score >= NOTIFY_THRESHOLD (default 70%).
        Reads the FCM token from /users/{uid}.fcm_token in Firestore.
        """
        try:
            user_doc = await self.db.collection("users").document(user_uid).get()
            if not user_doc.exists:
                logger.info(f"No user doc for uid {user_uid}")
                return

            fcm_token = user_doc.to_dict().get("fcm_token")
            if not fcm_token:
                logger.info(f"No FCM token for user {user_uid}")
                return

            body = (
                f"{match_count} potential matches found for your lost item!"
                if match_count > 1 else
                "A potential match was found for your lost item!"
            )

            message = messaging.Message(
                notification = messaging.Notification(
                    title = f"🔍 Match Found: {lost_item_title}",
                    body  = body,
                ),
                data = {
                    "type":            "AI_MATCH",
                    "lostItemTitle":   lost_item_title,
                    "matchCount":      str(match_count),
                    "topMatchId":      top_match_id,
                    "screen":          "matches",  # deep-link hint
                },
                token   = fcm_token,
                android = messaging.AndroidConfig(
                    priority     = "high",
                    notification = messaging.AndroidNotification(
                        channel_id = "matches_channel",
                        icon       = "ic_notification",
                        color      = "#4CAF50",
                    ),
                ),
            )

            messaging.send(message)
            logger.info(f"📲 FCM notification sent to {user_uid}")

        except Exception as e:
            logger.warning(f"FCM failed for {user_uid}: {e}")