package dev.turingcomplete.maven._search.searchmavenorg

import com.fasterxml.jackson.databind.JsonNode
import dev.turingcomplete.maven._search.Artifact
import dev.turingcomplete.maven._search.SearchResult
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import java.util.*
import kotlin.collections.ArrayList

class MavenOrgArtifactsSearchHandler : MavenOrgSearchHandler<Artifact>("All Artifact Versions") {
  override fun getDescriptionHtml(): String {
    return "Foo"
  }

  override fun getParameters(query: String): Map<String, String> {
    assert(query.matches(Regex("^g:\"[^\"]*\"\\s+AND\\s+a:\"[^\"]*\"$"))) { "Query should have the format g:{groupId} AND a:{artifactId}" }
    return mapOf(Pair("q", query), Pair("core", "gav"))
  }

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
   *      "v": "4.2.3",
   *      "p": "jar",
   *      "timestamp": 1584647910000,
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
  override fun parseResponse(wholeResponse: JsonNode, query: String): SearchResult<Artifact> {
    val response = wholeResponse.get("response")
    val totalCount = response.get("numFound").intValue()
    val start = response.get("start").intValue()
    val artifacts: List<Artifact> = response.withArray<JsonNode>("docs").map { doc ->
      val artifactId = doc.get("a").asText()
      val groupId = doc.get("g").asText()
      val version = DefaultArtifactVersion(doc.get("v").asText())
      val time = Date(doc.get("timestamp").asLong())
      val packaging = doc.get("p").asText()
      val fileNames = doc.get("ec").map { "$artifactId-$version${it.asText()}" }.toCollection(ArrayList())
      Artifact(groupId, artifactId, version, time, packaging, fileNames)
    }.toList()

    return SearchResult(query, this, totalCount, start, artifacts)
  }
}