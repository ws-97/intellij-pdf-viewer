package com.firsttimeinforever.intellij.pdf.viewer.ui.widgets

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.firsttimeinforever.intellij.pdf.viewer.ui.editor.PdfFileEditor
import com.firsttimeinforever.intellij.pdf.viewer.ui.editor.view.PdfJcefPreviewController
import com.firsttimeinforever.intellij.pdf.viewer.settings.RecentPdfService

internal class PdfStatusBarProjectActivity : ProjectActivity {
  private val logger: Logger = Logger.getInstance(PdfStatusBarProjectActivity::class.java)

  override suspend fun execute(project: Project) {
    logger.debug("Registering new FileEditorManagerListener for newly opened project")
    project.messageBus.connect().subscribe(
      FileEditorManagerListener.FILE_EDITOR_MANAGER,
      object : FileEditorManagerListener {
        override fun selectionChanged(event: FileEditorManagerEvent) {
          logger.debug("Selection changed")
          @Suppress("IncorrectServiceRetrieving") // Incorrect: PdfStatusBarProjectManagerListener is a project service
          project.service<StatusBarWidgetsManager>().run {
            logger.debug("Updating widget")
            updateWidget(PdfDocumentPageStatusBarWidgetFactory::class.java)
          }
          
          // 移除重复的最近浏览记录逻辑，由 PdfFileEditor 负责记录
          /*
          // 记录最近浏览的PDF文件
          event.newEditor.takeIf { it is PdfFileEditor }?.let { pdfEditor ->
            val controller = (pdfEditor as PdfFileEditor).viewComponent.controller
            if (controller != null) {
              val filePath = pdfEditor.virtualFile.path
              val currentPage = controller.viewState.page
              val totalPages = controller.viewProperties.pagesCount
              
              // 使用服务记录最近浏览的PDF
              project.service<RecentPdfService>().addRecentPdf(filePath, currentPage, totalPages)
            }
          }
          */
        }
      }
    )
  }
}