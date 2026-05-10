package com.firsttimeinforever.intellij.pdf.viewer.actions.view

import com.firsttimeinforever.intellij.pdf.viewer.PdfViewerActionsBundle
import com.firsttimeinforever.intellij.pdf.viewer.actions.PdfAction
import com.firsttimeinforever.intellij.pdf.viewer.ui.editor.view.PdfJcefPreviewController
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import javax.swing.Timer
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.util.concurrent.ConcurrentHashMap

/**
 * 自动滚动动作 - 支持开启/关闭自动滚动并调整速度
 * 直接继承 AnAction 避免父类 update() 覆盖可见性设置
 */
class PdfAutoScrollAction : AnAction(), DumbAware {
    
    companion object {
        // 滚动速度级别：1-5，对应不同的滚动间隔（毫秒）
        private val SPEED_LEVELS = mapOf(
            1 to 200,  // 最慢
            2 to 100,
            3 to 50,   // 中等
            4 to 30,
            5 to 15    // 最快
        )
        
        // 默认速度级别
        const val DEFAULT_SPEED_LEVEL = 3
        
        // 用于存储每个控制器的自动滚动状态
        private val autoScrollStates = ConcurrentHashMap<PdfJcefPreviewController, AutoScrollState>()
        
        internal data class AutoScrollState(
            val timer: Timer? = null,
            val enabled: Boolean = false,
            val speedLevel: Int = DEFAULT_SPEED_LEVEL
        )
        
        internal fun getState(controller: PdfJcefPreviewController): AutoScrollState {
            return autoScrollStates[controller] ?: AutoScrollState()
        }
        
        internal fun setState(controller: PdfJcefPreviewController, state: AutoScrollState) {
            autoScrollStates[controller] = state
        }
    }
    
    override fun actionPerformed(event: AnActionEvent) {
        val controller = PdfAction.findController(event) ?: return
        
        val currentState = getState(controller)
        if (currentState.enabled) {
            // 关闭自动滚动
            disableAutoScroll(controller)
        } else {
            // 开启自动滚动
            enableAutoScroll(controller)
        }
    }
    
    override fun update(event: AnActionEvent) {
        // 完全不调用 super.update()，避免父类覆盖可见性
        val controller = PdfAction.findController(event)
        val isAutoScrolling = controller?.let { getState(it).enabled } ?: false
        
        // 强制设置按钮可见
        event.presentation.isVisible = true
        event.presentation.isEnabled = controller != null
        
        // 设置文本和描述
        when {
            controller == null -> {
                event.presentation.text = PdfViewerActionsBundle.message("action.pdf.viewer.AutoScrollAction.text")
                event.presentation.description = PdfViewerActionsBundle.message("action.pdf.viewer.AutoScrollAction.description.start")
            }
            isAutoScrolling -> {
                event.presentation.text = PdfViewerActionsBundle.message("action.pdf.viewer.AutoScrollAction.text.stop")
                event.presentation.description = PdfViewerActionsBundle.message("action.pdf.viewer.AutoScrollAction.description.stop")
            }
            else -> {
                event.presentation.text = PdfViewerActionsBundle.message("action.pdf.viewer.AutoScrollAction.text.start")
                event.presentation.description = PdfViewerActionsBundle.message("action.pdf.viewer.AutoScrollAction.description.start")
            }
        }
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    
    /**
     * 开启自动滚动
     */
    private fun enableAutoScroll(controller: PdfJcefPreviewController) {
        val currentState = getState(controller)
        val speedLevel = if (currentState.speedLevel != DEFAULT_SPEED_LEVEL) {
            currentState.speedLevel
        } else {
            loadSpeedFromSettings(controller)
        }
        val scrollInterval = SPEED_LEVELS[speedLevel] ?: SPEED_LEVELS[DEFAULT_SPEED_LEVEL]!!
        
        val timer = Timer(scrollInterval, object : ActionListener {
            override fun actionPerformed(e: ActionEvent?) {
                try {
                    controller.browser.cefBrowser.executeJavaScript(
                        """
                        (function() {
                            var container = document.getElementById('viewerContainer');
                            if (container) {
                                container.scrollTop += 2;
                            }
                        })();
                        """.trimIndent(),
                        controller.browser.cefBrowser.url,
                        0
                    )
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        })
        
        setState(controller, currentState.copy(timer = timer, enabled = true))
        timer.start()
    }
    
    /**
     * 关闭自动滚动
     */
    private fun disableAutoScroll(controller: PdfJcefPreviewController) {
        val currentState = getState(controller)
        currentState.timer?.stop()
        setState(controller, currentState.copy(timer = null, enabled = false))
    }
    
    /**
     * 更改自动滚动速度
     */
    fun changeSpeed(controller: PdfJcefPreviewController, speedLevel: Int) {
        val clampedSpeed = speedLevel.coerceIn(1, 5)
        val currentState = getState(controller)
        
        setState(controller, currentState.copy(speedLevel = clampedSpeed))
        saveSpeedToSettings(controller, clampedSpeed)
        
        if (currentState.enabled) {
            disableAutoScroll(controller)
            enableAutoScroll(controller)
        }
    }
    
    /**
     * 获取当前速度级别
     */
    fun getSpeedLevel(controller: PdfJcefPreviewController): Int {
        val stateSpeed = getState(controller).speedLevel
        return if (stateSpeed != DEFAULT_SPEED_LEVEL) {
            stateSpeed
        } else {
            loadSpeedFromSettings(controller)
        }
    }
    
    /**
     * 从设置中加载速度
     */
    private fun loadSpeedFromSettings(controller: PdfJcefPreviewController): Int {
        val pdfPath = controller.virtualFile.url
        val settings = com.firsttimeinforever.intellij.pdf.viewer.settings.PdfViewerSettings.instance
        return settings.pdfAutoScrollSpeedMap[pdfPath] ?: DEFAULT_SPEED_LEVEL
    }
    
    /**
     * 保存速度到设置
     */
    private fun saveSpeedToSettings(controller: PdfJcefPreviewController, speedLevel: Int) {
        val pdfPath = controller.virtualFile.url
        val settings = com.firsttimeinforever.intellij.pdf.viewer.settings.PdfViewerSettings.instance
        settings.pdfAutoScrollSpeedMap[pdfPath] = speedLevel
    }
}
