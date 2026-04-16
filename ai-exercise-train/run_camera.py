"""
实时运动识别 - 摄像头推理主程序

功能:
- 识别用户正在做的运动类型 (使用训练好的 YOLO 模型)
- 使用姿态估计计数运动次数
- 记录每项运动的时长
- 根据身高体重估算消耗的卡路里

使用方法:
  python run_camera.py                    # 使用默认摄像头
  python run_camera.py --model path.pt    # 指定模型路径
  python run_camera.py --weight 70        # 设置体重
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
    """在 OpenCV 图像上绘制中文 (cv2.putText 不支持中文)"""
    if _FONT_PATH is None or not text.strip():
        cv2.putText(img, text.encode("ascii", "replace").decode(), xy, cv2.FONT_HERSHEY_SIMPLEX, 0.6, color, 2)
        return
    pil = Image.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))
    draw = ImageDraw.Draw(pil)
    font = ImageFont.truetype(_FONT_PATH, font_size)
    # PIL fill 为 RGB, OpenCV 为 BGR
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
    parser = argparse.ArgumentParser(description="运动识别 - 摄像头实时推理")
    parser.add_argument("--model", default=None, help="训练好的检测模型路径 (如 runs/detect/exercise_train/weights/best.pt)")
    parser.add_argument("--pose-model", default="yolo11n-pose.pt", help="姿态估计模型 (用于次数计数)")
    parser.add_argument("--weight", type=float, default=None, help="体重(kg)")
    parser.add_argument("--height", type=float, default=None, help="身高(cm)")
    parser.add_argument("--camera", type=int, default=0, help="摄像头索引")
    parser.add_argument("--conf", type=float, default=0.5, help="检测置信度阈值 (误检多可提高到0.6)")
    parser.add_argument("--stable-secs", type=float, default=0.5, help="同一运动连续检测N秒才切换")
    parser.add_argument("--no-pose", action="store_true", help="禁用姿态估计/次数计数 (仅做运动识别)")
    args = parser.parse_args()

    config = load_config()
    weight_kg = args.weight or config.get("user_profile", {}).get("weight_kg", 70)
    height_cm = args.height or config.get("user_profile", {}).get("height_cm", 170)

    # 运动检测模型
    model_path = args.model or find_model(PROJECT_DIR)

    if not model_path or not Path(model_path).exists():
        print("=" * 60)
        print("未找到训练好的检测模型!")
        print("请先完成以下步骤:")
        print("1. python scripts/download_dataset.py  下载数据集")
        print("2. python scripts/train.py             训练模型")
        print("或使用 --model 指定模型路径")
        print("=" * 60)
        return

    from ultralytics import YOLO

    det_model = YOLO(model_path)
    pose_model = None
    if not args.no_pose:
        try:
            pose_model = YOLO(args.pose_model)
        except Exception as e:
            print(f"姿态模型加载失败 ({e}), 将仅做运动识别")
            args.no_pose = True

    # 加载 rep 配置
    rep_configs = config.get("rep_count_configs", {})
    current_exercise = None
    exercise_start_time = None
    session_start = time.time()
    exercise_stats = {}  # {exercise: {"reps": 0, "seconds": 0, "calories": 0}}

    from src.calorie_calculator import calculate_calories
    from src.rep_counter import RepCounter

    rep_counter = RepCounter()
    last_detected_time = None
    pending_exercise = None
    pending_start_time = None

    cap = cv2.VideoCapture(args.camera)
    if not cap.isOpened():
        print("无法打开摄像头")
        return

    # 类别名来自训练时的 data.yaml (模型已内置)
    class_names = getattr(det_model.model, "names", None) or (det_model.names if hasattr(det_model, "names") else {})
    classes = list(class_names.values()) if isinstance(class_names, dict) else []
    cn_list = [ex_display_name(str(c)) for c in classes]
    print("检测类别:", cn_list or classes)

    print("摄像头已启动. 按 'q' 退出, 按 'r' 重置当前运动统计")
    print(f"用户体重: {weight_kg}kg, 身高: {height_cm}cm")
    print("-" * 60)

    last_console_time = 0

    while True:
        ret, frame = cap.read()
        if not ret:
            break

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
        now = time.time()

        # 2. 检测稳定性: 同一运动连续 N 秒才切换
        if raw_detected:
            last_detected_time = now
            if raw_detected == pending_exercise:
                pass
            else:
                pending_exercise = raw_detected
                pending_start_time = now
        else:
            pending_exercise = None
            pending_start_time = None

        if pending_exercise and (now - pending_start_time) >= args.stable_secs:
            detected_exercise = pending_exercise
        else:
            detected_exercise = current_exercise

        # 3. 更新当前运动状态
        if detected_exercise:
            if detected_exercise != current_exercise:
                # 切换运动: 结算上一项
                if current_exercise and exercise_start_time:
                    dur = time.time() - exercise_start_time
                    cals = calculate_calories(weight_kg, dur, current_exercise)
                    if current_exercise not in exercise_stats:
                        exercise_stats[current_exercise] = {"reps": 0, "seconds": 0, "calories": 0}
                    exercise_stats[current_exercise]["seconds"] += dur
                    exercise_stats[current_exercise]["calories"] += cals
                    exercise_stats[current_exercise]["reps"] += rep_counter.count
                current_exercise = detected_exercise
                exercise_start_time = time.time()
                rc = rep_configs.get(detected_exercise, rep_configs.get("default", {}))
                rep_counter = RepCounter(
                    up_angle=rc.get("up_angle", 145),
                    down_angle=rc.get("down_angle", 90),
                    kpts=rc.get("kpts", [6, 8, 10]),
                )
        else:
            # 无检测超过 3 秒: 结算当前运动
            if current_exercise and last_detected_time and (time.time() - last_detected_time) > 3:
                dur = time.time() - exercise_start_time
                cals = calculate_calories(weight_kg, dur, current_exercise)
                if current_exercise not in exercise_stats:
                    exercise_stats[current_exercise] = {"reps": 0, "seconds": 0, "calories": 0}
                exercise_stats[current_exercise]["seconds"] += dur
                exercise_stats[current_exercise]["calories"] += cals
                exercise_stats[current_exercise]["reps"] += rep_counter.count
                current_exercise = None
                exercise_start_time = None

        # 3. 姿态估计 + 次数计数
        if pose_model and not args.no_pose and current_exercise:
            pose_results = pose_model(frame, verbose=False)
            kpts = None
            if pose_results and len(pose_results) > 0:
                pk = pose_results[0].keypoints
                if pk is not None:
                    data = pk.data.cpu().numpy()
                    if len(data) > 0:
                        kpts = data[0]  # 取第一个人
            if kpts is not None:
                rep_counter.update(kpts)

        # 4. 绘制
        display = det_results[0].plot() if det_results else frame

        # 信息叠加
        total_cals = sum(s["calories"] for s in exercise_stats.values())
        total_secs = time.time() - session_start
        current_dur = time.time() - exercise_start_time if (current_exercise and exercise_start_time) else 0
        # 当前运动产生的卡路里 (进行中) + 已完成的
        live_cals = total_cals
        if current_exercise and exercise_start_time:
            live_cals += calculate_calories(weight_kg, current_dur, current_exercise)

        # 控制台实时输出 (每 0.3 秒刷新同一行)
        if time.time() - last_console_time >= 0.3:
            last_console_time = time.time()
            ex_cn = ex_display_name(current_exercise) if current_exercise else "等待检测"
            console_line = (
                f"[实时] 运动: {ex_cn} | 次数: {rep_counter.count} | "
                f"当前时长: {current_dur:.0f}秒 | 累计: {total_secs:.0f}秒 | "
                f"卡路里: {live_cals:.1f} 千卡"
            )
            print(f"\r{console_line}   ", end="", flush=True)
        ex_cn = ex_display_name(current_exercise) if current_exercise else "-"
        info_lines = [
            f"体重: {weight_kg}kg",
            f"当前: {ex_cn}",
            f"时长: {current_dur:.0f}秒" + (f" | 次数: {rep_counter.count}" if current_exercise else ""),
            f"本次: {total_secs:.0f}秒",
            f"卡路里: {live_cals:.1f} 千卡",
        ]
        # 使用 PIL 绘制中文 (cv2.putText 不支持中文会显示乱码)
        items = [(line, (10, 30 + i * 28), (0, 255, 0)) for i, line in enumerate(info_lines)]
        y_off = 30 + len(info_lines) * 28
        for ex, stat in list(exercise_stats.items())[-5:]:
            ex_cn = ex_display_name(ex)
            items.append((f"  {ex_cn}: {stat['reps']} 次, {stat['calories']:.1f} 千卡", (10, y_off), (200, 200, 200)))
            y_off += 22
        draw_chinese_batch(display, items, 22)

        cv2.imshow("运动识别", display)
        key = cv2.waitKey(1) & 0xFF
        if key == ord("q"):
            break
        if key == ord("r"):
            if current_exercise:
                rep_counter.reset()
                exercise_start_time = time.time()

    cap.release()
    cv2.destroyAllWindows()
    print()  # 换行，避免覆盖最终输出

    # 最终统计
    print("\n--- 本次运动统计 ---")
    for ex, stat in exercise_stats.items():
        ex_cn = ex_display_name(ex)
        print(f"  {ex_cn}: {stat['reps']} 次, {stat['seconds']:.0f} 秒, {stat['calories']:.1f} 千卡")


if __name__ == "__main__":
    main()
