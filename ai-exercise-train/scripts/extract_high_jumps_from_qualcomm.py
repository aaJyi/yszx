"""
从 HuggingFace Qualcomm 运动数据集提取高抬腿 (high knees) 并转为 YOLO 格式

数据集: https://huggingface.co/datasets/Voxel51/qualcomm-exercise-video-dataset-benchmark
- 74 个健身视频，含时序标注 (feedback_events)
- 根据 "high knees" 相关反馈定位高抬腿片段，抽帧并用人物检测生成 bbox

使用方法:
  pip install fiftyone
  python scripts/extract_high_jumps_from_qualcomm.py
"""

import re
from pathlib import Path

import cv2

PROJECT_DIR = Path(__file__).resolve().parent.parent
OUT_DIR = PROJECT_DIR / "dataset" / "high_jumps_extra"
HIGH_JUMPS_CLASS_ID = 0  # high_jumps 在 4 类中的 id

# 匹配高抬腿的反馈文本 (不区分大小写)
HIGH_KNEE_PATTERNS = [
    r"high\s*knee",
    r"high\s*knees",
    r"highknee",
    r"love the high knee",
]


def is_high_knees_label(label: str) -> bool:
    if not label:
        return False
    label_lower = label.lower()
    for pat in HIGH_KNEE_PATTERNS:
        if re.search(pat, label_lower):
            return True
    return False


def get_person_bbox(frame, model):
    """用 YOLO 检测人物，返回最大 bbox (归一化 cx,cy,w,h)"""
    results = model(frame, verbose=False)
    boxes = results[0].boxes if results and len(results) > 0 else None
    if boxes is None or len(boxes) == 0:
        return None
    # COCO person class = 0
    best = None
    best_area = 0
    h, w = frame.shape[:2]
    for i in range(len(boxes)):
        cls_id = int(boxes.cls[i])
        if cls_id != 0:
            continue
        xyxy = boxes.xyxy[i].cpu().numpy()
        area = (xyxy[2] - xyxy[0]) * (xyxy[3] - xyxy[1])
        if area > best_area:
            best_area = area
            x1, y1, x2, y2 = xyxy
            cx = (x1 + x2) / 2 / w
            cy = (y1 + y2) / 2 / h
            bw = (x2 - x1) / w
            bh = (y2 - y1) / h
            best = (cx, cy, bw, bh)
    return best


def extract_frames_from_video(video_path, frame_indices):
    """从视频中提取指定帧"""
    cap = cv2.VideoCapture(str(video_path))
    if not cap.isOpened():
        return []
    frames = {}
    for idx in frame_indices:
        cap.set(cv2.CAP_PROP_POS_FRAMES, idx)
        ret, frame = cap.read()
        if ret:
            frames[idx] = frame
    cap.release()
    return frames


def main():
    try:
        import fiftyone as fo
        from fiftyone.utils.huggingface import load_from_hub
    except ImportError:
        print("=" * 60)
        print("请先安装 FiftyOne: pip install fiftyone")
        print("=" * 60)
        return False

    from ultralytics import YOLO

    print("=" * 60)
    print("加载 Qualcomm 运动数据集 (HuggingFace)")
    print("=" * 60)
    try:
        ds = load_from_hub("Voxel51/qualcomm-exercise-video-dataset-benchmark")
    except Exception as e:
        print(f"加载失败: {e}")
        return False

    person_model = YOLO("yolov8n.pt")

    out_train_img = OUT_DIR / "train" / "images"
    out_train_lbl = OUT_DIR / "train" / "labels"
    out_train_img.mkdir(parents=True, exist_ok=True)
    out_train_lbl.mkdir(parents=True, exist_ok=True)

    total_frames = 0
    sample_idx = 0

    for sample in ds:
        filepath = getattr(sample, "filepath", None)
        if not filepath or not Path(filepath).exists():
            continue
        video_path = Path(filepath)
        fps = 30
        try:
            cap = cv2.VideoCapture(str(video_path))
            fps = cap.get(cv2.CAP_PROP_FPS) or 30
            cap.release()
        except Exception:
            pass

        feedback_events = getattr(sample, "feedback_events", None)
        if feedback_events is None:
            continue
        detections = getattr(feedback_events, "detections", [])
        if not detections:
            continue

        high_knee_ranges = []
        for det in detections:
            label = getattr(det, "label", None) or str(det)
            if not is_high_knees_label(label):
                continue
            support = getattr(det, "support", None)
            if support and len(support) >= 2:
                s, e = int(support[0]), int(support[1])
                high_knee_ranges.append((s, min(e + 90, e + int(3 * fps))))

        if not high_knee_ranges:
            continue

        frame_indices = set()
        for s, e in high_knee_ranges:
            step = max(1, (e - s) // 15)
            for idx in range(s, e, step):
                frame_indices.add(idx)
        frame_indices = sorted(frame_indices)[:120]

        if not frame_indices:
            continue

        vid = getattr(sample, "video_id", sample_idx)
        prefix = f"qualcomm_{vid}_"

        cap = cv2.VideoCapture(str(video_path))
        if not cap.isOpened():
            continue

        saved = 0
        for idx in frame_indices:
            cap.set(cv2.CAP_PROP_POS_FRAMES, idx)
            ret, frame = cap.read()
            if not ret:
                continue
            bbox = get_person_bbox(frame, person_model)
            if bbox is None:
                continue
            cx, cy, bw, bh = bbox
            fname = f"{prefix}f{idx:05d}.jpg"
            img_path = out_train_img / fname
            cv2.imwrite(str(img_path), frame)

            lbl_path = out_train_lbl / (Path(fname).stem + ".txt")
            with open(lbl_path, "w") as f:
                f.write(f"{HIGH_JUMPS_CLASS_ID} {cx:.6f} {cy:.6f} {bw:.6f} {bh:.6f}\n")
            saved += 1
            total_frames += 1

        cap.release()
        sample_idx += 1
        if saved > 0:
            print(f"  视频 {vid}: 提取 {saved} 帧 -> high_jumps")

    print(f"\n共提取 {total_frames} 张高抬腿图像 -> {OUT_DIR}")
    print("运行 python scripts/download_and_merge_datasets.py 合并到完整数据集")
    print("=" * 60)
    return total_frames > 0


if __name__ == "__main__":
    success = main()
    exit(0 if success else 1)
