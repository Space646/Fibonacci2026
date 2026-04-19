from typing import Protocol, Optional


class UserLike(Protocol):
    weight_kg: float
    height_cm: float
    age: int
    sex: str
    activity_level: str
    daily_calorie_goal: Optional[float]


_ACTIVITY_MULTIPLIERS = {
    "sedentary": 1.2,
    "light": 1.375,
    "moderate": 1.55,
    "active": 1.725,
}


def calculate_calories(calories_per_100g: float, weight_g: float) -> float:
    return round(calories_per_100g * weight_g / 100, 1)


def calculate_bmr(weight_kg: float, height_cm: float, age: int, sex: str) -> float:
    base = 10 * weight_kg + 6.25 * height_cm - 5 * age
    if sex == "male":
        return base + 5
    elif sex == "female":
        return base - 161
    else:
        male_bmr = base + 5
        female_bmr = base - 161
        return (male_bmr + female_bmr) / 2


def calculate_daily_goal(user: UserLike) -> float:
    if user.daily_calorie_goal is not None:
        return user.daily_calorie_goal
    # Guest / unfinished profiles won't have the metrics needed for BMR —
    # fall back to a neutral default until a phone syncs the real profile.
    if user.weight_kg is None or user.height_cm is None or user.age is None:
        return 2000.0
    bmr = calculate_bmr(user.weight_kg, user.height_cm, user.age, user.sex)
    multiplier = _ACTIVITY_MULTIPLIERS.get(user.activity_level, 1.2)
    return round(bmr * multiplier, 1)
