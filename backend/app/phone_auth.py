import hashlib
import hmac
import json
import os
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any, Callable, Protocol
from urllib import parse, request
from uuid import uuid4

from .sms import normalize_phone


class PhoneAuthService(Protocol):
    def mobile_for_access_token(self, access_token: str) -> str:
        ...


class PhoneAuthConfigurationError(RuntimeError):
    pass


class PhoneAuthError(RuntimeError):
    pass


@dataclass
class AliyunPhoneAuthProvider:
    access_key_id: str
    access_key_secret: str
    endpoint: str = "dypnsapi.aliyuncs.com"
    sender: Callable[[str, bytes, dict[str, str], int], dict[str, Any]] | None = None
    now: Callable[[], str] | None = None
    nonce: Callable[[], str] | None = None

    def mobile_for_access_token(self, access_token: str) -> str:
        body = parse.urlencode({"AccessToken": access_token}, quote_via=parse.quote).encode("utf-8")
        hashed_body = hashlib.sha256(body).hexdigest()
        headers = self._signed_headers(hashed_body)
        response = (self.sender or self._send_http)(
            f"https://{self.endpoint}/",
            body,
            headers,
            10,
        )
        if str(response.get("Code", "")) != "OK":
            message = response.get("Message") or response.get("Code") or "unknown Aliyun phone auth error"
            raise PhoneAuthError(str(message))
        mobile = response.get("GetMobileResultDTO", {}).get("Mobile", "")
        try:
            return normalize_phone(str(mobile))
        except ValueError as error:
            raise PhoneAuthError("Aliyun phone auth did not return a valid mainland mobile number") from error

    def _signed_headers(self, hashed_body: str) -> dict[str, str]:
        request_time = self.now() if self.now is not None else datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
        signature_nonce = self.nonce() if self.nonce is not None else uuid4().hex
        signed_headers = "host;x-acs-action;x-acs-content-sha256;x-acs-date;x-acs-signature-nonce;x-acs-version"
        canonical_headers = "\n".join(
            [
                f"host:{self.endpoint}",
                "x-acs-action:GetMobile",
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
            "x-acs-action": "GetMobile",
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


class MissingPhoneAuthProvider:
    def mobile_for_access_token(self, access_token: str) -> str:
        raise PhoneAuthConfigurationError(
            "missing Aliyun phone auth configuration: set ALIBABA_CLOUD_ACCESS_KEY_ID and "
            "ALIBABA_CLOUD_ACCESS_KEY_SECRET"
        )


def create_phone_auth_service() -> PhoneAuthService:
    access_key_id = os.environ.get("ALIBABA_CLOUD_ACCESS_KEY_ID", "")
    access_key_secret = os.environ.get("ALIBABA_CLOUD_ACCESS_KEY_SECRET", "")
    if not all([access_key_id, access_key_secret]):
        return MissingPhoneAuthProvider()
    return AliyunPhoneAuthProvider(
        access_key_id=access_key_id,
        access_key_secret=access_key_secret,
        endpoint=os.environ.get("ALIYUN_PHONE_AUTH_ENDPOINT", "dypnsapi.aliyuncs.com"),
    )


phone_auth_service = create_phone_auth_service()
