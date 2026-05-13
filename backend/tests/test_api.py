import os
from pathlib import Path

from fastapi.testclient import TestClient

import app.main as main_module
from app.main import app


client = TestClient(app)


def auth_headers():
    client.post(
        "/auth/register",
        json={"email": "demo@example.com", "password": "secret123"},
    )
    response = client.post(
        "/auth/login",
        json={"email": "demo@example.com", "password": "secret123"},
    )
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
    assert draft["answer_md_latex"] == ""
    assert draft["explanation_md_latex"] == ""
    assert draft["image_url"].startswith("/uploads/users/")
    uploaded_file = Path(os.environ["WQ_LEARNER_UPLOAD_DIR"]) / draft["image_url"].removeprefix("/uploads/")
    assert uploaded_file.read_bytes() == b"fake-image"

    before_confirm = client.get("/questions", headers=headers)

    assert before_confirm.status_code == 200
    assert before_confirm.json() == []

    confirm = client.post(
        f"/questions/{draft['id']}/confirm",
        headers=headers,
        json={
            "content_md_latex": "Binary tree traversal has time complexity $O(n)$.",
            "subject": "Data Structures",
            "chapter": "Trees",
            "mastery": "reviewing",
        },
    )

    assert confirm.status_code == 200
    assert confirm.json()["status"] == "confirmed"

    listed = client.get("/questions?subject=Data%20Structures", headers=headers)

    assert listed.status_code == 200
    assert len(listed.json()) == 1

    practice = client.post("/practice/original", headers=headers, json={"count": 1})

    assert practice.status_code == 200
    assert practice.json()["mode"] == "original"
    assert len(practice.json()["questions"]) == 1


def test_upload_uses_ocr_result_for_draft_without_answer_or_explanation(monkeypatch):
    headers = auth_headers()
    fake_ocr = FakeOcrService()
    monkeypatch.setattr(main_module, "ocr_service", fake_ocr)

    upload = client.post(
        "/questions/upload",
        headers=headers,
        files={"image": ("network.jpg", b"jpeg-image", "image/jpeg")},
    )

    assert upload.status_code == 200
    draft = upload.json()
    assert fake_ocr.calls == [
        {
            "image_content": b"jpeg-image",
            "content_type": "image/jpeg",
            "image_url": draft["image_url"],
        }
    ]
    assert draft["content_md_latex"] == "TCP congestion window formula: $cwnd=2^k$."
    assert draft["subject"] == "Computer Networks"
    assert draft["chapter"] == "Transport Layer"
    assert draft["answer_md_latex"] == ""
    assert draft["explanation_md_latex"] == ""


def test_variant_practice_uses_source_question_and_variant_service(monkeypatch):
    headers = auth_headers()
    fake_variant_service = FakeVariantService()
    monkeypatch.setattr(main_module, "variant_service", fake_variant_service)
    upload = client.post(
        "/questions/upload",
        headers=headers,
        files={"image": ("tree.png", b"fake-image", "image/png")},
    )
    source = upload.json()

    response = client.post(
        "/practice/variant",
        headers=headers,
        json={"source_question_id": source["id"], "topic": "Trees"},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["mode"] == "variant"
    assert body["variant"]["title"] == "Binary tree traversal variant"
    assert body["variant"]["source_question_id"] == source["id"]
    assert fake_variant_service.calls[0]["source_question_id"] == source["id"]
    assert fake_variant_service.calls[0]["topic"] == "Trees"


def test_variant_practice_returns_404_for_missing_source_question():
    headers = auth_headers()

    response = client.post(
        "/practice/variant",
        headers=headers,
        json={"source_question_id": "missing", "topic": "Trees"},
    )

    assert response.status_code == 404


class FakeOcrService:
    def __init__(self):
        self.calls = []

    def recognize(self, image_content: bytes, content_type: str, image_url: str):
        from app.ocr import OcrResult

        self.calls.append(
            {
                "image_content": image_content,
                "content_type": content_type,
                "image_url": image_url,
            }
        )
        return OcrResult(
            content_md_latex="TCP congestion window formula: $cwnd=2^k$.",
            subject="Computer Networks",
            chapter="Transport Layer",
            confidence=2,
            answer_md_latex="This should not be stored during upload.",
            explanation_md_latex="This should not be stored during upload.",
        )


class FakeVariantService:
    def __init__(self):
        self.calls = []

    def generate(self, source_question, topic: str):
        self.calls.append(
            {
                "source_question_id": source_question.id,
                "topic": topic,
            }
        )
        return {
            "source_question_id": source_question.id,
            "title": "Binary tree traversal variant",
            "content_md_latex": "If the tree shape changes, analyze traversal complexity.",
            "answer_md_latex": "The time complexity is still $O(n)$.",
            "explanation_md_latex": "Each node is visited once.",
        }
