package dev.turingcomplete.maven._gui._search

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.GuiUtils
import com.intellij.ui.LayeredIcon
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.scale.JBUIScale
import com.intellij.ui.treeStructure.actions.CollapseAllAction
import com.intellij.ui.treeStructure.actions.ExpandAllAction
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import dev.turingcomplete.maven.MavenSearchContext
import dev.turingcomplete.maven._search.Artifact
import dev.turingcomplete.maven._search.ArtifactModule
import dev.turingcomplete.maven._search.SearchHandler
import dev.turingcomplete.maven._search.SearchResult
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.KeyEvent
import java.util.function.Supplier
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.KeyStroke

class SearchPanel(private val searchContext: MavenSearchContext, selectArtifact: (Artifact) -> Unit) : JBPanel<SearchPanel>(), DumbAware {
  companion object {
    @JvmField
    val LOGGER = Logger.getInstance(SearchPanel::class.java)
  }

  private val searchBarComponent: JComponent
  private val searchQueryField = ExtendableTextField()
  private val searchQueryFieldValidator = ComponentValidator(searchContext.project)
  private val searchResultComponent: JComponent
  private val searchResultsTree = SearchResultTree(selectArtifact, loadMoreArtifacts())

  private val searchResults: MutableMap<Int, SearchResult<ArtifactModule>> = mutableMapOf()
  private var currentSearchResult: SearchResult<ArtifactModule>? = null

  private val searchResultNavigationBackwardAction: DumbAwareAction
  private val searchResultNavigationForwardAction: DumbAwareAction
  private val searchResultExpandAllAction: ExpandAllAction
  private val searchResultCollapseAllAction: CollapseAllAction

  init {
    layout = GridBagLayout()
    border = JBUI.Borders.empty()

    searchResultNavigationBackwardAction = createSearchResultNavigationBackwardAction()
    searchResultNavigationForwardAction = createSearchResultNavigationNextAction()
    searchResultExpandAllAction = object : ExpandAllAction(searchResultsTree) {
      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = currentSearchResult != null
      }
    }
    searchResultCollapseAllAction = object : CollapseAllAction(searchResultsTree) {
      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = currentSearchResult != null
      }
    }

    searchBarComponent = createSearchBarComponent()
    add(searchBarComponent, GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, JBUI.emptyInsets(), 0, 0))

    searchResultComponent = createSearchResultComponent()
    add(searchResultComponent, GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0))

    syncSearchResult(false)
  }

  private fun syncSearchResult(searchInProgress: Boolean) {
    GuiUtils.enableChildren(!searchInProgress, searchBarComponent, searchResultComponent)

    revalidate()
    repaint()
  }

  private fun createSearchBarComponent(): JComponent {
    return JPanel(GridBagLayout()).apply {
      searchQueryField.apply {
        addExtension(object : ExtendableTextComponent.Extension {
          override fun getIcon(hovered: Boolean): Icon = AllIcons.Actions.Search
          override fun isIconBeforeText(): Boolean = true
        })

        searchQueryFieldValidator.withValidator(Supplier {
          val query = searchQueryField.text
          return@Supplier searchContext.searchManager.searchEngine.getActiveSearchHandler().validateQuery(query)?.forComponent(searchQueryField)
        }).andRegisterOnDocumentListener(searchQueryField).installOn(searchQueryField)

        val searchHandler = searchContext.searchManager.searchEngine.getActiveSearchHandler()
        registerKeyboardAction({
                                 val query = searchQueryField.text
                                 if (query.isBlank() || searchHandler.validateQuery(query)?.warning?.not() == true) {
                                   return@registerKeyboardAction
                                 }
                                 searchArtifactModules(query, 0, searchHandler)
                               },
                               KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                               JComponent.WHEN_FOCUSED)
      }
      add(searchQueryField, GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, JBUI.emptyInsets(), 0, 0))

      add(SearchHandlerSelectionAction().actionButton, GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, JBUI.emptyInsets(), 0, 0))

      add(createSearchQueryHelpButton(), GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, JBUI.emptyInsets(), 0, 0))
    }
  }

  private fun createSearchQueryHelpButton(): JComponent {
    val showHelpAction = DumbAwareAction.create {
      SearchHelpDialog(searchContext.searchManager.searchEngine, searchContext.project).show()
    }

    showHelpAction.templatePresentation.apply {
      icon = AllIcons.Actions.Help
      text = "Show Help..."
    }

    return ActionButton(showHelpAction, showHelpAction.templatePresentation, ActionPlaces.UNKNOWN, JBDimension(25, 25))
  }

  private fun createSearchResultComponent(): JComponent {
    return SimpleToolWindowPanel(true).apply {
      toolbar = createSearchResultActionToolBarComponent()
      setContent(ScrollPaneFactory.createScrollPane(searchResultsTree, true))
    }
  }

  private fun searchArtifactModules(query: String, offset: Int, searchHandler: SearchHandler) {
    syncSearchResult(true)

    val onSuccess: (SearchResult<ArtifactModule>) -> Unit = {
      searchResults[it.getPage()] = it
      ApplicationManager.getApplication().invokeLater {
        searchResultsTree.clearSearchResult()
        currentSearchResult = it
        searchResultsTree.setSearchResult(it)
      }
    }
    val onFailed: (Throwable) -> Unit = { e ->
      LOGGER.info("Failed to search for '$query'.", e)
      searchContext.errorNotify("Failed to search for '$query'. See idea.log for details.")
    }
    val onFinished = {
      ApplicationManager.getApplication().invokeLater {
        syncSearchResult(false)
      }
    }
    searchContext.searchManager.searchArtifactModulesAsync(query, offset, searchHandler, onSuccess, onFailed, onFinished)
  }

  private fun loadMoreArtifacts(): (ArtifactModule, () -> Unit) -> Unit {
    return { artifactModule, _onSuccess ->
      syncSearchResult(true)

      val onSuccess: (SearchResult<Artifact>) -> Unit = {
        artifactModule.artifacts.clear()
        artifactModule.artifacts.addAll(it.records)
        ApplicationManager.getApplication().invokeLater {
          _onSuccess()
        }
      }
      val onFailed: (Throwable) -> Unit = { e ->
        LOGGER.warn("Failed to load more versions of '${artifactModule.artifactId}'.", e)
        searchContext.errorNotify("Failed to load more versions of '${artifactModule.artifactId}'. See idea.log for details.")
      }
      val onFinished = {
        ApplicationManager.getApplication().invokeLater {
          syncSearchResult(false)
        }
      }
      val artifactsSize = artifactModule.artifacts.size
      val offset = if (artifactsSize == 0) 0 else (artifactsSize - 1)
      searchContext.searchManager.searchArtifactsAsync(artifactModule.groupId, artifactModule.artifactId, offset, onSuccess, onFailed, onFinished)
    }
  }

  private fun createSearchResultActionToolBarComponent(): JComponent {
    val searchResultActionGroup = DefaultActionGroup()

    // Navigate backward action
    searchResultActionGroup.add(searchResultNavigationBackwardAction)

    // Navigate forward action
    searchResultActionGroup.add(searchResultNavigationForwardAction)

    searchResultActionGroup.addSeparator()
/*
    // Sort artifacts by date
    val toggleSortArtifactsByDateAction = object : DumbAwareToggleAction() {
      override fun isSelected(e: AnActionEvent) = searchResultsTree.sortArtifactsByDate

      override fun setSelected(e: AnActionEvent, state: Boolean) {
        searchResultsTree.sortArtifactsByDate = state
      }
    }
    toggleSortArtifactsByDateAction.templatePresentation.apply {
      icon = AllIcons.RunConfigurations.SortbyDuration
      text = "Sort Artifacts by Date"
    }
    searchResultActionGroup.add(toggleSortArtifactsByDateAction)

    // Navigate with single click
    val toggleNavigateWithSingleClickAction = object : DumbAwareToggleAction() {
      override fun isSelected(e: AnActionEvent) = searchResultsTree.sortArtifactsByDate

      override fun setSelected(e: AnActionEvent, state: Boolean) {
        searchResultsTree.sortArtifactsByDate = state
      }
    }
    toggleNavigateWithSingleClickAction.templatePresentation.apply {
      icon = AllIcons.General.AutoscrollToSource
      text = "Navigate with Single Click"
    }
    searchResultActionGroup.add(toggleNavigateWithSingleClickAction)

    searchResultActionGroup.addSeparator()
*/
    // Expand all
    searchResultActionGroup.add(searchResultExpandAllAction)

    // Collapse all
    searchResultActionGroup.add(searchResultCollapseAllAction)

    return ActionManager.getInstance()
            .createActionToolbar(ActionPlaces.UNKNOWN, searchResultActionGroup, false)
            .component
  }

  private fun createSearchResultNavigationBackwardAction(): DumbAwareAction {
    return object : DumbAwareAction("Show Previous Results", null, AllIcons.Actions.Back) {
      override fun actionPerformed(e: AnActionEvent) {
        if (currentSearchResult != null && currentSearchResult!!.hasPreviousPage()) {
          val currentPage = currentSearchResult!!.getPage()
          val previousSearchResult = searchResults.getValue(currentPage - 1)
          searchResultsTree.setSearchResult(previousSearchResult)
          currentSearchResult = previousSearchResult
          syncSearchResult(false)
        }
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = currentSearchResult?.hasPreviousPage() ?: false
      }
    }
  }

  private fun createSearchResultNavigationNextAction(): DumbAwareAction {
    return object : DumbAwareAction("Load Next Results", null, AllIcons.Actions.Forward) {
      override fun actionPerformed(e: AnActionEvent) {
        if (currentSearchResult != null && currentSearchResult!!.hasNextPage()) {
          val currentPage = currentSearchResult!!.getPage()

          if (searchResults.containsKey(currentPage + 1)) {
            val nextSearchResult = searchResults.getValue(currentPage + 1)
            searchResultsTree.setSearchResult(nextSearchResult)
            currentSearchResult = nextSearchResult
            syncSearchResult(false)
          }
          else {
            val nextOffset = currentSearchResult!!.offset + currentSearchResult!!.records.size
            searchArtifactModules(currentSearchResult!!.query, nextOffset, currentSearchResult!!.searchHandler)
          }
        }
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = currentSearchResult?.hasNextPage() ?: false
      }
    }
  }

  private inner class SearchHandlerSelectionAction internal constructor() : DumbAwareAction(AllIcons.General.Filter) {
    private val actionGroup: DefaultActionGroup
    val actionButton: ActionButton

    init {
      val icon = JBUIScale.scaleIcon(LayeredIcon(2))
      icon.setIcon(AllIcons.General.GearPlain, 0)
      icon.setIcon(AllIcons.General.Dropdown, 1)
      templatePresentation.icon = icon

      actionGroup = DefaultActionGroup()
      searchContext.searchManager.searchEngine.getSearchHandlers().forEach { searchHandler ->
        actionGroup.add(object : ToggleAction(searchHandler.title) {
          override fun isSelected(e: AnActionEvent): Boolean {
            return this@SearchPanel.searchContext.searchManager.searchEngine.getActiveSearchHandler() == searchHandler
          }

          override fun setSelected(e: AnActionEvent, state: Boolean) {
            this@SearchPanel.searchContext.searchManager.searchEngine.setActiveSearchHandler(searchHandler)
            searchQueryFieldValidator.revalidate()
          }
        })
      }
      actionGroup.isPopup = true

      actionButton = ActionButton(this, templatePresentation, ActionPlaces.UNKNOWN, JBDimension(25, 25))
    }

    override fun actionPerformed(e: AnActionEvent) {
      if (e.getData(PlatformDataKeys.CONTEXT_COMPONENT) == null) return
      val listPopup = JBPopupFactory.getInstance().createActionGroupPopup(null, actionGroup, e.dataContext, false, null, 10)
      listPopup.showUnderneathOf(actionButton)
    }
  }
}