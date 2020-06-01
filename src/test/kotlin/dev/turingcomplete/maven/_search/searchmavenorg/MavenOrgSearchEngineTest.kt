package dev.turingcomplete.maven._search.searchmavenorg

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test

class MavenOrgSearchEngineTest {
  @Test
  fun test() {
    val resourceAsStream = MavenOrgSearchEngineTest::class.java.classLoader.getResourceAsStream("dev/turingcomplete/maven/_search/searchmavenorg/mavenOrgResponse.json")
                           ?:throw IllegalStateException("Test resource not found")
    val readTree = ObjectMapper().readTree(resourceAsStream)
    //val artifactSearchResult = MavenOrgSearchEngine().parseArtifactSearchResult(readTree)
    //print(artifactSearchResult)
  }
}