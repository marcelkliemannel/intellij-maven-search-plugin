package dev.turingcomplete.maven._search.searchmavenorg

import com.fasterxml.jackson.databind.JsonNode
import dev.turingcomplete.maven._search.Artifact
import dev.turingcomplete.maven._search.ArtifactModule
import dev.turingcomplete.maven._search.SearchResult
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import java.util.*
import kotlin.collections.ArrayList

abstract class MavenOrgArtifactModulesSearchHandler(title: String): MavenOrgSearchHandler<ArtifactModule>(title) {
  /**
   * Example response JSON object:
   * ```
   * "response": {
   *   "numFound": 803,
   *   "start": 0,
   *   "docs": [
   *     {
   *      "id": "com.google.inject:guice",
   *      "g": "com.google.inject",
   *      "a": "guice",
   *      "latestVersion": "4.2.3",
   *      "p": "jar",
   *      "timestamp": 1584647910000,
   *      "versionCount": 15,
   *      "ec": [
   *        "-javadoc.jar",
   *        "-sources.jar",
   *        "-test-sources.jar",
   *        ".jar",
   *        "-tests.jar",
   *        "-no_aop.jar",
   *        "-classes.jar",
   *        ".pom"
   *      ]
   *     }
   *     ...
   *   ]
   * }
   * ```
   */
  override fun parseResponse(wholeResponse: JsonNode, query: String): SearchResult<ArtifactModule> {
    val response = wholeResponse.get("response")
    val totalCount = response.get("numFound").intValue()
    val start = response.get("start").intValue()
    val artifactResults = response.withArray<JsonNode>("docs").map { doc ->
      val artifactId = doc.get("a").asText()
      val groupId = doc.get("g").asText()
      val versionCount = doc.get("versionCount").asInt()
      val latestVersion = DefaultArtifactVersion(doc.get("latestVersion").asText())
      val time = Date(doc.get("timestamp").asLong())
      val packaging = doc.get("p").asText()
      val fileNames = doc.get("ec").map { "$artifactId-$latestVersion${it.asText()}" }.toCollection(ArrayList())
      val artifact = Artifact(groupId, artifactId, latestVersion, time, packaging, fileNames)
      ArtifactModule(groupId, artifactId, versionCount, latestVersion, mutableListOf(artifact))
    }.toCollection(ArrayList())

    return SearchResult(query, this, totalCount, start, artifactResults)
  }
}