package com.firsttimeinforever.intellij.pdf.viewer.actions.view

import com.firsttimeinforever.intellij.pdf.viewer.actions.PdfAction
import com.firsttimeinforever.intellij.pdf.viewer.actions.PdfToggleAction
import com.firsttimeinforever.intellij.pdf.viewer.model.SidebarViewMode
import com.firsttimeinforever.intellij.pdf.viewer.ui.editor.view.PdfJcefPreviewController
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

sealed class PdfSetSidebarViewModeAction(private val targetViewMode: SidebarViewMode) : PdfToggleAction(), DumbAware {
  override fun isSelected(event: AnActionEvent): Boolean {
    val viewController = PdfAction.findController(event) ?: return false
    return viewController.viewState.sidebarViewMode == targetViewMode
  }

  override fun setSelected(event: AnActionEvent, state: Boolean) {
    val viewController = PdfAction.findController(event) ?: return
    viewController.setSidebarViewMode(targetViewMode)
  }

  override fun update(event: AnActionEvent) {
    super.update(event)
    // 检查是否有编辑器或工具窗口中的控制器
    val hasEditor = PdfAction.hasEditorInView(event)
    val controller = PdfAction.findController(event)
    
    event.presentation.isVisible = hasEditor || controller != null
    event.presentation.isEnabled = canBeEnabled(controller)
  }

  private fun canBeEnabled(controller: PdfJcefPreviewController?): Boolean {
    if (controller == null) return false
    // 如果 viewProperties 还未初始化（availableSidebarViewModes 为空），默认启用 Thumbnails
    val availableModes = controller.viewProperties.availableSidebarViewModes
    return if (availableModes.isEmpty()) {
      // PDF 加载中或刚加载，默认只启用 Thumbnails
      targetViewMode == SidebarViewMode.THUMBNAILS || targetViewMode == SidebarViewMode.NONE
    } else {
      targetViewMode in availableModes || targetViewMode == SidebarViewMode.NONE
    }
  }

  class Hide : PdfSetSidebarViewModeAction(SidebarViewMode.NONE) {
    override fun update(event: AnActionEvent) {
      super.update(event)
      event.presentation.isEnabled = true
    }
  }

  class Thumbnails : PdfSetSidebarViewModeAction(SidebarViewMode.THUMBNAILS)

  class Outline : PdfSetSidebarViewModeAction(SidebarViewMode.OUTLINE)

  class Attachments : PdfSetSidebarViewModeAction(SidebarViewMode.ATTACHMENTS)
}
