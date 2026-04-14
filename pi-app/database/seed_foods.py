from database.db import get_connection, run_migrations


def compute_health_score(protein, fiber, sugar, fat):
    score = 50.0
    score += min(fiber * 10, 20)
    score += min(protein * 5, 20)
    score -= min(sugar * 8, 20)
    score -= min(fat * 10, 20)
    return round(max(0.0, min(100.0, score)), 1)


# (name, kcal/100g, protein, fat, sugar, fiber, is_healthy, huskylens_label_id)
FOODS = [
    ("Apple",          52,  0.3, 0.2, 10.0, 2.4, 1, 1),
    ("Banana",         89,  1.1, 0.3, 12.2, 2.6, 1, 2),
    ("Orange",         47,  0.9, 0.1,  9.4, 2.4, 1, 3),
    ("Broccoli",       34,  2.8, 0.4,  1.7, 2.6, 1, 4),
    ("Carrot",         41,  0.9, 0.2,  4.7, 2.8, 1, 5),
    ("Chicken Breast", 165, 31.0, 3.6,  0.0, 0.0, 1, 6),
    ("Egg",            155, 13.0, 11.0,  0.6, 0.0, 1, 7),
    ("White Rice",     130,  2.7, 0.3,  0.0, 0.4, 0, 8),
    ("Whole Wheat Bread", 247, 13.0, 3.5, 5.0, 6.0, 1, 9),
    ("Cheddar Cheese", 402, 25.0, 33.0,  0.1, 0.0, 0, 10),
    ("Chocolate Bar",  535,  4.9, 29.7, 56.9, 3.4, 0, 11),
    ("Potato Chips",   547,  6.5, 37.0,  0.4, 4.4, 0, 12),
    ("Salmon",         208, 20.0, 13.0,  0.0, 0.0, 1, 13),
    ("Greek Yogurt",    59,  10.0,  0.4,  3.2, 0.0, 1, 14),
    ("Avocado",        160,  2.0, 15.0,  0.7, 6.7, 1, 15),
    ("Almonds",        579, 21.0, 50.0,  4.4, 12.5, 1, 16),
    ("Oats",           389, 17.0,  7.0,  1.0, 10.6, 1, 17),
    ("White Bread",    265,  9.0,  3.2,  5.0,  2.7, 0, 18),
    ("Coca-Cola",       37,  0.0,  0.0, 10.6,  0.0, 0, 19),
    ("Butter",         717,  0.9, 81.0,  0.1,  0.0, 0, 20),
    ("Spinach",         23,  2.9,  0.4,  0.4,  2.2, 1, 21),
    ("Lentils",        116,  9.0,  0.4,  1.8,  7.9, 1, 22),
    ("Pizza (plain)",  266, 11.0, 10.0,  3.6,  2.3, 0, 23),
    ("Milk (whole)",    61,  3.2,  3.3,  4.8,  0.0, 1, 24),
    ("Strawberry",      32,  0.7,  0.3,  4.9,  2.0, 1, 25),
]


def seed_foods(conn):
    for (name, kcal, protein, fat, sugar, fiber, healthy, label_id) in FOODS:
        score = compute_health_score(protein, fiber, sugar, fat)
        conn.execute(
            """INSERT OR IGNORE INTO foods
               (name, calories_per_100g, protein_per_100g, fat_per_100g,
                sugar_per_100g, fiber_per_100g, is_healthy, health_score,
                huskylens_label_id)
               VALUES (?,?,?,?,?,?,?,?,?)""",
            (name, kcal, protein, fat, sugar, fiber, healthy, score, label_id),
        )
    conn.commit()


if __name__ == "__main__":
    conn = get_connection()
    run_migrations(conn)
    seed_foods(conn)
    count = conn.execute("SELECT COUNT(*) FROM foods").fetchone()[0]
    print(f"Seeded {count} foods.")
