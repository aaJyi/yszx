"""
视频运动识别 - 上传视频分析

功能:
- 识别视频中人物做的运动类型 (使用训练好的 YOLO 模型)
- 使用姿态估计计数运动次数
- 根据时长估算消耗的卡路里

使用方法:
  python run_video.py video.mp4
  python run_video.py video.mp4 --model path.pt --output result.mp4
  python run_video.py video.mp4 --weight 70 --height 170 --no-display
"""

import argparse
import time
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont

PROJECT_DIR = Path(__file__).resolve().parent

# 中文字体 (Windows 常见路径)
_FONT_PATH = None
for p in [
    Path("C:/Windows/Fonts/msyh.ttc"),
    Path("C:/Windows/Fonts/msyhbd.ttc"),
    Path("C:/Windows/Fonts/simhei.ttf"),
    Path("C:/Windows/Fonts/simsun.ttc"),
]:
    if p.exists():
        _FONT_PATH = str(p)
        break


def put_chinese(img, text, xy, color=(0, 255, 0), font_size=20):
    """在 OpenCV 图像上绘制中文"""
    if _FONT_PATH is None or not text.strip():
        cv2.putText(img, text.encode("ascii", "replace").decode(), xy, cv2.FONT_HERSHEY_SIMPLEX, 0.6, color, 2)
        return
    pil = Image.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))
    draw = ImageDraw.Draw(pil)
    font = ImageFont.truetype(_FONT_PATH, font_size)
    fill_rgb = (color[2], color[1], color[0]) if len(color) >= 3 else (0, 255, 0)
    draw.text(xy, text, font=font, fill=fill_rgb)
    img[:, :] = cv2.cvtColor(np.array(pil), cv2.COLOR_RGB2BGR)


def draw_chinese_batch(img, items, font_size=22):
    """一次性绘制多行中文. items: [(text, (x,y), color_bgr), ...]"""
    if _FONT_PATH is None:
        for text, (x, y), color in items:
            cv2.putText(img, text, (x, y), cv2.FONT_HERSHEY_SIMPLEX, 0.6, color, 2)
        return
    pil = Image.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))
    draw = ImageDraw.Draw(pil)
    font = ImageFont.truetype(_FONT_PATH, font_size)
    for text, (x, y), color in items:
        fill_rgb = (color[2], color[1], color[0]) if len(color) >= 3 else (0, 255, 0)
        draw.text((x, y), text, font=font, fill=fill_rgb)
    img[:, :] = cv2.cvtColor(np.array(pil), cv2.COLOR_RGB2BGR)


from src.exercise_utils import load_config, norm_exercise_name, ex_display_name, find_model


def main():
    parser = argparse.ArgumentParser(description="视频运动识别 - 检测运动类型、次数、卡路里")
    parser.add_argument("video", help="输入视频文件路径")
    parser.add_argument("--model", default=None, help="训练好的检测模型路径")
    parser.add_argument("--pose-model", default="yolo11n-pose.pt", help="姿态估计模型")
    parser.add_argument("--output", "-o", default=None, help="输出视频保存路径 (可选)")
    parser.add_argument("--weight", type=float, default=None, help="体重(kg)")
    parser.add_argument("--height", type=float, default=None, help="身高(cm)")
    parser.add_argument("--conf", type=float, default=0.5, help="检测置信度阈值 (0.5~0.9，误检多可提高到0.6)")
    parser.add_argument("--stable-frames", type=int, default=15, help="同一运动连续检测N帧才切换 (减少误判)")
    parser.add_argument("--no-display", action="store_true", help="不显示窗口 (加速处理)")
    parser.add_argument("--no-pose", action="store_true", help="禁用姿态估计/次数计数")
    args = parser.parse_args()

    video_path = Path(args.video)
    if not video_path.exists():
        print(f"错误: 视频文件不存在: {video_path}")
        return

    config = load_config()
    weight_kg = args.weight or config.get("user_profile", {}).get("weight_kg", 70)
    height_cm = args.height or config.get("user_profile", {}).get("height_cm", 170)

    # 加载检测模型
    model_path = args.model or find_model(PROJECT_DIR)

    if not model_path or not Path(model_path).exists():
        print("未找到训练好的检测模型，请先运行 scripts/train.py 或使用 --model 指定路径")
        return

    from ultralytics import YOLO

    det_model = YOLO(model_path)
    pose_model = None
    if not args.no_pose:
        try:
            pose_model = YOLO(args.pose_model)
        except Exception as e:
            print(f"姿态模型加载失败: {e}，将仅做运动识别")
            args.no_pose = True

    rep_configs = config.get("rep_count_configs", {})
    current_exercise = None
    exercise_start_frame = None
    exercise_stats = {}
    last_detected_frame = None
    pending_exercise = None
    pending_count = 0

    from src.calorie_calculator import calculate_calories
    from src.rep_counter import RepCounter

    rep_counter = RepCounter()
    DETECT_TIMEOUT_FRAMES = 90  # 约 3 秒 (假设 30fps)

    cap = cv2.VideoCapture(str(video_path))
    if not cap.isOpened():
        print(f"无法打开视频: {video_path}")
        return

    fps = cap.get(cv2.CAP_PROP_FPS) or 30
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    w = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    h = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))

    writer = None
    if args.output:
        fourcc = cv2.VideoWriter_fourcc(*"mp4v")
        writer = cv2.VideoWriter(args.output, fourcc, fps, (w, h))

    class_names = getattr(det_model.model, "names", None) or (det_model.names if hasattr(det_model, "names") else {})
    # 检测 cv2.imshow 是否可用 (headless 或部分 OpenCV 安装无 GUI)
    use_display = not args.no_display
    if use_display:
        try:
            cv2.namedWindow("_test", cv2.WINDOW_NORMAL)
            cv2.destroyWindow("_test")
        except cv2.error:
            use_display = False
            if not args.output:
                print("提示: OpenCV 无 GUI，使用 --output 保存视频")

    print(f"视频: {video_path.name} | {total_frames} 帧 | {fps:.1f} FPS | {weight_kg}kg")
    print("正在分析... (按 q 可提前退出)" if use_display else "正在分析...")
    print("-" * 60)

    frame_idx = 0
    while True:
        ret, frame = cap.read()
        if not ret:
            break

        frame_idx += 1

        # 1. 运动类型检测
        det_results = det_model(frame, verbose=False, conf=args.conf)
        boxes = det_results[0].boxes if det_results else None
        best_cls = None
        best_conf = 0.0

        if boxes is not None and len(boxes) > 0:
            for i in range(len(boxes)):
                conf = float(boxes.conf[i])
                if conf > best_conf:
                    best_conf = conf
                    cls_id = int(boxes.cls[i])
                    best_cls = class_names.get(cls_id, f"class_{cls_id}")

        raw_detected = norm_exercise_name(best_cls) if best_cls and best_conf >= args.conf else None

        # 2. 检测稳定性: 同一运动连续 N 帧才切换，减少误判
        if raw_detected:
            last_detected_frame = frame_idx
            if raw_detected == pending_exercise:
                pending_count += 1
            else:
                pending_exercise = raw_detected
                pending_count = 1
        else:
            pending_exercise = None
            pending_count = 0

        detected_exercise = pending_exercise if pending_count >= args.stable_frames else current_exercise

        # 3. 更新当前运动状态
        if detected_exercise:
            if detected_exercise != current_exercise:
                if current_exercise and exercise_start_frame is not None:
                    dur = (frame_idx - exercise_start_frame) / fps
                    cals = calculate_calories(weight_kg, dur, current_exercise)
                    if current_exercise not in exercise_stats:
                        exercise_stats[current_exercise] = {"reps": 0, "seconds": 0, "calories": 0}
                    exercise_stats[current_exercise]["seconds"] += dur
                    exercise_stats[current_exercise]["calories"] += cals
                    exercise_stats[current_exercise]["reps"] += rep_counter.count
                current_exercise = detected_exercise
                exercise_start_frame = frame_idx
                rc = rep_configs.get(detected_exercise, rep_configs.get("default", {}))
                rep_counter = RepCounter(
                    up_angle=rc.get("up_angle", 145),
                    down_angle=rc.get("down_angle", 90),
                    kpts=rc.get("kpts", [6, 8, 10]),
                )
        else:
            if current_exercise and last_detected_frame and (frame_idx - last_detected_frame) > DETECT_TIMEOUT_FRAMES:
                dur = (frame_idx - exercise_start_frame) / fps
                cals = calculate_calories(weight_kg, dur, current_exercise)
                if current_exercise not in exercise_stats:
                    exercise_stats[current_exercise] = {"reps": 0, "seconds": 0, "calories": 0}
                exercise_stats[current_exercise]["seconds"] += dur
                exercise_stats[current_exercise]["calories"] += cals
                exercise_stats[current_exercise]["reps"] += rep_counter.count
                current_exercise = None
                exercise_start_frame = None

        # 3. 姿态估计 + 次数计数
        if pose_model and not args.no_pose and current_exercise:
            pose_results = pose_model(frame, verbose=False)
            kpts = None
            if pose_results and len(pose_results) > 0:
                pk = pose_results[0].keypoints
                if pk is not None:
                    data = pk.data.cpu().numpy()
                    if len(data) > 0:
                        kpts = data[0]
            if kpts is not None:
                rep_counter.update(kpts)

        # 4. 绘制
        display = det_results[0].plot() if det_results else frame.copy()

        current_dur = (frame_idx - exercise_start_frame) / fps if (current_exercise and exercise_start_frame is not None) else 0
        total_secs = frame_idx / fps
        total_cals = sum(s["calories"] for s in exercise_stats.values())
        if current_exercise and exercise_start_frame is not None:
            total_cals += calculate_calories(weight_kg, current_dur, current_exercise)

        ex_cn = ex_display_name(current_exercise) if current_exercise else "-"
        info_lines = [
            f"体重: {weight_kg}kg",
            f"当前: {ex_cn}",
            f"时长: {current_dur:.0f}秒" + (f" | 次数: {rep_counter.count}" if current_exercise else ""),
            f"进度: {frame_idx}/{total_frames} ({100*frame_idx/total_frames:.0f}%)",
            f"卡路里: {total_cals:.1f} 千卡",
        ]
        items = [(line, (10, 30 + i * 28), (0, 255, 0)) for i, line in enumerate(info_lines)]
        y_off = 30 + len(info_lines) * 28
        for ex, stat in list(exercise_stats.items())[-5:]:
            ex_cn = ex_display_name(ex)
            items.append((f"  {ex_cn}: {stat['reps']} 次, {stat['calories']:.1f} 千卡", (10, y_off), (200, 200, 200)))
            y_off += 22
        draw_chinese_batch(display, items, 22)

        if writer:
            writer.write(display)
        if use_display:
            try:
                cv2.imshow("视频运动识别", display)
                if cv2.waitKey(1) & 0xFF == ord("q"):
                    print("\n用户中断")
                    break
            except cv2.error:
                use_display = False

        if frame_idx % 100 == 0:
            print(f"\r进度: {frame_idx}/{total_frames} ({100*frame_idx/total_frames:.0f}%)", end="", flush=True)

    # 结算最后一项运动
    if current_exercise and exercise_start_frame is not None:
        dur = (frame_idx - exercise_start_frame) / fps
        cals = calculate_calories(weight_kg, dur, current_exercise)
        if current_exercise not in exercise_stats:
            exercise_stats[current_exercise] = {"reps": 0, "seconds": 0, "calories": 0}
        exercise_stats[current_exercise]["seconds"] += dur
        exercise_stats[current_exercise]["calories"] += cals
        exercise_stats[current_exercise]["reps"] += rep_counter.count

    cap.release()
    if writer:
        writer.release()
    if use_display:
        try:
            cv2.destroyAllWindows()
        except cv2.error:
            pass

    # 最终统计
    total_cals = sum(s["calories"] for s in exercise_stats.values())
    total_secs = frame_idx / fps
    print()
    print("=" * 60)
    print("视频分析完成")
    print("=" * 60)
    if exercise_stats:
        for ex, stat in exercise_stats.items():
            ex_cn = ex_display_name(ex)
            print(f"  {ex_cn}: {stat['reps']} 次 | {stat['seconds']:.1f} 秒 | {stat['calories']:.1f} 千卡")
        print("-" * 60)
        print(f"  总计: {total_cals:.1f} 千卡 (视频时长 {total_secs:.1f} 秒)")
    else:
        print("  未检测到运动")
    if args.output:
        print(f"  输出已保存: {args.output}")
    print("=" * 60)


if __name__ == "__main__":
    main()
