package dev.turingcomplete.maven._gui._artifactsview

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.labels.DropDownLink
import com.intellij.util.Consumer
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.maven.MavenSearchContext
import dev.turingcomplete.maven._search.Artifact
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JPanel


class ArtifactViewComponent(private val artifact: Artifact, private val mavenSearchContext: MavenSearchContext) : JPanel() {
  companion object {
    @JvmField
    val LOGGER = Logger.getInstance(ArtifactViewComponent::class.java)
  }

  init {
    border = JBUI.Borders.empty(8, 10)
    layout = GridBagLayout()

    // Group ID
    add(JBLabel("Group ID:"), GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, JBUI.emptyInsets(), 0, 0))
    val groupIdLabel: HyperlinkLabel = HyperlinkLabel().apply {
      setHyperlinkText(artifact.groupId)
      val groupIdBrowseUrl = mavenSearchContext.searchManager.searchEngine.getGroupIdBrowseUrl(artifact.groupId)
      toolTipText = "Browse '$groupIdBrowseUrl'"
      setHyperlinkTarget(groupIdBrowseUrl)
    }
    add(groupIdLabel, GridBagConstraints(1, 0, GridBagConstraints.REMAINDER, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, JBUI.insetsLeft(UIUtil.DEFAULT_HGAP), 0, 0))

    // Artifact ID
    add(JBLabel("Artifact ID:"), GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, JBUI.insets(UIUtil.DEFAULT_VGAP, 0, 0, 0), 0, 0))
    val artifactIdLabel: HyperlinkLabel = HyperlinkLabel().apply {
      setHyperlinkText(artifact.artifactId)
      val artifactIdBrowseUrl = mavenSearchContext.searchManager.searchEngine.getArtifactIdBrowseUrl(artifact.groupId, artifact.artifactId)
      toolTipText = "Browse '$artifactIdBrowseUrl'"
      setHyperlinkTarget(artifactIdBrowseUrl)
    }
    add(artifactIdLabel, GridBagConstraints(1, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, JBUI.insets(UIUtil.DEFAULT_VGAP, UIUtil.DEFAULT_HGAP, 0, 0), 0, 0))

    // Version
    add(JBLabel("Version:"), GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, JBUI.insets(UIUtil.DEFAULT_VGAP, 0, 0, 0), 0, 0))
    val versionWrapper = JPanel(FlowLayout(FlowLayout.LEFT, UIUtil.DEFAULT_HGAP, UIUtil.DEFAULT_VGAP)).apply {
      add(HyperlinkLabel().apply {
        setHyperlinkText(artifact.version.toString())
        val versionBrowseUrl = mavenSearchContext.searchManager.searchEngine.getVersionBrowseUrl(artifact.groupId, artifact.artifactId, artifact.version.toString())
        toolTipText = "Browse '$versionBrowseUrl'"
        setHyperlinkTarget(versionBrowseUrl)
      })
      add(JBLabel("(${DateFormatUtil.formatPrettyDate(artifact.date)})"))
    }
    add(versionWrapper, GridBagConstraints(1, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, JBUI.emptyInsets(), 0, 0))

    // Download artifact files drop down
    val downloadArtifactFilesDropDown = DropDownLink("Download Artifact Files",
                                                     artifact.fileNames.sorted(),
                                                     Consumer { downloadArtifact(it) },
                                                     false)
    add(downloadArtifactFilesDropDown, GridBagConstraints(0, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1.0, 1.0, GridBagConstraints.NORTHEAST, GridBagConstraints.BOTH, JBUI.insetsTop(UIUtil.DEFAULT_VGAP), 0, 0))
  }

  private fun downloadArtifact(fileName: String) {
    FileChooserFactory.getInstance()
            .createSaveFileDialog(FileSaverDescriptor("Download '${fileName}'", "Save to"), mavenSearchContext.project)
            .save(null, fileName)?.let { targetFile ->
              mavenSearchContext.searchManager.downloadArtifactFileAsync(artifact, fileName, targetFile.file.outputStream()) { e ->
                LOGGER.warn("Failed to download '${fileName}'.", e)
                mavenSearchContext.errorNotify("Failed to download '${fileName}'. See idea.log for details.")
              }
            }
  }
}