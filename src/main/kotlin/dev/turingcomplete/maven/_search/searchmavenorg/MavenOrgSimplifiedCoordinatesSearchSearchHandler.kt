package dev.turingcomplete.maven._search.searchmavenorg

import com.intellij.openapi.ui.ValidationInfo

class MavenOrgSimplifiedCoordinatesSearchSearchHandler : MavenOrgArtifactModulesSearchHandler("Simplified Coordinates") {
  override fun getDescriptionHtml(): String {
    return "<p>Simplified Maven coordinates are by colons separated terms with the following scheme: <b>{groupID}<u>:</u>{artifactID}<u>:</u>{version}<u>:</u>{package}</b></p>" +
           "<p>It is possible to leave tokens blank to have some kind of 'all selector'. For example, to search for all artifacts with the group ID <i>com.example</i> and version <i>1.2.3</i> the search query would be: <b>com.example::1.2.3</b><br />" +
           "If the blank token is at the end, the colons can be omit. For example, to search for all versions with the group ID <i>com.example</i> and artifact ID <i>foo</i> the search query would be: <b>com.example:foo</b></p>"
  }

  override fun getParameters(query: String): Map<String, String> {
    val termKeys = MavenOrgAdvancedSearchSearchHandler.TERM_KEYS
    val termKeysSize = termKeys.size
    val queryParameterValue = query.split(Regex(":"))
            .mapIndexed { i, mavenCoordinatesTokenValue ->
              if (i < termKeysSize) "${termKeys[i]}:\"${mavenCoordinatesTokenValue.trim()}\"" else ""
            }
            .joinToString(separator = "+AND+") { it }
    return mapOf(Pair("q", queryParameterValue))
  }

  override fun validateQuery(query: String): ValidationInfo? {
    val termKeysSize = MavenOrgAdvancedSearchSearchHandler.TERM_KEYS.size

    if (query.filter { it == ':' }.count() >= termKeysSize) {
      return ValidationInfo("Coordinates can only have $termKeysSize terms.")
    }

    if (query.contains(Regex("\\s+"))) {
      return ValidationInfo("Use colons instead of spaces for separation.").asWarning().withOKEnabled()
    }

    return null
  }
}