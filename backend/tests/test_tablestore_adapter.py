from app.store import AliyunTableStoreAdapter


def test_tablestore_get_range_uses_configured_secondary_primary_key(monkeypatch):
    fake_tablestore = FakeTableStoreModule()
    captured = {}

    class FakeClient:
        def __init__(self, *args, **kwargs):
            pass

        def get_range(
            self,
            table_name,
            direction,
            inclusive_start_primary_key,
            exclusive_end_primary_key,
            limit,
            max_version,
        ):
            captured["table_name"] = table_name
            captured["inclusive_start_primary_key"] = inclusive_start_primary_key
            captured["exclusive_end_primary_key"] = exclusive_end_primary_key
            return []

    fake_tablestore.OTSClient = FakeClient
    monkeypatch.setitem(__import__("sys").modules, "tablestore", fake_tablestore)
    monkeypatch.setenv("ALIBABA_CLOUD_ACCESS_KEY_ID", "ak-id")
    monkeypatch.setenv("ALIBABA_CLOUD_ACCESS_KEY_SECRET", "ak-secret")

    adapter = AliyunTableStoreAdapter(endpoint="https://example.com", instance_name="wq")

    adapter.get_range("wq_questions", [("user_id", "U-1")])

    assert captured["inclusive_start_primary_key"] == [("user_id", "U-1"), ("id", fake_tablestore.INF_MIN)]
    assert captured["exclusive_end_primary_key"] == [("user_id", "U-1"), ("id", fake_tablestore.INF_MAX)]


class FakeTableStoreModule:
    INF_MIN = "INF_MIN"
    INF_MAX = "INF_MAX"

    class Direction:
        FORWARD = "FORWARD"
