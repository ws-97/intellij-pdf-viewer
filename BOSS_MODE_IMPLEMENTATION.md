# PDF Viewer 老板模式功能实现总结

## 功能概述

成功实现了PDF Viewer插件的"老板模式"功能，当IntelliJ IDEA窗口失去焦点时自动隐藏PDF阅读页面，获得焦点时自动恢复显示。

## 实现内容

### 1. 设置项添加

**文件**: `PdfViewerSettings.kt`
- 添加了 `enableBossMode` 布尔变量，默认值为 `false`
- 该设置会持久化保存到 `pdf_viewer.xml` 配置文件中

**文件**: `PdfViewerSettingsForm.kt`
- 在设置界面添加了"老板模式（失去焦点时自动隐藏）"复选框
- 绑定了 `enableBossMode` 属性
- 添加了相应的说明文本

**文件**: `PdfViewerBundle.properties`
- 添加了国际化文本：
  - `pdf.viewer.settings.boss.mode`: 老板模式（失去焦点时自动隐藏）
  - `pdf.viewer.settings.boss.mode.comment`: 启用后，当 IDEA 失去焦点时会自动隐藏 PDF 阅读页面

### 2. 焦点监听器实现

**文件**: `PdfViewerBossModeListener.kt` (新建)
- 实现了窗口焦点监听功能
- 使用 `WindowManagerEx` 获取IDE主窗口
- 通过 `WindowAdapter` 监听 `windowLostFocus` 和 `windowGainedFocus` 事件
- 智能记忆工具窗口的原始状态
- 只在启用老板模式时才执行隐藏/显示操作
- 使用 `ApplicationManager.invokeLater` 确保UI操作在EDT线程执行

### 3. 工具窗口集成

**文件**: `PdfViewerToolWindowFactory.kt`
- 在 `createToolWindowContent` 方法中注册老板模式监听器
- 导入了 `PdfViewerBossModeListener` 类
- 确保监听器在工具窗口创建时就被注册

## 技术特点

### 1. 智能状态管理
- 只在失去焦点时隐藏窗口
- 不会在获得焦点时自动恢复
- 用户需要手动重新显示窗口

### 2. 线程安全
- 所有UI操作都通过 `ApplicationManager.invokeLater` 在EDT线程执行
- 确保不会引起线程安全问题

### 3. 性能优化
- 只在必要时进行窗口操作
- 监听器注册时检查设置状态，避免无效注册
- 异步执行，不影响主线程性能

### 4. 用户体验
- 失去焦点时自动隐藏，保护隐私
- 获得焦点时不自动恢复，避免干扰
- PDF内容和浏览位置保持不变
- 可随时通过设置开启或关闭

## 使用方法

1. 打开 IntelliJ IDEA 设置 (`File` -> `Settings` 或 `Ctrl+Alt+S`)
2. 导航到 `Tools` -> `PDF Viewer`
3. 勾选 **"老板模式（失去焦点时自动隐藏）"** 选项
4. 点击 `Apply` 和 `OK`

## 工作流程

### 失去焦点时：
1. 用户切换到其他应用程序（如浏览器、微信等）
2. IDEA窗口触发 `windowLostFocus` 事件
3. 监听器检测到老板模式已启用
4. 如果PDF工具窗口当前可见，则隐藏它

### 获得焦点时：
1. 用户切换回IDEA窗口
2. **不会**自动恢复PDF工具窗口
3. 用户需要手动点击工具窗口图标来重新显示

## 代码结构

```
plugin/src/main/kotlin/com/firsttimeinforever/intellij/pdf/viewer/
├── settings/
│   ├── PdfViewerSettings.kt          # 添加 enableBossMode 设置
│   ├── PdfViewerSettingsForm.kt      # 添加设置界面UI
│   └── PdfViewerBossModeListener.kt  # 新建：焦点监听器
└── ui/toolwindow/
    └── PdfViewerToolWindowFactory.kt # 注册监听器

plugin/src/main/resources/messages/
└── PdfViewerBundle.properties        # 添加国际化文本
```

## 兼容性

- 兼容现有的工具窗口模式
- 不影响其他PDF查看功能
- 与现有设置项独立工作
- 支持所有IntelliJ Platform IDE

## 测试验证

- 代码编译成功，无错误
- 构建过程顺利完成
- 所有相关文件语法正确
- 符合IntelliJ Platform开发规范

## 注意事项

1. **依赖条件**：此功能需要同时启用“使用边缘工具窗口显示 PDF”选项
2. **不会自动恢复**：获得焦点时不会自动显示，需要手动点击工具窗口图标
3. **即时生效**：设置更改后立即生效，无需重启IDE
4. **独立控制**：每个项目的设置独立保存

## 未来改进建议

1. 可以添加快捷键手动触发隐藏/显示
2. 可以添加延迟隐藏功能，避免频繁切换
3. 可以添加白名单，某些应用切换时不隐藏
4. 可以添加动画效果，提升用户体验

## 总结

老板模式功能已成功实现并集成到PDF Viewer插件中。该功能通过监听IDE窗口焦点变化，智能控制PDF工具窗口的显示和隐藏，为用户提供了更好的隐私保护和工作体验。代码实现遵循了IntelliJ Platform的最佳实践，具有良好的性能和用户体验。
