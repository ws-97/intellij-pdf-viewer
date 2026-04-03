package com.firsttimeinforever.intellij.pdf.viewer.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service(Service.Level.PROJECT)
@State(name = "RecentPdfService", storages = [Storage("recent_pdf_viewer.xml")])
class RecentPdfService : PersistentStateComponent<RecentPdfService.State> {
    class State(
        var recentPdfs: MutableList<RecentPdfItem> = mutableListOf()
    ) {
        fun addOrUpdatePdf(filePath: String, currentPage: Int, totalPages: Int) {
            val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val existingItem = recentPdfs.find { it.filePath == filePath }
            if (existingItem != null) {
                // 更新现有项目的时间戳和页码信息
                recentPdfs.remove(existingItem)
                recentPdfs.add(0, RecentPdfItem(filePath, currentPage, totalPages, now))
            } else {
                // 添加新项目到开头
                recentPdfs.add(0, RecentPdfItem(filePath, currentPage, totalPages, now))
            }
            // 保持最多10个最近文件
            if (recentPdfs.size > 10) {
                recentPdfs = recentPdfs.take(10).toMutableList()
            }
        }
    }

    class RecentPdfItem(
        var filePath: String = "",
        var currentPage: Int = 0,
        var totalPages: Int = 0,
        var lastOpened: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        try {
            XmlSerializerUtil.copyBean(state, myState)
        } catch (e: Exception) {
            // 如果加载失败，清空列表并记录错误
            myState.recentPdfs.clear()
            java.util.logging.Logger.getLogger(RecentPdfService::class.java.name)
                .warning("Failed to load recent PDF state: ${e.message}")
        }
    }

    fun addRecentPdf(filePath: String, currentPage: Int, totalPages: Int) {
        myState.addOrUpdatePdf(filePath, currentPage, totalPages)
    }

    fun getRecentPdfs(): List<RecentPdfItem> = myState.recentPdfs
}