# 运动识别训练项目 (YOLO + 卡路里估算)

通过 YOLO 识别摄像头下的运动类型，计数动作次数与时长，并根据身高体重估算卡路里消耗。

**各文件说明、数据集数量与模型效果**见 [docs/项目说明文档.md](docs/项目说明文档.md)。

## 功能

- **运动识别**: 使用自定义训练的 YOLO 模型识别俯卧撑、深蹲、仰卧起坐等
- **次数计数**: 基于姿态关键点角度变化自动计数
- **时长记录**: 记录每项运动的持续时间
- **卡路里估算**: 按 MET 公式 `体重(kg) × MET × 时长(h)` 计算消耗

## 环境准备

```bash
# 创建虚拟环境 (推荐)
python -m venv venv
venv\Scripts\activate   # Windows

# 安装依赖
pip install -r requirements.txt
```

## 使用流程

### 1. 下载数据集

**方式 A：合并数据集（含深蹲 + 高抬腿补充，推荐）**

1. 下载 Roboflow 数据并合并深蹲：
```bash
set ROBOFLOW_API_KEY=你的API密钥
python scripts/download_and_merge_datasets.py
```

2. （可选）从 [Qualcomm 运动数据集](https://huggingface.co/datasets/Voxel51/qualcomm-exercise-video-dataset-benchmark) 提取高抬腿：
```bash
pip install fiftyone
python scripts/extract_high_jumps_from_qualcomm.py
python scripts/download_and_merge_datasets.py   # 再次运行以合并
```

合并后数据在 `dataset/combined/`，训练时自动优先使用。

**方式 B：仅基础数据集**

```bash
set ROBOFLOW_API_KEY=你的API密钥
Dl8IaqLHzbtUKM2lR0BO
python scripts/download_dataset.py
```

若 Fork 后项目名或工作区变化，可设置 `ROBOFLOW_WORKSPACE`、`ROBOFLOW_PROJECT`。

### 2. 训练模型

```bash
python scripts/train.py
```

#### TensorBoard 训练监控（已集成）

训练时默认启用 TensorBoard，可按以下步骤查看 Loss、mAP 等曲线：

**① 安装 TensorBoard**（若未安装）
```bash
pip install tensorboard
```
或安装全部依赖：`pip install -r requirements.txt`

**② 启动训练**
```bash
python scripts/train.py
```

**③ 另开一个终端，启动 TensorBoard**
```bash
cd D:\ai-exercise-train
.\venv\Scripts\activate
python -m tensorboard.main --logdir runs --port 6006
```
或双击运行 `scripts/start_tensorboard.bat`

**④ 打开浏览器访问**
```
http://localhost:6006
```

可查看：训练/验证 Loss、Precision、Recall、mAP50 等曲线。

关闭 TensorBoard：在运行 `tensorboard` 的终端按 `Ctrl+C`。

可选参数:
```bash
set EPOCHS=150
set BATCH=8
set TRAIN_NAME=exercise_train_v2   # 不同名称保留旧模型，不覆盖
set DEVICE=0                       # GPU，cpu=CPU
set MODEL=yolov8s.pt               # 默认 yolov8s（更大模型），可改为 yolov8n.pt
python scripts/train.py
```

训练完成后，模型位于 `runs/<TRAIN_NAME>/weights/best.pt`。  
详见 [使用说明.md](使用说明.md)。

### 3. 实时识别 (摄像头)

```bash
python run_camera.py
```

命令行参数:
- `--model path.pt` - 指定训练好的检测模型
- `--weight 70` - 体重 (kg)
- `--height 170` - 身高 (cm)
- `--camera 0` - 摄像头索引
- `--no-pose` - 不启用姿态估计/次数计数

按键:
- `q` - 退出
- `r` - 重置当前运动次数

## 项目结构

```
ai-exercise-train/
├── config/
│   └── exercise_config.yaml   # 用户信息、MET 值、姿态配置
├── dataset/                   # 下载后的数据集（combined 为训练用合并集）
├── docs/
│   └── 项目说明文档.md         # 各文件/代码说明、数据集数量、模型效果（详见此处）
├── runs/                      # 训练输出（如 exercise_train/weights/best.pt）
├── scripts/
│   ├── download_dataset.py    # 下载 Roboflow 单数据集
│   ├── download_and_merge_datasets.py  # 下载并合并多数据集
│   ├── extract_high_jumps_from_qualcomm.py  # 从 Qualcomm 提取高抬腿
│   ├── train.py               # YOLO 训练
│   ├── monitor_train.py       # 训练曲线监控
│   └── start_tensorboard.bat   # 启动 TensorBoard
├── src/
│   ├── exercise_utils.py      # 公共：配置、模型查找、单帧检测
│   ├── calorie_calculator.py  # 卡路里计算
│   └── rep_counter.py         # 次数计数
├── run_camera.py              # 摄像头实时推理
├── run_video.py               # 视频文件分析
├── run_server.py              # HTTP 服务（供小程序/后端）
├── requirements.txt
├── README.md
└── 使用说明.md                 # 详细使用与参数说明
```

## 配置说明

编辑 `config/exercise_config.yaml` 可修改:

- **user_profile**: 默认体重、身高
- **exercise_met_values**: 各运动的 MET 值 (可查 [Compendium of Physical Activities](https://pacompendium.com/))
- **rep_count_configs**: 各运动的姿态关键点与角度阈值，用于次数计数

## 数据集说明

本项目使用 [exercise-ekbld](https://universe.roboflow.com/activityrecognition-noyo4/exercise-ekbld) 数据集。若你使用其他运动数据集，需保证:

1. 导出格式为 YOLOv8
2. 在 `config/exercise_config.yaml` 中添加对应运动类型的 MET 与 rep 配置

## 注意事项

- 首次运行 `run_camera.py` 会下载姿态模型 `yolo11n-pose.pt`
- 卡路里为估算值，仅供参考
- 建议在光线充足、背景简单环境下使用，以获得更好识别效果


python run_video.py all.mp4 --conf 0.6 --stable-frames 4 -o all_result.mp4 


pip install fastapi uvicorn python-multipart
python run_server.py
# 默认端口 5000，可用 --port 5000 --model path.pt 指定