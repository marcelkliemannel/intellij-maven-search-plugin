package dev.turingcomplete.maven._search

class ArtifactSearchException : Exception {
  constructor(message: String) : super(message)

  constructor(message: String, cause: Throwable) : super(message, cause)
}