"""
卡路里计算模块
基于 MET (代谢当量) 公式: 卡路里 = 体重(kg) × MET × 时长(小时)
"""

import yaml
from pathlib import Path

CONFIG_PATH = Path(__file__).resolve().parent.parent / "config" / "exercise_config.yaml"


def load_met_values():
    """加载运动 MET 配置"""
    if CONFIG_PATH.exists():
        with open(CONFIG_PATH, "r", encoding="utf-8") as f:
            config = yaml.safe_load(f)
            return config.get("exercise_met_values", {})
    return {
        "push_up": 3.8,
        "sit_up": 3.8,
        "squat": 3.8,
        "jumping_jack": 8.0,
        "default": 4.0,
    }


def calculate_calories(
    weight_kg: float,
    duration_seconds: float,
    exercise_type: str,
    reps: int = 0,
) -> float:
    """
    计算运动消耗的卡路里

    Args:
        weight_kg: 体重(公斤)
        duration_seconds: 运动时长(秒)
        exercise_type: 运动类型 (需与 config 中的 key 匹配)
        reps: 动作次数 (可选, 用于更精确估算)

    Returns:
        估算的卡路里消耗
    """
    mets = load_met_values()
    met = mets.get(exercise_type.lower().replace(" ", "_"), mets.get("default", 4.0))
    # 公式: 卡路里 = 体重 × MET × 时长(小时)
    hours = duration_seconds / 3600
    calories = weight_kg * met * hours
    return round(calories, 1)
