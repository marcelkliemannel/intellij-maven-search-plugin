package dev.turingcomplete.maven._search

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.ValidationInfo

abstract class SearchHandler(val title: String) : DumbAware {
  abstract fun getDescriptionHtml(): String

  open fun getPresentableGroupId(query: String, groupId: String): String = groupId

  open fun getPresentableArtifactId(query: String, artifactId: String): String = artifactId

  open fun getPresentableVersion(query: String, version: String, isLatestVersion: Boolean): String {
    return if (isLatestVersion) "$version (latest)" else version
  }

  open fun validateQuery(query: String): ValidationInfo? = null
}