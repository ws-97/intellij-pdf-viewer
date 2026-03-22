import os
import shutil
import subprocess
from pathlib import Path
from glob import glob
# 我现在正在开发一款pdf浏览的idea插件，我希望能够将当前pdf的浏览进度保存在setting/pdfViewer中。 期望输出： 1.新建一个和General同样格式的栏目用来显示pdf列表 2.每一项都可以被点击，点击之后打开对应的pdf 3.每一项后面都有一个按钮，点击之后打开pdf所在的文件 4.每一项都有一个渐变色的阅读进度条
# 定义路径
target_dir = Path("target")
jar_source_dir = Path("plugin/build/idea-sandbox/IC-2025.2/plugins/intellij-pdf-viewer/lib")

# 清理并创建目标目录
def setup_target_directory():
    if target_dir.exists():
        shutil.rmtree(target_dir)
    target_dir.mkdir(parents=True, exist_ok=True)

# 解压所有需要的 JAR 包
def extract_jar_files():
    print(f"🔍 正在查找 JAR 文件的目录: {jar_source_dir.resolve()}")
    if not jar_source_dir.exists():
        print(f"❌ 错误：目录不存在: {jar_source_dir.resolve()}")
        exit(1)

    jar_files = list(jar_source_dir.glob("*.jar"))
    print(f"📦 找到 {len(jar_files)} 个 JAR 文件: {[f.name for f in jar_files]}")

    if not jar_files:
        print("❌ 错误：未找到 JAR 文件，请先构建插件")
        exit(1)

    for jar_file in jar_files:
        print(f"📦 正在解压：{jar_file.name} -> {str(jar_file)}")
        try:
            jar_path = str(jar_file.resolve())
            print(f"🔍 使用绝对路径: {jar_path}")
            result = subprocess.run(["jar", "-xf", jar_path], capture_output=True, text=True, cwd=target_dir)
            if result.returncode != 0:
                print(f"❌ jar 输出错误信息: {result.stderr}")
                exit(1)
        except subprocess.CalledProcessError as e:
            print(f"❌ 解压失败 {jar_file.name}: {e}")
            exit(1)

# 创建新的 fat jar
def create_fat_jar():
    print("🔨 创建 fat jar...")
    try:
        subprocess.run(["jar", "-cf", "intellij-pdf-viewer.jar", "."], check=True, cwd="target")
        print("✅ 完成！生成的 jar 文件位于：target/intellij-pdf-viewer.jar")
    except subprocess.CalledProcessError as e:
        print(f"❌ 创建 fat jar 失败: {e}")
        exit(1)

if __name__ == "__main__":
    setup_target_directory()
    extract_jar_files()
    create_fat_jar()
