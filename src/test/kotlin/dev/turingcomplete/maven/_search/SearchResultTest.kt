package dev.turingcomplete.maven._search

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.stream.IntStream
import kotlin.streams.toList

class SearchResultTest {
  @ParameterizedTest(name = "totalCount: {0}, start: {1}, entriesSize: {2}, page: {3}")
  @CsvSource("10,0,10,0" /* just one page */,
          "100,0,8,0" /* first page */,
          "100,8,8,1",
          "100,16,8,2",
          "100,24,8,3",
          "100,32,8,4",
          "100,40,8,5",
          "100,48,8,6",
          "100,56,8,7",
          "100,64,8,8",
          "100,72,8,9",
          "100,80,8,10",
          "100,88,8,11",
          "100,96,8,12" /* last page with less entries than records per page */,
          "100,90,10,9" /* last page with exact entries then records per page */,
          "100,99,1,99",
          "0,0,0,0" /* no result */)
  fun testGetPage(totalCount: Int, start: Int, entriesSize: Int, expectedPage: Int) {
    val searchResult = SearchResult("foo", object : SearchHandler("foo") {
      override fun getDescriptionHtml(): String = "foo"
    }, totalCount, start, IntStream.range(0, entriesSize).toList())

    Assertions.assertEquals(expectedPage, searchResult.getPage())
  }

  @ParameterizedTest(name = "totalCount: {0}, start: {1}, entriesSize: {2}, hasNextPreviousPage: {3}")
  @CsvSource("100,0,10,false" /* first page */,
             "100,10,10,true",
             "10,0,10,false" /* just one page */,
             "0,0,0,false" /* no result */)
  fun testHasPreviousPage(totalCount: Int, start: Int, entriesSize: Int, expectedResult: Boolean) {
    val searchResult = SearchResult("foo", object : SearchHandler("foo") {
      override fun getDescriptionHtml(): String = "foo"
    }, totalCount, start, IntStream.range(0, entriesSize).toList())

    Assertions.assertEquals(expectedResult, searchResult.hasPreviousPage())
  }

  @ParameterizedTest(name = "totalCount: {0}, start: {1}, entriesSize: {2}, hasNextPage: {3}")
  @CsvSource("100,0,10,true" /* first page */,
             "100,10,10,true",
             "10,0,10,false" /* just one page */,
             "100,90,10,false" /* last page */,
             "100,99,1,false" /* last page */,
             "0,0,0,false" /* no result */)
  fun testHasNextPage(totalCount: Int, start: Int, entriesSize: Int, expectedResult: Boolean) {
    val searchResult = SearchResult("foo", object : SearchHandler("foo") {
      override fun getDescriptionHtml(): String = "foo"
    }, totalCount, start, IntStream.range(0, entriesSize).toList())

    Assertions.assertEquals(expectedResult, searchResult.hasNextPage())
  }
}