import pytest
from services.calorie_calculator import (
    calculate_calories,
    calculate_bmr,
    calculate_daily_goal,
)
from dataclasses import dataclass
from typing import Optional


@dataclass
class FakeUser:
    weight_kg: float
    height_cm: float
    age: int
    sex: str
    activity_level: str
    daily_calorie_goal: Optional[float] = None


def test_calculate_calories_basic():
    assert calculate_calories(52, 182) == pytest.approx(94.64, rel=1e-2)

def test_calculate_calories_zero_weight():
    assert calculate_calories(52, 0) == 0.0

def test_calculate_bmr_male():
    assert calculate_bmr(70, 175, 30, "male") == pytest.approx(1668.75)

def test_calculate_bmr_female():
    assert calculate_bmr(60, 165, 25, "female") == pytest.approx(1370.25)

def test_calculate_bmr_other_is_average():
    male = calculate_bmr(70, 175, 30, "male")
    female = calculate_bmr(70, 175, 30, "female")
    other = calculate_bmr(70, 175, 30, "other")
    assert other == pytest.approx((male + female) / 2)

def test_daily_goal_uses_manual_override():
    user = FakeUser(70, 175, 30, "male", "moderate", daily_calorie_goal=2000.0)
    assert calculate_daily_goal(user) == 2000.0

def test_daily_goal_calculates_from_bmr():
    user = FakeUser(70, 175, 30, "male", "moderate")
    assert calculate_daily_goal(user) == pytest.approx(2586.6, rel=1e-2)

def test_daily_goal_sedentary_multiplier():
    user = FakeUser(70, 175, 30, "male", "sedentary")
    bmr = calculate_bmr(70, 175, 30, "male")
    assert calculate_daily_goal(user) == pytest.approx(bmr * 1.2, rel=1e-2)
