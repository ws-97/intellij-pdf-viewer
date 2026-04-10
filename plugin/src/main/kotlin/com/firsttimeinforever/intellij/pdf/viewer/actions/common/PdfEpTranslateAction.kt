package com.firsttimeinforever.intellij.pdf.viewer.actions.common

import com.firsttimeinforever.intellij.pdf.viewer.actions.PdfDumbAwareAction
import com.firsttimeinforever.intellij.pdf.viewer.actions.ViewModeAwareness
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent
import java.awt.Dimension
import java.net.URLEncoder

class PdfEpTranslateAction : PdfDumbAwareAction(ViewModeAwareness.IDE) {
  override fun actionPerformed(event: AnActionEvent) {
    val project = event.project ?: return
    
    // 获取选中的文本
    val selectedText = getSelectedText(event)
    val encodedWord = if (selectedText.isNullOrEmpty()) "" else URLEncoder.encode(selectedText, "UTF-8")
    val url = "http://45.207.201.56/word?word=$encodedWord"
    
    // 创建对话框
    val dialog = object : DialogWrapper(project, true) {
      init {
        init()
        title = "epTranslate"
      }

      override fun createCenterPanel(): JComponent? {
        // 创建 JCEF 浏览器
        val browser = JBCefBrowser(url)
        browser.component.preferredSize = Dimension(1200, 800)
        return browser.component
      }

      override fun getPreferredFocusedComponent(): JComponent? {
        return createCenterPanel()
      }
    }
    
    dialog.setSize(1200, 800)
    dialog.show()
  }

  private fun getSelectedText(event: AnActionEvent): String? {
    // 尝试从编辑器中获取选中的文本
    val editor = event.getData(CommonDataKeys.EDITOR)
    if (editor != null) {
      val selectionModel = editor.selectionModel
      if (selectionModel.hasSelection()) {
        return selectionModel.selectedText
      }
    }
    return null
  }

  override fun update(event: AnActionEvent) {
    // 在编辑器视图或工具窗口中有 PDF 时才可见
    val controller = findController(event)
    val editor = event.getData(CommonDataKeys.EDITOR)
    
    with(event.presentation) {
      isVisible = editor != null || controller != null
      isEnabled = true
    }
  }
}
