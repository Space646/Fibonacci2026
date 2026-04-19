CREATE TABLE IF NOT EXISTS foods (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    name                TEXT NOT NULL,
    calories_per_100g   REAL NOT NULL,
    protein_per_100g    REAL DEFAULT 0,
    fat_per_100g        REAL DEFAULT 0,
    sugar_per_100g      REAL DEFAULT 0,
    fiber_per_100g      REAL DEFAULT 0,
    is_healthy          INTEGER DEFAULT 0,
    huskylens_label_id  INTEGER UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    bluetooth_mac       TEXT UNIQUE NOT NULL,
    name                TEXT,
    age                 INTEGER,
    weight_kg           REAL,
    height_cm           REAL,
    sex                 TEXT DEFAULT 'other',
    activity_level      TEXT DEFAULT 'sedentary',
    daily_calorie_goal  REAL
);

CREATE TABLE IF NOT EXISTS food_log (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    food_id     INTEGER NOT NULL REFERENCES foods(id) ON DELETE CASCADE,
    weight_g    REAL NOT NULL,
    calories    REAL NOT NULL,
    timestamp   TEXT DEFAULT (strftime('%Y-%m-%dT%H:%M:%S', 'now'))
);

CREATE TABLE IF NOT EXISTS health_snapshots (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id         INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date            TEXT NOT NULL,
    steps           INTEGER DEFAULT 0,
    calories_burned REAL DEFAULT 0,
    active_minutes  INTEGER DEFAULT 0,
    workouts        INTEGER DEFAULT 0,
    UNIQUE(user_id, date)
);
