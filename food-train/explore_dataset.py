"""
Nutrition5k数据集探索脚本
功能：了解数据集结构，验证数据完整性
作者：AI助手
"""

import os
import pandas as pd
import numpy as np
from PIL import Image
import matplotlib.pyplot as plt
import seaborn as sns
from tqdm import tqdm
import json
import argparse

# 设置中文字体（可选）
plt.rcParams['font.sans-serif'] = ['SimHei', 'DejaVu Sans']
plt.rcParams['axes.unicode_minus'] = False


def explore_nutrition5k(base_path="nutrition5k_data"):
    """
    探索Nutrition5k数据集结构
    """
    print("=" * 60)
    print("Nutrition5k数据集探索")
    print("=" * 60)

    if not os.path.exists(base_path):
        print(f"❌ 数据集路径不存在: {base_path}")
        print("请确保数据集已下载并解压到正确位置")
        return None

    print(f"📁 数据集根目录: {base_path}")

    # 1. 列出所有文件和文件夹
    print("\n📁 目录结构:")
    for root, dirs, files in os.walk(base_path, topdown=True):
        level = root.replace(base_path, '').count(os.sep)
        indent = ' ' * 2 * level
        print(f"{indent}📂 {os.path.basename(root)}/" if level > 0 else f"📂 {root}")
        subindent = ' ' * 2 * (level + 1)
        for file in files[:5]:  # 只显示前5个文件
            if file.endswith(('.csv', '.json', '.txt')):
                print(f"{subindent}📄 {file}")
        if len(files) > 5:
            print(f"{subindent}... 还有 {len(files) - 5} 个文件")

    # 2. 查找CSV文件
    print("\n" + "=" * 60)
    print("查找和检查CSV文件")
    print("=" * 60)

    csv_files = []
    for root, dirs, files in os.walk(base_path):
        for file in files:
            if file.endswith('.csv'):
                csv_path = os.path.join(root, file)
                csv_files.append(csv_path)

    print(f"找到 {len(csv_files)} 个CSV文件:")

    csv_info = {}
    for csv_path in csv_files:
        try:
            # 尝试读取前几行
            df = pd.read_csv(csv_path, nrows=5)
            file_name = os.path.basename(csv_path)
            csv_info[file_name] = {
                'path': csv_path,
                'shape': (len(pd.read_csv(csv_path)), len(df.columns)),
                'columns': list(df.columns),
                'head': df.head(2)
            }
            print(f"\n📊 文件: {file_name}")
            print(f"   路径: {csv_path}")
            print(f"   形状: {csv_info[file_name]['shape']} (行×列)")
            print(f"   列名: {csv_info[file_name]['columns']}")
            print(f"   前2行:")
            print(df.head(2).to_string())
        except Exception as e:
            print(f"❌ 读取 {csv_path} 失败: {e}")

    # 3. 查找图片文件
    print("\n" + "=" * 60)
    print("查找和检查图片文件")
    print("=" * 60)

    image_extensions = ('.jpg', '.jpeg', '.png', '.JPG', '.JPEG', '.PNG')
    image_dirs = {}

    for root, dirs, files in os.walk(base_path):
        image_count = 0
        for file in files:
            if file.lower().endswith(image_extensions):
                image_count += 1

        if image_count > 0:
            rel_path = os.path.relpath(root, base_path)
            image_dirs[rel_path] = image_count
            if image_count <= 10:  # 如果数量少，列出文件名
                image_files = [f for f in files if f.lower().endswith(image_extensions)]
                print(f"📷 {rel_path}/ - {image_count}张图片")
                for img in image_files[:5]:
                    print(f"    - {img}")
            else:
                print(f"📷 {rel_path}/ - {image_count}张图片")

    # 4. 查找JSON文件
    print("\n" + "=" * 60)
    print("查找和检查JSON文件")
    print("=" * 60)

    json_files = []
    for root, dirs, files in os.walk(base_path):
        for file in files:
            if file.endswith('.json'):
                json_path = os.path.join(root, file)
                json_files.append(json_path)

    for json_path in json_files[:3]:  # 只检查前3个
        try:
            with open(json_path, 'r') as f:
                data = json.load(f)
                file_name = os.path.basename(json_path)
                print(f"\n📄 {file_name}")
                print(f"   路径: {json_path}")
                if isinstance(data, dict):
                    print(f"   键: {list(data.keys())[:5]}...")
                elif isinstance(data, list):
                    print(f"   列表长度: {len(data)}")
        except Exception as e:
            print(f"❌ 读取 {json_path} 失败: {e}")

    # 5. 统计总结
    print("\n" + "=" * 60)
    print("数据集统计总结")
    print("=" * 60)

    total_csv_size = 0
    for csv_name, info in csv_info.items():
        total_csv_size += info['shape'][0]

    total_images = sum(image_dirs.values())

    print(f"📈 数据集总览:")
    print(f"   CSV文件数量: {len(csv_info)}")
    print(f"   CSV总行数: {total_csv_size}")
    print(f"   图片文件夹数量: {len(image_dirs)}")
    print(f"   图片总数: {total_images}")
    print(f"   JSON文件数量: {len(json_files)}")

    # 6. 显示关键文件的详细信息
    print("\n" + "=" * 60)
    print("关键文件分析")
    print("=" * 60)

    # 寻找可能的营养数据文件
    nutrition_files = []
    for csv_name, info in csv_info.items():
        if any(keyword in csv_name.lower() for keyword in ['nutrition', 'calori', 'ingredient', 'dish', 'food']):
            nutrition_files.append((csv_name, info))

    if nutrition_files:
        print(f"找到 {len(nutrition_files)} 个可能的营养数据文件:")
        for csv_name, info in nutrition_files:
            print(f"\n📊 {csv_name}:")
            print(f"   形状: {info['shape']}")
            print(f"   前2行数据:")
            print(info['head'].to_string())
    else:
        print("未找到明显的营养数据文件")

    return {
        'base_path': base_path,
        'csv_info': csv_info,
        'image_dirs': image_dirs,
        'json_files': json_files
    }


def visualize_nutrition_data(csv_info):
    """
    可视化营养数据分布
    """
    print("\n" + "=" * 60)
    print("营养数据可视化")
    print("=" * 60)

    # 寻找包含营养值的CSV
    nutrition_csv = None
    for csv_name, info in csv_info.items():
        # 检查是否包含营养相关列
        columns_lower = [col.lower() for col in info['columns']]
        nutrition_keywords = ['calori', 'protein', 'fat', 'carb', 'energy', 'kcal']
        if any(keyword in col for keyword in nutrition_keywords for col in columns_lower):
            nutrition_csv = info['path']
            print(f"使用营养数据文件: {csv_name}")
            break

    if nutrition_csv:
        try:
            # 读取完整数据
            df = pd.read_csv(nutrition_csv)

            # 识别营养列
            nutrition_cols = []
            for col in df.columns:
                col_lower = col.lower()
                if any(keyword in col_lower for keyword in
                       ['calori', 'protein', 'fat', 'carb', 'energy', 'kcal', 'sugar', 'fiber', 'sodium']):
                    nutrition_cols.append(col)

            if len(nutrition_cols) > 0:
                print(f"找到营养列: {nutrition_cols}")

                # 创建可视化
                fig, axes = plt.subplots(2, 3, figsize=(15, 10))
                axes = axes.flatten()

                for i, col in enumerate(nutrition_cols[:6]):  # 最多显示6个
                    if i < len(axes):
                        ax = axes[i]

                        # 清理数据
                        data = pd.to_numeric(df[col], errors='coerce')
                        data_clean = data.dropna()

                        if len(data_clean) > 0:
                            # 直方图
                            ax.hist(data_clean, bins=30, alpha=0.7, color='skyblue', edgecolor='black')
                            ax.set_title(f'{col} 分布', fontsize=12)
                            ax.set_xlabel(col)
                            ax.set_ylabel('频数')

                            # 添加统计信息
                            stats_text = f'均值: {data_clean.mean():.1f}\n中位数: {data_clean.median():.1f}\n数量: {len(data_clean)}'
                            ax.text(0.7, 0.9, stats_text, transform=ax.transAxes, fontsize=9,
                                    verticalalignment='top', bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.5))

                plt.tight_layout()
                plt.savefig('nutrition_distribution.png', dpi=150, bbox_inches='tight')
                print("📈 营养分布图已保存: nutrition_distribution.png")

                # 显示相关性热图
                if len(nutrition_cols) > 1:
                    plt.figure(figsize=(10, 8))

                    # 提取营养数据
                    nutrition_data = df[nutrition_cols].apply(pd.to_numeric, errors='coerce')
                    corr_matrix = nutrition_data.corr()

                    # 创建热图
                    sns.heatmap(corr_matrix, annot=True, cmap='coolwarm', center=0,
                                square=True, linewidths=1, cbar_kws={"shrink": 0.8})
                    plt.title('营养成分相关性热图', fontsize=14)
                    plt.tight_layout()
                    plt.savefig('nutrition_correlation.png', dpi=150, bbox_inches='tight')
                    print("📈 营养相关性热图已保存: nutrition_correlation.png")

                plt.show()

        except Exception as e:
            print(f"❌ 可视化失败: {e}")
    else:
        print("未找到营养数据进行可视化")


def check_image_samples(base_path, image_dirs, num_samples=3):
    """
    检查图片样本
    """
    print("\n" + "=" * 60)
    print("图片样本检查")
    print("=" * 60)

    if not image_dirs:
        print("未找到图片文件夹")
        return

    # 选择第一个有图片的文件夹
    for rel_path, count in image_dirs.items():
        if count > 0:
            img_dir = os.path.join(base_path, rel_path)
            print(f"检查文件夹: {rel_path} ({count}张图片)")

            # 获取图片文件列表
            image_files = []
            for file in os.listdir(img_dir):
                if file.lower().endswith(('.jpg', '.jpeg', '.png')):
                    image_files.append(os.path.join(img_dir, file))

            if image_files:
                print(f"随机显示 {min(num_samples, len(image_files))} 张图片样本:")

                # 随机选择图片
                np.random.seed(42)
                sample_files = np.random.choice(image_files, min(num_samples, len(image_files)), replace=False)

                fig, axes = plt.subplots(1, len(sample_files), figsize=(4 * len(sample_files), 4))
                if len(sample_files) == 1:
                    axes = [axes]

                for idx, img_path in enumerate(sample_files):
                    try:
                        img = Image.open(img_path)
                        axes[idx].imshow(img)
                        axes[idx].set_title(os.path.basename(img_path), fontsize=10)
                        axes[idx].axis('off')

                        # 显示图片信息
                        print(f"  {os.path.basename(img_path)}: {img.size}像素, {img.mode}模式")
                    except Exception as e:
                        print(f"❌ 无法加载图片 {img_path}: {e}")

                plt.tight_layout()
                plt.savefig('image_samples.png', dpi=150, bbox_inches='tight')
                print("📷 图片样本已保存: image_samples.png")
                plt.show()
                break
    else:
        print("未找到可用的图片文件")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Nutrition5k 数据集探索")
    parser.add_argument(
        "--data-dir",
        type=str,
        default="nutrition5k_data",
        help="Nutrition5k 数据目录（默认: nutrition5k_data）"
    )
    args = parser.parse_args()

    print("开始探索Nutrition5k数据集...")
    print(f"使用数据目录: {args.data_dir}")

    # 探索数据集
    dataset_info = explore_nutrition5k(args.data_dir)

    if dataset_info:
        # 可视化营养数据
        visualize_nutrition_data(dataset_info['csv_info'])

        # 检查图片样本
        check_image_samples(dataset_info['base_path'], dataset_info['image_dirs'])

        print("\n" + "=" * 60)
        print("探索完成！")
        print("=" * 60)
        print("\n下一步：运行数据预处理脚本")
        print("python preprocess_nutrition5k.py")