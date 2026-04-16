"""
运动识别公共工具：配置加载、模型查找、单帧检测、运动名称与中文映射。
供 run_camera.py、run_video.py、run_server.py 共用，避免重复代码。
"""

import yaml
from pathlib import Path
from typing import Tuple, Optional, Dict, Any

PROJECT_DIR = Path(__file__).resolve().parent.parent
CONFIG_PATH = PROJECT_DIR / "config" / "exercise_config.yaml"

# 运动英文名 -> 中文显示（与 data.yaml names 一致）
EXERCISE_CN = {
    "high_jumps": "高抬腿",
    "jumping_jacks": "开合跳",
    "lunges": "弓箭步",
    "squats": "深蹲",
}


def load_config() -> Dict[str, Any]:
    """加载 config/exercise_config.yaml"""
    if CONFIG_PATH.exists():
        with open(CONFIG_PATH, "r", encoding="utf-8") as f:
            return yaml.safe_load(f) or {}
    return {}


def norm_exercise_name(name: str) -> str:
    """将检测到的类别名规范化为 config 中的 key（与 data.yaml names 一致）"""
    return str(name or "").lower().replace(" ", "_").replace("-", "_").strip()


def ex_display_name(name: str) -> str:
    """返回运动的中文显示名"""
    return EXERCISE_CN.get(name, name) if name else ""


def find_model(project_dir: Optional[Path] = None) -> Optional[str]:
    """查找训练好的检测模型路径，优先 best.pt，按修改时间取最新。"""
    root = project_dir or PROJECT_DIR
    candidates = [
        root / "runs" / "exercise_train" / "weights" / "best.pt",
        root / "runs" / "exercise_train" / "weights" / "last.pt",
        root / "runs" / "detect" / "exercise_train" / "weights" / "best.pt",
        root / "runs" / "detect" / "exercise_train" / "weights" / "last.pt",
    ]
    for p in candidates:
        if p.exists():
            return str(p)
    runs_dir = root / "runs"
    best_pts = list(runs_dir.rglob("weights/best.pt"))
    if best_pts:
        return str(max(best_pts, key=lambda p: p.stat().st_mtime))
    return None


def predict_image(det_model, frame_bgr, conf_threshold: float = 0.6) -> Tuple[Optional[str], float]:
    """
    单帧检测，返回 (exercise_type, confidence)。
    exercise_type 为 norm 后的名称，未检测到或置信度不足时为 None。
    """
    det_results = det_model(frame_bgr, verbose=False, conf=conf_threshold)
    boxes = det_results[0].boxes if det_results else None
    best_cls = None
    best_conf = 0.0
    class_names = getattr(det_model.model, "names", None) or (
        det_model.names if hasattr(det_model, "names") else {}
    )
    if boxes is not None and len(boxes) > 0:
        for i in range(len(boxes)):
            c = float(boxes.conf[i])
            if c > best_conf:
                best_conf = c
                cls_id = int(boxes.cls[i])
                best_cls = class_names.get(cls_id, f"class_{cls_id}")
    raw = norm_exercise_name(best_cls) if best_cls and best_conf >= conf_threshold else None
    return raw, float(best_conf) if best_conf else 0.0
