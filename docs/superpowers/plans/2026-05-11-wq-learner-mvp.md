# WQ Learner MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a runnable MVP matching the approved WQ Learner design: a FastAPI backend for account/question/practice APIs and a Jetpack Compose Android app shell for upload review, question bank, practice, and account flows.

**Architecture:** The first implementation uses an in-memory FastAPI backend so the API contract, recognition draft flow, classification, and practice modes work without external infrastructure. The Android app uses local sample state and shared domain logic to present the complete product flow while leaving real HTTP integration as the next incremental step.

**Tech Stack:** Android Kotlin, Jetpack Compose Material 3, JUnit 4, Python 3, FastAPI, Pydantic, pytest.

---

## File Structure

- Create `backend/app/__init__.py`: backend package marker.
- Create `backend/app/models.py`: Pydantic request/response models and in-memory domain records.
- Create `backend/app/classifier.py`: 11408 subject/chapter keyword classifier.
- Create `backend/app/store.py`: in-memory user, token, question, and practice session store.
- Create `backend/app/main.py`: FastAPI routes.
- Create `backend/tests/test_classifier.py`: classifier behavior tests.
- Create `backend/tests/test_api.py`: API contract tests.
- Create `backend/README.md`: run and test commands.
- Create `app/src/main/java/com/example/wq_learner1/domain/SubjectClassifier.kt`: Android-side classifier for UI suggestions.
- Create `app/src/test/java/com/example/wq_learner1/domain/SubjectClassifierTest.kt`: Android classifier tests.
- Replace `app/src/main/java/com/example/wq_learner1/MainActivity.kt`: Compose MVP app shell.

## Task 1: Backend Classifier

**Files:**
- Create: `backend/app/classifier.py`
- Create: `backend/tests/test_classifier.py`

- [ ] **Step 1: Write failing classifier tests**

```python
from app.classifier import classify_question


def test_classifies_data_structure_tree_question():
    result = classify_question("二叉树遍历和哈希查找的平均时间复杂度是多少？")
    assert result.subject == "数据结构"
    assert result.chapter == "树与二叉树"
    assert result.confidence > 0


def test_low_confidence_for_unknown_content():
    result = classify_question("这是一道无法判断来源的题目")
    assert result.subject == "待分类"
    assert result.chapter == "待选择"
    assert result.confidence == 0
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `pytest backend/tests/test_classifier.py -v`

Expected: FAIL because `app.classifier` does not exist.

- [ ] **Step 3: Implement classifier**

Create `ClassificationResult` and `classify_question(text: str)` with keyword scoring for 数据结构、计算机组成原理、操作系统、计算机网络. Return `待分类/待选择/0` when no keyword matches.

- [ ] **Step 4: Run tests to verify they pass**

Run: `pytest backend/tests/test_classifier.py -v`

Expected: PASS.

## Task 2: Backend API

**Files:**
- Create: `backend/app/models.py`
- Create: `backend/app/store.py`
- Create: `backend/app/main.py`
- Create: `backend/tests/test_api.py`
- Create: `backend/README.md`

- [ ] **Step 1: Write failing API tests**

```python
from fastapi.testclient import TestClient
from app.main import app


client = TestClient(app)


def auth_headers():
    client.post("/auth/register", json={"email": "demo@example.com", "password": "secret123"})
    response = client.post("/auth/login", json={"email": "demo@example.com", "password": "secret123"})
    token = response.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}


def test_upload_confirm_list_and_draw_original_question():
    headers = auth_headers()
    upload = client.post(
        "/questions/upload",
        headers=headers,
        files={"image": ("tree.png", b"fake-image", "image/png")},
    )
    assert upload.status_code == 200
    draft = upload.json()
    assert draft["content_md_latex"]
    assert draft["status"] == "draft"

    confirm = client.post(
        f"/questions/{draft['id']}/confirm",
        headers=headers,
        json={
            "content_md_latex": "二叉树遍历的时间复杂度为 $O(n)$。",
            "subject": "数据结构",
            "chapter": "树与二叉树",
            "mastery": "reviewing",
        },
    )
    assert confirm.status_code == 200
    assert confirm.json()["status"] == "confirmed"

    listed = client.get("/questions?subject=数据结构", headers=headers)
    assert listed.status_code == 200
    assert len(listed.json()) == 1

    practice = client.post("/practice/original", headers=headers, json={"count": 1})
    assert practice.status_code == 200
    assert practice.json()["mode"] == "original"
    assert len(practice.json()["questions"]) == 1


def test_variant_practice_returns_simulated_variant():
    headers = auth_headers()
    response = client.post(
        "/practice/variant",
        headers=headers,
        json={"source_question_id": "sample", "topic": "树与二叉树"},
    )
    assert response.status_code == 200
    assert response.json()["mode"] == "variant"
    assert "模拟变形题" in response.json()["variant"]["title"]
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `pytest backend/tests/test_api.py -v`

Expected: FAIL because API files do not exist.

- [ ] **Step 3: Implement models, store, and routes**

Implement register/login/token auth, upload draft creation, confirm/update/list/get questions, original practice, variant practice, and review recording. Use in-memory dictionaries and deterministic simulated OCR text.

- [ ] **Step 4: Run API tests**

Run: `pytest backend/tests/test_api.py -v`

Expected: PASS.

## Task 3: Android Domain Classifier

**Files:**
- Create: `app/src/main/java/com/example/wq_learner1/domain/SubjectClassifier.kt`
- Create: `app/src/test/java/com/example/wq_learner1/domain/SubjectClassifierTest.kt`

- [ ] **Step 1: Write failing Android unit tests**

```kotlin
package com.example.wq_learner1.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubjectClassifierTest {
    @Test
    fun classifiesNetworkTcpQuestion() {
        val result = SubjectClassifier.classify("TCP 拥塞控制和滑动窗口")
        assertEquals("计算机网络", result.subject)
        assertEquals("传输层", result.chapter)
        assertTrue(result.confidence > 0)
    }

    @Test
    fun fallsBackWhenNoKeywordMatches() {
        val result = SubjectClassifier.classify("暂时无法判断的错题")
        assertEquals("待分类", result.subject)
        assertEquals("待选择", result.chapter)
        assertEquals(0, result.confidence)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.example.wq_learner1.domain.SubjectClassifierTest"`

Expected: FAIL because `SubjectClassifier` does not exist.

- [ ] **Step 3: Implement Android classifier**

Implement a small Kotlin object with the same subject/chapter vocabulary used by the backend.

- [ ] **Step 4: Run tests**

Run: `./gradlew testDebugUnitTest --tests "com.example.wq_learner1.domain.SubjectClassifierTest"`

Expected: PASS.

## Task 4: Android Compose MVP

**Files:**
- Replace: `app/src/main/java/com/example/wq_learner1/MainActivity.kt`

- [ ] **Step 1: Implement Compose app shell**

Create four tabs: 上传、题库、练习、我的. Use local sample state for recognized Markdown+LaTeX, subject/chapter suggestions, question list, original practice, and simulated variant practice.

- [ ] **Step 2: Run Android unit tests**

Run: `./gradlew testDebugUnitTest`

Expected: PASS.

- [ ] **Step 3: Build debug APK**

Run: `./gradlew assembleDebug`

Expected: BUILD SUCCESSFUL.

## Self-Review Checklist

- Spec coverage: authentication, upload draft, Markdown+LaTeX content, subject/chapter classification, question bank, original practice, simulated variant practice, and Android screen shell are covered.
- Deliberate first-version simplification: database, object storage, real OCR provider, real HTTP Android client, and real LLM integration are not implemented in this MVP. Their API boundaries are represented and ready for the next iteration.
- Test coverage: backend classifier, backend API contract, and Android domain classifier are covered.
