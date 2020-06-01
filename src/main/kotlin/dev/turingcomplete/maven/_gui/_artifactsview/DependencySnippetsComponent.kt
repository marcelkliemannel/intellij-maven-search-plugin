package dev.turingcomplete.maven._gui._artifactsview

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.TabbedPaneWrapper
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.TextTransferable
import dev.turingcomplete.maven.MavenSearchContext
import dev.turingcomplete.maven._gui._helper.PopUpAction
import dev.turingcomplete.maven._search.Artifact
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class DependencySnippetsComponent(private val artifact: Artifact, mavenSearchContext: MavenSearchContext) : JPanel() {
  companion object {
    private val snippetProviders: List<SnippetProvider> = listOf(MavenSnippetProvider(),
                                                                 GradleSnippetProvider(),
                                                                 GroovyGrapeSnippetProvider())
  }

  init {
    layout = BorderLayout()
    border = JBUI.Borders.empty()

    add(TabbedPaneWrapper(mavenSearchContext.project).apply {
      snippetProviders.forEach { addTab(it.title, createSnippetComponent(it)) }
    }.component, BorderLayout.CENTER)
  }

  private fun createSnippetComponent(snippetProvider: SnippetProvider): JComponent {
    return SimpleToolWindowPanel(false).apply {
      border = JBUI.Borders.empty()
      toolbar = createToolBarComponent(snippetProvider)

      val snippetTextArea = JBTextArea().apply {
        isEditable = false
        border = JBUI.Borders.empty(3, 6)
      }
      setContent(ScrollPaneFactory.createScrollPane(snippetTextArea, true))

      snippetProvider.refreshSnippetText = {
        val snippetText = snippetProvider.getSnippetText(artifact)
        snippetTextArea.text = snippetText
        snippetTextArea.rows = snippetText.split("\n").size
      }
      snippetProvider.refreshSnippetText()
    }
  }

  private fun createToolBarComponent(snippetProvider: SnippetProvider): JComponent {
    val toolBarActionGroup = DefaultActionGroup()

    // Copy to clipboard action
    val copyToClipboardAction = object : DumbAwareAction("Copy to Clipboard", null, AllIcons.Actions.Copy) {
      override fun actionPerformed(e: AnActionEvent) {
        CopyPasteManager.getInstance().setContents(TextTransferable(snippetProvider.getSnippetText(artifact) as CharSequence))
      }
    }
    toolBarActionGroup.add(copyToClipboardAction)

    // Snippets provider actions
    snippetProvider.getToolBarActionGroup()?.run {
      toolBarActionGroup.addSeparator()
      toolBarActionGroup.addAll(this)
    }

    // Help action
    toolBarActionGroup.addSeparator()
    val browseSnippetsProviderHelpUrl = object : DumbAwareAction("Show Help...", null, AllIcons.Actions.Help) {
      override fun actionPerformed(e: AnActionEvent) {
        BrowserUtil.browse(snippetProvider.helpUrl)
      }
    }
    toolBarActionGroup.add(browseSnippetsProviderHelpUrl)


    return ActionManager.getInstance()
            .createActionToolbar(ActionPlaces.UNKNOWN, toolBarActionGroup, false)
            .component
  }

  private abstract class SnippetProvider(val title: String, val helpUrl: String) {
    lateinit var refreshSnippetText: () -> Unit

    abstract fun getSnippetText(artifact: Artifact): String

    open fun getToolBarActionGroup(): DefaultActionGroup? = null
  }

  private class MavenSnippetProvider : SnippetProvider("Maven", "https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html") {
    var selectedScope: Scope = Scope.NONE

    override fun getSnippetText(artifact: Artifact): String {
      var snippetText = "<dependency>\n" +
                        "\t<groupId>${artifact.groupId}</groupId>\n" +
                        "\t<artifactId>${artifact.artifactId}</artifactId>\n" +
                        "\t<version>${artifact.version}</version>\n"

      if (selectedScope != Scope.NONE) {
        snippetText += "\t<scope>${selectedScope.name.toLowerCase()}</scope>\n"
      }

      snippetText += "</dependency>"

      return snippetText
    }

    override fun getToolBarActionGroup(): DefaultActionGroup {
      return DefaultActionGroup(PopUpAction.forToggleAction(Scope.values().map { it.name.toLowerCase() to it }.toMap(),
                                                            AllIcons.General.Settings,
                                                            "Select Scope",
                                                            { selectedScope == it },
                                                            {
                                                              selectedScope = it
                                                              refreshSnippetText()
                                                            }))
    }

    private enum class Scope { NONE, COMPILE, PROVIDED, RUNTIME, TEST }
  }

  private class GradleSnippetProvider : SnippetProvider("Gradle", "https://docs.gradle.org/current/userguide/dependency_management_for_java_projects.html") {
    var selectedLanguage: Language = Language.GROOVY
    var selectedSyntax: Syntax = Syntax.FULL
    var selectedConfiguration: Configuration = Configuration.IMPLEMENTATION

    override fun getSnippetText(artifact: Artifact): String {
      return when (selectedLanguage) {
        Language.GROOVY -> when (selectedSyntax) {
          Syntax.FULL      -> "${selectedConfiguration.configurationName} group: '${artifact.groupId}', name: '${artifact.artifactId}', version: '${artifact.version}'"
          Syntax.SHORTHAND -> "${selectedConfiguration.configurationName}('${artifact.groupId}:${artifact.artifactId}:${artifact.version}')"
        }
        Language.KOTLIN -> when (selectedSyntax) {
          Syntax.FULL      -> "${selectedConfiguration.configurationName}(group = \"${artifact.groupId}\", name = \"${artifact.artifactId}\", version = \"${artifact.version}\")"
          Syntax.SHORTHAND -> "${selectedConfiguration.configurationName}(\"${artifact.groupId}:${artifact.artifactId}:${artifact.version}\")"
        }
      }
    }

    override fun getToolBarActionGroup(): DefaultActionGroup {
      val languageAction = PopUpAction.forToggleAction(Language.values().map { it.name.toLowerCase().capitalize() to it }.toMap(),
                                                       AllIcons.General.Filter,
                                                       "Select Language",
                                                       { selectedLanguage == it },
                                                       {
                                                         selectedLanguage = it
                                                         refreshSnippetText()
                                                       })
      val syntaxAction = PopUpAction.forToggleAction(Syntax.values().map { it.name.toLowerCase().capitalize() to it }.toMap(),
                                                     AllIcons.Ide.LocalScope,
                                                     "Select Syntax",
                                                     { selectedSyntax == it },
                                                     {
                                                       selectedSyntax = it
                                                       refreshSnippetText()
                                                     })
      val configurationAction = PopUpAction.forToggleAction(Configuration.values().map { it.configurationName to it }.toMap(),
                                                            AllIcons.General.Settings,
                                                            "Select Configuration",
                                                            { selectedConfiguration == it },
                                                            {
                                                              selectedConfiguration = it
                                                              refreshSnippetText()
                                                            })
      return DefaultActionGroup(languageAction, syntaxAction, configurationAction)
    }

    private enum class Language { GROOVY, KOTLIN }

    private enum class Syntax { FULL, SHORTHAND }

    private enum class Configuration(val configurationName: String) {
      API("api"),
      IMPLEMENTATION("implementation"),
      COMPILE("compile"),
      COMPILE_ONLY("compileOnly"),
      RUNTIME_ONLY("runtimeOnly"),
      TEST_IMPLEMENTATION("testImplementation"),
      TEST_COMPILE_ONLY("testCompileOnly"),
      TEST_RUNTIME_ONLY("testRuntimeOnly")
    }
  }

  private class GroovyGrapeSnippetProvider : SnippetProvider("Groovy Grape", "http://groovy-lang.org/grape.html") {
    var selectedSyntax = Syntax.FULL
    var useLegacyGrapesWrapper = false

    override fun getSnippetText(artifact: Artifact): String {
      val snippet = when (selectedSyntax) {
        Syntax.FULL      -> "@Grab(group='${artifact.groupId}', module='${artifact.artifactId}', version='${artifact.version}')"
        Syntax.SHORTHAND -> "@Grab('${artifact.groupId}:${artifact.artifactId}:${artifact.version}')"
      }

      if (useLegacyGrapesWrapper) {
        return """
          @Grapes([
            $snippet
          ])
        """.trimIndent()
      }

      return snippet
    }

    override fun getToolBarActionGroup(): DefaultActionGroup {
      val selectSyntaxAction = PopUpAction.forToggleAction(Syntax.values().map { it.name.toLowerCase().capitalize() to it }.toMap(),
                                                           AllIcons.Ide.LocalScope,
                                                           "Select Syntax",
                                                           { selectedSyntax == it },
                                                           {
                                                            selectedSyntax = it
                                                            refreshSnippetText()
                                                          })

      val legacyGrapesWrapper = object: DumbAwareToggleAction("Legacy @Grapes Wrapper", null, AllIcons.Json.Array) {
        override fun isSelected(e: AnActionEvent): Boolean = useLegacyGrapesWrapper

        override fun setSelected(e: AnActionEvent, state: Boolean) {
          useLegacyGrapesWrapper = state
          refreshSnippetText()
        }
      }

      return DefaultActionGroup(selectSyntaxAction, legacyGrapesWrapper)
    }

    private enum class Syntax { FULL, SHORTHAND }
  }
}