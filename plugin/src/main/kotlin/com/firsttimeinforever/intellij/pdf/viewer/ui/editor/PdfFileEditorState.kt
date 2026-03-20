package com.firsttimeinforever.intellij.pdf.viewer.ui.editor

import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel

open class PdfFileEditorState(
  private val currentPage: Int = 1
) : FileEditorState {
  
  override fun canBeMergedWith(otherState: FileEditorState, level: FileEditorStateLevel): Boolean {
    return false
  }
  
  fun getCurrentPage(): Int = currentPage
}
