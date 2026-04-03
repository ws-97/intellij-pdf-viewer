#!/bin/bash

# MySQL 数据库迁移脚本

set -e

echo "=== MySQL 数据库迁移准备 ==="

# 1. 准备迁移环境
read -p "源数据库主机: " source_host
read -p "源数据库端口(默认3306): " source_port
source_port=${source_port:-3306}
read -p "源数据库用户名: " source_user
read -s -p "源数据库密码: " source_password
echo

read -p "目标数据库主机: " target_host
read -p "目标数据库端口(默认3306): " target_port
target_port=${target_port:-3306}
read -p "目标数据库用户名: " target_user
read -s -p "目标数据库密码: " target_password
echo

read -p "要迁移的数据库名: " db_name

# 2. 导出源数据库
echo "正在导出数据库 $db_name..."
mkdir -p ./migrate
date_str=$(date +%Y%m%d_%H%M%S)
backup_file="./migrate/${db_name}_backup_${date_str}.sql"

mysqldump -h "$source_host" -P "$source_port" -u "$source_user" -p"$source_password" --single-transaction --routines --triggers --databases "$db_name" > "$backup_file"
echo "数据库导出完成: $backup_file"

# 3. 提示传输备份文件
echo "请将备份文件 $backup_file 传输到目标服务器:"
echo "scp $backup_file $target_user@$target_host:/tmp/"

# 4. 导入目标数据库
echo "在目标服务器执行以下命令导入数据："
echo "mysql -h \"$target_host\" -P \"$target_port\" -u \"$target_user\" -p\"$target_password\" < /tmp/$(basename $backup_file)"
