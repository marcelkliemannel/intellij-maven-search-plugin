package dev.turingcomplete.maven

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory

class MyToolWindowFactory: ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MavenSearchToolWindow(project)
        val content: Content = ContentFactory.SERVICE
            .getInstance()
            .createContent(myToolWindow, "search.maven.org", true)
        toolWindow.contentManager.addContent(content)
    }
}