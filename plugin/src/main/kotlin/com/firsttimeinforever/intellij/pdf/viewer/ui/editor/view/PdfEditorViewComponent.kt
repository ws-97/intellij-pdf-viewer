package com.firsttimeinforever.intellij.pdf.viewer.ui.editor.view

import com.firsttimeinforever.intellij.pdf.viewer.BrowserMessages
import com.firsttimeinforever.intellij.pdf.viewer.mpi.MessagePipeSupport.subscribe
import com.firsttimeinforever.intellij.pdf.viewer.ui.editor.view.controls.PdfEditorControlPanel
import com.firsttimeinforever.intellij.pdf.viewer.ui.editor.view.controls.PdfSearchPanel
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import net.miginfocom.swing.MigLayout
import javax.swing.JPanel

class PdfEditorViewComponent(val project: Project, virtualFile: VirtualFile) : JPanel(), Disposable {
  // 使用委托的 Disposable 来管理子组件的生命周期
  private val disposable = Disposer.newDisposable("PdfEditorViewComponent")
  
  val controlPanel = PdfEditorControlPanel()
  val controller = PdfPreviewControllerProvider.createViewController(project, virtualFile)
  val searchPanel = PdfSearchPanel(this)

  // 将 wrapperPanel 改为 public，供工具窗口使用
  val wrapperPanel = JPanel(MigLayout("flowy, fillx, ins 0, gap 0, hidemode 3")).apply {
    add(controlPanel, "growx, pushx")
    add(searchPanel, "growx, pushx")
  }

  init {
    logger.info("Creating PdfEditorViewComponent for: ${virtualFile.path}")
    logger.info("JCEF supported: ${com.intellij.ui.jcef.JBCefApp.isSupported()}")
    
    // 注册 controller 和 searchPanel，但不注册 controlPanel
    // controlPanel 是可复用组件，不应该被 dispose
    Disposer.register(disposable, searchPanel)
    if (controller != null) {
      Disposer.register(disposable, controller)
      logger.info("Controller created successfully")
    } else {
      logger.error("View controller is null! JCEF may not be supported or initialization failed.")
    }
    // 不再使用 BoxLayout，改用 MigLayout 来管理整体布局
    layout = MigLayout("flowy, fillx, ins 0, gap 0, hidemode 3")
    add(wrapperPanel, "growx, pushx")
    val componentToAdd = controller?.component ?: PdfUnsupportedViewPanel()
    logger.info("Adding component: ${componentToAdd.javaClass.simpleName}")
    add(componentToAdd, "grow, push")
    controller?.pipe?.subscribe<BrowserMessages.SearchResponse> {
      searchPanel.updateResults(it.result.currentMatch, it.result.totalMatches)
    }
  }

  override fun dispose() {
    // 清理所有子组件
    Disposer.dispose(disposable)
  }

  companion object {
    private val logger = logger<PdfEditorViewComponent>()
  }
}
