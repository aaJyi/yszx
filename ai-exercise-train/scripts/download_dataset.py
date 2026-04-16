"""
从 Roboflow 下载运动识别数据集
数据集: https://universe.roboflow.com/activityrecognition-noyo4/exercise-ekbld

使用方法:
1. 在 https://app.roboflow.com 注册/登录
2. 获取 API Key (Account Settings -> API)
3. 设置环境变量: set ROBoflow_API_KEY=your_key
   或在脚本中直接填写 api_key
4. 运行: python scripts/download_dataset.py
"""

import os
from pathlib import Path

# 配置 - 请将 YOUR_ROBOFLOW_API_KEY 替换为你的API密钥
ROBOFLOW_API_KEY = os.environ.get("ROBOFLOW_API_KEY", os.environ.get("ROBoflow_API_KEY", "YOUR_ROBOFLOW_API_KEY"))
PROJECT_DIR = Path(__file__).resolve().parent.parent
DATASET_DIR = PROJECT_DIR / "dataset"



def download_dataset():
    """从 Roboflow 下载 exercise-ekbld 数据集"""
    if ROBOFLOW_API_KEY == "YOUR_ROBOFLOW_API_KEY":
        print("=" * 60)
        print("错误: 请先设置 Roboflow API Key!")
        print("1. 访问 https://app.roboflow.com 登录")
        print("2. 进入 Account Settings -> API 复制 API Key")
        print("3. 设置环境变量: set ROBOFLOW_API_KEY=你的密钥")
        print("   或修改本脚本中的 ROBOFLOW_API_KEY 变量")
        print("=" * 60)
        return False

    try:
        from roboflow import Roboflow
        from roboflow.adapters.rfapi import RoboflowError
    except ImportError:
        print("请先安装 roboflow: pip install roboflow")
        return False

    print("正在连接 Roboflow...")
    rf = Roboflow(api_key=ROBOFLOW_API_KEY)

    WORKSPACE = os.environ.get("ROBOFLOW_WORKSPACE", "").strip()
    if "set " in WORKSPACE or "=" in WORKSPACE or len(WORKSPACE) > 50:
        WORKSPACE = ""
    PROJECT = os.environ.get("ROBOFLOW_PROJECT", "exercise-ekbld-kjve3").strip()
    if "set " in PROJECT or PROJECT.startswith("ROBOFLOW_"):
        PROJECT = "exercise-ekbld-kjve3"

    try:
        if WORKSPACE:
            print(f"正在下载 {WORKSPACE}/{PROJECT} ...")
            project = rf.workspace(WORKSPACE).project(PROJECT)
        else:
            print(f"正在下载 (默认工作区)/{PROJECT} ...")
            project = rf.workspace().project(PROJECT)
    except RoboflowError as e:
        if WORKSPACE and ("404" in str(e) or "does not exist" in str(e)):
            print(f"工作区 '{WORKSPACE}' 无效，改用默认工作区...")
            project = rf.workspace().project(PROJECT)
        else:
            raise

    # 获取可用版本，优先使用 ROBOFLOW_VERSION，否则用最新版本
    version_info = project.get_version_information()
    available = [os.path.basename(v["id"]) for v in version_info]
    if not available:
        raise RuntimeError("该项目尚无可用版本，请先在 Roboflow 中生成数据集版本。")
    version_num = os.environ.get("ROBOFLOW_VERSION", "").strip() or available[0]
    if version_num not in available:
        print(f"版本 {version_num} 不存在，可用版本: {available}，使用 {available[0]}")
        version_num = available[0]
    print(f"使用版本: {version_num}")
    dataset = project.version(int(version_num) if version_num.isdigit() else version_num).download(
        "yolov8", location=str(DATASET_DIR), overwrite=True
    )

    print(f"\n数据集已保存到: {DATASET_DIR}")
    print("目录结构:")
    for p in DATASET_DIR.rglob("*"):
        if p.is_file():
            print(f"  {p.relative_to(DATASET_DIR)}")
    return True


if __name__ == "__main__":
    DATASET_DIR.mkdir(parents=True, exist_ok=True)
    success = download_dataset()
    exit(0 if success else 1)
