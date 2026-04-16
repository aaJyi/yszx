"""
YOLO 运动识别模型训练脚本

使用下载的数据集训练 YOLOv8 模型识别各类运动
训练完成后模型将保存在 runs/detect/train/weights/best.pt
"""

import os
import sys
from pathlib import Path

PROJECT_DIR = Path(__file__).resolve().parent.parent
DATASET_DIR = PROJECT_DIR / "dataset"


def find_dataset_yaml():
    """查找数据集配置文件 data.yaml，优先使用合并后的 dataset/combined/"""
    # 优先合并数据集 (含深蹲补充)
    combined = DATASET_DIR / "combined" / "data.yaml"
    if combined.exists():
        return str(combined)
    for yaml_path in DATASET_DIR.rglob("data.yaml"):
        return str(yaml_path)
    data_yaml = DATASET_DIR / "data.yaml"
    if data_yaml.exists():
        return str(data_yaml)
    return None


def train(
    data_yaml: str,
    epochs: int = 61,
    batch: int = 16,
    imgsz: int = 640,
    model: str = "yolov8n.pt",
    project: str = "runs",
    name: str = "exercise_train",
    device: str = "0",  # "0"=GPU0, "0,1"=多卡, "cpu"=CPU
):
    """训练 YOLO 模型"""
    from ultralytics import YOLO
    from ultralytics.utils import SETTINGS

    # 默认启用 TensorBoard，关闭: set TENSORBOARD=0 (避免 Permission denied 时可关闭)
    if os.environ.get("TENSORBOARD", "1").lower() in ("0", "false", "no"):
        SETTINGS.update({"tensorboard": False})
        print("TensorBoard 已关闭")
    else:
        SETTINGS.update({"tensorboard": True})
        print(f"TensorBoard 已启用 | 监控: tensorboard --logdir runs --port 6006")

    print(f"使用数据集: {data_yaml}")
    print(f"预训练模型: {model}")
    print(f"设备: {device}")
    print(f"训练轮数: {epochs}, 批次大小: {batch}")
    print("-" * 50)

    # 加载预训练模型
    yolo = YOLO(model)
    # 训练
    results = yolo.train(
        data=data_yaml,
        epochs=epochs,
        batch=batch,
        imgsz=imgsz,
        device=device,
        project=str(PROJECT_DIR / project),
        name=name,
        exist_ok=True,
        pretrained=True,
        verbose=True,
    )
    return results


if __name__ == "__main__":
    os.chdir(PROJECT_DIR)

    data_yaml = find_dataset_yaml()
    if not data_yaml:
        print("错误: 未找到 data.yaml!")
        print("请先运行 python scripts/download_and_merge_datasets.py 下载并合并数据集")
        print("或 python scripts/download_dataset.py 下载基础数据集")
        print(f"预期路径: {DATASET_DIR}/combined/data.yaml 或 {DATASET_DIR}/**/data.yaml")
        sys.exit(1)

    # 从环境变量或命令行读取参数
    epochs = int(os.environ.get("EPOCHS", "100"))
    batch = int(os.environ.get("BATCH", "16"))
    import torch
    device = os.environ.get("DEVICE", "0")
    if device == "0" and not torch.cuda.is_available():
        device = "cpu"
        print("未检测到 CUDA，使用 CPU 训练")
    name = os.environ.get("TRAIN_NAME", "exercise_train")  # 不同名称保留旧模型

    model = os.environ.get("MODEL", "yolov8s.pt")
    train(
        data_yaml=data_yaml,
        epochs=epochs,
        batch=batch,
        device=device,
        name=name,
        model=model,
    )
    print(f"\n训练完成! 最佳模型: runs/{name}/weights/best.pt")
