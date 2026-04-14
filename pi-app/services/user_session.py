import sqlite3
from typing import Optional


GUEST_MAC = "00:00:00:00:00:00"


class UserSessionManager:
    def __init__(self, conn: sqlite3.Connection):
        self._conn = conn

    def upsert_user(self, bluetooth_mac: str, profile: dict) -> dict:
        self._conn.execute(
            """INSERT INTO users
               (bluetooth_mac, name, age, weight_kg, height_cm, sex,
                activity_level, daily_calorie_goal)
               VALUES (:mac,:name,:age,:weight_kg,:height_cm,:sex,
                       :activity_level,:daily_calorie_goal)
               ON CONFLICT(bluetooth_mac) DO UPDATE SET
                 name=excluded.name, age=excluded.age,
                 weight_kg=excluded.weight_kg, height_cm=excluded.height_cm,
                 sex=excluded.sex, activity_level=excluded.activity_level,
                 daily_calorie_goal=excluded.daily_calorie_goal""",
            {"mac": bluetooth_mac, **profile},
        )
        self._conn.commit()
        row = self._conn.execute(
            "SELECT * FROM users WHERE bluetooth_mac = ?", (bluetooth_mac,)
        ).fetchone()
        return dict(row)

    def get_user_by_mac(self, bluetooth_mac: str) -> Optional[dict]:
        row = self._conn.execute(
            "SELECT * FROM users WHERE bluetooth_mac = ?", (bluetooth_mac,)
        ).fetchone()
        return dict(row) if row else None

    def log_food(self, user_id: int, food_id: int,
                 weight_g: float, calories: float) -> dict:
        cursor = self._conn.execute(
            """INSERT INTO food_log (user_id, food_id, weight_g, calories)
               VALUES (?,?,?,?)""",
            (user_id, food_id, weight_g, calories),
        )
        self._conn.commit()
        row = self._conn.execute(
            "SELECT * FROM food_log WHERE id = ?", (cursor.lastrowid,)
        ).fetchone()
        return dict(row)

    def get_todays_log(self, user_id: int) -> list[dict]:
        rows = self._conn.execute(
            """SELECT fl.*, f.name as food_name, f.is_healthy, f.health_score,
                      f.protein_per_100g, f.fat_per_100g,
                      f.sugar_per_100g, f.fiber_per_100g
               FROM food_log fl
               JOIN foods f ON fl.food_id = f.id
               WHERE fl.user_id = ?
                 AND date(fl.timestamp) = date('now')
               ORDER BY fl.timestamp DESC""",
            (user_id,),
        ).fetchall()
        return [dict(r) for r in rows]

    def total_calories_today(self, user_id: int) -> float:
        result = self._conn.execute(
            """SELECT COALESCE(SUM(calories), 0)
               FROM food_log
               WHERE user_id = ? AND date(timestamp) = date('now')""",
            (user_id,),
        ).fetchone()[0]
        return float(result)

    def delete_log_entry(self, entry_id: int) -> None:
        self._conn.execute("DELETE FROM food_log WHERE id = ?", (entry_id,))
        self._conn.commit()

    def update_health_snapshot(self, user_id: int, snapshot: dict) -> None:
        """Store today's HealthKit data received from iOS app."""
        self._conn.execute(
            """INSERT OR REPLACE INTO health_snapshots
               (user_id, date, steps, calories_burned, active_minutes, workouts)
               VALUES (:user_id, :date, :steps, :calories_burned,
                       :active_minutes, :workouts)""",
            {"user_id": user_id, **snapshot},
        )
        self._conn.commit()

    def get_health_snapshot(self, user_id: int) -> Optional[dict]:
        row = self._conn.execute(
            """SELECT * FROM health_snapshots
               WHERE user_id = ? AND date = date('now')""",
            (user_id,),
        ).fetchone()
        return dict(row) if row else None
