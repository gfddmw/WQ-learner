from app.image_storage import create_oss_auth


def test_oss_auth_ignores_fc_security_token_when_explicit_access_key_is_configured(monkeypatch):
    fake_oss2 = FakeOss2()
    monkeypatch.setenv("ALIBABA_CLOUD_ACCESS_KEY_ID", "explicit-id")
    monkeypatch.setenv("ALIBABA_CLOUD_ACCESS_KEY_SECRET", "explicit-secret")
    monkeypatch.setenv("ALIBABA_CLOUD_SECURITY_TOKEN", "fc-token")

    auth = create_oss_auth(fake_oss2)

    assert auth == ("Auth", "explicit-id", "explicit-secret")


class FakeOss2:
    @staticmethod
    def Auth(access_key_id, access_key_secret):
        return ("Auth", access_key_id, access_key_secret)

    @staticmethod
    def StsAuth(access_key_id, access_key_secret, security_token):
        return ("StsAuth", access_key_id, access_key_secret, security_token)
