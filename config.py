"""
Configuration — loads from environment variables / .env file
"""

from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    # ── Groq (FREE — no regional restrictions) ────────────
    GROQ_API_KEY: str                            # From console.groq.com (FREE)
    GROQ_MODEL: str = "llama-3.1-8b-instant"    # Fast, free, great for scoring

    # ── Firebase ───────────────────────────────────────────
    FIREBASE_PROJECT_ID: str
    FIREBASE_SERVICE_ACCOUNT_PATH: str = "firebase-service-account.json"

    # ── API Security ───────────────────────────────────────
    API_KEY: str = "change-me-in-production"

    # ── Matching Tuning ────────────────────────────────────
    MAX_FOUND_ITEMS_PER_MATCH: int = 50   # Cap to avoid rate limits
    MIN_SCORE_THRESHOLD: int = 40          # Discard matches below this %
    NOTIFY_THRESHOLD: int = 70             # Send FCM push above this %
    MAX_MATCHES_RETURNED: int = 5          # Top N results to return

    # ── Score Weights (must sum to 100) ────────────────────
    WEIGHT_TEXT: int = 50      # Title + description + category (Gemini AI)
    WEIGHT_LOCATION: int = 20  # Location string overlap
    WEIGHT_TIME: int = 10      # Time proximity (exponential decay)
    WEIGHT_IMAGE: int = 20     # Image visual similarity (Gemini Vision)

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()