import sqlite3
from pathlib import Path

SCHEMA_PATH = Path(__file__).parent / "schema.sql"
DEFAULT_DB_PATH = Path(__file__).parent.parent / "data" / "fibonacci.db"


def get_connection(path: str = None) -> sqlite3.Connection:
    """Return a sqlite3 connection. Pass ':memory:' for tests.

    check_same_thread=False is required because the connection is created on
    the Qt main thread but BLE callbacks (_on_profile_received,
    _on_health_received) fire on the asyncio event-loop thread and call into
    UserSessionManager which writes to this connection.  This is safe: sqlite3
    uses its default serialized threading mode, only one BLE callback runs at
    a time (single asyncio loop / single thread), and Qt main-thread access is
    read-only via synchronous pyqtProperty getters that do not overlap with BLE
    writes.
    """
    db_path = path or str(DEFAULT_DB_PATH)
    if db_path != ":memory:":
        Path(db_path).parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(db_path, check_same_thread=False)
    conn.execute("PRAGMA foreign_keys = ON")
    conn.row_factory = sqlite3.Row
    return conn


def run_migrations(conn: sqlite3.Connection) -> None:
    """Create tables from schema.sql if they don't exist."""
    schema = SCHEMA_PATH.read_text()
    conn.executescript(schema)
    conn.commit()
