import importlib
import sys
from types import SimpleNamespace


def test_fc_bootstrap_uses_uvicorn_arguments_supported_by_fc_runtime(monkeypatch):
    calls = []

    def fake_run(*args, **kwargs):
        if "timeout_notify" in kwargs:
            raise TypeError("run() got an unexpected keyword argument 'timeout_notify'")
        calls.append((args, kwargs))

    monkeypatch.setitem(sys.modules, "uvicorn", SimpleNamespace(run=fake_run))
    sys.modules.pop("fc_bootstrap", None)
    fc_bootstrap = importlib.import_module("fc_bootstrap")
    monkeypatch.setenv("PORT", "8080")

    fc_bootstrap.main()

    assert calls == [
        (
            ("app.main:app",),
            {
                "host": "0.0.0.0",
                "port": 8080,
                "timeout_keep_alive": 120,
            },
        )
    ]
