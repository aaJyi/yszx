"""
下载并合并运动识别数据集

1. 下载原始 exercise-ekbld-kjve3 (4类: 高抬腿/开合跳/弓箭步/深蹲)
2. 下载深蹲补充数据集 squats-ffjsf-hdckg (workspace: aajyis-workspace)
3. 合并到 dataset/combined/ 统一训练

使用方法:
  set ROBOFLOW_API_KEY=你的密钥
  python scripts/download_and_merge_datasets.py
"""

import os
import shutil
from pathlib import Path

ROBOFLOW_API_KEY = os.environ.get("ROBOFLOW_API_KEY", os.environ.get("ROBoflow_API_KEY", "YOUR_ROBOFLOW_API_KEY"))
PROJECT_DIR = Path(__file__).resolve().parent.parent
DATASET_DIR = PROJECT_DIR / "dataset"

# 原始 4 类数据集
EXERCISE_WORKSPACE = "aajyis-workspace"
EXERCISE_PROJECT = "exercise-ekbld-kjve3"

# 深蹲补充数据集 (ID: squats-ffjsf-hdckg, 版本: squate)
SQUATS_WORKSPACE = "aajyis-workspace"
SQUATS_PROJECT = "squats-ffjsf-hdckg"
SQUATS_VERSION = "squate"  # 版本名

# 4 类统一 schema: high_jumps=0, jumping_jacks=1, lunges=2, squats=3
CLASS_NAMES = ["high_jumps", "jumping_jacks", "lunges", "squats"]
HIGH_JUMPS_CLASS_ID = 0
SQUATS_CLASS_ID = 3


def get_roboflow():
    if ROBOFLOW_API_KEY == "YOUR_ROBOFLOW_API_KEY":
        print("=" * 60)
        print("错误: 请先设置 ROBOFLOW_API_KEY 环境变量")
        print("  set ROBOFLOW_API_KEY=你的密钥")
        print("=" * 60)
        return None
    try:
        from roboflow import Roboflow
        return Roboflow(api_key=ROBOFLOW_API_KEY)
    except ImportError:
        print("请先安装: pip install roboflow")
        return None


def find_version_by_name(project, name: str):
    """按版本名或版本号查找"""
    if name.isdigit():
        return int(name)
    try:
        versions = project.get_version_information()
        for v in versions:
            vid = v.get("id") or v.get("name", "")
            vname = str(v.get("name", vid)).lower()
            if vname == name.lower() or str(vid) == name:
                return int(vid) if isinstance(vid, (int, float)) else vid
        if versions:
            return int(versions[0].get("id", 1))
    except Exception:
        pass
    return 1


def download_dataset(rf, workspace: str, project_name: str, version_hint: str, dest: Path):
    """下载单个数据集到 dest"""
    from roboflow.adapters.rfapi import RoboflowError

    try:
        proj = rf.workspace(workspace).project(project_name)
    except RoboflowError as e:
        if "404" in str(e) or "does not exist" in str(e):
            print(f"尝试默认工作区...")
            proj = rf.workspace().project(project_name)
        else:
            raise

    ver_id = find_version_by_name(proj, version_hint)
    print(f"  使用版本: {ver_id}")
    dataset = proj.version(int(ver_id) if isinstance(ver_id, (int, float)) else ver_id).download(
        "yolov8", location=str(dest), overwrite=True
    )
    return dest


def remap_labels(labels_dir: Path, out_dir: Path, old_class_id: int, new_class_id: int, prefix: str = ""):
    """将标签中的 old_class_id 映射为 new_class_id，复制到 out_dir"""
    out_dir.mkdir(parents=True, exist_ok=True)
    if not labels_dir.exists():
        return 0
    count = 0
    for f in labels_dir.glob("*.txt"):
        lines = []
        with open(f, "r", encoding="utf-8") as fp:
            for line in fp:
                parts = line.strip().split()
                if len(parts) >= 5:
                    cid = int(parts[0])
                    if cid == old_class_id:
                        parts[0] = str(new_class_id)
                    lines.append(" ".join(parts) + "\n")
        out_name = f"{prefix}{f.name}" if prefix else f.name
        out_path = out_dir / out_name
        with open(out_path, "w", encoding="utf-8") as fp:
            fp.writelines(lines)
        count += 1
    return count


def copy_dataset(src_base: Path, dst_images: Path, dst_labels: Path, label_remap: dict = None, prefix: str = ""):
    """复制图像和标签，可选 remap {old_id: new_id}"""
    src_images = src_base / "images" if (src_base / "images").exists() else src_base
    src_labels = src_base / "labels" if (src_base / "labels").exists() else src_base.parent / "labels"
    # Roboflow 下载结构可能是 dataset/train/images, dataset/train/labels
    for sub in ["train", "valid", "val", "test"]:
        si = src_base / sub / "images"
        sl = src_base / sub / "labels"
        if si.exists():
            src_images = si
            src_labels = sl
            break
    if not src_images.exists():
        si = src_base / "train" / "images"
        if si.exists():
            src_images, src_labels = si, src_base / "train" / "labels"

    dst_images.mkdir(parents=True, exist_ok=True)
    dst_labels.mkdir(parents=True, exist_ok=True)
    img_count, label_count = 0, 0

    for img in src_images.glob("*"):
        if img.suffix.lower() in (".jpg", ".jpeg", ".png", ".bmp"):
            n = f"{prefix}{img.name}" if prefix else img.name
            shutil.copy2(img, dst_images / n)
            img_count += 1
            # 对应标签
            label_name = img.stem + ".txt"
            label_path = src_labels / label_name
            if label_path.exists():
                with open(label_path, "r", encoding="utf-8") as fp:
                    lines = fp.readlines()
                if label_remap:
                    new_lines = []
                    for line in lines:
                        parts = line.strip().split()
                        if len(parts) >= 5:
                            cid = int(parts[0])
                            parts[0] = str(label_remap.get(cid, cid))
                        new_lines.append(" ".join(parts) + "\n")
                    lines = new_lines
                out_name = f"{prefix}{label_name}" if prefix else label_name
                with open(dst_labels / out_name, "w", encoding="utf-8") as fp:
                    fp.writelines(lines)
                label_count += 1
    return img_count, label_count


def _get_split_dirs(base: Path, split: str):
    """获取 split 对应的 images/labels 路径 (兼容 train/valid/val/test)"""
    for s in [split, "valid" if split == "val" else "val" if split == "valid" else split]:
        img = base / s / "images"
        lbl = base / s / "labels"
        if img.exists():
            return img, lbl
    return base / "train" / "images", base / "train" / "labels"


def merge_yolo_dataset(base_dir: Path, extra_dir: Path, out_dir: Path, extra_class_remap: dict, prefix: str = "sq_", extra_train_only: bool = False):
    """合并 base 和 extra，extra 的类别按 extra_class_remap 映射. extra_train_only=True 时仅将 extra 加入 train"""
    for split in ["train", "valid", "test"]:
        base_img, base_lbl = _get_split_dirs(base_dir, split)
        if not base_img.exists() and split == "valid":
            base_img, base_lbl = _get_split_dirs(base_dir, "val")
        ext_img, ext_lbl = _get_split_dirs(extra_dir, split)
        if not ext_img.exists() and split == "valid":
            ext_img, ext_lbl = _get_split_dirs(extra_dir, "val")
        if extra_train_only and split != "train":
            ext_img = Path("/nonexistent")

        dst_img = out_dir / split / "images"
        dst_lbl = out_dir / split / "labels"
        dst_img.mkdir(parents=True, exist_ok=True)
        dst_lbl.mkdir(parents=True, exist_ok=True)

        # 仅当 base 与 out 不同目录时才复制 base，避免 SameFileError
        if base_img.exists() and Path(base_img).resolve() != Path(dst_img).resolve():
            for f in base_img.glob("*"):
                if f.suffix.lower() in (".jpg", ".jpeg", ".png", ".bmp"):
                    shutil.copy2(f, dst_img / f.name)
            if base_lbl.exists():
                for f in base_lbl.glob("*.txt"):
                    shutil.copy2(f, dst_lbl / f.name)

        if ext_img.exists() and ext_img.is_dir():
            for f in ext_img.glob("*"):
                if f.suffix.lower() in (".jpg", ".jpeg", ".png", ".bmp"):
                    n = f"{prefix}{f.name}"
                    shutil.copy2(f, dst_img / n)
                    lbl = ext_lbl / (f.stem + ".txt")
                    if lbl.exists():
                        with open(lbl, "r", encoding="utf-8") as fp:
                            lines = fp.readlines()
                        new_lines = []
                        for line in lines:
                            parts = line.strip().split()
                            if len(parts) >= 5:
                                cid = int(parts[0])
                                parts[0] = str(extra_class_remap.get(cid, cid))
                            new_lines.append(" ".join(parts) + "\n")
                        with open(dst_lbl / (Path(n).stem + ".txt"), "w", encoding="utf-8") as fp:
                            fp.writelines(new_lines)


def merge_from_local_only(combined_dir: Path):
    """无 Roboflow 时，仅用本地已有数据合并（base + squats + high_jumps，不含 jumping_jacks）"""
    base_dir = DATASET_DIR
    squats_dir = DATASET_DIR / "squats_extra"
    if not (base_dir / "train" / "images").exists() and not (base_dir / "train").exists():
        print("本地无有效 base 数据，请设置 ROBOFLOW_API_KEY 下载")
        return False
    if not (base_dir / "train").exists():
        base_dir = DATASET_DIR / "exercise_base"
    if not (base_dir / "train").exists():
        base_dir = DATASET_DIR
    combined_dir.mkdir(parents=True, exist_ok=True)
    extra_remap = {0: SQUATS_CLASS_ID, 1: SQUATS_CLASS_ID, 2: SQUATS_CLASS_ID, 3: SQUATS_CLASS_ID}
    if squats_dir.exists() and ((squats_dir / "train").exists() or (squats_dir / "train" / "images").exists()):
        for sub in squats_dir.iterdir():
            if sub.is_dir() and ((sub / "train").exists() or (sub / "images").exists()):
                squats_dir = sub
                break
        merge_yolo_dataset(base_dir, squats_dir, combined_dir, extra_remap, prefix="squats_")
        print("  已合并深蹲数据，类别已映射为 squats(3)")
    else:
        for split in ["train", "valid", "test"]:
            src_img, src_lbl = _get_split_dirs(base_dir, split)
            if not src_img.exists() and split == "valid":
                src_img, src_lbl = _get_split_dirs(base_dir, "val")
            if src_img.exists():
                dst_img = combined_dir / split / "images"
                dst_lbl = combined_dir / split / "labels"
                dst_img.mkdir(parents=True, exist_ok=True)
                dst_lbl.mkdir(parents=True, exist_ok=True)
                for f in src_img.glob("*"):
                    if f.suffix.lower() in (".jpg", ".jpeg", ".png", ".bmp"):
                        shutil.copy2(f, dst_img / f.name)
                if src_lbl.exists():
                    for f in src_lbl.glob("*.txt"):
                        shutil.copy2(f, dst_lbl / f.name)
    high_jumps_dir = DATASET_DIR / "high_jumps_extra"
    if (high_jumps_dir / "train" / "images").exists():
        hj_remap = {0: HIGH_JUMPS_CLASS_ID, 1: HIGH_JUMPS_CLASS_ID, 2: HIGH_JUMPS_CLASS_ID, 3: HIGH_JUMPS_CLASS_ID}
        merge_yolo_dataset(combined_dir, high_jumps_dir, combined_dir, hj_remap, prefix="hj_", extra_train_only=True)
        print("  已合并高抬腿数据 (Qualcomm)，类别已映射为 high_jumps(0)")
    val_path = combined_dir / "valid"
    if not val_path.exists() and (combined_dir / "val").exists():
        val_path = combined_dir / "val"
    sources = []
    if squats_dir.exists() and ((squats_dir / "train").exists() or (squats_dir / "images").exists()):
        sources.append("深蹲 squats-ffjsf-hdckg")
    if (high_jumps_dir / "train" / "images").exists():
        sources.append("高抬腿 Qualcomm")
    src_txt = f"含 {', '.join(sources)}" if sources else "基础数据集"
    yaml_path = combined_dir / "data.yaml"
    with open(yaml_path, "w", encoding="utf-8") as f:
        f.write(f"""# 合并后的运动识别数据集 ({src_txt})
names: {CLASS_NAMES}
nc: {len(CLASS_NAMES)}

train: train/images
val: valid/images
test: test/images
""")
    print(f"  data.yaml 已生成: {yaml_path}")
    print("\n完成! 合并数据集路径: dataset/combined/")
    return True


def main():
    rf = get_roboflow()
    base_dir = DATASET_DIR / "exercise_base"
    squats_dir = DATASET_DIR / "squats_extra"
    combined_dir = DATASET_DIR / "combined"

    if not rf:
        print("未设置 ROBOFLOW_API_KEY，尝试仅用本地数据合并...")
        ok = merge_from_local_only(combined_dir)
        if ok:
            print("\n运行训练: python scripts/train.py")
        return ok

    print("=" * 60)
    print("1. 下载原始运动数据集 exercise-ekbld-kjve3")
    print("=" * 60)
    base_dir.mkdir(parents=True, exist_ok=True)
    use_existing = False
    try:
        download_dataset(rf, EXERCISE_WORKSPACE, EXERCISE_PROJECT, "1", base_dir)
    except Exception as e:
        print(f"下载失败: {e}")
        if (DATASET_DIR / "train").exists() and (DATASET_DIR / "data.yaml").exists():
            print("使用已有 dataset/ 作为 base...")
            base_dir = DATASET_DIR
            use_existing = True
        else:
            return False

    if not use_existing:
        for sub in base_dir.iterdir():
            if sub.is_dir() and (sub / "data.yaml").exists():
                base_dir = sub
                break
        for sub in base_dir.iterdir():
            if sub.is_dir() and (sub / "train").exists():
                base_dir = sub
                break
    if not (base_dir / "data.yaml").exists() and (base_dir / "train").exists():
        pass
    elif (DATASET_DIR / "data.yaml").exists():
        base_dir = DATASET_DIR

    print("\n" + "=" * 60)
    print("2. 下载深蹲补充数据集 squats-ffjsf-hdckg (版本 squate)")
    print("=" * 60)
    squats_dir.mkdir(parents=True, exist_ok=True)
    try:
        download_dataset(rf, SQUATS_WORKSPACE, SQUATS_PROJECT, SQUATS_VERSION, squats_dir)
    except Exception as e:
        print(f"下载失败: {e}")
        print("将仅使用原始数据集，不合并深蹲数据。")
        squats_dir = None

    if squats_dir and squats_dir.exists():
        for sub in squats_dir.iterdir():
            if sub.is_dir() and ((sub / "data.yaml").exists() or (sub / "train").exists() or (sub / "train/images").exists()):
                squats_dir = sub
                break

    print("\n" + "=" * 60)
    print("3. 合并数据集到 dataset/combined/")
    print("=" * 60)
    combined_dir.mkdir(parents=True, exist_ok=True)
    # 深蹲数据集通常只有 1 类 (id=0)，映射到 squats=3
    extra_remap = {0: SQUATS_CLASS_ID, 1: SQUATS_CLASS_ID, 2: SQUATS_CLASS_ID, 3: SQUATS_CLASS_ID}

    if squats_dir and ((squats_dir / "train").exists() or (squats_dir / "images").exists()):
        merge_yolo_dataset(base_dir, squats_dir, combined_dir, extra_remap, prefix="squats_")
        print("  已合并深蹲数据，类别已映射为 squats(3)")
    else:
        for split in ["train", "valid", "test"]:
            src_img, src_lbl = _get_split_dirs(base_dir, split)
            if not src_img.exists() and split == "valid":
                src_img, src_lbl = _get_split_dirs(base_dir, "val")
            if src_img.exists():
                dst_img = combined_dir / split / "images"
                dst_lbl = combined_dir / split / "labels"
                dst_img.mkdir(parents=True, exist_ok=True)
                dst_lbl.mkdir(parents=True, exist_ok=True)
                for f in src_img.glob("*"):
                    if f.suffix.lower() in (".jpg", ".jpeg", ".png", ".bmp"):
                        shutil.copy2(f, dst_img / f.name)
                if src_lbl.exists():
                    for f in src_lbl.glob("*.txt"):
                        shutil.copy2(f, dst_lbl / f.name)

    high_jumps_dir = DATASET_DIR / "high_jumps_extra"
    if (high_jumps_dir / "train" / "images").exists():
        hj_remap = {0: HIGH_JUMPS_CLASS_ID, 1: HIGH_JUMPS_CLASS_ID, 2: HIGH_JUMPS_CLASS_ID, 3: HIGH_JUMPS_CLASS_ID}
        merge_yolo_dataset(combined_dir, high_jumps_dir, combined_dir, hj_remap, prefix="hj_", extra_train_only=True)
        print("  已合并高抬腿数据 (Qualcomm)，类别已映射为 high_jumps(0)")

    # 统一用 valid
    val_path = combined_dir / "valid"
    if not val_path.exists() and (combined_dir / "val").exists():
        val_path = combined_dir / "val"

    yaml_path = combined_dir / "data.yaml"
    sources = []
    if squats_dir and ((squats_dir / "train").exists() or (squats_dir / "images").exists()):
        sources.append("深蹲 squats-ffjsf-hdckg")
    if (high_jumps_dir / "train" / "images").exists():
        sources.append("高抬腿 Qualcomm")
    src_txt = f"含 {', '.join(sources)}" if sources else "基础数据集"
    with open(yaml_path, "w", encoding="utf-8") as f:
        f.write(f"""# 合并后的运动识别数据集 ({src_txt})
names: {CLASS_NAMES}
nc: {len(CLASS_NAMES)}

train: train/images
val: valid/images
test: test/images
""")
    print(f"  data.yaml 已生成: {yaml_path}")

    print("\n" + "=" * 60)
    print("完成! 合并数据集路径: dataset/combined/")
    print("运行训练: python scripts/train.py")
    print("  (训练脚本已配置为使用 yolov8s 并优先 dataset/combined/)")
    print("=" * 60)
    return True


if __name__ == "__main__":
    success = main()
    exit(0 if success else 1)
