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
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel

// 空的占位组件，不显示任何内容且不占用空间
private class EmptyEditorComponent : JPanel() {
  init {
    isOpaque = false
    preferredSize = Dimension(0, 0)
    minimumSize = Dimension(0, 0)
    maximumSize = Dimension(0, 0)
  }
  
  override fun paint(g: java.awt.Graphics?) {
    // 完全不绘制任何内容
  }
}

class PdfFileEditor(project: Project, val virtualFile: VirtualFile) : FileEditorBase(), DumbAware {
  val viewComponent: PdfEditorViewComponent = PdfEditorViewComponent(project, virtualFile)
  
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

    javax.swing.SwingUtilities.invokeLater {
      viewComponent.controller?.let { controller ->
        val connection = project.messageBus.connect(this)
        connection.subscribe(PdfViewStateChangedListener.TOPIC, object : PdfViewStateChangedListener {
          override fun viewStateChanged(controller: PdfJcefPreviewController, state: ViewState, reason: ViewStateChangeReason) {
            currentPage = state.page
            if (reason == ViewStateChangeReason.PAGE_NUMBER || reason == ViewStateChangeReason.UNSPECIFIED) {
              try {
                val totalPages = controller.viewProperties.pagesCount
                val recentPdfService = project.service<com.firsttimeinforever.intellij.pdf.viewer.settings.RecentPdfService>()
                recentPdfService.addRecentPdf(virtualFile.path, currentPage, totalPages)
              } catch (e: Throwable) {
                logger.warn("保存 PDF 浏览进度失败", e)
              }
            }
          }
        })
      }
    }
  }

  override fun getName(): String = "PDF Viewer"
  override fun getFile(): VirtualFile = virtualFile
  
  // 返回空组件，不在编辑区域显示
  private val emptyComponent = EmptyEditorComponent()
  override fun getComponent(): JComponent = emptyComponent
  override fun getPreferredFocusedComponent(): JComponent? = null
  
  // 关键：返回 false，这样编辑器不会创建标签页
  override fun isModified(): Boolean = false

  private inner class FileChangedListener(var isEnabled: Boolean = true) : BulkFileListener {
    override fun after(events: MutableList<out VFileEvent>) {
      if (!isEnabled || viewComponent.controller == null) return
      if (events.any { it.file == virtualFile }) {
        viewComponent.controller.reload(tryToPreserveState = true)
      }
    }
  }

  override fun getStructureViewBuilder(): StructureViewBuilder = PdfStructureViewBuilder(this)

  override fun getState(level: FileEditorStateLevel): FileEditorState {
    return PdfFileEditorState(currentPage)
  }

  override fun setState(state: FileEditorState) {
    if (state is PdfFileEditorState) {
      viewComponent.controller?.let { controller ->
        javax.swing.SwingUtilities.invokeLater {
          controller.updateViewState(state.getCurrentPage())
          controller.reload(tryToPreserveState = true)
        }
      }
    }
  }

  companion object {
    private val logger = logger<PdfFileEditor>()
  }
}
