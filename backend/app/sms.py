import hashlib
import hmac
import json
import os
import random
import re
import time
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any, Callable, Protocol
from urllib import parse, request
from uuid import uuid4


class SmsService(Protocol):
    def send_code(self, phone: str, code: str) -> None:
        ...


class SmsConfigurationError(RuntimeError):
    pass


class SmsSendError(RuntimeError):
    pass


def normalize_phone(phone: str) -> str:
    normalized = re.sub(r"[\s-]", "", phone.strip())
    if normalized.startswith("+86"):
        normalized = normalized[3:]
    elif normalized.startswith("0086"):
        normalized = normalized[4:]
    elif normalized.startswith("86") and len(normalized) == 13:
        normalized = normalized[2:]
    if not re.fullmatch(r"1[3-9]\d{9}", normalized):
        raise ValueError("invalid phone number")
    return normalized


class SmsCodeStore:
    def __init__(
        self,
        ttl_seconds: int = 300,
        now: Callable[[], float] | None = None,
        code_factory: Callable[[], str] | None = None,
    ) -> None:
        self.ttl_seconds = ttl_seconds
        self.now = now or time.time
        self.code_factory = code_factory or (lambda: f"{random.SystemRandom().randint(0, 999999):06d}")
        self._codes: dict[str, tuple[str, float]] = {}

    def issue(self, phone: str) -> str:
        code = self.code_factory()
        self._codes[phone] = (code, self.now() + self.ttl_seconds)
        return code

    def verify(self, phone: str, code: str) -> bool:
        stored = self._codes.get(phone)
        if stored is None:
            return False
        expected, expires_at = stored
        if self.now() > expires_at:
            self._codes.pop(phone, None)
            return False
        if not hmac.compare_digest(expected, code):
            return False
        self._codes.pop(phone, None)
        return True


@dataclass
class AliyunSmsProvider:
    access_key_id: str
    access_key_secret: str
    sign_name: str
    template_code: str
    endpoint: str = "dysmsapi.aliyuncs.com"
    sender: Callable[[str, bytes, dict[str, str], int], dict[str, Any]] | None = None
    now: Callable[[], str] | None = None
    nonce: Callable[[], str] | None = None

    def send_code(self, phone: str, code: str) -> None:
        body = self._request_body(phone, code)
        hashed_body = hashlib.sha256(body).hexdigest()
        headers = self._signed_headers(body, hashed_body)
        response = (self.sender or self._send_http)(
            f"https://{self.endpoint}/",
            body,
            headers,
            10,
        )
        if str(response.get("Code", "")) != "OK":
            message = response.get("Message") or response.get("Code") or "unknown Aliyun SMS error"
            raise SmsSendError(str(message))

    def _request_body(self, phone: str, code: str) -> bytes:
        template_param = json.dumps({"code": code}, ensure_ascii=False, separators=(",", ":"))
        return parse.urlencode(
            {
                "PhoneNumbers": phone,
                "SignName": self.sign_name,
                "TemplateCode": self.template_code,
                "TemplateParam": template_param,
            },
            quote_via=parse.quote,
        ).encode("utf-8")

    def _signed_headers(self, body: bytes, hashed_body: str) -> dict[str, str]:
        request_time = self.now() if self.now is not None else datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
        signature_nonce = self.nonce() if self.nonce is not None else uuid4().hex
        signed_headers = "host;x-acs-action;x-acs-content-sha256;x-acs-date;x-acs-signature-nonce;x-acs-version"
        canonical_headers = "\n".join(
            [
                f"host:{self.endpoint}",
                "x-acs-action:SendSms",
                f"x-acs-content-sha256:{hashed_body}",
                f"x-acs-date:{request_time}",
                f"x-acs-signature-nonce:{signature_nonce}",
                "x-acs-version:2017-05-25",
            ]
        )
        canonical_request = "\n".join(
            [
                "POST",
                "/",
                "",
                canonical_headers,
                "",
                signed_headers,
                hashed_body,
            ]
        )
        string_to_sign = "ACS3-HMAC-SHA256\n" + hashlib.sha256(canonical_request.encode("utf-8")).hexdigest()
        signature = hmac.new(
            self.access_key_secret.encode("utf-8"),
            string_to_sign.encode("utf-8"),
            hashlib.sha256,
        ).hexdigest()
        return {
            "host": self.endpoint,
            "content-type": "application/x-www-form-urlencoded",
            "x-acs-action": "SendSms",
            "x-acs-content-sha256": hashed_body,
            "x-acs-date": request_time,
            "x-acs-signature-nonce": signature_nonce,
            "x-acs-version": "2017-05-25",
            "Authorization": (
                "ACS3-HMAC-SHA256 "
                f"Credential={self.access_key_id},SignedHeaders={signed_headers},Signature={signature}"
            ),
        }

    @staticmethod
    def _send_http(url: str, data: bytes, headers: dict[str, str], timeout: int) -> dict[str, Any]:
        http_request = request.Request(url=url, data=data, headers=headers, method="POST")
        with request.urlopen(http_request, timeout=timeout) as response:
            return json.loads(response.read().decode("utf-8"))


class MissingSmsProvider:
    def send_code(self, phone: str, code: str) -> None:
        raise SmsConfigurationError(
            "missing Aliyun SMS configuration: set ALIBABA_CLOUD_ACCESS_KEY_ID, "
            "ALIBABA_CLOUD_ACCESS_KEY_SECRET, ALIYUN_SMS_SIGN_NAME, and ALIYUN_SMS_TEMPLATE_CODE"
        )


def create_sms_service() -> SmsService:
    access_key_id = os.environ.get("ALIBABA_CLOUD_ACCESS_KEY_ID", "")
    access_key_secret = os.environ.get("ALIBABA_CLOUD_ACCESS_KEY_SECRET", "")
    sign_name = os.environ.get("ALIYUN_SMS_SIGN_NAME", "")
    template_code = os.environ.get("ALIYUN_SMS_TEMPLATE_CODE", "")
    if not all([access_key_id, access_key_secret, sign_name, template_code]):
        return MissingSmsProvider()
    return AliyunSmsProvider(
        access_key_id=access_key_id,
        access_key_secret=access_key_secret,
        sign_name=sign_name,
        template_code=template_code,
        endpoint=os.environ.get("ALIYUN_SMS_ENDPOINT", "dysmsapi.aliyuncs.com"),
    )


sms_code_store = SmsCodeStore()
sms_service = create_sms_service()
