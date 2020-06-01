package dev.turingcomplete.maven._search

import org.apache.maven.artifact.versioning.ArtifactVersion
import java.util.*

class ArtifactModule(val groupId: String,
                     val artifactId: String,
                     var totalAvailableVersions: Int,
                     var latestVersion: ArtifactVersion?,
                     val artifacts: MutableList<Artifact>) {

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }

    if (javaClass != other?.javaClass) {
      return false
    }

    other as ArtifactModule
    return Objects.equals(groupId, other.groupId)
           && Objects.equals(artifactId, other.artifactId)
  }

  override fun hashCode(): Int = Objects.hash(groupId, artifactId)
}