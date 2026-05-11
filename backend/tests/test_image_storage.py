from app.image_storage import ImageStorage, LocalImageStorage, OssImageStorage, create_image_storage


def test_local_image_storage_writes_file_and_returns_upload_url(tmp_path):
    storage = LocalImageStorage(tmp_path)

    stored = storage.save_upload(
        user_id="user-1",
        content=b"fake-image",
        content_type="image/png",
    )

    assert isinstance(storage, ImageStorage)
    assert stored.object_key.startswith("users/user-1/questions/")
    assert stored.object_key.endswith(".png")
    assert stored.image_url == f"/uploads/{stored.object_key}"
    assert (tmp_path / stored.object_key).read_bytes() == b"fake-image"


def test_local_image_storage_uses_jpg_extension_for_jpeg(tmp_path):
    storage = LocalImageStorage(tmp_path)

    stored = storage.save_upload(
        user_id="user-1",
        content=b"fake-image",
        content_type="image/jpeg",
    )

    assert stored.object_key.endswith(".jpg")


def test_create_image_storage_uses_local_storage_without_oss_bucket(monkeypatch, tmp_path):
    monkeypatch.delenv("WQ_LEARNER_OSS_BUCKET", raising=False)
    monkeypatch.setenv("WQ_LEARNER_UPLOAD_DIR", str(tmp_path))

    storage = create_image_storage()

    assert isinstance(storage, LocalImageStorage)


def test_oss_image_storage_uploads_object_and_returns_private_reference():
    bucket = FakeOssBucket()
    storage = OssImageStorage(
        bucket="wq-learner",
        endpoint="oss-cn-hangzhou.aliyuncs.com",
        bucket_client=bucket,
    )

    stored = storage.save_upload(
        user_id="user-1",
        content=b"fake-image",
        content_type="image/jpeg",
    )

    assert isinstance(storage, ImageStorage)
    assert stored.object_key.startswith("users/user-1/questions/")
    assert stored.object_key.endswith(".jpg")
    assert stored.image_url == f"oss://wq-learner/{stored.object_key}"
    assert bucket.put_calls == [
        (
            stored.object_key,
            b"fake-image",
            {"Content-Type": "image/jpeg"},
        )
    ]


def test_create_image_storage_uses_oss_storage_when_bucket_is_configured(monkeypatch):
    monkeypatch.setenv("WQ_LEARNER_OSS_BUCKET", "wq-learner")
    monkeypatch.setenv("WQ_LEARNER_OSS_ENDPOINT", "oss-cn-hangzhou.aliyuncs.com")

    storage = create_image_storage()

    assert isinstance(storage, OssImageStorage)


class FakeOssBucket:
    def __init__(self):
        self.put_calls = []

    def put_object(self, key, content, headers=None):
        self.put_calls.append((key, content, headers))
