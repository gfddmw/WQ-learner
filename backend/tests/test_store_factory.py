import pytest

from app.store import CloudDatabaseStore, SQLiteStore, Store, create_store


def test_sqlite_store_satisfies_store_protocol(tmp_path):
    store = SQLiteStore(tmp_path / "wq_learner.db")

    assert isinstance(store, Store)


def test_create_store_uses_sqlite_when_cloud_database_url_is_absent(monkeypatch, tmp_path):
    monkeypatch.delenv("WQ_LEARNER_DATABASE_URL", raising=False)
    monkeypatch.setenv("WQ_LEARNER_DB", str(tmp_path / "wq_learner.db"))

    store = create_store()

    assert isinstance(store, SQLiteStore)


def test_create_store_uses_cloud_database_placeholder_when_url_is_configured(monkeypatch):
    monkeypatch.setenv("WQ_LEARNER_DATABASE_URL", "postgresql://user:pass@example.com/wq")

    store = create_store()

    assert isinstance(store, CloudDatabaseStore)
    with pytest.raises(NotImplementedError, match="云端数据库"):
        store.login("demo@example.com", "secret123")
