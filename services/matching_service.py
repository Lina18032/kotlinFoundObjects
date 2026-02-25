"""
MatchingService — AI Matching using Groq (FREE, no regional restrictions)

Model: llama-3.1-8b-instant
  - Free tier: 14,400 requests/day, 500,000 tokens/day
  - No regional restrictions
  - Text-only (image scoring falls back to neutral 50)

Scoring per pair:
  Text     (50%) — Groq/LLaMA reads title / description / category
  Image    (20%) — neutral 50 (Groq doesn't support vision)
  Location (20%) — keyword token overlap
  Time     (10%) — exponential decay (72h half-life)
"""

import asyncio
import json
import logging
import re
from math import exp

from groq import AsyncGroq

from config import settings
from models.item import LostItemRequest, FoundItem, MatchResult, ScoreBreakdown

logger = logging.getLogger(__name__)

_SYSTEM_PROMPT = """You are an AI assistant for LGUINAH, a Lost & Found system at ESTIN university (Algeria).
You compare lost and found items and estimate how likely they are the same object.
Respond ONLY with valid JSON. No markdown, no explanation outside JSON."""

_TEXT_PROMPT_TEMPLATE = """Compare these two items and estimate how likely they are the same object.

LOST ITEM:
- Title: {lost_title}
- Category: {lost_category}
- Description: {lost_description}
- Location: {lost_location}

FOUND ITEM:
- Title: {found_title}
- Category: {found_category}
- Description: {found_description}
- Location: {found_location}

Respond ONLY with this JSON (no other text):
{{"score": <integer 0-100>, "explanation": "<one short sentence>"}}

Scoring:
85-100 = Almost certainly the same item
60-84  = Probably the same item
40-59  = Possibly the same item
0-39   = Unlikely the same item"""


class MatchingService:

    def __init__(self):
        self.client = AsyncGroq(api_key=settings.GROQ_API_KEY)
        self.model  = settings.GROQ_MODEL
        logger.info(f"✅ Groq client ready — model: {self.model}")

    # ─────────────────────────────────────────────────────────────────
    # PUBLIC
    # ─────────────────────────────────────────────────────────────────

    async def find_matches(
        self,
        lost_item:   LostItemRequest,
        found_items: list[FoundItem],
    ) -> list[MatchResult]:
        candidates = found_items[: settings.MAX_FOUND_ITEMS_PER_MATCH]

        # Run all comparisons concurrently
        tasks   = [self._score_pair(lost_item, found) for found in candidates]
        results = await asyncio.gather(*tasks, return_exceptions=True)

        matches: list[MatchResult] = []
        for found, result in zip(candidates, results):
            if isinstance(result, Exception):
                logger.warning(f"Scoring failed for {found.id}: {result}")
                continue
            score, breakdown, explanation = result
            if score >= settings.MIN_SCORE_THRESHOLD:
                matches.append(
                    MatchResult(
                        id               = found.id,
                        userId           = found.userId,
                        userName         = found.userName,
                        userEmail        = found.userEmail,
                        title            = found.title,
                        description      = found.description,
                        category         = found.category,
                        location         = found.location,
                        timestamp        = found.timestamp,
                        imageURLs        = found.imageURLs,
                        similarity_score = score,
                        score_breakdown  = breakdown,
                        ai_explanation   = explanation,
                    )
                )

        matches.sort(key=lambda m: m.similarity_score, reverse=True)
        return matches[: settings.MAX_MATCHES_RETURNED]

    # ─────────────────────────────────────────────────────────────────
    # PRIVATE — SCORING
    # ─────────────────────────────────────────────────────────────────

    async def _score_pair(
        self, lost: LostItemRequest, found: FoundItem
    ) -> tuple[int, ScoreBreakdown, str]:

        text_score, explanation = await self._text_score(lost, found)
        image_score             = 50   # Groq is text-only; neutral score
        location_score          = self._location_score(lost.location, found.location)
        time_score              = self._time_score(lost.timestamp, found.timestamp)

        overall = (
            text_score       * settings.WEIGHT_TEXT
            + location_score * settings.WEIGHT_LOCATION
            + time_score     * settings.WEIGHT_TIME
            + image_score    * settings.WEIGHT_IMAGE
        ) // 100

        breakdown = ScoreBreakdown(
            text_score     = text_score,
            location_score = location_score,
            time_score     = time_score,
            image_score    = image_score,
        )
        return overall, breakdown, explanation

    # ── 1. Text — Groq / LLaMA ───────────────────────────────────────

    async def _text_score(
        self, lost: LostItemRequest, found: FoundItem
    ) -> tuple[int, str]:

        prompt = _TEXT_PROMPT_TEMPLATE.format(
            lost_title        = lost.title,
            lost_category     = lost.category.value,
            lost_description  = lost.description,
            lost_location     = lost.location or "not specified",
            found_title       = found.title,
            found_category    = found.category.value,
            found_description = found.description,
            found_location    = found.location or "not specified",
        )

        for attempt in range(3):
            try:
                response = await self.client.chat.completions.create(
                    model       = self.model,
                    temperature = 0.1,
                    max_tokens  = 150,
                    messages    = [
                        {"role": "system", "content": _SYSTEM_PROMPT},
                        {"role": "user",   "content": prompt},
                    ],
                )
                raw         = response.choices[0].message.content.strip()
                data        = self._parse_json(raw)
                score       = max(0, min(100, int(data["score"])))
                explanation = data.get("explanation", "")
                return score, explanation

            except Exception as e:
                err = str(e)
                if ("429" in err or "503" in err or "rate" in err.lower()) and attempt < 2:
                    wait = (attempt + 1) * 5
                    logger.warning(f"Groq rate limited — retrying in {wait}s (attempt {attempt+1}/3)")
                    await asyncio.sleep(wait)
                else:
                    logger.warning(f"Text scoring error: {e}")
                    return self._keyword_score(lost, found), "AI unavailable — used keyword matching."

        return self._keyword_score(lost, found), "AI unavailable after retries — used keyword matching."

    # ── 2. Location — keyword overlap ────────────────────────────────

    @staticmethod
    def _location_score(loc_lost: str | None, loc_found: str | None) -> int:
        if not loc_lost or not loc_found:
            return 50
        lost_t  = set(loc_lost.lower().split())
        found_t = set(loc_found.lower().split())
        if not lost_t or not found_t:
            return 50
        overlap = len(lost_t & found_t)
        return int((overlap / max(len(lost_t), len(found_t))) * 100)

    # ── 3. Time — exponential decay ──────────────────────────────────

    @staticmethod
    def _time_score(ts_lost: int, ts_found: int) -> int:
        """Both timestamps are milliseconds."""
        diff_hours = abs(ts_found - ts_lost) / (1000 * 3600)
        return max(0, min(100, int(100 * exp(-diff_hours / 72))))

    # ── Fallback — keyword Jaccard ────────────────────────────────────

    @staticmethod
    def _keyword_score(lost: LostItemRequest, found: FoundItem) -> int:
        def tokenize(t: str) -> set[str]:
            return set(re.sub(r'[^\w\s]', '', t.lower()).split())
        lt             = tokenize(lost.title  + " " + lost.description)
        ft             = tokenize(found.title + " " + found.description)
        category_bonus = 20 if lost.category == found.category else 0
        union          = len(lt | ft)
        overlap        = len(lt & ft)
        return min(100, (int((overlap / union) * 80) if union else 0) + category_bonus)

    # ── JSON parser — handles truncated responses ──────────────────────

    @staticmethod
    def _parse_json(text: str) -> dict:
        clean = re.sub(r"```(?:json)?", "", text).replace("```", "").strip()

        # Try full JSON first
        match = re.search(r'\{.*\}', clean, re.DOTALL)
        if match:
            try:
                return json.loads(match.group())
            except json.JSONDecodeError:
                pass

        # Fallback: extract score from partial JSON
        score_match = re.search(r'"score"\s*:\s*(\d+)', clean)
        if score_match:
            score      = int(score_match.group(1))
            expl_match = re.search(r'"explanation"\s*:\s*"([^"]+)', clean)
            explanation = expl_match.group(1) if expl_match else "AI matched this item."
            return {"score": score, "explanation": explanation}

        raise ValueError(f"No JSON in: {text!r}")