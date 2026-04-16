"""
食物营养预测脚本（与 train_nutrition_model.py 结构一致，不依赖 seaborn 等训练可视化库）
用法:
  python predict_nutrition.py --image path/to/photo.jpg
  python predict_nutrition.py --image food_1.jpg --checkpoint training_outputs/nutrition5k_final_model.pth
"""

from __future__ import annotations

import argparse
import json
import os
import sys

import torch
import torch.nn as nn
import torchvision.transforms as transforms
from PIL import Image
from torchvision import models

# ---------------------------------------------------------------------------
# 与 train_nutrition_model.NutritionModel 保持同步（推理时 backbone 不加载预训练）
# ---------------------------------------------------------------------------


class NutritionModel(nn.Module):
    """食物营养分析模型（与训练脚本一致）"""

    def __init__(self, num_nutrients=7, backbone_name="resnet50", pretrained=False, dropout_rate=0.5):
        super().__init__()

        def build_backbone(name, use_pretrained):
            if name == "resnet18":
                weights = models.ResNet18_Weights.DEFAULT if use_pretrained else None
                return models.resnet18(weights=weights)
            if name == "resnet34":
                weights = models.ResNet34_Weights.DEFAULT if use_pretrained else None
                return models.resnet34(weights=weights)
            if name == "resnet50":
                weights = models.ResNet50_Weights.DEFAULT if use_pretrained else None
                return models.resnet50(weights=weights)
            if name == "efficientnet_b0":
                weights = models.EfficientNet_B0_Weights.DEFAULT if use_pretrained else None
                return models.efficientnet_b0(weights=weights)
            raise ValueError(f"不支持的骨干网络: {name}")

        try:
            backbone = build_backbone(backbone_name, pretrained)
        except RuntimeError as e:
            if pretrained and "invalid hash value" in str(e).lower():
                backbone = build_backbone(backbone_name, False)
            else:
                raise

        if backbone_name in ("resnet18", "resnet34", "resnet50"):
            num_features = backbone.fc.in_features
            self.backbone = nn.Sequential(*list(backbone.children())[:-2])
        elif backbone_name == "efficientnet_b0":
            num_features = backbone.classifier[1].in_features
            self.backbone = backbone.features
        else:
            raise ValueError(f"不支持的骨干网络: {backbone_name}")

        self.global_pool = nn.AdaptiveAvgPool2d((1, 1))
        self.regressor = nn.Sequential(
            nn.Flatten(),
            nn.Linear(num_features, 512),
            nn.BatchNorm1d(512),
            nn.ReLU(inplace=True),
            nn.Dropout(dropout_rate),
            nn.Linear(512, 256),
            nn.BatchNorm1d(256),
            nn.ReLU(inplace=True),
            nn.Dropout(dropout_rate * 0.8),
            nn.Linear(256, 128),
            nn.BatchNorm1d(128),
            nn.ReLU(inplace=True),
            nn.Dropout(dropout_rate * 0.6),
            nn.Linear(128, num_nutrients),
        )

    def forward(self, x):
        features = self.backbone(x)
        features = self.global_pool(features)
        return self.regressor(features)


NUTRIENT_LABELS_CN = {
    "energy_kcal": "热量 (kcal)",
    "protein_g": "蛋白质 (g)",
    "fat_g": "脂肪 (g)",
    "carbs_g": "碳水 (g)",
    "fiber_g": "膳食纤维 (g)",
    "sugar_g": "糖 (g)",
    "sodium_mg": "钠 (mg)",
}


def _default_checkpoint_paths(artifact_dir: str) -> list[str]:
    return [
        os.path.join(artifact_dir, "nutrition5k_final_model.pth"),
        os.path.join(artifact_dir, "best_nutrition_model.pth"),
    ]


def _torch_load(path: str, device: torch.device) -> dict:
    try:
        return torch.load(path, map_location=device, weights_only=False)
    except TypeError:
        return torch.load(path, map_location=device)


def build_model_from_checkpoint(ckpt: dict, device: torch.device) -> tuple[NutritionModel, list[str], dict]:
    config = ckpt.get("config") or {}
    backbone = config.get("backbone", "resnet50")
    dropout = float(config.get("dropout_rate", 0.5))
    nutrient_names = ckpt.get("nutrient_names")
    if not nutrient_names:
        nutrient_names = [
            "energy_kcal",
            "protein_g",
            "fat_g",
            "carbs_g",
            "fiber_g",
            "sugar_g",
            "sodium_mg",
        ]
    num_nutrients = len(nutrient_names)

    model = NutritionModel(
        num_nutrients=num_nutrients,
        backbone_name=backbone,
        pretrained=False,
        dropout_rate=dropout,
    ).to(device)
    model.load_state_dict(ckpt["model_state_dict"], strict=True)
    model.eval()
    return model, nutrient_names, config


def build_transform(img_size: tuple[int, int], mean: list[float], std: list[float]):
    return transforms.Compose(
        [
            transforms.Resize(img_size),
            transforms.ToTensor(),
            transforms.Normalize(mean=mean, std=std),
        ]
    )


def predict_one(
    image_path: str,
    checkpoint_path: str,
    device: torch.device,
) -> dict:
    if not os.path.isfile(image_path):
        raise FileNotFoundError(f"图片不存在: {image_path}")
    if not os.path.isfile(checkpoint_path):
        raise FileNotFoundError(
            f"模型文件不存在: {checkpoint_path}\n请先完成训练，或使用 --checkpoint 指定 .pth"
        )

    ckpt = _torch_load(checkpoint_path, device)
    model, nutrient_names, _ = build_model_from_checkpoint(ckpt, device)

    input_size = ckpt.get("input_size") or (224, 224)
    mean = ckpt.get("mean") or [0.485, 0.456, 0.406]
    std = ckpt.get("std") or [0.229, 0.224, 0.225]
    transform = build_transform(input_size, mean, std)

    img = Image.open(image_path).convert("RGB")
    x = transform(img).unsqueeze(0).to(device)

    with torch.no_grad():
        out = model(x).squeeze(0).cpu().numpy()

    result: dict[str, float] = {}
    for i, name in enumerate(nutrient_names):
        result[name] = float(max(0.0, out[i]))

    return {
        "image": os.path.abspath(image_path),
        "checkpoint": os.path.abspath(checkpoint_path),
        "nutrients": result,
        "nutrient_order": nutrient_names,
    }


def main():
    parser = argparse.ArgumentParser(description="食物营养预测（Nutrition5k 模型）")
    parser.add_argument("--image", "-i", type=str, required=True, help="输入图片路径")
    parser.add_argument(
        "--artifact-dir",
        type=str,
        default="training_outputs",
        help="训练输出目录（默认与 train 一致）",
    )
    parser.add_argument(
        "--checkpoint",
        "-c",
        type=str,
        default=None,
        help="指定 .pth；不指定则依次尝试 nutrition5k_final_model.pth / best_nutrition_model.pth",
    )
    parser.add_argument("--json", action="store_true", help="仅输出 JSON")
    args = parser.parse_args()

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

    if args.checkpoint:
        ckpt_path = args.checkpoint
    else:
        ckpt_path = ""
        for p in _default_checkpoint_paths(args.artifact_dir):
            if os.path.isfile(p):
                ckpt_path = p
                break
        if not ckpt_path:
            print(
                "未找到模型文件。请先运行训练，或指定 --checkpoint。\n"
                "  期望路径之一:\n"
                + "\n".join(f"    - {p}" for p in _default_checkpoint_paths(args.artifact_dir)),
                file=sys.stderr,
            )
            sys.exit(1)

    try:
        out = predict_one(args.image, ckpt_path, device)
    except Exception as e:
        print(f"预测失败: {e}", file=sys.stderr)
        sys.exit(1)

    if args.json:
        print(json.dumps(out, ensure_ascii=False, indent=2))
        return

    print(f"图片: {out['image']}")
    print(f"模型: {out['checkpoint']}")
    print(f"设备: {device}")
    print("-" * 40)
    for name in out["nutrient_order"]:
        val = out["nutrients"][name]
        cn = NUTRIENT_LABELS_CN.get(name, name)
        print(f"  {cn}: {val:.2f}")
    print("-" * 40)


if __name__ == "__main__":
    main()
