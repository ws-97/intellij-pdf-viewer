package com.firsttimeinforever.intellij.pdf.viewer.actions

import com.firsttimeinforever.intellij.pdf.viewer.PdfViewerBundle
import com.firsttimeinforever.intellij.pdf.viewer.ui.editor.PdfFileEditor
import com.firsttimeinforever.intellij.pdf.viewer.ui.editor.view.PdfJcefPreviewController
import com.intellij.ide.ui.UISettings
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager

abstract class PdfAction(protected val viewModeAwareness: ViewModeAwareness = ViewModeAwareness.IDE) : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(event: AnActionEvent) {
    val editor = findEditorInView(event)
    var controller = findController(editor)
    
    // 如果编辑器中找不到，尝试从工具窗口中找
    if (controller == null) {
      val project = event.project
      if (project != null) {
        val toolWindow = com.intellij.openapi.wm.ToolWindowManager.getInstance(project).getToolWindow("PDF Viewer")
        val content = toolWindow?.contentManager?.selectedContent?.component as? javax.swing.JPanel
        controller = content?.getClientProperty("PDF_CONTROLLER") as? PdfJcefPreviewController
      }
    }
    
    with(event.presentation) {
      // 在编辑器视图或工具窗口中有 PDF 时才可见
      isVisible = editor != null || controller != null
      isEnabled = controller != null
    }
    adjustPresentationVisibility(event)
  }

  protected fun adjustPresentationVisibility(event: AnActionEvent) {
    val controller = findController(event) ?: return
    val idePresentation = UISettings.getInstance().presentationMode
    val documentPresentation = controller.presentationController.isPresentationModeActive
    event.presentation.isEnabledAndVisible = when (viewModeAwareness) {
      ViewModeAwareness.NONE -> !idePresentation && !documentPresentation
      ViewModeAwareness.IDE -> !documentPresentation
      ViewModeAwareness.IDE_ONLY -> idePresentation && !documentPresentation
      ViewModeAwareness.DOCUMENT -> !idePresentation
      ViewModeAwareness.DOCUMENT_ONLY -> !idePresentation && documentPresentation
      ViewModeAwareness.BOTH -> true
      ViewModeAwareness.BOTH_ONLY -> idePresentation && documentPresentation
    }
  }

  companion object {

    /**
     * Find the editor that belongs to the given [event].
     *
     * If the editor that currently has focus is a [PdfFileEditor], return that one. If not, look for any other PdfFileEditors that are open
     * (split view) and select the first we find there. If there is no [PdfFileEditor] selected, this returns null. Note that in that case it
     * is possible that there is a PDF open somewhere, but it is not in view.
     */
    fun findEditorInView(event: AnActionEvent): PdfFileEditor? {
      val focusedEditor = event.getData(PlatformDataKeys.FILE_EDITOR) as? PdfFileEditor

      return focusedEditor ?: run {
        val project = event.project ?: return null
        FileEditorManager.getInstance(project).selectedEditors.firstOrNull {it is PdfFileEditor} as? PdfFileEditor
      }
    }

    fun hasEditorInView(event: AnActionEvent): Boolean {
      return findEditorInView(event) != null
    }

    fun findController(event: AnActionEvent): PdfJcefPreviewController? {
      // 先尝试从编辑器中找
      return findEditorInView(event)?.viewComponent?.controller ?: run {
        // 如果找不到，尝试从工具窗口中找
        val project = event.project
        if (project != null) {
          val toolWindow = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
            .getToolWindow("PDF Viewer")
          val content = toolWindow?.contentManager?.selectedContent
          val mainPanel = content?.component as? javax.swing.JPanel
          if (mainPanel != null) {
            // 从 client property 中获取 controller
            val controller = mainPanel.getClientProperty("PDF_CONTROLLER") as? PdfJcefPreviewController
            if (controller != null) {
              com.intellij.openapi.diagnostic.logger<PdfAction>().info("findController from tool window: success")
              return controller
            }
          }
        }
        
        com.intellij.openapi.diagnostic.logger<PdfAction>().warn("findController failed")
        return null
      }
    }

    fun findController(editor: PdfFileEditor?): PdfJcefPreviewController? {
      return editor?.viewComponent?.controller
    }

    fun showUnsupportedActionNotification(event: AnActionEvent) {
      Notifications.Bus.notify(
        Notification(
          PdfViewerBundle.message("pdf.viewer.notifications.group.id"),
          PdfViewerBundle.message("pdf.viewer.actions.pdfjs.notifications.usupported.action.title"),
          PdfViewerBundle.message(
            "pdf.viewer.actions.pdfjs.notifications.unsupported.action.content",
            event.presentation.text
          ),
          NotificationType.ERROR
        )
      )
    }
  }
}
