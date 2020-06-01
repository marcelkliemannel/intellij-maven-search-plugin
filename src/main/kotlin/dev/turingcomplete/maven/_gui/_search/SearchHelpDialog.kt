package dev.turingcomplete.maven._gui._search

import com.intellij.CommonBundle
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.DimensionService
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.maven._search.ArtifactSearchEngine
import java.awt.BorderLayout
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

class SearchHelpDialog(private val searchEngine: ArtifactSearchEngine<*>, project: Project) : DialogWrapper(project), DumbAware {
  companion object {
    @JvmField
    val SEARCH_HANDLERS_HELP_DIALOG_SIZE_KEY = SearchHelpDialog::class.qualifiedName
  }

  init {
    Disposer.register(project, disposable)
    isModal = false

    title = "Search Handlers Help"
    setCancelButtonText(CommonBundle.getCloseButtonText())

    init()

    val size = DimensionService.getInstance().getSize(dimensionServiceKey!!, project)
    if (size == null) {
      DimensionService.getInstance().setSize(dimensionServiceKey!!, JBUI.size(750, 450))
    }
  }

  override fun createActions(): Array<Action> {
    return arrayOf(cancelAction)
  }

  override fun getDimensionServiceKey(): String? {
    return SEARCH_HANDLERS_HELP_DIALOG_SIZE_KEY
  }

  override fun createCenterPanel(): JComponent? {
    return JPanel(BorderLayout()).apply {
      border = JBUI.Borders.empty()

      val helpTextLabel = JBLabel("<html>${searchEngine.getSearchHandlers().joinToString(separator = "") { "<h2>${it.title}</h2>${it.getDescriptionHtml()}" }}</html>").apply {
        border = EmptyBorder(UIUtil.PANEL_REGULAR_INSETS)
      }
      add(helpTextLabel, BorderLayout.NORTH)
    }
  }
}