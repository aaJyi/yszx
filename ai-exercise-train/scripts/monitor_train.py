"""
训练过程实时监控 - 多折线图

在训练时另开终端运行，自动读取 results.csv 并绘制 Loss、mAP 等曲线
用法: python scripts/monitor_train.py [run_dir]
  不指定 run_dir 时自动查找最新一次训练
"""

import os
import sys
import time
from pathlib import Path

PROJECT_DIR = Path(__file__).resolve().parent.parent


def find_latest_results():
    """查找最新的 results.csv"""
    runs = PROJECT_DIR / "runs"
    candidates = list(runs.rglob("results.csv"))
    if not candidates:
        return None
    # 按修改时间取最新
    return max(candidates, key=lambda p: p.stat().st_mtime)


def plot_results(csv_path: Path, axs, live: bool = True):
    """读取 CSV 并绘制"""
    try:
        import pandas as pd
        import matplotlib.pyplot as plt
    except ImportError:
        print("请安装: pip install matplotlib pandas")
        return False

    df = pd.read_csv(csv_path)
    epoch = df["epoch"] if "epoch" in df.columns else range(1, len(df) + 1)
    n = len(epoch)

    # 定义要绘制的指标
    metrics = [
        ("train/box_loss", "train/cls_loss", "train/dfl_loss"),
        ("metrics/precision(B)", "metrics/recall(B)"),
        ("metrics/mAP50(B)", "metrics/mAP50-95(B)"),
    ]
    titles = ["Loss", "Precision & Recall", "mAP"]

    for i, (ax, cols, title) in enumerate(zip(axs.flat, metrics, titles)):
        ax.clear()
        for col in cols:
            if col in df.columns and df[col].notna().any():
                ax.plot(epoch, df[col], label=col.split("/")[-1].replace("(B)", ""), marker="o", markersize=3)
        ax.set_title(title)
        ax.legend(loc="best", fontsize=8)
        ax.grid(True, alpha=0.3)
        ax.set_xlabel("Epoch")

    plt.tight_layout()
    if live:
        plt.pause(0.1)
    return True


def main():
    run_dir = sys.argv[1] if len(sys.argv) > 1 else None
    csv_path = None

    if run_dir:
        p = Path(run_dir)
        csv_path = (p / "results.csv") if p.is_dir() else p
        if not csv_path.exists():
            csv_path = p / "results.csv" if p.is_dir() else None
    if not csv_path or not csv_path.exists():
        csv_path = find_latest_results()

    if not csv_path or not csv_path.exists():
        print("未找到 results.csv")
        print("请先启动训练: python scripts/train.py")
        print("或指定目录: python scripts/monitor_train.py runs/detect/exercise_train")
        return

    try:
        import matplotlib.pyplot as plt
        import pandas as pd
    except ImportError:
        print("请安装: pip install matplotlib pandas")
        return

    plt.ion()
    fig, axs = plt.subplots(1, 3, figsize=(12, 4))
    fig.suptitle(f"Training Monitor - {csv_path.parent.name}", fontsize=12)
    last_mtime = 0

    print(f"监控: {csv_path}")
    print("关闭图表窗口退出")
    print("-" * 50)

    while plt.fignum_exists(fig.number):
        try:
            mtime = csv_path.stat().st_mtime
            if mtime != last_mtime:
                last_mtime = mtime
                df = pd.read_csv(csv_path)
                epoch = df["epoch"] if "epoch" in df.columns else range(1, len(df) + 1)
                n = len(epoch)

                metrics = [
                    ("train/box_loss", "train/cls_loss", "train/dfl_loss"),
                    ("metrics/precision(B)", "metrics/recall(B)"),
                    ("metrics/mAP50(B)", "metrics/mAP50-95(B)"),
                ]
                titles = ["Loss", "Precision & Recall", "mAP"]

                for i, (ax, cols, title) in enumerate(zip(axs.flat, metrics, titles)):
                    ax.clear()
                    for col in cols:
                        if col in df.columns and df[col].notna().any():
                            lbl = col.split("/")[-1].replace("(B)", "").replace("-", " ")
                            ax.plot(epoch, df[col], label=lbl, marker="o", markersize=2)
                    ax.set_title(title)
                    ax.legend(loc="best", fontsize=7)
                    ax.grid(True, alpha=0.3)
                    ax.set_xlabel("Epoch")

                plt.tight_layout()
                fig.canvas.draw()
                fig.canvas.flush_events()
                print(f"  Epoch {n} 已更新")
        except Exception as e:
            print(f"读取异常: {e}")
        time.sleep(2)

    plt.ioff()
    plt.close()


if __name__ == "__main__":
    main()
