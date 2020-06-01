package dev.turingcomplete.maven._gui._artifactsview

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.tabs.JBTabsFactory
import com.intellij.ui.tabs.TabInfo
import com.intellij.util.ui.JBUI
import dev.turingcomplete.maven.MavenSearchContext
import dev.turingcomplete.maven._search.Artifact
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class ArtifactsViewComponent(private val searchContext: MavenSearchContext) : JBPanelWithEmptyText(), DumbAware {
  private var artifactTabs = JBTabsFactory.createTabs(searchContext.project)

  init {
    layout = BorderLayout()
    border = JBUI.Borders.empty()
    emptyText.text = "No artifact selected"
  }

  fun showArtifact(artifact: Artifact) {
    if (artifactTabs.tabs.isEmpty()) {
      add(artifactTabs.component, BorderLayout.CENTER)
    }

    // If the there is already a tab for this artifact, select the tab.
    val alreadyOpenTab = artifactTabs.tabs.find { tab -> (tab.`object` as Artifact) == artifact }
    if (alreadyOpenTab != null) {
      artifactTabs.select(alreadyOpenTab, false)
      return
    }

    // Create a new tab
    val closeArtifactTabAction = object: DumbAwareAction() {
      override fun actionPerformed(e: AnActionEvent) {
        closeArtifact(artifact)
      }

      override fun update(e: AnActionEvent) {
        e.presentation.icon = AllIcons.Actions.Close
        e.presentation.hoveredIcon = AllIcons.Actions.CloseHovered
        e.presentation.text = "Close"
      }
    }
    artifactTabs.addTab(TabInfo(createArtifactViewContent(artifact)))
            .setObject(artifact)
            .setText("${artifact.artifactId} (${artifact.version})")
            .setTabLabelActions(DefaultActionGroup(closeArtifactTabAction), ActionPlaces.UNKNOWN)
            .run {
              artifactTabs.select(this, false)
            }
  }

  private fun closeArtifact(artifact: Artifact) {
    artifactTabs.tabs.find { it.`object` == artifact }?.run {
      artifactTabs.removeTab(this)

      if (artifactTabs.tabs.isEmpty()) {
        remove(artifactTabs.component)
      }
    }
  }

  private fun createArtifactViewContent(artifact: Artifact) : JComponent {
    return JPanel(BorderLayout()).apply {
      add(ArtifactViewComponent(artifact, searchContext), BorderLayout.NORTH)
      add(DependencySnippetsComponent(artifact, searchContext), BorderLayout.CENTER)
    }
  }
}