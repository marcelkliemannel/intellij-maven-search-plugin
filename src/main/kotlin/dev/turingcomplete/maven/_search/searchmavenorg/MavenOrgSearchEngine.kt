package dev.turingcomplete.maven._search.searchmavenorg

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.Urls
import com.intellij.util.io.HttpRequests
import com.intellij.util.net.NetUtils
import dev.turingcomplete.maven._search.*
import java.io.IOException
import java.io.OutputStream

open class MavenOrgSearchEngine : ArtifactSearchEngine<MavenOrgArtifactModulesSearchHandler> {
  companion object {
    @JvmField
    val LOGGER = Logger.getInstance(MavenOrgSearchEngine::class.java)

    @JvmField
    val SEARCH_MAVEN_ORG_BASE_URL = Urls.newUrl("https", "search.maven.org", "/solrsearch/select")
  }

  private val mavenOrgAllArtifactsSearchHandler = MavenOrgArtifactsSearchHandler()
  private val searchHandlers = listOf(MavenOrgKeywordSearchHandler(), MavenOrgSimplifiedCoordinatesSearchSearchHandler(), MavenOrgAdvancedSearchSearchHandler())
  private var activeSearchHandler: MavenOrgArtifactModulesSearchHandler = searchHandlers[0]

  private val rows = 100

  override fun getSearchHandlers(): List<MavenOrgArtifactModulesSearchHandler> = searchHandlers

  override fun setActiveSearchHandler(searchHandler: MavenOrgArtifactModulesSearchHandler) {
    activeSearchHandler = searchHandler
  }

  override fun getActiveSearchHandler(): MavenOrgArtifactModulesSearchHandler = activeSearchHandler

  override fun searchArtifactModules(query: String, offset: Int, searchHandler: MavenOrgArtifactModulesSearchHandler, indicator: ProgressIndicator): SearchResult<ArtifactModule> {
    return executeSearch(query, offset, searchHandler, indicator)
  }

  override fun getSearchArtifactModulesTaskTitle(query: String) = "Search for '$query' on search.maven.org..."

  override fun searchArtifacts(groupId: String, artifactId: String, offset: Int, indicator: ProgressIndicator): SearchResult<Artifact> {
    return executeSearch("g:\"$groupId\" AND a:\"$artifactId\"", offset, mavenOrgAllArtifactsSearchHandler, indicator)
  }

  override fun getSearchArtifactTasksTitle(groupId: String, artifactId: String): String {
    return "Load more versions of '$artifactId' on search.maven.org..."
  }

  private fun <T> executeSearch(query: String,
                                offset: Int,
                                searchHandler: MavenOrgSearchHandler<T>,
                                indicator: ProgressIndicator): SearchResult<T> {
    indicator.checkCanceled()

    val requestUrl = SEARCH_MAVEN_ORG_BASE_URL
            .addParameters(searchHandler.getParameters(query))
            .addParameters(mapOf(Pair("wt", "json"),
                                 Pair("rows", "$rows"),
                                 Pair("start", "$offset"),
                                 Pair("sort", "g asc, a asc, v asc"),
                                 Pair("fl", "g,a,latestVersion,p,ec,repositoryId,timestamp,versionCount"))) // todo for versions different
    LOGGER.info("Search maven artifacts with request: '$requestUrl'.")

    return HttpRequests.request(requestUrl)
            .accept("application/json")
            .productNameAsUserAgent()
            .connect { request ->
              indicator.checkCanceled()
              try {
                // TODO check HTTP status
                searchHandler.parseResponse(ObjectMapper().readTree(request.reader), query)
              }
              catch (e: IOException) {
                val errorMessage = HttpRequests.createErrorMessage(e, request, false)
                LOGGER.info("Failed to search for '$query': $errorMessage", e)
                throw ArtifactSearchException("Failed to search for '$query'.")
              }
            }
  }

  override fun downloadArtifactFile(artifact: Artifact, fileName: String, target: OutputStream, indicator: ProgressIndicator) {
    HttpRequests.request("https://search.maven.org/remotecontent?filepath=${artifact.groupId.replace(".", "/")}/${artifact.artifactId}/${artifact.version}/$fileName")
            .productNameAsUserAgent()
            .connect { request ->
              try {
                val contentLength = request.connection.contentLength
                NetUtils.copyStreamContent(indicator, request.inputStream, target, contentLength)
              }
              catch (e: IOException) {
                val errorMessage = HttpRequests.createErrorMessage(e, request, false)
                throw ArtifactSearchException("Failed to download '$fileName': $errorMessage", e)
              }
            }
  }

  override fun getGroupIdBrowseUrl(groupId: String) : String {
    return "https://repo1.maven.org/maven2/${groupId.replace(".", "/")}/"
  }

  override fun getArtifactIdBrowseUrl(groupId: String, artifactId: String) : String {
    return "https://repo1.maven.org/maven2/${groupId.replace(".", "/")}/$artifactId/"
  }

  override fun getVersionBrowseUrl(groupId: String, artifactId: String, version: String): String {
    return "https://repo1.maven.org/maven2/${groupId.replace(".", "/")}/$artifactId/$version/"
  }
}