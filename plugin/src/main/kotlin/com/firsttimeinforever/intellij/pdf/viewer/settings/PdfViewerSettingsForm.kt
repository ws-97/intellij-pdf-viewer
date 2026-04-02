package com.firsttimeinforever.intellij.pdf.viewer.settings

import com.firsttimeinforever.intellij.pdf.viewer.PdfViewerBundle
import com.firsttimeinforever.intellij.pdf.viewer.model.SidebarViewMode
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.not
import com.intellij.ui.ColorPanel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.dsl.builder.*
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import javax.swing.DefaultComboBoxModel
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

class PdfViewerSettingsForm : JPanel() {
  private val settings
    get() = PdfViewerSettings.instance

  private val properties = PropertyGraph()

  val enableDocumentAutoReload = properties.property(settings.enableDocumentAutoReload)
  val defaultSidebarViewMode = properties.property(settings.defaultSidebarViewMode)
  val useToolWindowMode = properties.property(settings.useToolWindowMode)
  
  private var recentPdfPanel: RecentPdfPanel? = null

  private val generalSettingsGroup = panel {
    group(PdfViewerBundle.message("pdf.viewer.settings.group.general")) {
      row {
        checkBox(PdfViewerBundle.message("pdf.viewer.settings.reload.document"))
          .bindSelected(enableDocumentAutoReload)
      }
      row(PdfViewerBundle.message("pdf.viewer.settings.sidebar.viewer.default")) {
        val renderer = SimpleListCellRenderer.create<SidebarViewMode> { label, value, _ ->
          label.text = when (value) {
            SidebarViewMode.NONE -> "Closed"
            SidebarViewMode.THUMBNAILS -> "Thumbnails"
            SidebarViewMode.OUTLINE -> "Outline (document structure)"
            SidebarViewMode.ATTACHMENTS -> "Attachments"
          }
        }
        comboBox(DefaultComboBoxModel(SidebarViewMode.entries.toTypedArray()), renderer)
          .bindItem(defaultSidebarViewMode)
      }
      row {
        checkBox("使用边缘工具窗口显示 PDF")
          .bindSelected(useToolWindowMode)
          .comment("启用后，PDF 文件将在右侧边缘工具窗口中打开，而不是占用编辑区域")
      }
    }
  }
  
  // 最近浏览的 PDF 面板组 - 单独创建以便延迟初始化
  private fun createRecentPdfGroup(): JPanel {
    return panel {
      // 添加最近浏览的 PDF 文档标题
      row {
        val titleLabel = JLabel(PdfViewerBundle.message("pdf.viewer.settings.group.recent.documents"))
        titleLabel.font = titleLabel.font.deriveFont(java.awt.Font.BOLD)
        titleLabel.border = EmptyBorder(10, 0, 5, 0)
        cell(titleLabel).align(AlignX.FILL)
      }
      // 添加最近浏览的 PDF 面板
      row {
        val panelToUse = recentPdfPanel ?: run {
          initRecentPdfPanel()
          recentPdfPanel!!
        }
        cell(panelToUse).align(AlignX.FILL).resizableColumn()
      }
    }
  }
  
  // 移除了重复的 recentDocumentsGroup 声明，现在由 UI DSL 动态创建

  val invertDocumentColorsWithTheme = properties.property(settings.invertColorsWithTheme).apply {
    afterPropagation {
      // Automatically toggle the invertDocumentColors checkbox so the pdf color switched to the current theme.
      if (this.get()) invertDocumentColors.set(EditorColorsManager.getInstance().isDarkEditor)
    }
  }
  val invertDocumentColors = properties.property(settings.invertDocumentColors)
  val documentColorsInvertIntensity = properties.property(settings.documentColorsInvertIntensity)

  private val invertColorsGroup = panel {
    group(PdfViewerBundle.message("pdf.viewer.settings.group.colors.document")) {
      row {
        checkBox(PdfViewerBundle.message("pdf.viewer.settings.colors.document.with.theme"))
          .bindSelected(invertDocumentColorsWithTheme)
          .comment(PdfViewerBundle.message("pdf.viewer.settings.colors.document.with.theme.comment"))
      }
      row {
        checkBox(PdfViewerBundle.message("pdf.viewer.settings.colors.document.invert"))
          .bindSelected(invertDocumentColors)
          .enabledIf(invertDocumentColorsWithTheme.not())
      }
      row(PdfViewerBundle.message("pdf.viewer.settings.colors.document.invert.intensity")) {
        intTextField(1..100, 1)
          .bindIntText(documentColorsInvertIntensity)
        rowComment(PdfViewerBundle.message("pdf.viewer.settings.colors.document.invert.intensity.comment"))
      }
    }
  }

  val useCustomColors = properties.property(settings.useCustomColors)
  val customBackgroundColor = properties.property(settings.customBackgroundColor)
  val customForegroundColor = properties.property(settings.customForegroundColor)
  val customIconColor = properties.property(settings.customIconColor)

  private val backgroundColorPanel = ColorPanel().apply {
    selectedColor = Color(customBackgroundColor.get())
    addActionListener {
      selectedColor?.let { customBackgroundColor.set(it.rgb) }
    }
  }
  private val foregroundColorPanel = ColorPanel().apply {
    addActionListener {
      selectedColor?.let { customForegroundColor.set(it.rgb) }
    }
  }
  private val iconColorPanel = ColorPanel().apply {
    addActionListener {
      selectedColor?.let { customIconColor.set(it.rgb) }
    }
  }

  private val customColorsGroup = panel {
    group(PdfViewerBundle.message("pdf.viewer.settings.group.colors.viewer")) {
      row {
        checkBox(PdfViewerBundle.message("pdf.viewer.settings.viewer.colors"))
          .bindSelected(useCustomColors)
          .comment(PdfViewerBundle.message("pdf.viewer.settings.group.colors.viewer.comment"))
      }
      indent {
          panel {
            row(PdfViewerBundle.message("pdf.viewer.settings.foreground")) {
              cell(foregroundColorPanel)
            }
            row(PdfViewerBundle.message("pdf.viewer.settings.background")) {
              cell(backgroundColorPanel)
            }
            row(PdfViewerBundle.message("pdf.viewer.settings.icons")) {
              cell(iconColorPanel)
              rowComment(PdfViewerBundle.message("pdf.viewer.settings.icons.color.notice"))
            }
            row {
              link(PdfViewerBundle.message("pdf.viewer.settings.set.current.theme")) {
                resetViewerColorsToTheme()
              }
            }
          }.enabledIf(useCustomColors)
      }
    }
  }

  init {
    layout = BorderLayout()
    // 先初始化最近 PDF 面板，确保在添加到 UI 之前已经准备好
    initRecentPdfPanel()
    add(panel {
      row { cell(generalSettingsGroup).align(AlignX.FILL) }
      row { cell(createRecentPdfGroup()).align(AlignX.FILL) }
      row { cell(invertColorsGroup).align(AlignX.FILL) }
      row { cell(customColorsGroup).align(AlignX.FILL) }
    })
  }
  
  fun initRecentPdfPanel() {
    if (recentPdfPanel == null) {
      recentPdfPanel = RecentPdfPanel()
    }
  }
  
  /**
   * 刷新最近阅读的 PDF 列表
   * 在设置面板打开时调用，确保显示最新的阅读记录
   */
  fun refreshRecentPdfPanel() {
    recentPdfPanel?.refresh()
  }

  fun reset() {
    enableDocumentAutoReload.set(settings.enableDocumentAutoReload)
    defaultSidebarViewMode.set(settings.defaultSidebarViewMode)
    useToolWindowMode.set(settings.useToolWindowMode)
    invertDocumentColorsWithTheme.set(settings.invertColorsWithTheme)
    invertDocumentColors.set(settings.invertDocumentColors)
    documentColorsInvertIntensity.set(settings.documentColorsInvertIntensity)
    useCustomColors.set(settings.useCustomColors)
    customForegroundColor.set(settings.customForegroundColor)
    customBackgroundColor.set(settings.customBackgroundColor)
    customIconColor.set(settings.customIconColor)
  }

  private fun resetViewerColorsToTheme() {
    PdfViewerSettings.run {
      backgroundColorPanel.selectedColor = defaultBackgroundColor
      customBackgroundColor.set(defaultBackgroundColor.rgb)
      foregroundColorPanel.selectedColor = defaultForegroundColor
      customForegroundColor.set(defaultForegroundColor.rgb)
      iconColorPanel.selectedColor = defaultIconColor
      customIconColor.set(defaultIconColor.rgb)
    }
  }
}
