# WQ Learner Backend

FastAPI MVP backend for the Android WQ Learner app.

## Run

```powershell
uvicorn app.main:app --app-dir backend --reload
```

## Test

```powershell
pytest backend/tests -v
```

The current version uses SQLite storage and simulated OCR/variant generation. It keeps the API shape ready for object storage, real OCR, and LLM integration.

## Data

By default the backend stores data in:

```text
backend/data/wq_learner.db
```

Override it with `WQ_LEARNER_DB` when you want a different SQLite file:

```powershell
$env:WQ_LEARNER_DB="E:\A-NJU\WQ-learner\backend\data\dev.db"
uvicorn app.main:app --app-dir backend --reload
```
