import os
from dataclasses import dataclass
from pathlib import Path
from typing import Protocol, runtime_checkable
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
    def __init__(self, bucket: str, endpoint: str) -> None:
        self.bucket = bucket
        self.endpoint = endpoint

    def save_upload(self, user_id: str, content: bytes, content_type: str) -> StoredImage:
        raise NotImplementedError("OSS 图片存储适配层已预留，真实 OSS 上传将在后续功能接入")


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
