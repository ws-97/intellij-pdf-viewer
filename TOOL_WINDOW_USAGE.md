# PDF Viewer Tool Window 模式使用说明

## 功能概述

PDF Viewer 插件现在支持通过边缘工具窗口（Tool Window）显示 PDF 文件，而不是占用编辑区域。这样可以在查看 PDF 的同时保留更多的代码编辑空间。

## 启用方法

1. 打开 IntelliJ IDEA 设置 (`File` -> `Settings` 或 `Ctrl+Alt+S`)
2. 导航到 `Tools` -> `PDF Viewer`
3. 勾选 **"使用边缘工具窗口显示 PDF"** 选项
4. 点击 `Apply` 和 `OK`

## 使用方式

### 开启 Tool Window 模式后：

1. **自动显示**：当您在项目视图中选择 PDF 文件时，PDF 会自动在右侧的 "PDF Viewer" 工具窗口中打开
2. **手动打开**：您也可以通过 `View` -> `Tool Windows` -> `PDF Viewer` 来打开该窗口
3. **切换文件**：在项目视图中选择不同的 PDF 文件会自动更新工具窗口中的内容

### 关闭 Tool Window 模式（默认行为）：

PDF 文件将在主编辑区域打开，与之前版本的行为一致。

## 优势

- **节省空间**：不占用主编辑区域，可以同时查看代码和 PDF
- **快速访问**：通过边缘图标快速打开/关闭 PDF 视图
- **灵活布局**：可以调整工具窗口的位置和大小
- **多任务处理**：更适合边写代码边看文档的场景

## 注意事项

- 此功能需要 JCEF (Chromium Embedded Framework) 支持
- 如果未启用 Tool Window 模式，PDF 仍将在编辑区域中打开
- 您可以在设置中随时切换此选项

## 技术实现

- Tool Window 固定在编辑器右侧 (`anchor="right"`)
- 使用 PDF.js 渲染引擎，与编辑区域模式功能一致
- 支持所有现有的 PDF 查看功能（缩放、搜索、大纲等）
