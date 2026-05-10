package com.firsttimeinforever.intellij.pdf.viewer.settings

import com.firsttimeinforever.intellij.pdf.viewer.PdfViewerBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.DefaultListModel
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import kotlin.math.min

/**
 * 自动滚动速度配置面板
 * 显示每个PDF文件的自动滚动速度设置
 */
class PdfAutoScrollSpeedPanel : JPanel(BorderLayout()) {
    private val listModel = DefaultListModel<PdfSpeedEntry>()
    private val speedList = JBList(listModel)
    
    data class PdfSpeedEntry(
        val pdfPath: String,
        var speedLevel: Int
    ) {
        override fun toString(): String {
            val fileName = pdfPath.substringAfterLast('/')
                .substringAfterLast('\\')
                .take(50) // 限制文件名长度
            return "$fileName - 速度级别: $speedLevel"
        }
    }
    
    init {
        preferredSize = Dimension(preferredSize.width, 200)
        
        // 配置列表
        speedList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        speedList.cellRenderer = com.intellij.ui.SimpleListCellRenderer.create { label, value, _ ->
            label.text = value.toString()
        }
        
        // 添加滚动面板
        val scrollPane = JBScrollPane(speedList)
        scrollPane.border = JBUI.Borders.empty()
        add(scrollPane, BorderLayout.CENTER)
        
        // 加载数据
        refresh()
    }
    
    /**
     * 刷新列表数据
     */
    fun refresh() {
        listModel.clear()
        val settings = PdfViewerSettings.instance
        
        // 从设置中加载所有PDF的滚动速度
        settings.pdfAutoScrollSpeedMap.forEach { (pdfPath, speed) ->
            // 检查文件是否仍然存在
            val file = VirtualFileManager.getInstance().findFileByUrl(pdfPath)
            if (file != null && file.exists()) {
                listModel.addElement(PdfSpeedEntry(pdfPath, speed))
            }
        }
        
        // 如果没有数据，显示提示
        if (listModel.isEmpty) {
            // 可以添加一个空状态提示
        }
    }
    
    /**
     * 更新指定PDF的滚动速度
     */
    fun updatePdfSpeed(pdfPath: String, speedLevel: Int) {
        val settings = PdfViewerSettings.instance
        settings.pdfAutoScrollSpeedMap[pdfPath] = speedLevel
        
        // 更新列表中的显示
        for (i in 0 until listModel.size) {
            val entry = listModel.getElementAt(i)
            if (entry.pdfPath == pdfPath) {
                entry.speedLevel = speedLevel
                // 刷新列表项显示
                speedList.repaint()
                break
            }
        }
        
        // 如果列表中不存在，添加新项
        val file = VirtualFileManager.getInstance().findFileByUrl(pdfPath)
        if (file != null && file.exists()) {
            var found = false
            for (i in 0 until listModel.size) {
                if (listModel.getElementAt(i).pdfPath == pdfPath) {
                    found = true
                    break
                }
            }
            if (!found) {
                listModel.addElement(PdfSpeedEntry(pdfPath, speedLevel))
            }
        }
    }
    
    /**
     * 获取指定PDF的滚动速度
     */
    fun getPdfSpeed(pdfPath: String): Int {
        return PdfViewerSettings.instance.pdfAutoScrollSpeedMap[pdfPath] ?: 3 // 默认速度级别3
    }
}
