#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
food-train 轻量 HTTP 服务

启动:
  python run_server.py --port 5001 --model nutrition5k_final_model.pth
"""

import io
from pathlib import Path
from typing import Optional

import numpy as np
from fastapi import FastAPI, File, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image
import uvicorn

from predict_food import FoodNutritionPredictor


app = FastAPI(title="food-train API", version="1.0.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

_predictor: Optional[FoodNutritionPredictor] = None
_model_path: Optional[str] = None


def _get_predictor() -> FoodNutritionPredictor:
    global _predictor
    if _predictor is None:
        _predictor = FoodNutritionPredictor(_model_path or "nutrition5k_final_model.pth")
    return _predictor


@app.get("/health")
def health():
    return {"status": "ok", "service": "food-train"}


@app.post("/predict")
async def predict(file: UploadFile = File(...)):
    raw = await file.read()
    if not raw:
        return {"code": 400, "message": "空文件"}
    try:
        img = Image.open(io.BytesIO(raw)).convert("RGB")
        tmp = Path(__file__).resolve().parent / "_tmp_predict_image.jpg"
        img.save(tmp)
        pred, _ = _get_predictor().predict_image(str(tmp))
        try:
            tmp.unlink(missing_ok=True)
        except Exception:
            pass
        if not pred:
            return {"code": 500, "message": "预测失败"}
        clean = {}
        for k, v in pred.items():
            try:
                clean[k] = float(max(0.0, float(v)))
            except (TypeError, ValueError):
                clean[k] = v
        return {"code": 200, "nutrition": clean}
    except Exception as e:
        return {"code": 500, "message": str(e)}


def _resolve_model_path(explicit: str) -> str:
    root = Path(__file__).resolve().parent
    if explicit and explicit.strip():
        p = Path(explicit)
        if not p.is_absolute():
            p = root / p
        if p.is_file():
            return str(p)
    for c in (
        root / "training_outputs" / "nutrition5k_final_model.pth",
        root / "training_outputs" / "best_nutrition_model.pth",
        root / "nutrition5k_final_model.pth",
    ):
        if c.is_file():
            return str(c)
    return str(root / "training_outputs" / "nutrition5k_final_model.pth")


def main():
    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=5001)
    parser.add_argument(
        "--model",
        default="",
        help="模型 .pth 路径；留空则依次尝试 training_outputs/ 与项目根目录",
    )
    args = parser.parse_args()

    global _model_path
    _model_path = _resolve_model_path(args.model)
    uvicorn.run(app, host="0.0.0.0", port=args.port)


if __name__ == "__main__":
    main()

