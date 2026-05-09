# PDF Viewer 老板模式功能说明

## 功能概述

PDF Viewer 插件新增了"老板模式"功能，当 IntelliJ IDEA 窗口失去焦点时，会自动隐藏 PDF 阅读页面；当窗口重新获得焦点时，会自动恢复显示。

## 启用方法

1. 打开 IntelliJ IDEA 设置 (`File` -> `Settings` 或 `Ctrl+Alt+S`)
2. 导航到 `Tools` -> `PDF Viewer`
3. 勾选 **"老板模式（失去焦点时自动隐藏）"** 选项
4. 点击 `Apply` 和 `OK`

## 使用场景

- **隐私保护**：在办公环境中，当您切换到其他应用程序时，PDF 内容会自动隐藏，避免被他人看到
- **专注工作**：减少视觉干扰，让您专注于当前任务
- **多任务处理**：在多个应用间切换时，保持工作区整洁

## 工作原理

1. **失去焦点时**：
   - 当 IDEA 窗口失去焦点（例如切换到浏览器、微信等其他应用）
   - PDF Viewer 工具窗口会自动隐藏

2. **获得焦点时**：
   - 当您切换回 IDEA 窗口
   - PDF 工具窗口**不会**自动恢复
   - 您需要手动点击工具窗口图标来重新显示

## 注意事项

- 此功能仅在启用了“使用边缘工具窗口显示 PDF”选项时有效
- **获得焦点时不会自动恢复**，需要手动点击工具窗口图标重新显示
- 您可以在任何时候通过设置关闭此功能

## 技术实现

- 使用 Java AWT 的 `WindowFocusListener` 监听窗口焦点变化
- 通过 IntelliJ Platform API 控制工具窗口的显示/隐藏
- 异步执行 UI 操作，确保不影响主线程性能

## 配置示例

```properties
# 在 pdf_viewer.xml 中
<PdfViewerSettings>
  <option name="enableBossMode" value="true" />
  <option name="useToolWindowMode" value="true" />
</PdfViewerSettings>
```

## 常见问题

**Q: 为什么我的 PDF 没有自动隐藏？**
A: 请确保同时启用了"使用边缘工具窗口显示 PDF"和"老板模式"两个选项。

**Q: 隐藏后如何快速恢复？**
A: 需要手动点击右侧工具窗口栏的 PDF Viewer 图标来重新显示。

**Q: 会影响其他工具窗口吗？**
A: 不会，此功能只影响 PDF Viewer 工具窗口。
