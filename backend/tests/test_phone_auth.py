from fastapi.testclient import TestClient

import app.main as main_module
from app.main import app
from app.phone_auth import AliyunPhoneAuthProvider, PhoneAuthError


client = TestClient(app)


def test_one_click_login_registers_mobile_user_and_returns_token(monkeypatch):
    fake_phone_auth = FakePhoneAuthService(phone="13800138000")
    monkeypatch.setattr(main_module, "phone_auth_service", fake_phone_auth)

    login = client.post(
        "/auth/phone/one-click-login",
        json={"access_token": "aliyun-sdk-token"},
    )

    assert login.status_code == 200
    assert login.json()["access_token"]
    assert login.json()["account"] == "13800138000"
    assert fake_phone_auth.tokens == ["aliyun-sdk-token"]

    questions = client.get(
        "/questions",
        headers={"Authorization": f"Bearer {login.json()['access_token']}"},
    )

    assert questions.status_code == 200
    assert questions.json() == []


def test_one_click_login_rejects_failed_mobile_lookup(monkeypatch):
    monkeypatch.setattr(main_module, "phone_auth_service", FakePhoneAuthService(error=PhoneAuthError("token expired")))

    login = client.post(
        "/auth/phone/one-click-login",
        json={"access_token": "expired-token"},
    )

    assert login.status_code == 401


def test_aliyun_phone_auth_provider_exchanges_access_token_for_mobile():
    requests = []

    def fake_sender(url, data, headers, timeout):
        requests.append(
            {
                "url": url,
                "data": data.decode("utf-8"),
                "headers": headers,
                "timeout": timeout,
            }
        )
        return {
            "Code": "OK",
            "Message": "OK",
            "RequestId": "REQ-1",
            "GetMobileResultDTO": {"Mobile": "13800138000"},
        }

    provider = AliyunPhoneAuthProvider(
        access_key_id="ak-id",
        access_key_secret="ak-secret",
        sender=fake_sender,
        now=lambda: "2026-05-13T08:00:00Z",
        nonce=lambda: "nonce-1",
    )

    phone = provider.mobile_for_access_token("aliyun-sdk-token")

    assert phone == "13800138000"
    assert requests[0]["url"] == "https://dypnsapi.aliyuncs.com/"
    assert "AccessToken=aliyun-sdk-token" in requests[0]["data"]
    assert requests[0]["headers"]["x-acs-action"] == "GetMobile"
    assert requests[0]["headers"]["x-acs-version"] == "2017-05-25"
    assert requests[0]["headers"]["x-acs-signature-nonce"] == "nonce-1"
    assert requests[0]["headers"]["Authorization"].startswith("ACS3-HMAC-SHA256 ")


class FakePhoneAuthService:
    def __init__(self, phone: str = "", error: Exception | None = None):
        self.phone = phone
        self.error = error
        self.tokens = []

    def mobile_for_access_token(self, access_token: str) -> str:
        self.tokens.append(access_token)
        if self.error is not None:
            raise self.error
        return self.phone
