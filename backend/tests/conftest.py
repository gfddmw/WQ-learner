import os
import sys
import tempfile
from pathlib import Path


BACKEND_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BACKEND_ROOT))

TEST_DB = Path(tempfile.gettempdir()) / "wq_learner_pytest.db"
if TEST_DB.exists():
    TEST_DB.unlink()
os.environ["WQ_LEARNER_DB"] = str(TEST_DB)
