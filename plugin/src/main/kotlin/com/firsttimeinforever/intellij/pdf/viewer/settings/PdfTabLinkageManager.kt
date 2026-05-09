package com.firsttimeinforever.intellij.pdf.viewer.settings

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

/**
 * PDF 与编辑器 Tab 关联管理器
 * 负责根据配置，自动打开/关闭每个PDF关联的编辑器Tab
 */
class PdfTabLinkageManager(private val project: Project) {
    companion object {
        private val logger = logger<PdfTabLinkageManager>()
    }
    
    /**
     * 关闭指定PDF关联的所有Tab
     */
    fun closeLinkedTabs(pdfPath: String) {
        logger.info("closeLinkedTabs called for: $pdfPath")
        
        if (!PdfViewerSettings.instance.enableTabLinkage) {
            logger.warn("Tab linkage is disabled, skipping")
            return
        }
        
        val settings = PdfViewerSettings.instance
        val linkedTabs = settings.pdfTabLinkageMap[pdfPath]
        
        logger.info("Linked tabs from settings: $linkedTabs")
        logger.info("All PDF tab linkage map keys: ${settings.pdfTabLinkageMap.keys}")
        
        if (linkedTabs == null || linkedTabs.isEmpty()) {
            logger.warn("No linked tabs found for PDF: $pdfPath")
            return
        }
        
        val fileEditorManager = FileEditorManager.getInstance(project)
        
        linkedTabs.forEach { tabPath ->
            logger.info("Processing linked tab: $tabPath")
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(tabPath)
            if (virtualFile != null) {
                logger.info("Found virtual file: ${virtualFile.path}")
                if (fileEditorManager.isFileOpen(virtualFile)) {
                    logger.info("Closing linked tab: $tabPath")
                    fileEditorManager.closeFile(virtualFile)
                } else {
                    logger.info("Tab is not open, skipping: $tabPath")
                }
            } else {
                logger.warn("Virtual file not found: $tabPath")
            }
        }
    }
    
    /**
     * 打开指定PDF关联的所有Tab
     */
    fun openLinkedTabs(pdfPath: String) {
        if (!PdfViewerSettings.instance.enableTabLinkage) {
            return
        }
        
        logger.info("Opening linked tabs for PDF: $pdfPath")
        
        val settings = PdfViewerSettings.instance
        val linkedTabs = settings.pdfTabLinkageMap[pdfPath] ?: return
        
        val fileEditorManager = FileEditorManager.getInstance(project)
        
        linkedTabs.forEach { tabPath ->
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(tabPath)
            if (virtualFile != null && !fileEditorManager.isFileOpen(virtualFile)) {
                logger.info("Opening linked tab: $tabPath")
                fileEditorManager.openFile(virtualFile, false)
            }
        }
    }
}
