package com.firsttimeinforever.intellij.pdf.viewer.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.JLabel
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JComponent
import javax.swing.ListCellRenderer
import javax.swing.plaf.basic.BasicProgressBarUI
import javax.swing.border.EmptyBorder
import javax.swing.BorderFactory
import java.awt.Desktop
import java.awt.Font
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities

class RecentPdfPanel : JPanel() {
    private val logger = Logger.getInstance(RecentPdfPanel::class.java)
    private val pdfListModel = DefaultListModel<PdfItem>()
    private val pdfListView = JBList(pdfListModel)
    private var scrollPane: JBScrollPane? = null
    private lateinit var messageLabel: JLabel
    
    companion object {
        private const val ICON_WIDTH = 30
        private const val ICON_HEIGHT = 30
        private const val ICON_PADDING_RIGHT = 40
        private const val ICON_PADDING_TOP = 10
        private const val CELL_HEIGHT = 60
    }

    init {
        layout = BorderLayout()
        pdfListView.cellRenderer = PdfListCellRenderer()
        pdfListView.fixedCellHeight = CELL_HEIGHT
        pdfListView.selectionMode = ListSelectionModel.SINGLE_SELECTION
            
        setupMouseListener()
        setupScrollPane()
        setupMessageLabel()
            
        loadRecentPdfs()
        updateVisibility()
    }
        
    private fun setupMouseListener() {
        pdfListView.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (!SwingUtilities.isLeftMouseButton(e)) return
                    
                val idx = pdfListView.locationToIndex(e.point)
                if (idx == -1) return
                    
                val item = pdfListModel.getElementAt(idx)
                val cellBounds = pdfListView.getCellBounds(idx, idx) ?: return
                    
                if (isClickInFolderIconArea(e, cellBounds)) {
                    openFolder(item.filePath)
                } else {
                    openPdfFile(item.filePath)
                }
            }
        })
    }
        
    private fun isClickInFolderIconArea(e: MouseEvent, cellBounds: java.awt.Rectangle): Boolean {
        val iconX = cellBounds.x + cellBounds.width - ICON_PADDING_RIGHT
        val iconY = cellBounds.y + ICON_PADDING_TOP
        return e.x in iconX..(iconX + ICON_WIDTH) && e.y in iconY..(iconY + ICON_HEIGHT)
    }
        
    private fun setupScrollPane() {
        scrollPane = JBScrollPane(pdfListView)
        scrollPane?.border = EmptyBorder(0, 0, 0, 0)
        add(scrollPane!!, BorderLayout.CENTER)
    }
        
    private fun setupMessageLabel() {
        messageLabel = JLabel("最近阅读的 PDF 将在项目中显示")
        messageLabel.horizontalAlignment = JLabel.CENTER
        messageLabel.foreground = Color.GRAY
        messageLabel.font = messageLabel.font.deriveFont(Font.BOLD, 14f)
        add(messageLabel, BorderLayout.SOUTH)
        preferredSize = Dimension(0, 200)
    }

    private fun updateVisibility() {
        val hasData = pdfListModel.size() > 0
        scrollPane?.isVisible = hasData
        messageLabel.isVisible = !hasData
    }

    private fun openPdfFile(filePath: String) {
        try {
            val projectManager = com.intellij.openapi.project.ProjectManager.getInstance()
            val openProjects = projectManager.openProjects
            if (openProjects.isEmpty()) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog("没有打开的项目，无法打开 PDF", "PDF Viewer")
                }
                return
            }
            
            // 在后台线程中查找文件
            com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    val file = com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath(filePath)
                    if (file == null) {
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showErrorDialog("文件不存在:\n$filePath", "PDF Viewer")
                        }
                        return@executeOnPooledThread
                    }
                    
                    ApplicationManager.getApplication().invokeLater {
                        try {
                            // 优先使用文件所属的项目，如果没有则使用第一个项目
                            val project = openProjects.firstOrNull { proj ->
                                com.intellij.openapi.roots.ProjectFileIndex.getInstance(proj).isInContent(file)
                            } ?: openProjects.firstOrNull()
                            
                            if (project != null) {
                                val fileEditorManager = FileEditorManager.getInstance(project)
                                val descriptor = OpenFileDescriptor(project, file)
                                fileEditorManager.openEditor(descriptor, true)
                            } else {
                                Messages.showErrorDialog("无法找到合适的项目来打开文件", "PDF Viewer")
                            }
                        } catch (e: Throwable) {
                            logger.error("打开 PDF 文件失败：$filePath", e)
                            Messages.showErrorDialog("打开 PDF 文件失败：${e.message}", "PDF Viewer")
                        }
                    }
                } catch (e: Throwable) {
                    logger.error("查找 PDF 文件失败：$filePath", e)
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog("查找 PDF 文件失败：${e.message}", "PDF Viewer")
                    }
                }
            }
        } catch (e: Throwable) {
            logger.error("调度打开 PDF 文件失败：$filePath", e)
            Messages.showErrorDialog("调度打开 PDF 文件失败：${e.message}", "PDF Viewer")
        }
    }

    private fun openFolder(fileName: String) {
        try {
            val file = File(fileName)
            val folder = if (file.isDirectory) file else file.parentFile
            if (folder != null && folder.exists()) {
                Desktop.getDesktop().open(folder)
            } else {
                Messages.showErrorDialog("文件夹不存在:\n$fileName", "PDF Viewer")
            }
        } catch (e: Exception) {
            logger.error("打开文件夹失败：$fileName", e)
            Messages.showErrorDialog("打开文件夹失败：${e.message}", "PDF Viewer")
        }
    }

    private fun loadRecentPdfs() {
        try {
            pdfListModel.clear()
            val openProjects = com.intellij.openapi.project.ProjectManager.getInstance().openProjects
            logger.debug("当前打开的项目数量：${openProjects.size}")
            if (openProjects.isEmpty()) {
                messageLabel.text = "没有打开的项目，无法显示最近阅读记录"
                logger.warn("没有打开的项目，无法加载最近阅读的 PDF")
                return
            }
            
            // 使用第一个打开的项目来获取服务
            val project = openProjects[0]
            logger.debug("使用项目：${project.name}")
            
            val recentPdfService = project.service<RecentPdfService>()
            val recentPdfs = recentPdfService.getRecentPdfs()
            logger.debug("从服务中获取了 ${recentPdfs.size} 个最近阅读的 PDF")
            if (recentPdfs.isEmpty()) {
                messageLabel.text = "暂无最近阅读的 PDF 记录"
            }
            recentPdfs.forEach { item ->
                logger.debug("加载 PDF: ${item.filePath}, 页码：${item.currentPage}/${item.totalPages}")
                pdfListModel.addElement(PdfItem(item.filePath, item.currentPage, item.totalPages))
            }
            logger.debug("加载了 ${recentPdfs.size} 个最近阅读的 PDF")
        } catch (e: Exception) {
            logger.warn("加载最近 PDF 失败", e)
            pdfListModel.clear()
            messageLabel.text = "加载失败：${e.message}"
        }
    }
    
    /**
     * 刷新最近阅读的 PDF 列表
     * 在设置面板打开时调用此方法以显示最新的阅读记录
     */
    fun refresh() {
        loadRecentPdfs()
        updateVisibility()
    }

    class PdfItem(
        var filePath: String = "",
        var currentPage: Int = 0,
        var totalPages: Int = 0
    ) {
        val fileName: String
            get() = java.io.File(filePath).name
        
        val progress: Int
            get() = if (totalPages > 0) ((currentPage.toDouble() / totalPages) * 100).toInt().coerceIn(0, 100) else 0
    }

    private inner class PdfListCellRenderer : JPanel(), ListCellRenderer<PdfItem> {
        private val fileNameLabel = JLabel()
        private val pageInfoLabel = JLabel()
        private val progressBar: JProgressBar
        private val openFolderButton = JLabel("📂")
        
        private val folderIconToolTip = "在文件夹中显示"

        init {
            removeAll()
            layout = BorderLayout(10, 5)
            border = EmptyBorder(5, 10, 5, 10)
            val leftPanel = JPanel(BorderLayout())
            fileNameLabel.font = fileNameLabel.font.deriveFont(Font.BOLD)
            leftPanel.add(fileNameLabel, BorderLayout.NORTH)
            leftPanel.add(pageInfoLabel, BorderLayout.CENTER)
            add(leftPanel, BorderLayout.CENTER)
            
            // 使用标准进度条，不自定义 UI
            progressBar = JProgressBar(0, 100).apply {
                preferredSize = Dimension(0, 12)
                border = BorderFactory.createLineBorder(Color.GRAY)
                foreground = Color(76, 175, 80)
                background = Color(33, 150, 243)
                isStringPainted = true
                isIndeterminate = false
            }
            add(progressBar, BorderLayout.SOUTH)
            
            openFolderButton.toolTipText = folderIconToolTip
            add(openFolderButton, BorderLayout.EAST)
            isOpaque = true
            leftPanel.isOpaque = false
            fileNameLabel.isOpaque = false
            pageInfoLabel.isOpaque = false
            openFolderButton.isOpaque = false
            
            // 确保渲染器组件不拦截鼠标事件
            fileNameLabel.isEnabled = false
            pageInfoLabel.isEnabled = false
            progressBar.isEnabled = false
            openFolderButton.isEnabled = false
            leftPanel.isEnabled = false
        }

        override fun getListCellRendererComponent(
            list: JList<out PdfItem>?,
            value: PdfItem,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            try {
                fileNameLabel.text = value.fileName
                pageInfoLabel.text = "${value.currentPage}/${value.totalPages} 页"
                progressBar.value = value.progress
                progressBar.string = "${value.progress}%"
                // 强制重绘进度条
                progressBar.repaint()
        
                val bgColor = if (isSelected) list?.selectionBackground else list?.background
                val fgColor = if (isSelected) list?.selectionForeground else list?.foreground
                background = bgColor
                foreground = fgColor
                fileNameLabel.background = bgColor
                fileNameLabel.foreground = fgColor
                pageInfoLabel.background = bgColor
                pageInfoLabel.foreground = fgColor
                openFolderButton.background = bgColor
                openFolderButton.foreground = fgColor
                isOpaque = true
            } catch (e: Throwable) {
                logger.error("渲染最近 PDF 列表项异常", e)
                fileNameLabel.text = "加载失败：${e.message}"
                pageInfoLabel.text = ""
                // 安全检查，防止 NPE
                try {
                    progressBar.value = 0
                    progressBar.string = ""
                } catch (e2: Throwable) {
                    logger.warn("设置进度条失败", e2)
                }
            }
            return this
        }
    }
}
