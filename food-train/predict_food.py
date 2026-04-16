"""
食物营养分析推理脚本 - 兼容PyTorch 2.6+版本
修复了weights_only加载错误
"""
import matplotlib
matplotlib.rcParams['font.sans-serif'] = ['SimHei']  # 使用黑体
matplotlib.rcParams['axes.unicode_minus'] = False  # 解决负号显示问题
import torch
import torch.nn as nn
from torchvision import models, transforms
import numpy as np
from PIL import Image
import argparse
import os
import json
import matplotlib.pyplot as plt
import pandas as pd
from pathlib import Path
import warnings
warnings.filterwarnings('ignore')

class FoodNutritionPredictor:
    """食物营养预测器 - 兼容PyTorch 2.6+版本"""

    def __init__(self, model_path='nutrition5k_final_model.pth'):
        """初始化预测器"""
        print("🔧 加载食物营养分析模型...")
        print(f"PyTorch版本: {torch.__version__}")

        # 检查模型文件
        if not os.path.exists(model_path):
            raise FileNotFoundError(f"模型文件不存在: {model_path}")

        # 加载模型检查点 - 修复PyTorch 2.6+兼容性问题
        try:
            # 方法1: 先尝试使用weights_only=False
            checkpoint = torch.load(model_path, map_location='cpu', weights_only=False)
            print("✅ 使用weights_only=False加载模型成功")
        except TypeError as e:
            # 如果参数不支持，尝试不带weights_only参数
            if "weights_only" in str(e):
                print("⚠️  当前PyTorch版本不支持weights_only参数，使用旧方法加载")
                checkpoint = torch.load(model_path, map_location='cpu')
            else:
                raise e
        except Exception as e:
            # 如果还有其他错误，尝试使用绝对安全的加载方式
            print(f"⚠️  标准加载失败: {e}")
            print("尝试使用pickle直接加载...")
            import pickle
            with open(model_path, 'rb') as f:
                checkpoint = pickle.load(f)

        # 获取配置信息
        self.nutrient_names = checkpoint.get('nutrient_names', [
            'energy_kcal', 'protein_g', 'fat_g', 'carbs_g',
            'fiber_g', 'sugar_g', 'sodium_mg'
        ])

        self.config = checkpoint.get('config', {})
        self.input_size = checkpoint.get('input_size', (224, 224))

        # 获取归一化参数
        self.mean = checkpoint.get('mean', [0.485, 0.456, 0.406])
        self.std = checkpoint.get('std', [0.229, 0.224, 0.225])

        print(f"📊 预测 {len(self.nutrient_names)} 种营养成分:")
        for i, name in enumerate(self.nutrient_names):
            print(f"  {i+1}. {name}")

        # 创建模型架构
        backbone_name = self.config.get('backbone', 'resnet50')
        self.model = self._create_model(backbone_name, len(self.nutrient_names))

        # 加载权重
        self.model.load_state_dict(checkpoint['model_state_dict'])
        self.model.eval()
        print("✅ 模型加载完成")

        # 图片预处理
        self.transform = transforms.Compose([
            transforms.Resize(self.input_size),
            transforms.ToTensor(),
            transforms.Normalize(mean=self.mean, std=self.std)
        ])

    def _create_model(self, backbone_name, num_nutrients):
        """根据配置创建模型架构（和训练脚本完全一致）"""
        if backbone_name == 'resnet18':
            backbone = models.resnet18(weights=None)
            num_features = backbone.fc.in_features
            backbone = nn.Sequential(*list(backbone.children())[:-2])
        elif backbone_name == 'resnet34':
            backbone = models.resnet34(weights=None)
            num_features = backbone.fc.in_features
            backbone = nn.Sequential(*list(backbone.children())[:-2])
        elif backbone_name == 'resnet50':
            backbone = models.resnet50(weights=None)
            num_features = backbone.fc.in_features
            backbone = nn.Sequential(*list(backbone.children())[:-2])
        elif backbone_name == 'efficientnet_b0':
            backbone = models.efficientnet_b0(weights=None)
            num_features = backbone.classifier[1].in_features
            backbone = backbone.features
        else:
            raise ValueError(f"不支持的骨干网络: {backbone_name}")

        # 全局平均池化
        global_pool = nn.AdaptiveAvgPool2d((1, 1))

        # 回归头（和训练脚本100%一致）
        regressor = nn.Sequential(
            nn.Flatten(),
            nn.Linear(num_features, 512),
            nn.BatchNorm1d(512),
            nn.ReLU(inplace=True),
            nn.Dropout(0.5),

            nn.Linear(512, 256),
            nn.BatchNorm1d(256),
            nn.ReLU(inplace=True),
            nn.Dropout(0.4),

            nn.Linear(256, 128),
            nn.BatchNorm1d(128),
            nn.ReLU(inplace=True),
            nn.Dropout(0.3),

            nn.Linear(128, num_nutrients)
        )

        # 创建模型
        class InferenceModel(nn.Module):
            def __init__(self, backbone, global_pool, regressor):
                super().__init__()
                self.backbone = backbone
                self.global_pool = global_pool
                self.regressor = regressor

            def forward(self, x):
                features = self.backbone(x)
                features = self.global_pool(features)
                return self.regressor(features)

        return InferenceModel(backbone, global_pool, regressor)

    def predict_image(self, image_path):
        """预测单张图片的营养成分"""
        print(f"\n🔍 分析图片: {image_path}")

        # 加载和预处理图片
        try:
            img = Image.open(image_path).convert('RGB')
        except Exception as e:
            print(f"❌ 无法加载图片: {e}")
            return None

        # 应用变换
        img_tensor = self.transform(img).unsqueeze(0)

        # 预测
        with torch.no_grad():
            predictions = self.model(img_tensor)

        # 转换为字典
        results = {}
        predictions = predictions[0].numpy()

        for i, name in enumerate(self.nutrient_names):
            results[name] = float(predictions[i])

        return results, img

    def visualize_prediction(self, results, img, food_name="食物", save_path=None):
        """可视化预测结果"""
        fig = plt.figure(figsize=(15, 8))

        # 1. 显示原图
        ax1 = plt.subplot(2, 3, 1)
        ax1.imshow(img)
        ax1.set_title(f'📷 {food_name}', fontsize=12)
        ax1.axis('off')

        # 2. 营养值表格
        ax2 = plt.subplot(2, 3, 2)
        ax2.axis('off')

        table_data = []
        for name, value in results.items():
            # 格式化显示
            display_name = name.replace('_', ' ').title()
            if 'kcal' in name:
                display_value = f"{value:.0f} kcal"
            elif 'mg' in name:
                display_value = f"{value:.0f} mg"
            else:
                display_value = f"{value:.1f} g"
            table_data.append([display_name, display_value])

        # 创建表格
        table = ax2.table(cellText=table_data,
                         colLabels=['营养成分', '含量'],
                         cellLoc='left',
                         loc='center',
                         colWidths=[0.4, 0.4])
        table.auto_set_font_size(False)
        table.set_fontsize(10)
        table.scale(1, 1.8)

        # 3. 能量来源饼图
        ax3 = plt.subplot(2, 3, 3)

        # 计算三大营养素供能
        protein_kcal = results.get('protein_g', 0) * 4
        fat_kcal = results.get('fat_g', 0) * 9
        carbs_kcal = results.get('carbs_g', 0) * 4
        total_kcal = protein_kcal + fat_kcal + carbs_kcal

        if total_kcal > 0:
            labels = ['蛋白质', '脂肪', '碳水化合物']
            sizes = [protein_kcal, fat_kcal, carbs_kcal]
            colors = ['#ff6b6b', '#4ecdc4', '#45b7d1']

            wedges, texts, autotexts = ax3.pie(sizes, labels=labels, colors=colors,
                                              autopct='%1.1f%%', startangle=90)

            # 美化百分比文本
            for autotext in autotexts:
                autotext.set_color('white')
                autotext.set_fontweight('bold')

            ax3.axis('equal')
            ax3.set_title('能量来源分布', fontsize=12)
        else:
            ax3.text(0.5, 0.5, '能量数据不足', ha='center', va='center')
            ax3.axis('off')

        # 4. 营养值柱状图
        ax4 = plt.subplot(2, 3, 4)

        # 选择主要营养成分显示
        main_nutrients = ['energy_kcal', 'protein_g', 'fat_g', 'carbs_g']
        display_names = ['能量(kcal)', '蛋白质(g)', '脂肪(g)', '碳水(g)']
        values = [results.get(n, 0) for n in main_nutrients]

        colors = ['#ff9a76', '#ffb347', '#ffcc33', '#c5e384']
        bars = ax4.bar(range(len(values)), values, color=colors)

        ax4.set_xlabel('营养成分')
        ax4.set_ylabel('含量')
        ax4.set_title('主要营养成分', fontsize=12)
        ax4.set_xticks(range(len(display_names)))
        ax4.set_xticklabels(display_names, rotation=45)

        # 在柱子上添加数值
        for bar, value in zip(bars, values):
            height = bar.get_height()
            ax4.text(bar.get_x() + bar.get_width()/2., height + 0.1,
                    f'{value:.1f}', ha='center', va='bottom', fontsize=9)

        # 5. 营养评分雷达图
        ax5 = plt.subplot(2, 3, 5, projection='polar')

        # 计算简单营养评分
        scores = self._calculate_nutrition_scores(results)
        score_labels = ['蛋白质', '脂肪', '碳水', '膳食纤维', '糖']

        angles = np.linspace(0, 2 * np.pi, len(scores), endpoint=False).tolist()
        scores.append(scores[0])  # 闭合图形
        angles.append(angles[0])

        ax5.plot(angles, scores, 'o-', linewidth=2, color='#6a5acd')
        ax5.fill(angles, scores, alpha=0.25, color='#6a5acd')
        ax5.set_thetagrids(np.degrees(angles[:-1]), score_labels)
        ax5.set_ylim(0, 10)
        ax5.set_title('营养评分雷达图', fontsize=12)

        # 6. 营养建议
        ax6 = plt.subplot(2, 3, 6)
        ax6.axis('off')

        advice_text = self._generate_nutrition_advice(results)
        ax6.text(0.1, 0.5, advice_text, fontsize=9, va='center',
                bbox=dict(boxstyle='round', facecolor='#f0f8ff', alpha=0.8))
        ax6.set_title('营养建议', fontsize=12)

        plt.suptitle(f'🍎 食物营养分析报告 - {food_name}', fontsize=16, y=1.02)
        plt.tight_layout()

        if save_path:
            plt.savefig(save_path, dpi=150, bbox_inches='tight')
            print(f"📈 分析报告已保存: {save_path}")

        plt.show()

        # 打印详细分析
        self._print_detailed_analysis(results)

        return fig

    def _calculate_nutrition_scores(self, results):
        """计算营养评分"""
        scores = []

        # 蛋白质评分（每100kcal应有3-4g蛋白质）
        energy = max(results.get('energy_kcal', 1), 1)
        protein_per_100kcal = results.get('protein_g', 0) / energy * 100
        protein_score = min(10, protein_per_100kcal / 4 * 10)
        scores.append(protein_score)

        # 脂肪评分（推荐20-30%能量来自脂肪）
        fat_kcal = results.get('fat_g', 0) * 9
        fat_percent = fat_kcal / energy * 100
        fat_score = 10 - abs(fat_percent - 25) / 5
        scores.append(max(0, min(10, fat_score)))

        # 碳水评分（推荐45-65%能量来自碳水）
        carbs_kcal = results.get('carbs_g', 0) * 4
        carbs_percent = carbs_kcal / energy * 100
        carbs_score = 10 - abs(carbs_percent - 55) / 10
        scores.append(max(0, min(10, carbs_score)))

        # 膳食纤维评分（每1000kcal应有14g纤维）
        fiber_per_1000kcal = results.get('fiber_g', 0) / energy * 1000
        fiber_score = min(10, fiber_per_1000kcal / 14 * 10)
        scores.append(fiber_score)

        # 糖评分（推荐<10%能量来自糖）
        sugar_kcal = results.get('sugar_g', 0) * 4
        sugar_percent = sugar_kcal / energy * 100
        sugar_score = max(0, 10 - sugar_percent)
        scores.append(sugar_score)

        return scores

    def _generate_nutrition_advice(self, results):
        """生成营养建议"""
        advice_lines = []
        energy = results.get('energy_kcal', 0)

        if energy > 0:
            # 能量评估
            if energy < 100:
                advice_lines.append("⚡ 能量较低，适合加餐")
            elif energy < 300:
                advice_lines.append("⚡ 能量适中，适合一餐")
            elif energy < 500:
                advice_lines.append("⚡ 能量较高，注意控制份量")
            else:
                advice_lines.append("⚡ 能量很高，建议分次食用")

            # 蛋白质评估
            protein = results.get('protein_g', 0)
            protein_per_100kcal = protein / energy * 100
            if protein_per_100kcal < 3:
                advice_lines.append("💪 蛋白质含量偏低")
            elif protein_per_100kcal > 8:
                advice_lines.append("💪 蛋白质含量丰富")

            # 脂肪评估
            fat_percent = results.get('fat_g', 0) * 9 / energy * 100
            if fat_percent < 20:
                advice_lines.append("🧈 脂肪供能比较低")
            elif fat_percent > 35:
                advice_lines.append("🧈 脂肪含量较高")

            # 膳食纤维评估
            fiber = results.get('fiber_g', 0)
            if fiber < 2:
                advice_lines.append("🌾 膳食纤维不足")
            elif fiber > 5:
                advice_lines.append("🌾 膳食纤维丰富")

        if len(advice_lines) == 0:
            advice_lines.append("请确保均衡饮食，多样化摄入")

        return "\n".join([f"• {line}" for line in advice_lines])

    def _print_detailed_analysis(self, results):
        """打印详细分析"""
        print("\n" + "="*60)
        print("📊 详细营养分析")
        print("="*60)

        for name, value in results.items():
            display_name = name.replace('_', ' ').title()
            if 'kcal' in name:
                print(f"  🔥 {display_name}: {value:.0f} 千卡")
            elif 'mg' in name:
                print(f"  🧂 {display_name}: {value:.0f} 毫克")
            else:
                print(f"  ⚖️  {display_name}: {value:.1f} 克")

        # 计算供能比例
        energy = results.get('energy_kcal', 0)
        if energy > 0:
            protein_kcal = results.get('protein_g', 0) * 4
            fat_kcal = results.get('fat_g', 0) * 9
            carbs_kcal = results.get('carbs_g', 0) * 4

            print(f"\n⚡ 能量来源:")
            print(f"  蛋白质: {protein_kcal:.0f} kcal ({protein_kcal/energy*100:.1f}%)")
            print(f"  脂肪: {fat_kcal:.0f} kcal ({fat_kcal/energy*100:.1f}%)")
            print(f"  碳水: {carbs_kcal:.0f} kcal ({carbs_kcal/energy*100:.1f}%)")

    def batch_predict(self, image_dir, output_csv='batch_predictions.csv'):
        """批量预测文件夹中的图片"""
        print(f"\n📁 批量处理目录: {image_dir}")

        # 支持的图片格式
        image_extensions = ('.jpg', '.jpeg', '.png', '.bmp', '.tiff')

        # 查找图片文件
        image_files = []
        for ext in image_extensions:
            image_files.extend(Path(image_dir).glob(f'*{ext}'))
            image_files.extend(Path(image_dir).glob(f'*{ext.upper()}'))

        print(f"找到 {len(image_files)} 张图片")

        results_list = []

        for img_path in image_files:
            print(f"处理: {img_path.name}")

            result, _ = self.predict_image(str(img_path))
            if result:
                result['image_name'] = img_path.name
                result['image_path'] = str(img_path)
                results_list.append(result)

        # 保存到CSV
        if results_list:
            df = pd.DataFrame(results_list)
            df.to_csv(output_csv, index=False, encoding='utf-8')
            print(f"\n✅ 批量预测完成，保存到: {output_csv}")
            print(f"   处理了 {len(results_list)} 张图片")

            # 生成统计报告
            self._generate_batch_report(df, output_csv.replace('.csv', '_report.txt'))

        return results_list

    def _generate_batch_report(self, df, report_path):
        """生成批量预测统计报告"""
        with open(report_path, 'w', encoding='utf-8') as f:
            f.write("="*60 + "\n")
            f.write("批量食物营养分析报告\n")
            f.write("="*60 + "\n\n")

            f.write(f"分析图片数量: {len(df)}\n")
            f.write(f"分析时间: {pd.Timestamp.now()}\n\n")

            f.write("📊 营养统计摘要:\n")
            for col in self.nutrient_names:
                if col in df.columns:
                    data = df[col]
                    f.write(f"\n{col.replace('_', ' ').title()}:\n")
                    f.write(f"  最小值: {data.min():.1f}\n")
                    f.write(f"  最大值: {data.max():.1f}\n")
                    f.write(f"  平均值: {data.mean():.1f}\n")
                    f.write(f"  中位数: {data.median():.1f}\n")

            # 找出最高和最低能量的食物
            if 'energy_kcal' in df.columns:
                f.write("\n🔥 能量分析:\n")
                max_energy_idx = df['energy_kcal'].idxmax()
                min_energy_idx = df['energy_kcal'].idxmin()

                f.write(f"  最高能量: {df.loc[max_energy_idx, 'image_name']} "
                       f"({df.loc[max_energy_idx, 'energy_kcal']:.0f} kcal)\n")
                f.write(f"  最低能量: {df.loc[min_energy_idx, 'image_name']} "
                       f"({df.loc[min_energy_idx, 'energy_kcal']:.0f} kcal)\n")

            f.write("\n" + "="*60 + "\n")

        print(f"📄 统计报告已保存: {report_path}")

# ==================== 主程序 ====================

def main():
    """主函数"""
    parser = argparse.ArgumentParser(description='食物营养分析')
    parser.add_argument('--image', type=str, help='要分析的图片路径')
    parser.add_argument('--batch', type=str, help='批量处理图片目录')
    parser.add_argument('--model', type=str, default='nutrition5k_final_model.pth',
                       help='模型路径 (默认: nutrition5k_final_model.pth)')
    parser.add_argument('--output', type=str, default='nutrition_analysis.png',
                       help='结果图片保存路径')
    parser.add_argument('--list', action='store_true', help='列出支持的营养成分')

    args = parser.parse_args()

    try:
        # 初始化预测器
        predictor = FoodNutritionPredictor(args.model)

        if args.list:
            # 列出支持的营养成分
            print("\n📋 支持的营养成分:")
            for i, name in enumerate(predictor.nutrient_names, 1):
                print(f"  {i:2d}. {name}")
            return

        if args.batch:
            # 批量处理模式
            if not os.path.exists(args.batch):
                print(f"❌ 目录不存在: {args.batch}")
                return

            results = predictor.batch_predict(args.batch, 'batch_results.csv')

        elif args.image:
            # 单张图片模式
            if not os.path.exists(args.image):
                print(f"❌ 图片不存在: {args.image}")
                return

            # 预测
            results, img = predictor.predict_image(args.image)
            if results:
                # 获取食物名称
                food_name = os.path.splitext(os.path.basename(args.image))[0]
                # 可视化
                predictor.visualize_prediction(results, img, food_name, args.output)

        else:
            # 如果没有参数，显示帮助并测试示例
            parser.print_help()

            print("\n" + "="*60)
            print("示例用法:")
            print("="*60)
            print("1. 分析单张图片:")
            print("   python predict_food_fixed.py --image apple.jpg")
            print("\n2. 批量分析文件夹:")
            print("   python predict_food_fixed.py --batch ./food_images/")
            print("\n3. 使用不同模型:")
            print("   python predict_food_fixed.py --image food.jpg --model best_model.pth")
            print("\n4. 自定义输出:")
            print("   python predict_food_fixed.py --image food.jpg --output result.png")
            print("\n5. 列出支持分析的营养成分:")
            print("   python predict_food_fixed.py --list")

    except FileNotFoundError as e:
        print(f"❌ 错误: {e}")
        print("请确保模型文件存在，或重新训练模型")
    except Exception as e:
        print(f"❌ 运行出错: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    # 检查必要的库
    try:
        import pandas as pd
    except ImportError:
        print("安装pandas: pip install pandas")
        exit(1)

    main()