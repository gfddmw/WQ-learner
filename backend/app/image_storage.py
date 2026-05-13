import os
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Protocol, runtime_checkable
from uuid import uuid4


@dataclass(frozen=True)
class StoredImage:
    object_key: str
    image_url: str


@runtime_checkable
class ImageStorage(Protocol):
    def save_upload(self, user_id: str, content: bytes, content_type: str) -> StoredImage:
        ...


class LocalImageStorage:
    def __init__(self, upload_dir: str | Path) -> None:
        self.upload_dir = Path(upload_dir)

    def save_upload(self, user_id: str, content: bytes, content_type: str) -> StoredImage:
        extension = extension_for_content_type(content_type)
        object_key = f"users/{user_id}/questions/{uuid4()}.{extension}"
        target = self.upload_dir / object_key
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(content)
        return StoredImage(object_key=object_key, image_url=f"/uploads/{object_key}")


class OssImageStorage:
    def __init__(
        self,
        bucket: str,
        endpoint: str,
        bucket_client: Any | None = None,
    ) -> None:
        self.bucket = bucket
        self.endpoint = endpoint
        self.bucket_client = bucket_client

    def save_upload(self, user_id: str, content: bytes, content_type: str) -> StoredImage:
        extension = extension_for_content_type(content_type)
        object_key = f"users/{user_id}/questions/{uuid4()}.{extension}"
        client = self.bucket_client or create_oss_bucket(
            bucket=self.bucket,
            endpoint=self.endpoint,
        )
        client.put_object(
            object_key,
            content,
            headers={"Content-Type": content_type},
        )
        return StoredImage(
            object_key=object_key,
            image_url=f"oss://{self.bucket}/{object_key}",
        )


def create_oss_bucket(bucket: str, endpoint: str) -> Any:
    try:
        import oss2
    except ImportError as error:
        raise RuntimeError("missing oss2 dependency; install backend/requirements.txt") from error

    auth = create_oss_auth(oss2)
    return oss2.Bucket(auth, endpoint, bucket)


def create_oss_auth(oss2: Any) -> Any:
    explicit_access_key_id = os.environ.get("ALIBABA_CLOUD_ACCESS_KEY_ID")
    explicit_access_key_secret = os.environ.get("ALIBABA_CLOUD_ACCESS_KEY_SECRET")
    access_key_id = explicit_access_key_id or os.environ.get("OSS_ACCESS_KEY_ID")
    access_key_secret = explicit_access_key_secret or os.environ.get("OSS_ACCESS_KEY_SECRET")
    security_token = ""
    if not (explicit_access_key_id and explicit_access_key_secret):
        security_token = os.environ.get("ALIBABA_CLOUD_SECURITY_TOKEN") or os.environ.get("OSS_SECURITY_TOKEN") or ""
    if not access_key_id or not access_key_secret:
        raise RuntimeError("missing OSS credentials")
    if security_token:
        return oss2.StsAuth(access_key_id, access_key_secret, security_token)
    return oss2.Auth(access_key_id, access_key_secret)


def extension_for_content_type(content_type: str) -> str:
    normalized = content_type.split(";")[0].strip().lower()
    if normalized == "image/png":
        return "png"
    if normalized in {"image/jpeg", "image/jpg"}:
        return "jpg"
    return "bin"


def default_upload_dir() -> Path:
    configured = os.environ.get("WQ_LEARNER_UPLOAD_DIR")
    if configured:
        return Path(configured)
    return Path(__file__).resolve().parents[1] / "data" / "uploads"


def create_image_storage() -> ImageStorage:
    bucket = os.environ.get("WQ_LEARNER_OSS_BUCKET")
    if bucket:
        endpoint = os.environ.get("WQ_LEARNER_OSS_ENDPOINT", "")
        return OssImageStorage(bucket=bucket, endpoint=endpoint)
    return LocalImageStorage(default_upload_dir())


image_storage: ImageStorage = create_image_storage()
