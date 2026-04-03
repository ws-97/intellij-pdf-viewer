# PDF Viewer 工具窗口模式修改说明

## 修改概述

将 PDF Viewer 插件从占用编辑区域改为通过边缘工具窗口展示界面。

## 主要修改

### 1. 移除文件编辑器提供者

**文件**: `plugin/src/main/resources/META-INF/plugin.xml`
- 删除了 `<fileEditorProvider implementation="com.firsttimeinforever.intellij.pdf.viewer.ui.editor.PdfFileEditorProvider"/>`
- 这样 PDF 文件不再在编辑区域打开

**文件**: `META-INF/cwm.xml`
- 注释掉了 Code With Me 远程编辑支持（因为不再使用文件编辑器）

### 2. 更新工具窗口工厂

**文件**: `plugin/src/main/kotlin/com/firsttimeinforever/intellij/pdf/viewer/ui/toolwindow/PdfViewerToolWindowFactory.kt`

主要改进：
- 监听 `FileEditorManagerListener` 的 `selectionChanged` 事件
- 当用户选择 PDF 文件时，自动激活并显示工具窗口
- 在工具窗口中直接渲染 PDF 内容
- 添加了 `activateToolWindow()` 方法，自动打开工具窗口
- 使用 `event.newFile` 而不是`event.newEditor` 来检测文件变化

### 3. 设置默认值调整

**文件**: `plugin/src/main/kotlin/com/firsttimeinforever/intellij/pdf/viewer/settings/PdfViewerSettings.kt`
- 将 `useToolWindowMode` 的默认值从 `false` 改为 `true`
- 这样新用户安装后默认就使用工具窗口模式

### 4. 保留的功能

- 设置页面中的"使用边缘工具窗口显示 PDF"选项仍然保留
- 用户可以取消勾选该选项来禁用工具窗口模式（但需要配合其他改动才能生效）
- 最近浏览的 PDF 列表功能保持不变

## 工作流程

1. 用户在项目视图中点击 PDF 文件
2. `FileEditorManagerListener` 检测到文件选择变化
3. 如果是 PDF 文件且启用了工具窗口模式：
   - 自动激活右侧工具窗口
   - 在工具窗口中加载并显示 PDF 内容
4. 如果切换到非 PDF 文件，工具窗口显示占位符提示

## 优势

- **不占用编辑区域**: PDF 在右侧边缘工具窗口显示，编辑器可以专注于代码
- **自动激活**: 点击 PDF 文件时自动打开工具窗口
- **保持同步**: 文件变化时 PDF 会自动刷新
- **易于切换**: 可以随时关闭或打开工具窗口

## 注意事项

- 移除了文件编辑器提供者后，PDF 不再作为标准编辑器打开
- Code With Me 远程编辑功能暂时禁用
- 状态栏的页码小部件可能需要适配工具窗口模式
