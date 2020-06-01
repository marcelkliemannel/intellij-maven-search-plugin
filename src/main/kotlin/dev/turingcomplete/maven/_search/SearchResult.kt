package dev.turingcomplete.maven._search

import kotlin.math.ceil

/**
 * @throws IllegalArgumentException if `totalAvailableRecords` or `offset`
 * is negative.
 * @throws IllegalArgumentException if `offset` is equal to or greater than
 * `totalAvailableRecords`.
 * @throws IllegalArgumentException if `offset` is not a multiples of
 * `records.size`.
 */
class SearchResult<T>(val query: String,
                      val searchHandler: SearchHandler,
                      val totalAvailableRecords: Int,
                      val offset: Int,
                      val records: List<T>) {
  init {
    require(totalAvailableRecords >= 0) { "Negative total available records." }
    require(offset >= 0) { "Negative offset." }
    require(offset <= totalAvailableRecords) { "Offset out of range ($offset/$totalAvailableRecords)." }

    val recordsSize = records.size
    //require(recordsSize == 0 || totalAvailableRecords -  || offset % recordsSize == 0) { "Records size should be a multiples of offset ($offset/$recordsSize)." }
  }

  /**
   * Gets the page of this search result based on the [offset] and records
   * per page, which is given by `records.size`.
   *
   * **The page numbers start at `0`.**
   *
   * It is assumed that the size of the records is the same between each two
   * [SearchResult], with the exception of the last one.
   */
  fun getPage(): Int {
    if (offset == 0) {
      return 0
    }

    val recordsPerPage = records.size
    return ceil(offset.toDouble() / recordsPerPage.toDouble()).toInt()
  }

  fun hasPreviousPage(): Boolean {
    val currentPage = getPage()

    if (currentPage == -1) {
      return false
    }

    return currentPage != 0
  }

  fun hasNextPage() : Boolean {
    val currentPage = getPage()
    val totalPages = (totalAvailableRecords - 1).div(records.size + 1)
    return currentPage < totalPages
  }

  fun getPresentableGroupId(groupId: String): String {
    return searchHandler.getPresentableGroupId(query, groupId)
  }

  fun getPresentableArtifactId(artifactId: String): String {
    return searchHandler.getPresentableArtifactId(query, artifactId)
  }

  fun getPresentableVersion(version: String, isLatestVersion: Boolean): String {
    return searchHandler.getPresentableVersion(query, version, isLatestVersion)
  }
}
