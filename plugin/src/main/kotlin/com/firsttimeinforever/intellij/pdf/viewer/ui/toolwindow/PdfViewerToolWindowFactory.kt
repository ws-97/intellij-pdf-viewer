package com.firsttimeinforever.intellij.pdf.viewer.ui.toolwindow

import com.firsttimeinforever.intellij.pdf.viewer.lang.PdfFileType
import com.firsttimeinforever.intellij.pdf.viewer.ui.editor.view.PdfEditorViewComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import net.miginfocom.swing.MigLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * PDF Viewer Tool Window Factory
 * 创建边缘工具窗口来显示 PDF 内容
 */
class PdfViewerToolWindowFactory : ToolWindowFactory, DumbAware {
    companion object {
        const val TOOL_WINDOW_ID = "PDF Viewer"
        private val logger = logger<PdfViewerToolWindowFactory>()
    }
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        logger.info("Creating tool window content for: $TOOL_WINDOW_ID")
            
        val contentFactory = ContentFactory.getInstance()
        // 使用 BorderLayout 让 PDF 组件能完整显示 (包括工具栏)
        val mainPanel = JPanel(java.awt.BorderLayout())
        val content = contentFactory.createContent(mainPanel, "", false)
        toolWindow.contentManager.addContent(content)
            
        // 添加一个占位标签
        updatePlaceholder(mainPanel)
            
        logger.info("Tool window created, waiting for PDF file selection...")
            
        // 用于跟踪当前正在显示的 PDF 文件
        var currentPdfFile: VirtualFile? = null
        var currentViewComponent: PdfEditorViewComponent? = null
            
        // 监听编辑器选择变化 - 当用户在编辑器中打开 PDF 文件时触发
        val connection = project.messageBus.connect(toolWindow.disposable)
        connection.subscribe(
            com.intellij.openapi.fileEditor.FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : com.intellij.openapi.fileEditor.FileEditorManagerListener {
                override fun selectionChanged(event: com.intellij.openapi.fileEditor.FileEditorManagerEvent) {
                    event.newFile?.let { file ->
                        if (file.fileType == PdfFileType) {
                            logger.info("PDF file selected: ${file.path}")
                            currentPdfFile = file
                            javax.swing.SwingUtilities.invokeLater {
                                activateToolWindow(project)
                                showPdfInToolWindow(project, file, mainPanel, toolWindow, toolWindow.disposable)
                            }
                        }
                    }
                    // 监听文件关闭事件
                    event.oldFile?.let { closedFile ->
                        if (closedFile.fileType == PdfFileType) {
                            logger.info("PDF file closed: ${closedFile.path}")
                            // 如果关闭的是当前显示的 PDF，保持工具窗口内容不变
                            // 因为我们在 showPdfInToolWindow 中已经处理了组件复用
                        }
                    }
                }
            }
        )
            
        // 初始检查当前是否有打开的 PDF 文件
        com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).selectedFiles.firstOrNull()?.let { file ->
            if (file.fileType == PdfFileType) {
                logger.info("Initial PDF file found: ${file.path}")
                currentPdfFile = file
                activateToolWindow(project)
                showPdfInToolWindow(project, file, mainPanel, toolWindow, toolWindow.disposable)
            }
        }
    }
    
    /**
     * 用于跟踪当前正在显示的 PDF 文件和对应的 viewComponent
     */
    private data class PdfViewContext(
        val file: VirtualFile,
        val viewComponent: PdfEditorViewComponent
    )
        
    private fun showPdfInToolWindow(
        project: Project,
        file: VirtualFile,
        mainPanel: JPanel,
        toolWindow: ToolWindow,
        disposable: Disposable
    ) {
        // 检查是否需要切换文件
        val currentContext = mainPanel.getClientProperty("PDF_VIEW_CONTEXT") as? PdfViewContext
            
        if (currentContext != null && currentContext.file == file) {
            // 已经是当前文件，不需要切换
            logger.debug("Already showing PDF: ${file.path}")
            return
        }
            
        // 清理旧的组件
        if (currentContext != null) {
            logger.info("Switching from PDF: ${currentContext.file.path} to: ${file.path}")
            mainPanel.remove(currentContext.viewComponent)
            Disposer.dispose(currentContext.viewComponent)
        } else {
            logger.info("Showing new PDF: ${file.path}")
        }
            
        mainPanel.removeAll()
            
        try {
            // 为工具窗口创建独立的 viewComponent
            // 这样不会受到编辑器生命周期的影响
            logger.info("Creating new PdfEditorViewComponent for: ${file.path}")
            val viewComponent = com.firsttimeinforever.intellij.pdf.viewer.ui.editor.view.PdfEditorViewComponent(project, file)
            
            // 关键：不要将 viewComponent 注册到 disposable，避免被意外清理
            // 只注册到 toolWindow.disposable，确保工具窗口关闭时才清理
            
            // 使用 BorderLayout 让组件能完整显示 (包括顶部工具栏)
            // 重要：移除所有可能的 MigLayout 影响，确保工具栏正常显示
            mainPanel.layout = java.awt.BorderLayout()
            
            // 先添加 wrapperPanel(包含工具栏) 到 NORTH 位置
            mainPanel.add(viewComponent.wrapperPanel, java.awt.BorderLayout.NORTH)
            
            // 然后添加 PDF 浏览器组件到 CENTER 位置
            val browserComponent = if (viewComponent.controller != null) {
                viewComponent.controller.component
            } else {
                com.firsttimeinforever.intellij.pdf.viewer.ui.editor.view.PdfUnsupportedViewPanel()
            }
            mainPanel.add(browserComponent, java.awt.BorderLayout.CENTER)
            mainPanel.revalidate()
            mainPanel.repaint()
                
            // 保存上下文
            mainPanel.putClientProperty("PDF_VIEW_CONTEXT", PdfViewContext(file, viewComponent))
                
            // 重要：不要将 viewComponent 注册到 disposable!
            // viewComponent 是可复用的 UI 容器，不应该被 dispose
            // 只注册 controller，因为它是资源密集型组件
            if (viewComponent.controller != null) {
                Disposer.register(disposable, viewComponent.controller)
            }
                
            // 关键修复：设置 toolbar 的 targetComponent 为 mainPanel
            // 这样 toolbar actions 在 update 时能找到正确的上下文
            viewComponent.controlPanel.setToolbarTarget(mainPanel)
                
            // 将 controller 注册到 DataContext，让 actions 能找到它
            mainPanel.putClientProperty("PDF_CONTROLLER", viewComponent.controller)
                
            // 检查控制器是否初始化成功 - 这是关键检查点
            if (viewComponent.controller == null) {
                logger.error("Controller is null! JCEF may not be supported.")
                val errorLabel = JLabel("JCEF 不支持，请检查 Registry 设置：ide.browser.jcef.enabled")
                errorLabel.horizontalAlignment = SwingConstants.CENTER
                mainPanel.add(errorLabel, java.awt.BorderLayout.CENTER)
            } else {
                logger.info("✓ Controller initialized successfully for: ${file.path}")
                logger.info("✓ Browser component: ${viewComponent.controller.component.javaClass.simpleName}")
                
                // 关键：监听控制器组件的状态，如果被 dispose 了需要重建
                Disposer.register(viewComponent.controller) {
                    logger.warn("Controller disposed for: ${file.path}, but tool window still showing it!")
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to show PDF in tool window", e)
            mainPanel.removeAll()
            val errorLabel = JLabel("加载 PDF 失败：${e.message}")
            errorLabel.horizontalAlignment = SwingConstants.CENTER
            mainPanel.add(errorLabel, java.awt.BorderLayout.CENTER)
        }
    }
    
    private fun updatePlaceholder(mainPanel: JPanel) {
        mainPanel.removeAll()
        val placeholderLabel = JLabel("请在项目视图中选择 PDF 文件")
        placeholderLabel.horizontalAlignment = SwingConstants.CENTER
        mainPanel.add(placeholderLabel, java.awt.BorderLayout.CENTER)
        mainPanel.revalidate()
        mainPanel.repaint()
    }
    
    private fun activateToolWindow(project: Project) {
        javax.swing.SwingUtilities.invokeLater {
            ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID)?.show {
                logger.info("Tool window activated")
            }
        }
    }
}
