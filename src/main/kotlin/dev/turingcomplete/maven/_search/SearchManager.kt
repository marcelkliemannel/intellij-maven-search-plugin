package dev.turingcomplete.maven._search

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.util.castSafelyTo
import dev.turingcomplete.maven._search.searchmavenorg.MavenOrgSearchEngine
import java.io.OutputStream
import java.util.concurrent.locks.ReentrantLock

class SearchManager(private val project: Project) : DumbAware {
  val searchEngine: ArtifactSearchEngine<SearchHandler> = MavenOrgSearchEngine().castSafelyTo<ArtifactSearchEngine<SearchHandler>>()!!

  private var searchTaskIndicator: ProgressIndicator? = null
  private val searchTaskLock: ReentrantLock = ReentrantLock()

  fun searchArtifactModulesAsync(query: String,
                                 offset: Int,
                                 searchHandler: SearchHandler,
                                 onSuccess: (SearchResult<ArtifactModule>) -> Unit,
                                 onFailed: (Throwable) -> Unit,
                                 onFinished: () -> Unit) {

    doSearchAsync({ indicator -> searchEngine.searchArtifactModules(query, offset, searchHandler, indicator) },
                  searchEngine.getSearchArtifactModulesTaskTitle(query),
                  onSuccess, onFailed, onFinished)
  }

  fun searchArtifactsAsync(groupId: String,
                           artifactId: String,
                           offset: Int,
                           onSuccess: (SearchResult<Artifact>) -> Unit,
                           onFailed: (Throwable) -> Unit,
                           onFinished: () -> Unit) {

    doSearchAsync({ indicator -> searchEngine.searchArtifacts(groupId, artifactId, offset, indicator) },
                  searchEngine.getSearchArtifactTasksTitle(groupId, artifactId),
                  onSuccess, onFailed, onFinished)
  }

  fun downloadArtifactFileAsync(artifact: Artifact, fileName: String, target: OutputStream, onFailed: (Throwable) -> Unit) {
    val downloadTask = object : Task.Backgroundable(project, "Downloading '${fileName}'") {
      override fun run(indicator: ProgressIndicator) {
        if (project.isDisposed) {
          return
        }
        searchEngine.downloadArtifactFile(artifact, fileName, target, indicator)
      }

      override fun onThrowable(error: Throwable) {
        if (project.isDisposed) {
          return
        }
        onFailed(error)
      }
    }
    ProgressManager.getInstance().runProcessWithProgressAsynchronously(downloadTask, BackgroundableProcessIndicator(downloadTask))
  }

  private fun <T> doSearchAsync(search: (ProgressIndicator) -> SearchResult<T>,
                                taskTitle: String,
                                onSuccess: (SearchResult<T>) -> Unit,
                                onFailed: (Throwable) -> Unit,
                                onFinished: () -> Unit) {
    try {
      searchTaskLock.lock()

      searchTaskIndicator?.cancel()

      val searchTask = object : Task.Backgroundable(project, taskTitle, true) {
        private lateinit var searchResult: SearchResult<T>

        override fun run(indicator: ProgressIndicator) {
          if (project.isDisposed) {
            return
          }
          indicator.checkCanceled()
          searchResult = search(indicator)
        }

        override fun onThrowable(error: Throwable) {
          if (project.isDisposed) {
            return
          }
          onFailed(error)
        }

        override fun onFinished() {
          if (project.isDisposed) {
            return
          }
          onFinished()
        }

        override fun onSuccess() {
          if (project.isDisposed) {
            return
          }
          onSuccess(searchResult)
        }
      }
      searchTaskIndicator = BackgroundableProcessIndicator(searchTask)
      ProgressManager.getInstance().runProcessWithProgressAsynchronously(searchTask, searchTaskIndicator!!)
    }
    finally {
      searchTaskLock.unlock()
    }
  }
}