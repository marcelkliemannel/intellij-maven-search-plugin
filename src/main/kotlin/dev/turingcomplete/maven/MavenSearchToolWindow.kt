package dev.turingcomplete.maven

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.OnePixelSplitter
import com.intellij.util.ui.JBUI
import dev.turingcomplete.maven._gui._artifactsview.ArtifactsViewComponent
import dev.turingcomplete.maven._gui._search.SearchPanel
import dev.turingcomplete.maven._search.SearchManager
import javax.swing.BoxLayout
import javax.swing.JPanel


class MavenSearchToolWindow(private val project: Project) : SimpleToolWindowPanel(true), DumbAware {
  private val mavenSearchContext = MavenSearchContext(project)

  init {
    border = JBUI.Borders.empty()

    setContent(object : JPanel(), DumbAware {
      init {
        layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
        border = JBUI.Borders.empty()

        addGui()
      }

      private fun addGui() {
        val mySplitter = OnePixelSplitter(true, .6f)
        val artifactsView = ArtifactsViewComponent(mavenSearchContext)
        mySplitter.firstComponent = SearchPanel(mavenSearchContext) { selectedArtifactSearchResult ->
          artifactsView.showArtifact(selectedArtifactSearchResult)
        }
        mySplitter.secondComponent = artifactsView
        add(mySplitter)
      }
    })
  }
}