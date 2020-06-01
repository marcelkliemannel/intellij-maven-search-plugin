package dev.turingcomplete.maven._search

import org.apache.maven.artifact.versioning.ArtifactVersion
import java.util.*

class Artifact(val groupId: String,
               val artifactId: String,
               val version: ArtifactVersion,
               val date: Date,
               val packaging: String,
               val fileNames: List<String>) {

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }

    if (javaClass != other?.javaClass) {
      return false
    }

    other as Artifact
    return Objects.equals(groupId, other.groupId)
           && Objects.equals(artifactId, other.artifactId)
           && Objects.equals(version, other.version)
  }

  override fun hashCode(): Int = Objects.hash(groupId, artifactId, version)
}