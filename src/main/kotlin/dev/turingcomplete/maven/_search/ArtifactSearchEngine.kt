package dev.turingcomplete.maven._search

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbAware
import java.io.OutputStream

interface ArtifactSearchEngine<T: SearchHandler> : DumbAware {
  fun getSearchHandlers(): List<T>

  fun setActiveSearchHandler(searchHandler: T)

  fun getActiveSearchHandler() : T

  fun searchArtifactModules(query: String, offset: Int, searchHandler: T, indicator: ProgressIndicator): SearchResult<ArtifactModule>

  fun getSearchArtifactModulesTaskTitle(query: String): String

  fun searchArtifacts(groupId: String, artifactId: String, offset: Int, indicator: ProgressIndicator): SearchResult<Artifact>

  fun getSearchArtifactTasksTitle(groupId: String, artifactId: String): String

  fun downloadArtifactFile(artifact: Artifact, fileName: String, target: OutputStream, indicator: ProgressIndicator)

  fun getGroupIdBrowseUrl(groupId: String) : String

  fun getArtifactIdBrowseUrl(groupId: String, artifactId: String) : String

  fun getVersionBrowseUrl(groupId: String, artifactId: String, version: String) : String
}