package dev.turingcomplete.maven._search.searchmavenorg

/**
 * "Mimics typing "guice" in the basic search box. Returns first page of artifacts
 * with "guice" in the groupId or artifactId and lists details for most recent
 * version released."
 */
class MavenOrgKeywordSearchHandler : MavenOrgArtifactModulesSearchHandler("Keywords") {
  override fun getDescriptionHtml(): String {
    return "<p>The search query can contain a whitespace separated list of keywords that are searched for in group IDs, artifact IDs.</p>" +
           "<p>The search result also includes artifacts whose group or artifact IDs contain a substring of a keyword. For example, the keyword <i>jetty</i> would also find the artifact <i>jetty-server</i>.</p>"
  }

  override fun getParameters(query: String): Map<String, String> {
    return mapOf(Pair("q", query))
  }

  override fun getPresentableGroupId(query: String, groupId: String): String {
    return highlightQueryPartsInText(groupId, query)
  }

  override fun getPresentableArtifactId(query: String, artifactId: String): String {
    return highlightQueryPartsInText(artifactId, query)
  }

  private fun highlightQueryPartsInText(text: String, query: String) : String {
    var highlightedText = text

    query.split(Regex("\\s+")).filter { !it.isBlank() }.forEach { queryPart ->
      highlightedText = highlightedText.replace(queryPart, "<b>$queryPart</b>")
    }

    return if (text != highlightedText) "<html>${highlightedText}</html>" else text
  }
}