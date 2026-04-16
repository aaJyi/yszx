"""
一键流程：下载数据集（可选）-> 预处理 -> 训练
"""

import argparse
import subprocess
import sys


def run_step(command):
    """执行一个步骤命令，失败即退出。"""
    print(f"\n>>> 执行: {' '.join(command)}")
    result = subprocess.run(command)
    if result.returncode != 0:
        raise RuntimeError(f"命令执行失败: {' '.join(command)}")


def main():
    parser = argparse.ArgumentParser(description="Nutrition5k 一键下载与训练")
    parser.add_argument(
        "--data-dir",
        type=str,
        default="nutrition5k_data",
        help="原始数据目录（默认: nutrition5k_data）"
    )
    parser.add_argument(
        "--output-dir",
        type=str,
        default="processed_nutrition5k",
        help="预处理输出目录（默认: processed_nutrition5k）"
    )
    parser.add_argument(
        "--download",
        action="store_true",
        help="当 data-dir 不存在时通过 kagglehub 自动下载"
    )
    parser.add_argument(
        "--artifact-dir",
        type=str,
        default="training_outputs",
        help="训练产物输出目录（默认: training_outputs）"
    )
    parser.add_argument(
        "--batch-size",
        type=int,
        default=16,
        help="训练 batch size（默认: 16）"
    )
    args = parser.parse_args()

    try:
        preprocess_cmd = [
            sys.executable,
            "preprocess_nutrition5k.py",
            "--data-dir",
            args.data_dir,
            "--output-dir",
            args.output_dir,
        ]
        if args.download:
            preprocess_cmd.append("--download")

        run_step(preprocess_cmd)
        run_step([
            sys.executable,
            "train_nutrition_model.py",
            "--data-dir",
            args.output_dir,
            "--artifact-dir",
            args.artifact_dir,
            "--batch-size",
            str(args.batch_size),
        ])

        print("\n✅ 全流程完成：预处理 + 训练")
    except Exception as e:
        print(f"\n❌ 流程失败: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
