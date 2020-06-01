package dev.turingcomplete.maven._search.searchmavenorg

import com.intellij.openapi.ui.ValidationInfo

/**
 * "Mimics searching by coordinate in Advanced Search. This search uses all
 * coordinates ("g" for groupId, "a" for artifactId, "v" for version, "p" for
 * packaging, "l" for classifier) and uses "AND" to require all terms by default.
 * Only one term is required for the search to work. Terms can also be connected
 * by "OR" separated to make them optional."
 */
class MavenOrgAdvancedSearchSearchHandler : MavenOrgArtifactModulesSearchHandler("Advanced Search") {
  companion object {
    @JvmField
    val TERM_KEYS = listOf("g", "a", "v", "p", "l")
  }

  private val booleanOperators = listOf("AND", "OR")
  private val validationErrorMessage = createValidationErrorMessage()

  override fun getDescriptionHtml(): String {
    return "<p>Fooo</p>"
  }

  override fun getParameters(query: String): Map<String, String> {
    return mapOf(Pair("q", query))
  }

  override fun validateQuery(query: String): ValidationInfo? {
    if (query.isBlank()) {
      return null
    }

    return query.split(Regex("\\s+")).filter { !it.isBlank() }.find { queryToken ->
      val isTermKey = TERM_KEYS.find { queryToken.startsWith("$it:") } != null
      val isBooleanOperator = booleanOperators.contains(queryToken)
      !isTermKey && !isBooleanOperator
    }?.let { ValidationInfo(validationErrorMessage) }
  }

  private fun createValidationErrorMessage(): String {
    val lastTermKeyIndex = TERM_KEYS.size - 1
    val termKeysAsPresentableString = listOf(TERM_KEYS.subList(0, lastTermKeyIndex).joinToString(separator = ", ", transform = { "'$it:'" }),
                                             "'${TERM_KEYS[lastTermKeyIndex]}:'").joinToString(separator = " or ")
    val lastBooleanOperatorsIndex = booleanOperators.size - 1
    val booleanOperatorsAsPresentableString = listOf(booleanOperators.subList(0, lastBooleanOperatorsIndex).joinToString(separator = ", ", transform = { "'$it'" }),
                                                     "'${booleanOperators[lastBooleanOperatorsIndex]}'").joinToString(separator = " or ")
    return "Terms muss either start with $termKeysAsPresentableString or be $booleanOperatorsAsPresentableString."
  }
}