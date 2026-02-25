"""
Pydantic models — matching your actual Firestore 'lostItems' collection structure
"""

from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime
from enum import Enum


class ItemCategory(str, Enum):
    """Uppercase to match your Firestore values exactly"""
    KEYS         = "KEYS"
    STUDENT_CARD = "STUDENT_CARD"
    PHONE        = "PHONE"
    BAG          = "BAG"
    DOCUMENTS    = "DOCUMENTS"
    ELECTRONICS  = "ELECTRONICS"
    CLOTHING     = "CLOTHING"
    OTHER        = "OTHER"


class ItemStatus(str, Enum):
    """Uppercase to match your Firestore values exactly"""
    LOST  = "LOST"
    FOUND = "FOUND"


# ─────────────────────────────────────────
# REQUEST  (sent to trigger matching)
# ─────────────────────────────────────────

class LostItemRequest(BaseModel):
    """
    POST body for /api/v1/match.
    Field names match your Firestore document fields exactly.
    """
    id:          str           = Field(..., description="Firestore document ID")
    userId:      str           = Field(..., description="Firebase UID of the poster")
    userName:    str           = Field(..., description="Display name of the poster")
    userEmail:   str           = Field(..., description="ESTIN email")
    title:       str           = Field(..., min_length=1, max_length=200)
    description: str           = Field(..., max_length=2000)
    category:    ItemCategory
    location:    Optional[str] = None
    timestamp:   int           = Field(..., description="Unix ms timestamp e.g. 1764977468368")
    imageURLs:   list[str]     = Field(default_factory=list)
    status:      ItemStatus    = ItemStatus.LOST
    resolved:    bool          = False

    @property
    def timestamp_dt(self) -> datetime:
        """Convert ms timestamp → datetime for time scoring."""
        from datetime import timezone
        return datetime.fromtimestamp(self.timestamp / 1000, tz=timezone.utc)

    class Config:
        json_schema_extra = {
            "example": {
                "id":          "nEVRyAkAeklCeh03yY4z",
                "userId":      "zcjzK0NrT7bGY84RzIil03d9YUB2",
                "userName":    "lina LALEM",
                "userEmail":   "l_lalem@estin.dz",
                "title":       "keys",
                "description": "with a key holder",
                "category":    "KEYS",
                "location":    "residence",
                "timestamp":   1764977468368,
                "imageURLs":   [],
                "status":      "LOST",
                "resolved":    False,
            }
        }


# ─────────────────────────────────────────
# INTERNAL — a found item fetched from Firestore
# ─────────────────────────────────────────

class FoundItem(BaseModel):
    """Mirrors your Firestore document structure exactly."""
    id:          str
    userId:      str
    userName:    str
    userEmail:   str
    title:       str
    description: str
    category:    ItemCategory
    location:    Optional[str] = None
    timestamp:   int
    imageURLs:   list[str]     = []

    @property
    def timestamp_dt(self) -> datetime:
        from datetime import timezone
        return datetime.fromtimestamp(self.timestamp / 1000, tz=timezone.utc)


# ─────────────────────────────────────────
# RESPONSE
# ─────────────────────────────────────────

class ScoreBreakdown(BaseModel):
    text_score:     int = Field(..., ge=0, le=100)
    location_score: int = Field(..., ge=0, le=100)
    time_score:     int = Field(..., ge=0, le=100)
    image_score:    int = Field(..., ge=0, le=100)


class MatchResult(BaseModel):
    id:               str
    userId:           str
    userName:         str
    userEmail:        str
    title:            str
    description:      str
    category:         ItemCategory
    location:         Optional[str]
    timestamp:        int
    imageURLs:        list[str]
    similarity_score: int            = Field(..., ge=0, le=100)
    score_breakdown:  ScoreBreakdown
    ai_explanation:   str


class MatchResponse(BaseModel):
    lost_item_id: str
    matches:      list[MatchResult]
    message:      str