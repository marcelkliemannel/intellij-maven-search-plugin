package dev.turingcomplete.maven._gui._search

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.DumbAware
import com.intellij.ui.components.JBLabel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.castSafelyTo
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.maven._search.Artifact
import dev.turingcomplete.maven._search.ArtifactModule
import dev.turingcomplete.maven._search.SearchResult
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeCellRenderer
import javax.swing.tree.TreePath

class SearchResultTree(private val selectArtifact: (Artifact) -> Unit,
                       private val loadMoreArtifacts: (ArtifactModule, () -> Unit) -> Unit) : Tree() {

  private val searchResultsRootNode = SearchResultRootNode()

  var sortArtifactsByDate = false

  init {
    model = DefaultTreeModel(searchResultsRootNode, false)
    setCellRenderer(SearchResultTreeCellRender())

    isRootVisible = false
    border = JBUI.Borders.empty()

    addTreeSelectionListener { event ->
      if (event.newLeadSelectionPath != null && event.oldLeadSelectionPath == event.newLeadSelectionPath) {
        return@addTreeSelectionListener
      }

      val lastPathComponent: Any? = event.newLeadSelectionPath?.lastPathComponent
      if (lastPathComponent is VersionNode) {
        selectArtifact(lastPathComponent.artifact)
      }
      else if (lastPathComponent is LoadArtifactsNode) {
        loadMoreArtifacts(lastPathComponent.artifactModuleNode.artifactModule) {
          lastPathComponent.artifactModuleNode.removeAllChildren()

          addArtifactNodesToArtifactModule(lastPathComponent.artifactModuleNode) { version, isLatestVersion ->
            searchResultsRootNode.searchResult!!.getPresentableVersion(version, isLatestVersion)
          }

          model.castSafelyTo<DefaultTreeModel>()!!.reload()

          expandPath(TreePath(lastPathComponent.artifactModuleNode.path))
        }
      }
    }
  }

  fun clearSearchResult() {
    searchResultsRootNode.removeAllChildren()
    model.castSafelyTo<DefaultTreeModel>()!!.reload()
  }

  fun setSearchResult(searchResult: SearchResult<ArtifactModule>) {
    searchResultsRootNode.removeAllChildren()

    searchResultsRootNode.searchResult = searchResult
    searchResultsRootNode.add(SearchResultStatsNode(searchResult))

    searchResult.records.groupBy { artifactModule -> artifactModule.groupId }
            .forEach { (groupId, artifactModulesOfGroupId) ->
              GroupIdNode(searchResult.getPresentableGroupId(groupId)).let { groupIdNode ->
                artifactModulesOfGroupId.forEach { artifactModule ->
                  val presentableArtifactId = searchResult.getPresentableArtifactId(artifactModule.artifactId)
                  val artifactNode = ArtifactModuleNode(presentableArtifactId, artifactModule)

                  addArtifactNodesToArtifactModule(artifactNode) { version, isLatestVersion ->
                    searchResult.getPresentableVersion(version, isLatestVersion)
                  }

                  groupIdNode.add(artifactNode)
                }
                searchResultsRootNode.add(groupIdNode)
              }
            }

    model.castSafelyTo<DefaultTreeModel>()!!.reload()
  }

  private fun addArtifactNodesToArtifactModule(artifactModuleNode: ArtifactModuleNode,
                                               getPresentableVersion: (String, Boolean) -> String) {
    // Add all known versions
    artifactModuleNode.artifactModule.artifacts.forEach { artifactVersion ->
      val isLatestVersion = artifactModuleNode.artifactModule.latestVersion == artifactVersion.version
      artifactModuleNode.add(VersionNode(getPresentableVersion(artifactVersion.version.toString(), isLatestVersion), artifactVersion))
    }

    // Add load more artifact versions
    val moreAvailableVersions = artifactModuleNode.artifactModule.totalAvailableVersions - artifactModuleNode.artifactModule.artifacts.size
    if (moreAvailableVersions > 0) {
      artifactModuleNode.add(LoadArtifactsNode("$moreAvailableVersions more versions available", artifactModuleNode))
    }
  }

  private class SearchResultTreeCellRender : JBLabel(""), TreeCellRenderer {
    init {
      UIUtil.addInsets(this, UIUtil.PANEL_SMALL_INSETS)
    }

    override fun getTreeCellRendererComponent(tree: JTree, value: Any, isSelected: Boolean, expanded: Boolean,
                                              leaf: Boolean, rowIndex: Int, hasFocus: Boolean): JComponent {
      componentOrientation = tree.componentOrientation
      background = tree.background
      foreground = tree.foreground
      isOpaque = isSelected

      if (value is SearchResultNode) {
        text = value.userObject.castSafelyTo<String>() ?: throw IllegalStateException("Expected a string")
        icon = value.getIcon()
      }

      return this
    }
  }

  abstract class SearchResultNode(text: String) : DefaultMutableTreeNode(text), DumbAware {
    abstract fun getIcon(): Icon?
  }

  private class SearchResultRootNode : SearchResultNode("Root") {
    var searchResult: SearchResult<ArtifactModule>? = null

    override fun getIcon(): Icon? = null
  }

  private class SearchResultStatsNode(private val searchResult: SearchResult<ArtifactModule>) : SearchResultNode("") {
    init {
      if (searchResult.totalAvailableRecords > searchResult.records.size) {
        setUserObject("Showing ${searchResult.offset + 1}-${searchResult.offset + searchResult.records.size} of ${searchResult.totalAvailableRecords} results")
      }
      else {
        setUserObject("Showing all ${searchResult.totalAvailableRecords} results")
      }
    }

    override fun getIcon(): Icon = AllIcons.General.Information
  }

  private class GroupIdNode(text: String) : SearchResultNode(text) {
    override fun getIcon(): Icon? = AllIcons.Nodes.ModuleGroup
  }

  private class ArtifactModuleNode(text: String, val artifactModule: ArtifactModule) : SearchResultNode(text) {
    override fun getIcon(): Icon? = AllIcons.Nodes.Module
  }

  private class VersionNode(text: String, val artifact: Artifact) : SearchResultNode(text) {
    override fun getIcon(): Icon = AllIcons.Nodes.Artifact
  }

  private class LoadArtifactsNode(text: String, val artifactModuleNode: ArtifactModuleNode) : SearchResultNode(text) {
    override fun getIcon(): Icon = AllIcons.Actions.Find
  }
}