package com.firsttimeinforever.intellij.pdf.viewer.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup

class PdfSidebarViewModeActionGroup : DefaultActionGroup() {
  init {
    templatePresentation.isPopupGroup = true
  }

  override fun update(event: AnActionEvent) {
    // 检查是否有编辑器或工具窗口中的控制器
    val hasEditor = PdfAction.hasEditorInView(event)
    val controller = PdfAction.findController(event)
    
    event.presentation.isVisible = hasEditor || controller != null
    event.presentation.isEnabled = controller != null
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

}
