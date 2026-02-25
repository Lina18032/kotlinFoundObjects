# LGUINAH — AI Matching API

Auto-matches lost & found items using **Groq AI (free)** + **Firebase Firestore**.

---

## Setup

### 1. Install dependencies
```bash
pip install -r requirements.txt
```

### 2. Configure environment
```bash
cp .env.example .env
```
Fill in `.env` with your keys:

| Key | Where to get it |
|-----|----------------|
| `GROQ_API_KEY` | [console.groq.com](https://console.groq.com) → API Keys → Create |
| `FIREBASE_PROJECT_ID` | Firebase Console → Project Settings |
| `FIREBASE_SERVICE_ACCOUNT_PATH` | Firebase Console → Project Settings → Service Accounts → Generate new private key |
| `API_KEY` | Any random string you choose |

### 3. Run
```bash
uvicorn main:app --reload --port 8000
```

---

## Test it

Open **http://127.0.0.1:8000/docs** → click **Authorize** → enter your `API_KEY` → try the `/api/v1/match` endpoint.

Or with curl:
```bash
curl -X POST http://127.0.0.1:8000/api/v1/match \
  -H "x-api-key: your_api_key" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "your_firestore_doc_id",
    "userId": "firebase_uid",
    "userName": "lina LALEM",
    "userEmail": "l_lalem@estin.dz",
    "title": "keys",
    "description": "with a key holder",
    "category": "KEYS",
    "location": "residence",
    "timestamp": 1764977468368,
    "imageURLs": [],
    "status": "LOST",
    "resolved": false
  }'
```

---

## Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/health` | Check if API is running |
| POST | `/api/v1/match` | Match a lost item against found items |
| POST | `/api/v1/match/batch` | Re-run matching for all lost items (admin) |

---

## How it works

1. POST a lost item → API fetches all **FOUND** items from Firestore
2. AI compares title, description, category, location, and time
3. Returns top matches with a **similarity score (0–100%)**
4. Results saved to `/matches/{id}` in Firestore automatically

---

## Category values
`KEYS` · `STUDENT_CARD` · `PHONE` · `BAG` · `DOCUMENTS` · `ELECTRONICS` · `CLOTHING` · `OTHER`