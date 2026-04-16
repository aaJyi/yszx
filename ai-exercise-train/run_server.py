"""
运动识别 HTTP 服务 - 供小程序/后端调用
支持会话：同一 session_id 下统计运动记录与深蹲等次数（姿态估计 + RepCounter）
支持导入视频识别：POST /recognize-video 上传视频，返回运动记录。

启动: python run_server.py [--port 5000] [--model path.pt] [--no-pose 禁用次数]
"""

import base64
import os
import sys
import tempfile
import time
import uuid
from pathlib import Path

PROJECT_DIR = Path(__file__).resolve().parent
if str(PROJECT_DIR) not in sys.path:
    sys.path.insert(0, str(PROJECT_DIR))

import numpy as np
import cv2
from ultralytics import YOLO

from src.exercise_utils import (
    load_config,
    EXERCISE_CN,
    norm_exercise_name,
    find_model,
    predict_image,
)


def _find_model():
    return find_model(PROJECT_DIR)


def _encode_annotated_frame_base64(det_model, pose_model, img_bgr, conf_threshold=0.6, no_pose=False):
    """返回带检测/姿态标记的 JPEG(base64)；失败时返回空字符串。"""
    try:
        draw_img = img_bgr.copy()
        det_results = det_model(draw_img, conf=conf_threshold, verbose=False)
        if det_results and len(det_results) > 0:
            draw_img = det_results[0].plot()
        if pose_model is not None and not no_pose:
            pose_results = pose_model(draw_img, verbose=False)
            if pose_results and len(pose_results) > 0:
                draw_img = pose_results[0].plot()
        ok, enc = cv2.imencode(".jpg", draw_img, [int(cv2.IMWRITE_JPEG_QUALITY), 70])
        if not ok:
            return ""
        return base64.b64encode(enc.tobytes()).decode("utf-8")
    except Exception:
        return ""


def process_video(
    video_path,
    det_model,
    pose_model,
    rep_configs,
    conf_threshold=0.6,
    stable_frames=4,
    no_pose=False,
    output_video_path=None,
):
    """对视频逐帧识别并统计运动记录与次数；可选输出带标注视频。"""
    from src.rep_counter import RepCounter

    DETECT_TIMEOUT_FRAMES = 90
    cap = cv2.VideoCapture(str(video_path))
    if not cap.isOpened():
        return {}, 0.0, 0

    fps = cap.get(cv2.CAP_PROP_FPS) or 30
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH) or 0)
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT) or 0)
    writer = None
    if output_video_path and width > 0 and height > 0:
        fourcc = cv2.VideoWriter_fourcc(*"mp4v")
        writer = cv2.VideoWriter(str(output_video_path), fourcc, float(fps), (width, height))
    current_exercise = None
    exercise_start_frame = None
    exercise_stats = {}
    last_detected_frame = None
    pending_exercise = None
    pending_count = 0
    rep_counter = RepCounter()

    frame_idx = 0
    while True:
        ret, frame = cap.read()
        if not ret:
            break
        frame_idx += 1

        exercise_type, _ = predict_image(det_model, frame, conf_threshold)
        raw_detected = exercise_type

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

        detected_exercise = pending_exercise if pending_count >= stable_frames else current_exercise

        if detected_exercise:
            if detected_exercise != current_exercise:
                if current_exercise and exercise_start_frame is not None:
                    dur = (frame_idx - exercise_start_frame) / fps
                    if current_exercise not in exercise_stats:
                        exercise_stats[current_exercise] = {"reps": 0, "seconds": 0}
                    exercise_stats[current_exercise]["seconds"] += dur
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
                if current_exercise not in exercise_stats:
                    exercise_stats[current_exercise] = {"reps": 0, "seconds": 0}
                exercise_stats[current_exercise]["seconds"] += dur
                exercise_stats[current_exercise]["reps"] += rep_counter.count
                current_exercise = None
                exercise_start_frame = None

        if pose_model and not no_pose and current_exercise:
            pose_results = pose_model(frame, verbose=False)
            kpts = None
            if pose_results and len(pose_results) > 0 and pose_results[0].keypoints is not None:
                data = pose_results[0].keypoints.data.cpu().numpy()
                if len(data) > 0:
                    kpts = data[0]
            if kpts is not None:
                rep_counter.update(kpts)

        if writer is not None:
            draw = frame.copy()
            try:
                dres = det_model(draw, conf=conf_threshold, verbose=False)
                if dres and len(dres) > 0:
                    draw = dres[0].plot()
                if pose_model and not no_pose:
                    pres = pose_model(draw, verbose=False)
                    if pres and len(pres) > 0:
                        draw = pres[0].plot()
            except Exception:
                pass
            label = EXERCISE_CN.get(current_exercise, current_exercise or "未识别")
            sec = frame_idx / fps if fps else 0.0
            cv2.putText(draw, f"Exercise: {label}", (24, 36), cv2.FONT_HERSHEY_SIMPLEX, 0.9, (0, 255, 0), 2)
            cv2.putText(draw, f"Reps: {rep_counter.count if current_exercise else 0}", (24, 72), cv2.FONT_HERSHEY_SIMPLEX, 0.9, (0, 255, 255), 2)
            cv2.putText(draw, f"Time: {sec:.1f}s", (24, 108), cv2.FONT_HERSHEY_SIMPLEX, 0.9, (255, 255, 0), 2)
            writer.write(draw)

    if current_exercise and exercise_start_frame is not None:
        dur = (frame_idx - exercise_start_frame) / fps
        if current_exercise not in exercise_stats:
            exercise_stats[current_exercise] = {"reps": 0, "seconds": 0}
        exercise_stats[current_exercise]["seconds"] += dur
        exercise_stats[current_exercise]["reps"] += rep_counter.count

    cap.release()
    if writer is not None:
        writer.release()
    total_seconds = frame_idx / fps if frame_idx else 0
    return exercise_stats, total_seconds, frame_idx


def main():
    import argparse
    parser = argparse.ArgumentParser(description="运动识别 HTTP 服务（含会话与次数统计）")
    parser.add_argument("--port", type=int, default=5000, help="服务端口")
    parser.add_argument("--model", default=None, help="检测模型路径")
    parser.add_argument("--pose-model", default="yolo11n-pose.pt", help="姿态模型（用于次数）")
    parser.add_argument("--conf", type=float, default=0.6, help="置信度阈值")
    parser.add_argument("--no-pose", action="store_true", help="禁用姿态估计/次数统计")
    args = parser.parse_args()

    model_path = args.model or _find_model()
    if not model_path or not Path(model_path).exists():
        print("未找到训练好的模型，请先训练或指定 --model")
        sys.exit(1)

    det_model = YOLO(model_path)
    pose_model = None
    if not args.no_pose:
        try:
            pose_model = YOLO(args.pose_model)
        except Exception as e:
            print(f"姿态模型加载失败: {e}，将不返回次数")
            args.no_pose = True

    config = load_config()
    rep_configs = config.get("rep_count_configs", {})

    from src.rep_counter import RepCounter

    sessions = {}
    annotated_videos = {}

    def get_or_create_session(session_id):
        if not session_id or session_id not in sessions:
            sid = str(uuid.uuid4())
            sessions[sid] = {
                "current_exercise": None,
                "exercise_start_time": None,
                "rep_counter": None,
                "exercise_stats": {},
            }
            return sid, sessions[sid]
        return session_id, sessions[session_id]

    def build_exercise_stats_list(stats):
        out = []
        for ex_type, data in stats.items():
            out.append({
                "exerciseType": ex_type,
                "exerciseTypeCn": EXERCISE_CN.get(ex_type, ex_type or ""),
                "reps": data.get("reps", 0),
                "seconds": round(data.get("seconds", 0), 1),
            })
        return out

    try:
        from fastapi import FastAPI, File, UploadFile, Form
        from fastapi.responses import FileResponse
        from fastapi.middleware.cors import CORSMiddleware
        from pydantic import BaseModel
    except ImportError:
        print("请安装: pip install fastapi uvicorn python-multipart")
        sys.exit(1)

    class RecognizeBody(BaseModel):
        imageBase64: str = ""
        sessionId: str = ""

    app = FastAPI(title="AI Exercise Train API")
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    @app.post("/recognize")
    async def recognize(
        file: UploadFile = File(default=None),
        sessionId: str = Form(default=""),
    ):
        """接收图片 + 可选 sessionId，返回运动类型、置信度、当前次数及本会话运动记录。"""
        img_bgr = None
        if file:
            raw = await file.read()
        else:
            raw = None
        if raw:
            arr = np.frombuffer(raw, dtype=np.uint8)
            img_bgr = cv2.imdecode(arr, cv2.IMREAD_COLOR)
        if img_bgr is None:
            return {"code": 400, "message": "无效图片", "exerciseType": None, "exerciseTypeCn": None, "confidence": 0, "reps": 0, "exerciseStats": []}

        exercise_type, confidence = predict_image(det_model, img_bgr, args.conf)
        sid, session = get_or_create_session(sessionId.strip() or None)
        now = time.time()

        if exercise_type and confidence >= args.conf:
            if exercise_type != session["current_exercise"]:
                if session["current_exercise"] and session["exercise_start_time"]:
                    dur = now - session["exercise_start_time"]
                    prev = session["current_exercise"]
                    if prev not in session["exercise_stats"]:
                        session["exercise_stats"][prev] = {"reps": 0, "seconds": 0}
                    session["exercise_stats"][prev]["seconds"] += dur
                    session["exercise_stats"][prev]["reps"] += (session["rep_counter"].count if session["rep_counter"] else 0)
                rc = rep_configs.get(exercise_type, rep_configs.get("default", {}))
                session["rep_counter"] = RepCounter(
                    up_angle=rc.get("up_angle", 145),
                    down_angle=rc.get("down_angle", 90),
                    kpts=rc.get("kpts", [6, 8, 10]),
                )
                session["current_exercise"] = exercise_type
                session["exercise_start_time"] = now
        else:
            exercise_type = session["current_exercise"]

        reps = 0
        if session["current_exercise"] and session["rep_counter"] and pose_model and not args.no_pose:
            pose_results = pose_model(img_bgr, verbose=False)
            kpts = None
            if pose_results and len(pose_results) > 0 and pose_results[0].keypoints is not None:
                data = pose_results[0].keypoints.data.cpu().numpy()
                if len(data) > 0:
                    kpts = data[0]
            if kpts is not None:
                session["rep_counter"].update(kpts)
            reps = session["rep_counter"].count

        if session["current_exercise"] and session["exercise_start_time"]:
            if session["current_exercise"] not in session["exercise_stats"]:
                session["exercise_stats"][session["current_exercise"]] = {"reps": 0, "seconds": 0}
            current_secs = now - session["exercise_start_time"]
            session["exercise_stats"][session["current_exercise"]]["seconds"] = round(current_secs, 1)
            session["exercise_stats"][session["current_exercise"]]["reps"] = reps

        exercise_stats_list = build_exercise_stats_list(session["exercise_stats"])
        annotated_image_base64 = _encode_annotated_frame_base64(
            det_model, pose_model, img_bgr, conf_threshold=args.conf, no_pose=args.no_pose
        )

        return {
            "code": 200,
            "sessionId": sid,
            "exerciseType": exercise_type,
            "exerciseTypeCn": EXERCISE_CN.get(exercise_type, exercise_type or ""),
            "confidence": round(confidence, 4),
            "reps": reps,
            "exerciseStats": exercise_stats_list,
            "annotatedImageBase64": annotated_image_base64,
        }

    @app.post("/recognize-base64")
    async def recognize_base64(body: RecognizeBody):
        """接收 JSON { "imageBase64": "...", "sessionId": "..." }"""
        if not (body and body.imageBase64):
            return {"code": 400, "message": "缺少 imageBase64", "exerciseType": None, "exerciseTypeCn": None, "confidence": 0, "reps": 0, "exerciseStats": []}
        try:
            raw = base64.b64decode(body.imageBase64)
        except Exception:
            return {"code": 400, "message": "base64 解码失败", "exerciseType": None, "exerciseTypeCn": None, "confidence": 0, "reps": 0, "exerciseStats": []}
        arr = np.frombuffer(raw, dtype=np.uint8)
        img_bgr = cv2.imdecode(arr, cv2.IMREAD_COLOR)
        if img_bgr is None:
            return {"code": 400, "message": "无效图片", "exerciseType": None, "exerciseTypeCn": None, "confidence": 0, "reps": 0, "exerciseStats": []}

        exercise_type, confidence = predict_image(det_model, img_bgr, args.conf)
        sid, session = get_or_create_session(body.sessionId.strip() or None)
        now = time.time()

        if exercise_type and confidence >= args.conf:
            if exercise_type != session["current_exercise"]:
                if session["current_exercise"] and session["exercise_start_time"]:
                    dur = now - session["exercise_start_time"]
                    prev = session["current_exercise"]
                    if prev not in session["exercise_stats"]:
                        session["exercise_stats"][prev] = {"reps": 0, "seconds": 0}
                    session["exercise_stats"][prev]["seconds"] += dur
                    session["exercise_stats"][prev]["reps"] += (session["rep_counter"].count if session["rep_counter"] else 0)
                rc = rep_configs.get(exercise_type, rep_configs.get("default", {}))
                session["rep_counter"] = RepCounter(
                    up_angle=rc.get("up_angle", 145),
                    down_angle=rc.get("down_angle", 90),
                    kpts=rc.get("kpts", [6, 8, 10]),
                )
                session["current_exercise"] = exercise_type
                session["exercise_start_time"] = now
        else:
            exercise_type = session["current_exercise"]

        reps = 0
        if session["current_exercise"] and session["rep_counter"] and pose_model and not args.no_pose:
            pose_results = pose_model(img_bgr, verbose=False)
            kpts = None
            if pose_results and len(pose_results) > 0 and pose_results[0].keypoints is not None:
                data = pose_results[0].keypoints.data.cpu().numpy()
                if len(data) > 0:
                    kpts = data[0]
            if kpts is not None:
                session["rep_counter"].update(kpts)
            reps = session["rep_counter"].count

        if session["current_exercise"] and session["exercise_start_time"]:
            if session["current_exercise"] not in session["exercise_stats"]:
                session["exercise_stats"][session["current_exercise"]] = {"reps": 0, "seconds": 0}
            current_secs = now - session["exercise_start_time"]
            session["exercise_stats"][session["current_exercise"]]["seconds"] = round(current_secs, 1)
            session["exercise_stats"][session["current_exercise"]]["reps"] = reps

        exercise_stats_list = build_exercise_stats_list(session["exercise_stats"])

        return {
            "code": 200,
            "sessionId": sid,
            "exerciseType": exercise_type,
            "exerciseTypeCn": EXERCISE_CN.get(exercise_type, exercise_type or ""),
            "confidence": round(confidence, 4),
            "reps": reps,
            "exerciseStats": exercise_stats_list,
        }

    @app.post("/recognize-video")
    async def recognize_video(file: UploadFile = File(default=None)):
        """上传视频文件，逐帧识别并返回运动记录（各项次数与时长）。"""
        if not file or not file.filename:
            return {"code": 400, "message": "请上传视频文件", "exerciseStats": [], "totalDuration": 0}
        raw = await file.read()
        if not raw:
            return {"code": 400, "message": "视频文件为空", "exerciseStats": [], "totalDuration": 0}
        suffix = Path(file.filename).suffix or ".mp4"
        if suffix.lower() not in (".mp4", ".avi", ".mov", ".mkv", ".webm"):
            suffix = ".mp4"
        tmp = None
        out_tmp = None
        try:
            fd, tmp = tempfile.mkstemp(suffix=suffix)
            os.write(fd, raw)
            os.close(fd)
            out_fd, out_tmp = tempfile.mkstemp(suffix="_result.mp4")
            os.close(out_fd)
            import asyncio
            loop = asyncio.get_event_loop()
            stats, total_secs, total_frames = await loop.run_in_executor(
                None,
                lambda: process_video(
                    tmp, det_model, pose_model, rep_configs,
                    conf_threshold=args.conf, stable_frames=4, no_pose=args.no_pose,
                    output_video_path=out_tmp,
                ),
            )
            exercise_stats_list = build_exercise_stats_list(stats)
            token = str(uuid.uuid4())
            if out_tmp and os.path.exists(out_tmp):
                annotated_videos[token] = out_tmp
            return {
                "code": 200,
                "message": "分析完成",
                "exerciseStats": exercise_stats_list,
                "totalDuration": round(total_secs, 1),
                "totalFrames": total_frames,
                "annotatedToken": token,
            }
        except Exception as e:
            return {"code": 500, "message": "视频分析失败: " + str(e), "exerciseStats": [], "totalDuration": 0}
        finally:
            if tmp and os.path.exists(tmp):
                try:
                    os.unlink(tmp)
                except Exception:
                    pass

    @app.get("/annotated-video/{token}")
    async def annotated_video(token: str):
        path = annotated_videos.get(token)
        if not path or not os.path.exists(path):
            return {"code": 404, "message": "标注视频不存在或已过期"}
        return FileResponse(path=path, media_type="video/mp4", filename=f"exercise_{token}.mp4")

    import uvicorn
    print(f"模型已加载: {model_path}, 端口: {args.port}, 姿态计数: {not args.no_pose}")
    uvicorn.run(app, host="0.0.0.0", port=args.port)


if __name__ == "__main__":
    main()
