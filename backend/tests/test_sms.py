import json

from app.sms import AliyunSmsProvider, SmsCodeStore, normalize_phone


def test_sms_code_store_verifies_once_and_expires():
    current_time = 1000
    store = SmsCodeStore(now=lambda: current_time)

    code = store.issue("13800138000")

    assert len(code) == 6
    assert store.verify("13800138000", code)
    assert not store.verify("13800138000", code)

    expired = store.issue("13800138000")
    current_time += 301

    assert not store.verify("13800138000", expired)


def test_normalize_phone_accepts_china_mainland_numbers():
    assert normalize_phone("13800138000") == "13800138000"
    assert normalize_phone("+8613800138000") == "13800138000"
    assert normalize_phone("86 13800138000") == "13800138000"


def test_aliyun_sms_provider_sends_signed_send_sms_request():
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
        return {"Code": "OK", "Message": "OK", "RequestId": "REQ-1", "BizId": "BIZ-1"}

    provider = AliyunSmsProvider(
        access_key_id="ak-id",
        access_key_secret="ak-secret",
        sign_name="错题学习",
        template_code="SMS_123456",
        sender=fake_sender,
        now=lambda: "2026-05-13T08:00:00Z",
        nonce=lambda: "nonce-1",
    )

    provider.send_code("13800138000", "123456")

    assert requests[0]["url"] == "https://dysmsapi.aliyuncs.com/"
    assert "PhoneNumbers=13800138000" in requests[0]["data"]
    assert "SignName=" in requests[0]["data"]
    assert "TemplateCode=SMS_123456" in requests[0]["data"]
    assert json.loads(requests[0]["headers"]["Authorization"].split("Signature=")[0] and '{"ok": true}')["ok"]
    assert requests[0]["headers"]["x-acs-action"] == "SendSms"
    assert requests[0]["headers"]["x-acs-version"] == "2017-05-25"
    assert requests[0]["headers"]["x-acs-signature-nonce"] == "nonce-1"
