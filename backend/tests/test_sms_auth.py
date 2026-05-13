from fastapi.testclient import TestClient

import app.main as main_module
from app.main import app


client = TestClient(app)


def test_sms_code_login_registers_phone_user_and_returns_token(monkeypatch):
    fake_sms = FakeSmsService()
    monkeypatch.setattr(main_module, "sms_service", fake_sms)

    send = client.post("/auth/sms/send", json={"phone": "13800138000"})

    assert send.status_code == 200
    assert send.json() == {"sent": True}
    assert fake_sms.sent == [{"phone": "13800138000", "code": send.json().get("debug_code", "") or fake_sms.last_code}]

    login = client.post(
        "/auth/sms/login",
        json={"phone": "13800138000", "code": fake_sms.last_code},
    )

    assert login.status_code == 200
    assert login.json()["access_token"]

    questions = client.get(
        "/questions",
        headers={"Authorization": f"Bearer {login.json()['access_token']}"},
    )

    assert questions.status_code == 200
    assert questions.json() == []


def test_sms_code_login_rejects_wrong_code(monkeypatch):
    monkeypatch.setattr(main_module, "sms_service", FakeSmsService())

    send = client.post("/auth/sms/send", json={"phone": "13800138001"})
    assert send.status_code == 200

    login = client.post(
        "/auth/sms/login",
        json={"phone": "13800138001", "code": "000000"},
    )

    assert login.status_code == 401


class FakeSmsService:
    def __init__(self):
        self.sent = []
        self.last_code = ""

    def send_code(self, phone: str, code: str):
        self.last_code = code
        self.sent.append({"phone": phone, "code": code})
