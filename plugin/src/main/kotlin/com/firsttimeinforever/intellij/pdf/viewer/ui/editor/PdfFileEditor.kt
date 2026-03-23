package com.firsttimeinforever.intellij.pdf.viewer.ui.editor

import com.firsttimeinforever.intellij.pdf.viewer.model.ViewState
import com.firsttimeinforever.intellij.pdf.viewer.model.ViewStateChangeReason
import com.firsttimeinforever.intellij.pdf.viewer.settings.PdfViewerSettings
import com.firsttimeinforever.intellij.pdf.viewer.settings.PdfViewerSettingsListener
import com.intellij.openapi.components.service
import com.firsttimeinforever.intellij.pdf.viewer.structureView.PdfStructureViewBuilder
import com.firsttimeinforever.intellij.pdf.viewer.ui.editor.view.PdfEditorViewComponent
import com.firsttimeinforever.intellij.pdf.viewer.ui.editor.view.PdfJcefPreviewController
import com.firsttimeinforever.intellij.pdf.viewer.ui.editor.view.PdfViewStateChangedListener
import com.intellij.diff.util.FileEditorBase
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import javax.swing.JComponent

// TODO: Implement state persistence
class PdfFileEditor(project: Project, val virtualFile: VirtualFile) : FileEditorBase(), DumbAware {
  val viewComponent = PdfEditorViewComponent(project, virtualFile)
  private val messageBusConnection = project.messageBus.connect()
  private val fileChangedListener = FileChangedListener(PdfViewerSettings.instance.enableDocumentAutoReload)
  private var currentPage = 1

  init {
    Disposer.register(this, viewComponent)
    Disposer.register(this, messageBusConnection)
    messageBusConnection.subscribe(VirtualFileManager.VFS_CHANGES, fileChangedListener)
    messageBusConnection.subscribe(PdfViewerSettings.TOPIC, PdfViewerSettingsListener {
      fileChangedListener.isEnabled = it.enableDocumentAutoReload
    })

    // 监听页码变化，保存当前页码
    // 使用延迟订阅，确保 controller 已经初始化完成
    javax.swing.SwingUtilities.invokeLater {
      viewComponent.controller?.let { controller ->
        val connection = project.messageBus.connect(this)
        connection.subscribe(PdfViewStateChangedListener.TOPIC, object : PdfViewStateChangedListener {
          override fun viewStateChanged(controller: PdfJcefPreviewController, state: ViewState, reason: ViewStateChangeReason) {
            currentPage = state.page
            // 保存 PDF 浏览进度 - 只在页面变化或文档加载完成时保存
            if (reason == ViewStateChangeReason.PAGE_NUMBER || reason == ViewStateChangeReason.UNSPECIFIED) {
              try {
                val totalPages = controller.viewProperties.pagesCount
                val recentPdfService = project.service<com.firsttimeinforever.intellij.pdf.viewer.settings.RecentPdfService>()
                recentPdfService.addRecentPdf(virtualFile.path, currentPage, totalPages)
                logger.debug("已保存 PDF 浏览进度：${virtualFile.name}, 第 $currentPage/$totalPages 页")
              } catch (e: Throwable) {
                logger.warn("保存 PDF 浏览进度失败：${virtualFile.path}", e)
              }
            }
          }
        })
      }
    }
  }

  override fun getName(): String = NAME

  override fun getFile(): VirtualFile = virtualFile

  override fun getComponent(): JComponent = viewComponent

  override fun getPreferredFocusedComponent(): JComponent = viewComponent.controlPanel

  private inner class FileChangedListener(var isEnabled: Boolean = true) : BulkFileListener {
    override fun after(events: MutableList<out VFileEvent>) {
      if (!isEnabled) {
        return
      }
      if (viewComponent.controller == null) {
        logger.warn("FileChangedListener was called for view with controller == null!")
      } else if (events.any { it.file == virtualFile }) {
        logger.debug("Target file ${virtualFile.path} changed. Reloading current view.")
        viewComponent.controller.reload(tryToPreserveState = true)
      }
    }
  }

  override fun getStructureViewBuilder(): StructureViewBuilder {
    return PdfStructureViewBuilder(this)
  }

  override fun getState(level: FileEditorStateLevel): FileEditorState {
    logger.debug("Saving state for ${virtualFile.path}, current page: $currentPage")
    return PdfFileEditorState(currentPage)
  }

  override fun setState(state: FileEditorState) {
    if (state is PdfFileEditorState) {
      val pageToRestore = state.getCurrentPage()
      logger.debug("Restoring state for ${virtualFile.path}, page: $pageToRestore")
      // 延迟恢复页码，等待视图加载完成
      viewComponent.controller?.let { controller ->
        javax.swing.SwingUtilities.invokeLater {
          // 先更新页码状态，然后重新加载
          controller.updateViewState(pageToRestore)
          controller.reload(tryToPreserveState = true)
        }
      }
    }
  }

  companion object {
    private const val NAME = "Pdf Viewer File Editor"
    private val logger = logger<PdfFileEditor>()
  }
}
