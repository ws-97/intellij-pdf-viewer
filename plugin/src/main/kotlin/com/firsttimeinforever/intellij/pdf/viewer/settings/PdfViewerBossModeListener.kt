package com.firsttimeinforever.intellij.pdf.viewer.settings

import com.firsttimeinforever.intellij.pdf.viewer.ui.toolwindow.PdfViewerToolWindowFactory
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.WindowManagerEx
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

/**
 * 老板模式监听器
 * 当 IDEA 窗口失去焦点时自动隐藏 PDF 工具窗口
 */
class PdfViewerBossModeListener(private val project: Project) {
    companion object {
        private val logger = logger<PdfViewerBossModeListener>()
        
        /**
         * 注册老板模式监听器
         */
        fun register(project: Project) {
            if (!PdfViewerSettings.instance.enableBossMode) {
                logger.debug("Boss mode is disabled, skipping registration")
                return
            }
            
            logger.info("Registering boss mode listener for project: ${project.name}")
            
            // 创建监听器实例
            val listener = PdfViewerBossModeListener(project)
            listener.setupWindowFocusListener()
            
            logger.info("Boss mode listener registered successfully")
        }
        
        /**
         * 显示调试通知
         */
        private fun showDebugNotification(project: Project, title: String, content: String) {
            val notification = Notification(
                "PDF Viewer Debug",
                title,
                content,
                NotificationType.INFORMATION
            )
            Notifications.Bus.notify(notification, project)
        }
    }
    
    private var tabLinkageManager: PdfTabLinkageManager? = null
    private var currentPdfPath: String? = null
    
    init {
        if (PdfViewerSettings.instance.enableTabLinkage) {
            tabLinkageManager = PdfTabLinkageManager(project)
            logger.info("Tab linkage manager initialized")
        }
        
        // 获取当前打开的PDF文件路径
        updateCurrentPdfPath()
    }
    
    /**
     * 更新当前PDF文件路径
     */
    private fun updateCurrentPdfPath() {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val openFiles = fileEditorManager.openFiles
        
        currentPdfPath = openFiles.firstOrNull { 
            it.extension.equals("pdf", ignoreCase = true) 
        }?.path
    }
    
    /**
     * 获取当前工具窗口中显示的 PDF 路径
     */
    private fun getCurrentToolWindowPdfPath(): String? {
        logger.debug("Getting current tool window PDF path")
        
        val toolWindow = ToolWindowManager.getInstance(project)
            .getToolWindow(PdfViewerToolWindowFactory.TOOL_WINDOW_ID)
        
        if (toolWindow == null) {
            logger.warn("Tool window not found")
            return null
        }
        
        if (!toolWindow.isVisible) {
            logger.debug("Tool window is not visible")
            return null
        }
        
        val content = toolWindow.contentManager.selectedContent
        logger.debug("Selected content: ${content?.displayName}")
        
        val mainPanel = content?.component as? javax.swing.JPanel
        
        if (mainPanel == null) {
            logger.warn("Main panel is null or not a JPanel")
            return null
        }
        
        // 从 client property 中获取上下文
        val contextClass = Class.forName("com.firsttimeinforever.intellij.pdf.viewer.ui.toolwindow.PdfViewerToolWindowFactory\$PdfViewContext")
        val context = mainPanel.getClientProperty("PDF_VIEW_CONTEXT")
        
        logger.debug("Context class: ${context?.javaClass?.name}")
        logger.debug("Context instance: $context")
        
        if (context != null && contextClass.isInstance(context)) {
            try {
                val fileField = contextClass.getDeclaredField("file")
                fileField.isAccessible = true
                val virtualFile = fileField.get(context) as? com.intellij.openapi.vfs.VirtualFile
                logger.info("Found PDF path: ${virtualFile?.path}")
                return virtualFile?.path
            } catch (e: Exception) {
                logger.warn("Failed to get PDF path from context", e)
            }
        } else {
            logger.warn("Context is null or not an instance of PdfViewContext")
        }
        
        return null
    }
    
    /**
     * 设置窗口焦点监听器
     */
    private fun setupWindowFocusListener() {
        val window = WindowManagerEx.getInstanceEx().getFrame(project)
        if (window == null) {
            logger.warn("Cannot get main window for project: ${project.name}")
            return
        }
        
        // 添加窗口焦点监听器
        window.addWindowFocusListener(object : WindowAdapter() {
            override fun windowLostFocus(e: WindowEvent?) {
                super.windowLostFocus(e)
                
                if (!PdfViewerSettings.instance.enableBossMode) {
                    return
                }
                
                // 延迟检查，避免工具栏操作导致的短暂焦点丢失
                javax.swing.SwingUtilities.invokeLater {
                    // 再次检查IDEA窗口是否真的失去焦点
                    if (window.isActive) {
                        logger.debug("Window regained focus, not hiding")
                        return@invokeLater
                    }
                    
                    val toolWindow = ToolWindowManager.getInstance(project)
                        .getToolWindow(PdfViewerToolWindowFactory.TOOL_WINDOW_ID)
                    
                    if (toolWindow != null && toolWindow.isVisible) {
                        val msg = "=== BOSS MODE TRIGGERED ===\nIDE lost focus, hiding PDF tool window"
                        logger.info(msg)
                        println(msg)  // 直接输出到 Gradle 控制台
                        
                        // 先获取当前工具窗口中显示的 PDF 路径
                        val currentPdfPath = getCurrentToolWindowPdfPath()
                        val infoMsg = "Current PDF path: $currentPdfPath\nTab linkage enabled: ${PdfViewerSettings.instance.enableTabLinkage}\nManager exists: ${tabLinkageManager != null}"
                        logger.info(infoMsg)
                        println(infoMsg)  // 直接输出到 Gradle 控制台
                        
                        // 显示调试通知
                        showDebugNotification(
                            project,
                            "老板模式触发",
                            "PDF 路径: ${currentPdfPath ?: "null"}\n" +
                            "Tab 关联启用: ${PdfViewerSettings.instance.enableTabLinkage}\n" +
                            "管理器存在: ${tabLinkageManager != null}"
                        )
                        
                        toolWindow.hide(null)
                        
                        // 如果启用了Tab关联，关闭匹配的Tab
                        if (tabLinkageManager != null && currentPdfPath != null) {
                            logger.info("Closing linked tabs for: $currentPdfPath")
                            tabLinkageManager!!.closeLinkedTabs(currentPdfPath)
                            
                            // 显示关闭结果通知
                            showDebugNotification(
                                project,
                                "关闭关联 Tab",
                                "正在关闭 PDF 的关联 Tab:\n$currentPdfPath"
                            )
                        } else {
                            logger.warn("Cannot close linked tabs: tabLinkageManager=${tabLinkageManager != null}, currentPdfPath=$currentPdfPath")
                            
                            // 显示错误通知
                            showDebugNotification(
                                project,
                                "无法关闭关联 Tab",
                                "原因:\n" +
                                "- Tab 关联管理器: ${if (tabLinkageManager == null) "不存在" else "存在"}\n" +
                                "- PDF 路径: ${currentPdfPath ?: "null"}"
                            )
                        }
                    }
                }
            }
            
            override fun windowGainedFocus(e: WindowEvent?) {
                super.windowGainedFocus(e)
                
                if (!PdfViewerSettings.instance.enableBossMode) {
                    return
                }
                
                // 更新当前PDF路径
                updateCurrentPdfPath()
                
                // 如果启用了Tab关联，打开匹配的Tab
                if (tabLinkageManager != null && currentPdfPath != null) {
                    tabLinkageManager!!.openLinkedTabs(currentPdfPath!!)
                }
            }
        })
    }
}
