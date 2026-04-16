import sys
import subprocess
import importlib.util
import importlib.metadata


def check_package(package_name, min_version=None):
    """检查指定的包是否已安装"""
    try:
        installed_version = importlib.metadata.version(package_name)
        if min_version:
            installed_tuple = tuple(map(int, installed_version.split('.')[:3]))
            min_tuple = tuple(map(int, min_version.split('.')[:3]))
            if installed_tuple < min_tuple:
                return False, installed_version, f"版本过低 (需要 >= {min_version})"
        return True, installed_version, "✅ OK"
    except importlib.metadata.PackageNotFoundError:
        return False, "未安装", "❌ 未找到此包"
    except Exception as e:
        return False, "未知", f"❌ 检查时出错: {e}"


def main():
    print("=" * 60)
    print("环境完整性验证")
    print("=" * 60)

    # 检查Python版本
    python_version = sys.version_info
    print(f"Python版本: {sys.version}")
    if python_version.major == 3 and python_version.minor >= 7:
        print("Python 3.7+ ✓ 符合要求")
    else:
        print("Python版本 ✗ 建议使用3.7+")
    print()

    # 检查核心包
    packages_to_check = {
        'torch': '1.9.0',
        'torchvision': '0.10.0',
        'numpy': '1.19.0',
        'pandas': '1.1.0',
        'matplotlib': '3.3.0',
        'Pillow': '8.0.0',
        'setuptools': '40.8.0'
    }

    print("核心依赖包检查:")
    all_ok = True
    for pkg, ver in packages_to_check.items():
        ok, cur_ver, msg = check_package(pkg, ver)
        print(f"  {pkg:15s} | 版本: {cur_ver:12s} | 状态: {msg}")
        if not ok and "OK" not in msg:
            all_ok = False

    print()
    print("=" * 60)
    if all_ok:
        print("✅ 所有依赖已正确安装！")
        print("您可以开始运行食物营养分析模型了。")
    else:
        print("❌ 部分依赖未正确安装。")
        print("请重新执行第4步的安装命令。")
    print("=" * 60)


if __name__ == "__main__":
    main()