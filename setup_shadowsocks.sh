#!/bin/bash

# 检查权限
if [ "$(id -u)" != "0" ]; then
   echo "请使用 sudo 运行此脚本"
   exit 1
fi

echo "正在安装 shadowsocks-libev..."
apt update
apt install -y shadowsocks-libev

echo "正在配置参数（端口: 9999, 密码: ws）..."

# 写入配置文件
cat <<EOF > /etc/shadowsocks-libev/config.json
{
    "server":"0.0.0.0",
    "mode":"tcp_and_udp",
    "server_port":9999,
    "local_port":1080,
    "password":"ws",
    "timeout":60,
    "method":"aes-256-gcm",
    "fast_open":false
}
EOF

echo "正在启动服务并设置开机自启..."
# 重启服务应用新配置
systemctl restart shadowsocks-libev
# 设置开机自动启动
systemctl enable shadowsocks-libev

# 开放系统防火墙端口 (如果使用了 ufw)
if command -v ufw > /dev/null; then
    ufw allow 9999/tcp
    ufw allow 9999/udp
    echo "已在 ufw 防火墙中放行 9999 端口"
fi

echo "------------------------------------------------"
echo "安装完成！"
echo "服务器端口: 9999"
echo "密码: ws"
echo "加密方式: aes-256-gcm"
echo "服务状态: $(systemctl is-active shadowsocks-libev)"
echo "------------------------------------------------"
echo "注意：如果是云服务器（如腾讯云、阿里云），请务必在控制台安全组开启 9999 端口。"
