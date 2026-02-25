"""
LGUINAH — AI Matching System API
Powered by Google Gemini (FREE tier) + Firebase Firestore
"""

import logging
from contextlib import asynccontextmanager

import uvicorn
from fastapi import BackgroundTasks, FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from config import settings
from models.item import LostItemRequest, MatchResponse
from services.firebase_service import FirebaseService
from services.matching_service import MatchingService

logging.basicConfig(
    level  = logging.INFO,
    format = "%(asctime)s  %(levelname)-8s  %(name)s — %(message)s",
)
logger = logging.getLogger(__name__)


# ── App lifespan: init services once at startup ──────────────────────

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("🚀 Starting LGUINAH AI Matching API (Gemini)...")
    app.state.firebase = FirebaseService()
    app.state.matcher  = MatchingService()
    logger.info("✅ All services ready")
    yield
    logger.info("👋 Shutting down")


app = FastAPI(
    title       = "LGUINAH AI Matching API",
    description = "Auto-matches lost & found items using Google Gemini + Firebase",
    version     = "2.0.0",
    lifespan    = lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins = ["*"],   # Restrict to your domain in production
    allow_methods = ["GET", "POST"],
    allow_headers = ["*"],
)


# ── Auth helper ───────────────────────────────────────────────────────

def check_api_key(x_api_key: str = Header(...)):
    if x_api_key != settings.API_KEY:
        raise HTTPException(status_code=401, detail="Invalid API key")


# ── Endpoints ─────────────────────────────────────────────────────────

@app.get("/health", tags=["System"])
async def health():
    """Quick liveness check."""
    return {"status": "ok", "service": "LGUINAH Matching API", "model": settings.GEMINI_MODEL}


@app.post(
    "/api/v1/match",
    response_model = MatchResponse,
    tags           = ["Matching"],
    summary        = "Match a lost item against all active found posts",
)
async def match_lost_item(
    request:          LostItemRequest,
    background_tasks: BackgroundTasks,
    x_api_key:        str = Header(...),
):
    """
    **Main endpoint** — called automatically after a user posts a lost item.

    Steps:
    1. Fetch all active "found" items from Firestore
    2. Run Gemini AI comparison for each (text + image, in parallel)
    3. Return top matches ranked by similarity score (0–100%)
    4. Save results to Firestore `/matches/{item_id}` in the background
    5. Send FCM push notification if top match ≥ 70% confidence
    """
    check_api_key(x_api_key)

    firebase: FirebaseService = app.state.firebase
    matcher:  MatchingService = app.state.matcher

    logger.info(f"🔍 Matching lost item '{request.title}' [category: {request.category.value}]")

    # 1. Fetch found items
    found_items = await firebase.get_active_found_items(
        exclude_user_id=request.userId
    )

    if not found_items:
        return MatchResponse(
            lost_item_id = request.id,
            matches      = [],
            message      = "No active found posts to compare against.",
        )

    # 2. AI Matching
    matches = await matcher.find_matches(
        lost_item   = request,
        found_items = found_items,
    )

    # 3. Save + notify (background — does not delay the response)
    if matches:
        background_tasks.add_task(
            firebase.save_match_results,
            lost_item_id = request.id,
            matches      = matches,
        )

        top = matches[0]
        if top.similarity_score >= settings.NOTIFY_THRESHOLD:
            background_tasks.add_task(
                firebase.send_match_notification,
                user_uid        = request.userId,
                lost_item_title = request.title,
                match_count     = len(matches),
                top_match_id    = top.id,
            )

    top_score = matches[0].similarity_score if matches else 0
    logger.info(f"✅ {len(matches)} match(es) found — top score: {top_score}%")

    return MatchResponse(
        lost_item_id = request.id,
        matches      = matches,
        message      = f"{len(matches)} potential match(es) found.",
    )


@app.post(
    "/api/v1/match/batch",
    tags    = ["Admin"],
    summary = "Re-run AI matching for ALL unresolved lost items",
)
async def batch_rematch(
    background_tasks: BackgroundTasks,
    x_api_key:        str = Header(...),
):
    """
    **Admin endpoint** — re-runs AI matching for every unresolved lost item.
    Useful when many new found items are posted at once.
    """
    check_api_key(x_api_key)

    firebase: FirebaseService = app.state.firebase
    matcher:  MatchingService = app.state.matcher

    lost_items  = await firebase.get_all_active_lost_items()
    found_items = await firebase.get_active_found_items()

    if not lost_items or not found_items:
        return {"message": "Nothing to process.", "lost": len(lost_items), "found": len(found_items)}

    results = []
    for lost in lost_items:
        matches = await matcher.find_matches(lost_item=lost, found_items=found_items)
        if matches:
            results.append({"item_id": lost.item_id, "match_count": len(matches)})
            background_tasks.add_task(
                firebase.save_match_results,
                lost_item_id = lost.item_id,
                matches      = matches,
            )

    return {
        "processed":    len(lost_items),
        "with_matches": len(results),
        "details":      results,
    }


if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)