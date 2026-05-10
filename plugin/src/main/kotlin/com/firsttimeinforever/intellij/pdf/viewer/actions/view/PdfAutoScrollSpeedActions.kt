package com.firsttimeinforever.intellij.pdf.viewer.actions.view

import com.firsttimeinforever.intellij.pdf.viewer.actions.PdfAction
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

/**
 * 增加自动滚动速度动作
 */
class PdfIncreaseAutoScrollSpeedAction : AnAction(), DumbAware {
    override fun actionPerformed(event: AnActionEvent) {
        val controller = PdfAction.findController(event) ?: return
        
        val currentSpeed = PdfAutoScrollAction().getSpeedLevel(controller)
        PdfAutoScrollAction().changeSpeed(controller, currentSpeed + 1)
    }
    
    override fun update(event: AnActionEvent) {
        val controller = PdfAction.findController(event)
        
        // 强制按钮可见
        event.presentation.isVisible = true
        event.presentation.isEnabled = controller != null && 
            PdfAutoScrollAction.getState(controller).enabled
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}

/**
 * 减少自动滚动速度动作
 */
class PdfDecreaseAutoScrollSpeedAction : AnAction(), DumbAware {
    override fun actionPerformed(event: AnActionEvent) {
        val controller = PdfAction.findController(event) ?: return
        
        val currentSpeed = PdfAutoScrollAction().getSpeedLevel(controller)
        PdfAutoScrollAction().changeSpeed(controller, currentSpeed - 1)
    }
    
    override fun update(event: AnActionEvent) {
        val controller = PdfAction.findController(event)
        
        // 强制按钮可见
        event.presentation.isVisible = true
        event.presentation.isEnabled = controller != null && 
            PdfAutoScrollAction.getState(controller).enabled
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
