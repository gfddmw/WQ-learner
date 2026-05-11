# WQ Learner Remaining Features Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the missing production-path features one at a time, reporting after each finished feature.

**Architecture:** Keep each feature independently shippable. Backend durability comes first, then Android-to-backend networking, then real Android image selection/capture, then OCR/LLM provider integrations.

**Tech Stack:** Python sqlite3/FastAPI/pytest, Android Kotlin/Jetpack Compose/JUnit, Gradle.

---

## Feature Order

1. Backend SQLite persistence.
2. Android HTTP client and token state.
3. Real Android gallery image selection.
4. Real Android camera capture.
5. Backend file storage for uploaded images.
6. OCR/formula-recognition provider adapter.
7. Real LLM variant-question provider adapter.

## Feature 1: Backend SQLite Persistence

**Files:**
- Modify: `backend/app/store.py`
- Modify: `backend/app/main.py`
- Add: `backend/tests/test_persistence.py`
- Modify: `backend/README.md`

Steps:

- [ ] Write a failing test that registers a user, uploads and confirms a question in one SQLite-backed store instance, creates a second store pointing at the same database file, logs in again, and confirms the question is still listed.
- [ ] Run the test and verify it fails because only the current in-memory store exists.
- [ ] Replace the in-memory dictionaries with a SQLite-backed store while preserving the public store methods used by `main.py`.
- [ ] Update `main.py` to avoid direct dictionary access and use store helper methods.
- [ ] Run backend tests.
- [ ] Report Feature 1 complete before starting Feature 2.
