package dev.turingcomplete.maven._search.searchmavenorg

import com.fasterxml.jackson.databind.JsonNode
import dev.turingcomplete.maven._search.SearchHandler
import dev.turingcomplete.maven._search.SearchResult

/**
 * [API Guide | REST API](https://search.maven.org/classic/#api)
 */
abstract class MavenOrgSearchHandler<T>(title: String) : SearchHandler(title) {
  abstract fun getParameters(query: String): Map<String, String>

  abstract fun parseResponse(wholeResponse: JsonNode, query: String): SearchResult<T>
}