import pytest

from app.store import CloudDatabaseStore, SQLiteStore, Store, TableStoreStore, create_store


def test_sqlite_store_satisfies_store_protocol(tmp_path):
    store = SQLiteStore(tmp_path / "wq_learner.db")

    assert isinstance(store, Store)


def test_create_store_uses_sqlite_when_cloud_database_url_is_absent(monkeypatch, tmp_path):
    monkeypatch.delenv("WQ_LEARNER_TABLESTORE_INSTANCE", raising=False)
    monkeypatch.delenv("WQ_LEARNER_TABLESTORE_ENDPOINT", raising=False)
    monkeypatch.delenv("WQ_LEARNER_DATABASE_URL", raising=False)
    monkeypatch.setenv("WQ_LEARNER_DB", str(tmp_path / "wq_learner.db"))

    store = create_store()

    assert isinstance(store, SQLiteStore)


def test_create_store_uses_tablestore_when_instance_and_endpoint_are_configured(monkeypatch):
    monkeypatch.setenv("WQ_LEARNER_TABLESTORE_INSTANCE", "wq-learner")
    monkeypatch.setenv("WQ_LEARNER_TABLESTORE_ENDPOINT", "https://wq-learner.cn-hangzhou.ots-internal.aliyuncs.com")
    monkeypatch.setattr("app.store.create_tablestore_adapter", lambda instance_name, endpoint: FakeTableStoreAdapter())

    store = create_store()

    assert isinstance(store, TableStoreStore)


def test_create_store_uses_cloud_database_placeholder_when_url_is_configured(monkeypatch):
    monkeypatch.delenv("WQ_LEARNER_TABLESTORE_INSTANCE", raising=False)
    monkeypatch.delenv("WQ_LEARNER_TABLESTORE_ENDPOINT", raising=False)
    monkeypatch.setenv("WQ_LEARNER_DATABASE_URL", "postgresql://user:pass@example.com/wq")

    store = create_store()

    assert isinstance(store, CloudDatabaseStore)
    with pytest.raises(NotImplementedError, match="云端数据库"):
        store.login("demo@example.com", "secret123")


class FakeTableStoreAdapter:
    def put_row(self, table_name, primary_key, attributes):
        pass

    def get_row(self, table_name, primary_key):
        return None

    def get_range(self, table_name, primary_key_prefix):
        return []
