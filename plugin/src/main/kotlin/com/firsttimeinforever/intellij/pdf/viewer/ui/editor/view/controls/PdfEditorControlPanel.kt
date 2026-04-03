package com.firsttimeinforever.intellij.pdf.viewer.ui.editor.view.controls

import com.firsttimeinforever.intellij.pdf.viewer.actions.PdfActionUtils.createActionToolbar
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionPlaces
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import javax.swing.JPanel

class PdfEditorControlPanel: JPanel(java.awt.BorderLayout()), Disposable {
  private val leftToolbar = createActionToolbar(
    "pdf.viewer.LeftToolbarActionGroup",
    ActionPlaces.UNKNOWN,  // 使用 UNKNOWN place，让 toolbar 在工具窗口中也能正常工作
    this
  )

  private val rightToolbar = createActionToolbar(
    "pdf.viewer.RightToolbarActionGroup",
    ActionPlaces.UNKNOWN,  // 使用 UNKNOWN place
    this
  )

  private val rightPanel = JPanel()

  init {
    leftToolbar.component.border = null
    rightToolbar.component.border = null
    
    // 将 leftToolbar 添加到左侧 (WEST)
    add(leftToolbar.component, java.awt.BorderLayout.WEST)

    rightPanel.layout = FlowLayout(FlowLayout.RIGHT, 0, 0)
    rightPanel.add(rightToolbar.component)
    
    // 将 rightPanel 添加到右侧 (EAST)
    add(rightPanel, java.awt.BorderLayout.EAST)
  }

  /**
   * 设置 toolbar 的 targetComponent
   * 用于工具窗口场景，让 toolbar actions 能找到正确的上下文
   */
  fun setToolbarTarget(targetComponent: java.awt.Component) {
    if (targetComponent is javax.swing.JComponent) {
      leftToolbar.targetComponent = targetComponent
      rightToolbar.targetComponent = targetComponent
    }
  }

  override fun dispose() = Unit
}
