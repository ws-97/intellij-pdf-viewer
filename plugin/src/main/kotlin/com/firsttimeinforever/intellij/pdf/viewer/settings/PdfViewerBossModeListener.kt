package com.firsttimeinforever.intellij.pdf.viewer.settings

import com.firsttimeinforever.intellij.pdf.viewer.ui.toolwindow.PdfViewerToolWindowFactory
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.WindowManagerEx
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

/**
 * 老板模式监听器
 * 当 IDEA 窗口失去焦点时自动隐藏 PDF 工具窗口
 */
class PdfViewerBossModeListener {
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
            
            // 获取主窗口
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
                            logger.info("IDE lost focus, hiding PDF tool window")
                            toolWindow.hide(null)
                        }
                    }
                }
            })
            
            logger.info("Boss mode listener registered successfully")
        }
    }
}
