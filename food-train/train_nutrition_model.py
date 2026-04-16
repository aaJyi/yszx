"""
Nutrition5k模型训练脚本 - 修正版
功能：训练食物营养分析模型
作者：AI助手
"""

import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import Dataset, DataLoader
import torchvision.transforms as transforms
from torchvision import models
import pandas as pd
import numpy as np
from PIL import Image
import os
from tqdm import tqdm
import matplotlib.pyplot as plt
import seaborn as sns
from sklearn.metrics import mean_absolute_error, r2_score
import argparse
import sys
import warnings
warnings.filterwarnings('ignore')

# 设置随机种子
torch.manual_seed(42)
np.random.seed(42)

# 设备设置
device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
print(f"使用设备: {device}")
if torch.cuda.is_available():
    print(f"GPU: {torch.cuda.get_device_name(0)}")

# ==================== 1. 自定义数据集 ====================

class NutritionDataset(Dataset):
    """食物营养数据集"""

    def __init__(self, csv_path, img_dir, transform=None, img_size=(224, 224), is_train=True):
        self.df = pd.read_csv(csv_path)
        self.img_dir = img_dir
        self.img_size = img_size
        self.is_train = is_train

        # 营养列
        self.nutrient_cols = [
            'energy_kcal', 'protein_g', 'fat_g', 'carbs_g',
            'fiber_g', 'sugar_g', 'sodium_mg'
        ]

        # 确保所有列都存在
        for col in self.nutrient_cols:
            if col not in self.df.columns:
                print(f"警告: 列 {col} 不存在于数据中，将使用0填充")
                self.df[col] = 0.0

        # 数据预处理
        if transform is None:
            if is_train:
                # 训练集使用数据增强
                self.transform = transforms.Compose([
                    transforms.Resize((256, 256)),
                    transforms.RandomCrop(img_size),
                    transforms.RandomHorizontalFlip(p=0.5),
                    transforms.ColorJitter(brightness=0.2, contrast=0.2, saturation=0.2),
                    transforms.RandomRotation(10),
                    transforms.ToTensor(),
                    transforms.Normalize(mean=[0.485, 0.456, 0.406],
                                       std=[0.229, 0.224, 0.225])
                ])
            else:
                # 验证/测试集不使用数据增强
                self.transform = transforms.Compose([
                    transforms.Resize(img_size),
                    transforms.ToTensor(),
                    transforms.Normalize(mean=[0.485, 0.456, 0.406],
                                       std=[0.229, 0.224, 0.225])
                ])
        else:
            self.transform = transform

        print(f"📊 数据集: {len(self.df)} 个样本")
        print(f"📷 图片目录: {self.img_dir}")

    def __len__(self):
        return len(self.df)

    def __getitem__(self, idx):
        row = self.df.iloc[idx]

        # 加载图片
        img_path = os.path.join(self.img_dir, str(row['image_path']))
        try:
            img = Image.open(img_path).convert('RGB')
        except Exception as e:
            # 如果图片加载失败，使用占位图
            print(f"⚠️ 无法加载图片 {img_path}: {e}")
            img = Image.new('RGB', self.img_size, color=(128, 128, 128))

        # 应用变换
        if self.transform:
            img = self.transform(img)

        # 获取营养值
        nutrients = []
        for col in self.nutrient_cols:
            value = float(row[col])
            nutrients.append(value)

        nutrients = torch.tensor(nutrients, dtype=torch.float32)

        return img, nutrients

# ==================== 2. 模型定义 ====================

class NutritionModel(nn.Module):
    """食物营养分析模型"""

    def __init__(self, num_nutrients=7, backbone_name='resnet50', pretrained=True, dropout_rate=0.5):
        super(NutritionModel, self).__init__()

        def build_backbone(name, use_pretrained):
            # 新版 torchvision 使用 weights 参数；遇到网络/哈希问题时会在外层回退
            if name == 'resnet18':
                weights = models.ResNet18_Weights.DEFAULT if use_pretrained else None
                return models.resnet18(weights=weights)
            if name == 'resnet34':
                weights = models.ResNet34_Weights.DEFAULT if use_pretrained else None
                return models.resnet34(weights=weights)
            if name == 'resnet50':
                weights = models.ResNet50_Weights.DEFAULT if use_pretrained else None
                return models.resnet50(weights=weights)
            if name == 'efficientnet_b0':
                weights = models.EfficientNet_B0_Weights.DEFAULT if use_pretrained else None
                return models.efficientnet_b0(weights=weights)
            raise ValueError(f"不支持的骨干网络: {name}")

        # 选择骨干网络
        try:
            backbone = build_backbone(backbone_name, pretrained)
        except RuntimeError as e:
            # 典型场景：预训练权重下载损坏导致 invalid hash value
            if pretrained and "invalid hash value" in str(e).lower():
                print("⚠️ 预训练权重校验失败，自动回退为不使用预训练权重继续训练。")
                backbone = build_backbone(backbone_name, False)
            else:
                raise

        if backbone_name == 'resnet18':
            num_features = backbone.fc.in_features
            self.backbone = nn.Sequential(*list(backbone.children())[:-2])
        elif backbone_name == 'resnet34':
            num_features = backbone.fc.in_features
            self.backbone = nn.Sequential(*list(backbone.children())[:-2])
        elif backbone_name == 'resnet50':
            num_features = backbone.fc.in_features
            self.backbone = nn.Sequential(*list(backbone.children())[:-2])
        elif backbone_name == 'efficientnet_b0':
            num_features = backbone.classifier[1].in_features
            self.backbone = backbone.features
        else:
            raise ValueError(f"不支持的骨干网络: {backbone_name}")

        # 添加全局平均池化
        self.global_pool = nn.AdaptiveAvgPool2d((1, 1))

        # 回归头
        self.regressor = nn.Sequential(
            nn.Flatten(),
            nn.Linear(num_features, 512),
            nn.BatchNorm1d(512),
            nn.ReLU(inplace=True),
            nn.Dropout(dropout_rate),

            nn.Linear(512, 256),
            nn.BatchNorm1d(256),
            nn.ReLU(inplace=True),
            nn.Dropout(dropout_rate * 0.8),

            nn.Linear(256, 128),
            nn.BatchNorm1d(128),
            nn.ReLU(inplace=True),
            nn.Dropout(dropout_rate * 0.6),

            nn.Linear(128, num_nutrients)
        )

    def forward(self, x):
        features = self.backbone(x)
        features = self.global_pool(features)
        return self.regressor(features)

# ==================== 3. 训练函数 ====================

def train_epoch(model, dataloader, criterion, optimizer, device, epoch, num_epochs, nutrient_names):
    """训练一个epoch"""
    model.train()
    running_loss = 0.0
    all_outputs = []
    all_targets = []

    pbar = tqdm(dataloader, desc=f'训练 Epoch {epoch+1}/{num_epochs}')
    for batch_idx, (images, nutrients) in enumerate(pbar):
        images = images.to(device)
        nutrients = nutrients.to(device)

        # 前向传播
        outputs = model(images)
        loss = criterion(outputs, nutrients)

        # 反向传播
        optimizer.zero_grad()
        loss.backward()
        optimizer.step()

        # 统计
        running_loss += loss.item() * images.size(0)

        # 保存结果用于计算指标
        all_outputs.append(outputs.detach().cpu())
        all_targets.append(nutrients.detach().cpu())

        # 修正的进度条更新 - 使用正确的字符串格式化
        pbar.set_postfix({'loss': f"{loss.item():.4f}"})  # 修正点：使用f-string格式化

        # 更新进度条
        # 每100个batch显示一次详细信息
        if batch_idx % 100 == 0:
            pbar.set_postfix({
                'loss': f"{loss.item():.4f}",
                'lr': f"{optimizer.param_groups[0]['lr']:.6f}"
            })

    # 计算epoch指标
    epoch_loss = running_loss / len(dataloader.dataset)

    if len(all_outputs) > 0:
        all_outputs = torch.cat(all_outputs, dim=0)
        all_targets = torch.cat(all_targets, dim=0)
        mae_per_nutrient = torch.abs(all_outputs - all_targets).mean(dim=0).numpy()
    else:
        mae_per_nutrient = np.zeros(len(nutrient_names))

    return epoch_loss, mae_per_nutrient

def evaluate(model, dataloader, criterion, device, nutrient_names):
    """评估模型"""
    model.eval()
    running_loss = 0.0
    all_outputs = []
    all_targets = []

    with torch.no_grad():
        for images, nutrients in tqdm(dataloader, desc='评估'):
            images = images.to(device)
            nutrients = nutrients.to(device)

            outputs = model(images)
            loss = criterion(outputs, nutrients)

            running_loss += loss.item() * images.size(0)

            all_outputs.append(outputs.cpu())
            all_targets.append(nutrients.cpu())

    # 计算指标
    avg_loss = running_loss / len(dataloader.dataset)

    if len(all_outputs) > 0:
        all_outputs = torch.cat(all_outputs, dim=0)
        all_targets = torch.cat(all_targets, dim=0)
        # MAE
        mae_per_nutrient = torch.abs(all_outputs - all_targets).mean(dim=0).numpy()

        # R²分数
        r2_per_nutrient = []
        for i in range(all_outputs.shape[1]):
            r2 = r2_score(all_targets[:, i].numpy(), all_outputs[:, i].numpy())
            r2_per_nutrient.append(r2)
    else:
        mae_per_nutrient = np.zeros(len(nutrient_names))
        r2_per_nutrient = np.zeros(len(nutrient_names))

    return avg_loss, mae_per_nutrient, r2_per_nutrient, all_outputs, all_targets

# ==================== 4. 可视化函数 ====================

def visualize_results(train_losses, val_losses, test_outputs, test_targets, nutrient_names, artifact_dir):
    """可视化训练结果"""
    print("\n📈 生成可视化结果...")

    # 1. 训练曲线
    plt.figure(figsize=(12, 10))

    # 损失曲线
    plt.subplot(2, 2, 1)
    epochs = range(1, len(train_losses) + 1)
    plt.plot(epochs, train_losses, 'b-', label='训练损失', linewidth=2)
    plt.plot(epochs, val_losses, 'r-', label='验证损失', linewidth=2)
    plt.xlabel('训练轮次')
    plt.ylabel('损失 (MSE)')
    plt.title('训练和验证损失曲线')
    plt.legend()
    plt.grid(True, alpha=0.3)

    # 2. 预测 vs 真实值散点图
    plt.subplot(2, 2, 2)
    for i, name in enumerate(nutrient_names[:4]):  # 只显示前4个
        outputs = test_outputs[:, i].numpy()
        targets = test_targets[:, i].numpy()

        # 计算R²
        r2 = r2_score(targets, outputs)

        plt.scatter(targets, outputs, alpha=0.5, label=f'{name} (R²={r2:.3f})')

    # 添加对角线
    if len(test_outputs) > 0:
        min_val = min(test_targets.min(), test_outputs.min())
        max_val = max(test_targets.max(), test_outputs.max())
        plt.plot([min_val, max_val], [min_val, max_val], 'k--', alpha=0.5)

    plt.xlabel('真实值')
    plt.ylabel('预测值')
    plt.title('预测 vs 真实值')
    plt.legend()
    plt.grid(True, alpha=0.3)

    # 3. MAE柱状图
    plt.subplot(2, 2, 3)
    # 计算每个营养素的MAE
    if len(test_outputs) > 0:
        mae_values = torch.abs(test_outputs - test_targets).mean(dim=0).numpy()
    else:
        mae_values = np.zeros(len(nutrient_names))

    colors = plt.cm.viridis(np.linspace(0, 1, len(nutrient_names)))
    bars = plt.bar(range(len(nutrient_names)), mae_values, color=colors)

    plt.xlabel('营养成分')
    plt.ylabel('平均绝对误差 (MAE)')
    plt.title('各营养成分预测误差')
    plt.xticks(range(len(nutrient_names)), nutrient_names, rotation=45)

    # 在柱子上添加数值
    for bar, mae in zip(bars, mae_values):
        plt.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 0.1,
                f'{mae:.1f}', ha='center', va='bottom', fontsize=9)

    # 4. 误差分布
    plt.subplot(2, 2, 4)
    if len(test_outputs) > 0:
        errors = (test_outputs - test_targets).numpy()
        for i, name in enumerate(nutrient_names[:3]):  # 只显示前3个
            plt.hist(errors[:, i], bins=30, alpha=0.5, label=name, density=True)
    else:
        plt.text(0.5, 0.5, '无测试数据', ha='center', va='center')

    plt.xlabel('预测误差')
    plt.ylabel('密度')
    plt.title('预测误差分布')
    if len(nutrient_names) > 0:
        plt.legend()
    plt.grid(True, alpha=0.3)

    plt.tight_layout()
    training_plot_path = os.path.join(artifact_dir, 'training_results.png')
    plt.savefig(training_plot_path, dpi=150, bbox_inches='tight')
    print(f"📈 训练结果图已保存: {training_plot_path}")

    # 5. 各营养素的详细分析
    if len(test_outputs) > 0:
        fig, axes = plt.subplots(3, 3, figsize=(15, 12))
        axes = axes.flatten()

        for i, (ax, name) in enumerate(zip(axes[:len(nutrient_names)], nutrient_names)):
            outputs = test_outputs[:, i].numpy()
            targets = test_targets[:, i].numpy()

            # 散点图
            ax.scatter(targets, outputs, alpha=0.6, s=20)

            # 对角线
            min_val = min(targets.min(), outputs.min())
            max_val = max(targets.max(), outputs.max())
            ax.plot([min_val, max_val], [min_val, max_val], 'r--', alpha=0.5)

            # 计算指标
            mae = np.mean(np.abs(outputs - targets))
            r2 = r2_score(targets, outputs)

            ax.set_xlabel('真实值')
            ax.set_ylabel('预测值')
            ax.set_title(f'{name}\nMAE={mae:.1f}, R²={r2:.3f}')
            ax.grid(True, alpha=0.3)

        # 隐藏多余的子图
        for i in range(len(nutrient_names), len(axes)):
            axes[i].axis('off')

        plt.tight_layout()
        nutrient_plot_path = os.path.join(artifact_dir, 'nutrient_analysis.png')
        plt.savefig(nutrient_plot_path, dpi=150, bbox_inches='tight')
        print(f"📈 营养素详细分析图已保存: {nutrient_plot_path}")


"""
Nutrition5k模型训练脚本 - 兼容性修正版
修复了PyTorch版本兼容性问题
"""


# ... 前面的代码保持不变 ...

# ==================== 5. 主训练循环 ====================

def main(cli_args):
    """主训练函数"""
    print("=" * 60)
    print("Nutrition5k食物营养分析模型训练")
    print("=" * 60)

    # 显示PyTorch版本
    print(f"PyTorch版本: {torch.__version__}")

    # 超参数
    config = {
        'data_dir': cli_args.data_dir,
        'batch_size': cli_args.batch_size,  # 根据GPU内存调整
        'num_epochs': 30,
        'learning_rate': 0.001,
        'backbone': 'resnet50',  # 可选: resnet18, resnet34, resnet50, efficientnet_b0
        'dropout_rate': 0.5,
        'weight_decay': 1e-5,
        'patience': 5,  # 早停耐心值
    }
    artifact_dir = cli_args.artifact_dir
    os.makedirs(artifact_dir, exist_ok=True)

    print("📋 训练配置:")
    for key, value in config.items():
        print(f"  {key}: {value}")

    # 1. 检查数据目录
    data_dir = config['data_dir']
    if not os.path.exists(data_dir):
        print(f"❌ 数据目录不存在: {data_dir}")
        print("请先运行数据预处理脚本: python preprocess_nutrition5k.py")
        print("或者手动创建目录并放置CSV文件和图片")
        return

    # 2. 准备数据
    print("\n📊 准备数据...")

    train_csv = os.path.join(data_dir, 'train.csv')
    val_csv = os.path.join(data_dir, 'val.csv')
    test_csv = os.path.join(data_dir, 'test.csv')
    img_dir = os.path.join(data_dir, 'images')

    # 检查文件是否存在
    for file_path in [train_csv, val_csv, test_csv]:
        if not os.path.exists(file_path):
            print(f"❌ 文件不存在: {file_path}")
            print("请先运行数据预处理脚本")
            print("如果还没有数据，可以先运行模拟数据生成代码")
            return

    if not os.path.exists(img_dir):
        print(f"❌ 图片目录不存在: {img_dir}")
        print("请确保图片目录存在并包含图片")
        return

    # 创建数据集
    try:
        train_dataset = NutritionDataset(
            csv_path=train_csv,
            img_dir=img_dir,
            is_train=True
        )

        val_dataset = NutritionDataset(
            csv_path=val_csv,
            img_dir=img_dir,
            is_train=False
        )

        test_dataset = NutritionDataset(
            csv_path=test_csv,
            img_dir=img_dir,
            is_train=False
        )
    except Exception as e:
        print(f"❌ 创建数据集失败: {e}")
        print("请检查CSV文件格式是否正确")
        return

    # 数据加载器
    train_loader = DataLoader(train_dataset, batch_size=config['batch_size'],
                              shuffle=True, num_workers=0, pin_memory=True)
    val_loader = DataLoader(val_dataset, batch_size=config['batch_size'],
                            shuffle=False, num_workers=0, pin_memory=True)
    test_loader = DataLoader(test_dataset, batch_size=config['batch_size'],
                             shuffle=False, num_workers=0, pin_memory=True)

    print(f"  训练集: {len(train_dataset)} 样本")
    print(f"  验证集: {len(val_dataset)} 样本")
    print(f"  测试集: {len(test_dataset)} 样本")

    # 3. 创建模型
    print("\n🧠 创建模型...")
    model = NutritionModel(
        num_nutrients=7,
        backbone_name=config['backbone'],
        pretrained=True,
        dropout_rate=config['dropout_rate']
    ).to(device)

    # 打印模型参数量
    total_params = sum(p.numel() for p in model.parameters())
    trainable_params = sum(p.numel() for p in model.parameters() if p.requires_grad)
    print(f"  模型总参数: {total_params:,}")
    print(f"  可训练参数: {trainable_params:,}")

    # 4. 损失函数和优化器
    criterion = nn.MSELoss()  # 回归任务用MSE
    optimizer = optim.AdamW(model.parameters(),
                            lr=config['learning_rate'],
                            weight_decay=config['weight_decay'])

    # 5. 学习率调度器 - 修复兼容性问题
    print("\n🔧 创建学习率调度器...")
    print(f"PyTorch版本: {torch.__version__}")

    # 根据PyTorch版本选择不同的参数
    pytorch_version = torch.__version__
    if pytorch_version >= '2.0.0':
        # PyTorch 2.0+ 版本，移除verbose参数
        print("使用PyTorch 2.0+兼容模式")
        scheduler = optim.lr_scheduler.ReduceLROnPlateau(
            optimizer, mode='min', factor=0.5, patience=3
        )
    else:
        # PyTorch 1.x 版本，包含verbose参数
        print("使用PyTorch 1.x兼容模式")
        scheduler = optim.lr_scheduler.ReduceLROnPlateau(
            optimizer, mode='min', factor=0.5, patience=3, verbose=True
        )

    # 或者使用更通用的方法，先尝试新版本格式，失败再尝试旧版本
    try:
        scheduler = optim.lr_scheduler.ReduceLROnPlateau(
            optimizer, mode='min', factor=0.5, patience=3
        )
        print("✅ 学习率调度器创建成功（无verbose参数）")
    except TypeError as e:
        if "verbose" in str(e):
            print("检测到旧版本PyTorch，使用verbose参数")
            scheduler = optim.lr_scheduler.ReduceLROnPlateau(
                optimizer, mode='min', factor=0.5, patience=3, verbose=True
            )
        else:
            raise e

    # 6. 训练循环
    print("\n🚀 开始训练...")

    train_losses = []
    val_losses = []
    best_val_loss = float('inf')
    patience_counter = 0
    nutrient_names = train_dataset.nutrient_cols

    for epoch in range(config['num_epochs']):
        print(f"\nEpoch {epoch + 1}/{config['num_epochs']}")
        print("-" * 40)

        # 训练
        train_loss, train_mae = train_epoch(
            model, train_loader, criterion, optimizer, device, epoch,
            config['num_epochs'], nutrient_names
        )
        train_losses.append(train_loss)

        # 验证
        val_loss, val_mae, val_r2, _, _ = evaluate(
            model, val_loader, criterion, device, nutrient_names
        )
        val_losses.append(val_loss)

        # 学习率调整
        scheduler.step(val_loss)

        # 打印结果
        print(f"训练损失: {train_loss:.4f}, 验证损失: {val_loss:.4f}")
        if len(val_mae) > 0:
            print("验证MAE:")
            for i, name in enumerate(nutrient_names):
                print(f"  {name}: {val_mae[i]:.2f} (R²={val_r2[i]:.3f})")

        # 保存最佳模型
        if val_loss < best_val_loss:
            best_val_loss = val_loss
            patience_counter = 0

            # 保存模型
            torch.save({
                'epoch': epoch,
                'model_state_dict': model.state_dict(),
                'optimizer_state_dict': optimizer.state_dict(),
                'train_loss': train_loss,
                'val_loss': val_loss,
                'val_mae': val_mae,
                'val_r2': val_r2,
                'nutrient_names': nutrient_names,
                'config': config
            }, os.path.join(artifact_dir, 'best_nutrition_model.pth'))

            print(f"💾 保存最佳模型: {os.path.join(artifact_dir, 'best_nutrition_model.pth')} (val_loss={val_loss:.4f})")
        else:
            patience_counter += 1
            print(f"耐心计数器: {patience_counter}/{config['patience']}")

        # 早停
        if patience_counter >= config['patience']:
            print(f"⏹️  早停触发，停止训练")
            break

    # 7. 最终测试
    print("\n" + "=" * 60)
    print("最终测试评估")
    print("=" * 60)

    # 加载最佳模型
    best_model_path = os.path.join(artifact_dir, 'best_nutrition_model.pth')
    if os.path.exists(best_model_path):
        try:
            checkpoint = torch.load(best_model_path, map_location=device)
            model.load_state_dict(checkpoint['model_state_dict'])
            print("✅ 加载最佳模型进行测试")
        except Exception as e:
            print(f"⚠️ 加载最佳模型失败: {e}")
            print("使用当前模型进行测试")
    else:
        print("⚠️ 未找到最佳模型，使用当前模型测试")

    # 在测试集上评估
    test_loss, test_mae, test_r2, test_outputs, test_targets = evaluate(
        model, test_loader, criterion, device, nutrient_names
    )

    print(f"测试损失: {test_loss:.4f}")
    if len(test_mae) > 0:
        print("测试MAE和R²:")
        for i, name in enumerate(nutrient_names):
            print(f"  {name}: MAE={test_mae[i]:.2f}, R²={test_r2[i]:.3f}")
    else:
        print("⚠️ 无法计算测试指标")

    # 8. 可视化结果
    try:
        visualize_results(train_losses, val_losses, test_outputs, test_targets, nutrient_names, artifact_dir)
    except Exception as e:
        print(f"⚠️ 可视化失败: {e}")

    # 9. 保存最终模型
    final_model_path = os.path.join(artifact_dir, 'nutrition5k_final_model.pth')
    try:
        torch.save({
            'model_state_dict': model.state_dict(),
            'nutrient_names': nutrient_names,
            'test_mae': test_mae,
            'test_r2': test_r2,
            'config': config,
            'input_size': (224, 224),
            'mean': [0.485, 0.456, 0.406],
            'std': [0.229, 0.224, 0.225]
        }, final_model_path)
        print(f"💾 最终模型保存到: {final_model_path}")
    except Exception as e:
        print(f"❌ 保存模型失败: {e}")

    return model, nutrient_names, test_mae, test_r2, artifact_dir


# ==================== 6. 检查PyTorch版本 ====================

def check_pytorch_version():
    """检查PyTorch版本并给出建议"""
    print("=" * 60)
    print("PyTorch版本检查")
    print("=" * 60)

    import torch
    version = torch.__version__
    print(f"当前PyTorch版本: {version}")

    # 解析版本号
    major_version = int(version.split('.')[0])

    if major_version >= 2:
        print("✅ 您使用的是PyTorch 2.x+ 版本")
        print("⚠️  注意: PyTorch 2.x+ 移除了ReduceLROnPlateau的verbose参数")
        print("解决方案: 在代码中移除verbose=True参数")
    elif major_version == 1:
        print("✅ 您使用的是PyTorch 1.x 版本")
        print("ℹ️  注意: ReduceLROnPlateau支持verbose参数")
    else:
        print("⚠️  未知的PyTorch版本")

    print("\n建议: 使用以下代码创建学习率调度器:")
    print("""
# 兼容所有版本的方法
try:
    # 先尝试不使用verbose参数（PyTorch 2.x+）
    scheduler = optim.lr_scheduler.ReduceLROnPlateau(
        optimizer, mode='min', factor=0.5, patience=3
    )
except TypeError as e:
    if "verbose" in str(e):
        # 如果报错提到verbose，则使用带verbose的版本（PyTorch 1.x）
        scheduler = optim.lr_scheduler.ReduceLROnPlateau(
            optimizer, mode='min', factor=0.5, patience=3, verbose=True
        )
    else:
        raise e
    """)


# ==================== 7. 执行入口 ====================

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Nutrition5k 训练")
    parser.add_argument("--data-dir", type=str, default="processed_nutrition5k", help="预处理数据目录")
    parser.add_argument("--artifact-dir", type=str, default="training_outputs", help="模型与图表输出目录")
    parser.add_argument("--batch-size", type=int, default=16, help="训练 batch size")
    args = parser.parse_args()

    # 检查PyTorch版本
    check_pytorch_version()

    # 检查其他依赖
    try:
        import torchvision
        import pandas as pd
        import numpy as np
        from PIL import Image
        from sklearn.metrics import r2_score

        print("\n✅ 所有依赖已安装")
    except ImportError as e:
        print(f"\n❌ 缺少依赖: {e}")
        print("请运行以下命令安装依赖:")
        print("pip install torch torchvision pandas numpy pillow matplotlib scikit-learn seaborn tqdm")
        exit(1)

    # 运行训练
    try:
        print("\n" + "=" * 60)
        print("开始训练...")
        print("=" * 60)

        model, nutrient_names, test_mae, test_r2, artifact_dir = main(args)

        print("\n" + "=" * 60)
        print("训练完成！")
        print("=" * 60)

        if len(test_mae) > 0:
            print(f"\n🎯 模型性能总结:")
            print(f"  平均MAE: {np.mean(test_mae):.2f}")
            print(f"  平均R²: {np.mean(test_r2):.3f}")
        else:
            print(f"\n⚠️  无法计算模型性能指标")

        print("\n📁 生成的文件:")
        print(f"  📄 {os.path.join(artifact_dir, 'best_nutrition_model.pth')} - 最佳模型")
        print(f"  📄 {os.path.join(artifact_dir, 'nutrition5k_final_model.pth')} - 最终模型")
        print(f"  📈 {os.path.join(artifact_dir, 'training_results.png')} - 训练结果图")
        print(f"  📈 {os.path.join(artifact_dir, 'nutrient_analysis.png')} - 营养素分析图")

        print("\n🚀 使用模型进行预测:")
        print("  1. 确保有 predict_nutrition.py 文件")
        print("  2. 运行: python predict_nutrition.py --image 您的图片.jpg")

    except Exception as e:
        print(f"\n❌ 训练过程中出错: {e}")
        import traceback

        traceback.print_exc()

        print("\n💡 常见问题解决方案:")
        print("1. 检查数据目录是否正确")
        print("2. 确保CSV文件和图片文件存在")
        print("3. 检查CSV文件格式是否正确")
        print("4. 如果显存不足，减小batch_size（修改config中的batch_size）")
        print("5. 如果没有真实数据，可以先运行模拟数据生成代码")
        print("6. 检查PyTorch版本，移除ReduceLROnPlateau中的verbose参数")
        sys.exit(1)