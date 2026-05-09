package com.firsttimeinforever.intellij.pdf.viewer.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import java.awt.BorderLayout
import javax.swing.*

/**
 * PDF Tab 关联配置面板
 * 用于管理每个PDF文件关联的编辑器Tab
 */
class PdfTabLinkagePanel(private val project: Project) : JPanel() {
    
    private val pdfComboBox = JComboBox<String>()
    private val linkedTabsList = DefaultListModel<String>()
    private val tabsList = JBList(linkedTabsList)
    private val availableTabsComboBox = JComboBox<String>()
    
    init {
        layout = BorderLayout(10, 10)
        border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        
        initComponents()
        setupLayout()
        refreshPdfList()
    }
    
    private fun initComponents() {
        // PDF文件下拉列表
        pdfComboBox.renderer = ListCellRenderer { _, value, _, _, _ ->
            JLabel(value?.substringAfterLast('/') ?: value)
        }
        
        // 已关联的Tab列表
        tabsList.cellRenderer = ListCellRenderer { _, value, _, _, _ ->
            JLabel(value?.substringAfterLast('/') ?: value)
        }
        
        // 可用的Tab下拉列表
        availableTabsComboBox.renderer = ListCellRenderer { _, value, _, _, _ ->
            JLabel(value?.substringAfterLast('/') ?: value)
        }
    }
    
    private fun setupLayout() {
        val mainPanel = panel {
            // PDF选择区域
            row("选择PDF文件:") {
                cell(pdfComboBox)
                    .align(AlignX.FILL)
                    .resizableColumn()
                
                button("刷新") {
                    refreshPdfList()
                }
            }
            
            // 已关联的Tab列表
            row("已关联的Tab:") {
                scrollCell(tabsList)
                    .align(AlignX.FILL)
                    .resizableColumn()
                    .apply { component.preferredSize = java.awt.Dimension(-1, 150) }
            }
            
            // 添加Tab按钮区域
            row {
                cell(availableTabsComboBox)
                    .align(AlignX.FILL)
                    .resizableColumn()
                
                button("添加关联") {
                    addLinkedTab()
                }
                
                button("移除选中") {
                    removeSelectedTab()
                }
                
                button("清空全部") {
                    clearAllTabs()
                }
            }
        }
        
        add(mainPanel, BorderLayout.CENTER)
        
        // 监听PDF选择变化
        pdfComboBox.addActionListener {
            refreshLinkedTabsList()
            refreshAvailableTabsList()
        }
    }
    
    /**
     * 刷新PDF文件列表
     */
    fun refreshPdfList() {
        val settings = PdfViewerSettings.instance
        val currentSelection = pdfComboBox.selectedItem as? String
        
        pdfComboBox.removeAllItems()
        
        // 首先添加最近浏览的PDF
        val recentPdfService = project.service<RecentPdfService>()
        val recentPdfs = recentPdfService.getRecentPdfs().map { it.filePath }
        recentPdfs.forEach { pdfPath ->
            pdfComboBox.addItem(pdfPath)
        }
        
        // 然后添加配置中已有的PDF（如果不在最近列表中）
        settings.pdfTabLinkageMap.keys.forEach { pdfPath ->
            // 检查是否已经添加过
            var alreadyAdded = false
            for (i in 0 until pdfComboBox.itemCount) {
                if (pdfComboBox.getItemAt(i) == pdfPath) {
                    alreadyAdded = true
                    break
                }
            }
            if (!alreadyAdded) {
                pdfComboBox.addItem(pdfPath)
            }
        }
        
        // 如果没有PDF，显示提示
        if (pdfComboBox.itemCount == 0) {
            pdfComboBox.addItem("<请先打开一个PDF文件>")
        }
        
        // 恢复之前的选择
        if (currentSelection != null && (recentPdfs.contains(currentSelection) || settings.pdfTabLinkageMap.containsKey(currentSelection))) {
            pdfComboBox.selectedItem = currentSelection
        } else if (pdfComboBox.itemCount > 0 && !pdfComboBox.getItemAt(0).toString().startsWith("<")) {
            pdfComboBox.selectedIndex = 0
        }
        
        refreshLinkedTabsList()
        refreshAvailableTabsList()
    }
    
    /**
     * 刷新已关联的Tab列表
     */
    private fun refreshLinkedTabsList() {
        linkedTabsList.clear()
        
        val selectedPdf = pdfComboBox.selectedItem as? String
        if (selectedPdf == null || selectedPdf.startsWith("<")) {
            return
        }
        
        val settings = PdfViewerSettings.instance
        val linkedTabs = settings.pdfTabLinkageMap[selectedPdf] ?: return
        
        linkedTabs.forEach { tabPath ->
            linkedTabsList.addElement(tabPath)
        }
    }
    
    /**
     * 刷新可用的Tab列表（当前打开的非PDF文件）
     */
    private fun refreshAvailableTabsList() {
        availableTabsComboBox.removeAllItems()
        
        val selectedPdf = pdfComboBox.selectedItem as? String
        if (selectedPdf == null || selectedPdf.startsWith("<")) {
            availableTabsComboBox.addItem("<请先选择PDF>")
            return
        }
        
        val fileEditorManager = FileEditorManager.getInstance(project)
        val openFiles = fileEditorManager.openFiles
        
        val settings = PdfViewerSettings.instance
        val linkedTabs = settings.pdfTabLinkageMap[selectedPdf] ?: emptyList()
        
        var hasAvailable = false
        openFiles.forEach { file ->
            // 跳过PDF文件
            if (file.extension.equals("pdf", ignoreCase = true)) {
                return@forEach
            }
            
            // 跳过已经关联的文件
            if (linkedTabs.contains(file.path)) {
                return@forEach
            }
            
            availableTabsComboBox.addItem(file.path)
            hasAvailable = true
        }
        
        if (!hasAvailable) {
            availableTabsComboBox.addItem("<没有可用的Tab>")
        }
    }
    
    /**
     * 添加关联的Tab
     */
    private fun addLinkedTab() {
        val selectedPdf = pdfComboBox.selectedItem as? String
        val selectedTab = availableTabsComboBox.selectedItem as? String
        
        if (selectedPdf == null || selectedPdf.startsWith("<")) {
            JOptionPane.showMessageDialog(
                this,
                "请先选择一个PDF文件",
                "提示",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }
        
        if (selectedTab == null || selectedTab.startsWith("<")) {
            JOptionPane.showMessageDialog(
                this,
                "请选择一个要关联的Tab",
                "提示",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }
        
        val settings = PdfViewerSettings.instance
        val linkedTabs = settings.pdfTabLinkageMap.getOrPut(selectedPdf) { mutableListOf() }
        
        if (linkedTabs.contains(selectedTab)) {
            JOptionPane.showMessageDialog(
                this,
                "该Tab已经关联到此PDF",
                "提示",
                JOptionPane.INFORMATION_MESSAGE
            )
            return
        }
        
        linkedTabs.add(selectedTab)
        settings.notifyListeners()
        
        refreshLinkedTabsList()
        refreshAvailableTabsList()
        
        JOptionPane.showMessageDialog(
            this,
            "已成功添加关联",
            "成功",
            JOptionPane.INFORMATION_MESSAGE
        )
    }
    
    /**
     * 移除选中的关联Tab
     */
    private fun removeSelectedTab() {
        val selectedIndex = tabsList.selectedIndex
        if (selectedIndex < 0) {
            JOptionPane.showMessageDialog(
                this,
                "请选择要移除的Tab",
                "提示",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }
        
        val selectedPdf = pdfComboBox.selectedItem as? String
        if (selectedPdf == null || selectedPdf.startsWith("<")) {
            return
        }
        
        val tabToRemove = linkedTabsList.getElementAt(selectedIndex)
        val settings = PdfViewerSettings.instance
        val linkedTabs = settings.pdfTabLinkageMap[selectedPdf]
        
        if (linkedTabs != null) {
            linkedTabs.remove(tabToRemove)
            settings.notifyListeners()
            
            refreshLinkedTabsList()
            refreshAvailableTabsList()
        }
    }
    
    /**
     * 清空所有关联的Tab
     */
    private fun clearAllTabs() {
        val selectedPdf = pdfComboBox.selectedItem as? String
        if (selectedPdf == null || selectedPdf.startsWith("<")) {
            return
        }
        
        val confirm = JOptionPane.showConfirmDialog(
            this,
            "确定要清空此PDF的所有关联Tab吗？",
            "确认",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        )
        
        if (confirm == JOptionPane.YES_OPTION) {
            val settings = PdfViewerSettings.instance
            settings.pdfTabLinkageMap.remove(selectedPdf)
            settings.notifyListeners()
            
            refreshLinkedTabsList()
            refreshAvailableTabsList()
        }
    }
}
