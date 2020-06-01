package dev.turingcomplete.maven._search.searchmavenorg

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class MavenOrgSimplifiedCoordinatesSearchHandlerTest {
  @Test
  fun test() {
    val mavenOrgCoordinatesSearchHandler = MavenOrgSimplifiedCoordinatesSearchSearchHandler()

    Assertions.assertEquals(mapOf(Pair("q", "g:group")), mavenOrgCoordinatesSearchHandler.getParameters("group"))
    Assertions.assertEquals(mapOf(Pair("q", "g:group+AND+a:artifact")), mavenOrgCoordinatesSearchHandler.getParameters("group:artifact"))
  }
}