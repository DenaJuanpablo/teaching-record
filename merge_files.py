import os

# ================= 配置区域 =================
# 你想要扫描的目录路径（'.' 表示当前脚本所在的文件夹）
TARGET_DIR = '.' 
# 合并后的输出文件名
OUTPUT_FILE = 'merged_code.txt'
# 需要合并的文件后缀名
EXTENSIONS = {'.java', '.vue', '.js'}
# 扫描时需要跳过的文件夹（避免把依赖包或编译产物加进去，同时忽略常见的编辑器配置）
IGNORE_DIRS = {'node_modules', '.git', 'dist', 'target', 'build', '.idea', '.vscode'}
# ============================================

def merge_files():
    print("开始扫描文件...")
    matched_files = []

    # 使用 os.walk 递归遍历目录
    for root, dirs, files in os.walk(TARGET_DIR):
        # 过滤掉不需要扫描的文件夹 (原地修改 dirs 列表，这样 os.walk 就不会进入这些目录)
        dirs[:] = [d for d in dirs if d not in IGNORE_DIRS]
        
        for file in files:
            # 检查后缀名
            ext = os.path.splitext(file)[1].lower()
            if ext in EXTENSIONS:
                file_path = os.path.join(root, file)
                matched_files.append(file_path)

    if not matched_files:
        print("没有找到符合条件的文件。")
        return

    print(f"共找到 {len(matched_files)} 个文件，开始合并...")

    # 开始合并并写入文件 (指定 utf-8 编码防止中文乱码)
    with open(OUTPUT_FILE, 'w', encoding='utf-8') as outfile:
        for file_path in matched_files:
            try:
                with open(file_path, 'r', encoding='utf-8') as infile:
                    content = infile.read()
                    
                # 写入分隔符和文件路径标注
                outfile.write(f"\n\n{'='*80}\n")
                outfile.write(f"文件源: {file_path}\n")
                outfile.write(f"{'='*80}\n\n")
                
                # 写入文件具体代码内容
                outfile.write(content)
            except Exception as e:
                print(f"读取文件时出错: {file_path} - 错误信息: {e}")

    print(f"✅ 合并完成！所有内容已保存至: {os.path.abspath(OUTPUT_FILE)}")

if __name__ == '__main__':
    merge_files()