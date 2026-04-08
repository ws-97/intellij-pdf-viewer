package com.firsttimeinforever.intellij.pdf.viewer.ui.toolwindow

import com.firsttimeinforever.intellij.pdf.viewer.settings.RecentPdfService
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLabel
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.ListCellRenderer
import javax.swing.border.EmptyBorder
import javax.swing.BorderFactory
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities

/**
 * 最近 PDF 列表面板，用于工具窗口中显示
 */
class RecentPdfListPanel(
    private val project: Project,
    recentPdfs: List<RecentPdfService.RecentPdfItem>,
    private val onItemClick: (String) -> Unit
) : JPanel() {

    companion object {
        private val logger = logger<RecentPdfListPanel>()
        private const val CELL_HEIGHT = 70
    }

    private val pdfListModel = DefaultListModel<PdfItem>()
    private val pdfListView = JBList(pdfListModel)

    init {
        layout = BorderLayout()

        // 加载数据
        recentPdfs.forEach { item ->
            pdfListModel.addElement(PdfItem(item.filePath, item.currentPage, item.totalPages))
        }

        // 配置列表
        pdfListView.cellRenderer = PdfListCellRenderer()
        pdfListView.fixedCellHeight = CELL_HEIGHT
        pdfListView.selectionMode = ListSelectionModel.SINGLE_SELECTION

        // 添加鼠标监听器
        pdfListView.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (!SwingUtilities.isLeftMouseButton(e)) return

                val idx = pdfListView.locationToIndex(e.point)
                if (idx == -1) return

                val item = pdfListModel.getElementAt(idx)
                onItemClick(item.filePath)
            }
        })

        // 添加到滚动面板
        val scrollPane = JBScrollPane(pdfListView)
        scrollPane.border = EmptyBorder(0, 0, 0, 0)
        add(scrollPane, BorderLayout.CENTER)

        preferredSize = Dimension(300, 400)
    }

    data class PdfItem(
        val filePath: String,
        val currentPage: Int,
        val totalPages: Int
    ) {
        val fileName: String
            get() = java.io.File(filePath).name

        val progress: Int
            get() = if (totalPages > 0) ((currentPage.toDouble() / totalPages) * 100).toInt().coerceIn(0, 100) else 0
    }

    private inner class PdfListCellRenderer : JPanel(), ListCellRenderer<PdfItem> {
        private val fileNameLabel = JLabel()
        private val pageInfoLabel = JLabel()
        private val progressBar = JProgressBar(0, 100).apply {
            preferredSize = Dimension(0, 12)
            border = BorderFactory.createLineBorder(Color.GRAY)
            foreground = Color(76, 175, 80)
            background = Color(33, 150, 243)
            isStringPainted = true
            isIndeterminate = false
        }

        init {
            layout = BorderLayout(10, 5)
            border = EmptyBorder(8, 12, 8, 12)

            val leftPanel = JPanel(BorderLayout())
            fileNameLabel.font = fileNameLabel.font.deriveFont(Font.BOLD, 13f)
            pageInfoLabel.font = pageInfoLabel.font.deriveFont(11f)
            pageInfoLabel.foreground = Color.GRAY

            leftPanel.add(fileNameLabel, BorderLayout.NORTH)
            leftPanel.add(pageInfoLabel, BorderLayout.CENTER)

            add(leftPanel, BorderLayout.CENTER)
            add(progressBar, BorderLayout.SOUTH)

            isOpaque = true
            leftPanel.isOpaque = false
            fileNameLabel.isOpaque = false
            pageInfoLabel.isOpaque = false

            // 确保渲染器组件不拦截鼠标事件
            fileNameLabel.isEnabled = false
            pageInfoLabel.isEnabled = false
            progressBar.isEnabled = false
            leftPanel.isEnabled = false
        }

        override fun getListCellRendererComponent(
            list: JList<out PdfItem>?,
            value: PdfItem,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            fileNameLabel.text = value.fileName
            pageInfoLabel.text = "阅读进度: ${value.currentPage}/${value.totalPages} 页"
            progressBar.value = value.progress
            progressBar.string = "${value.progress}%"
            progressBar.repaint()

            val bgColor = if (isSelected) list?.selectionBackground else list?.background
            val fgColor = if (isSelected) list?.selectionForeground else list?.foreground

            background = bgColor
            foreground = fgColor
            fileNameLabel.background = bgColor
            fileNameLabel.foreground = fgColor
            pageInfoLabel.background = bgColor
            pageInfoLabel.foreground = fgColor

            isOpaque = true

            return this
        }


    }
}
