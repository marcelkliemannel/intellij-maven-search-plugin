package dev.turingcomplete.maven

import com.intellij.notification.Notification
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import dev.turingcomplete.maven._search.SearchManager

class MavenSearchContext(val project: Project) {
  companion object {
    @JvmField
    val NOTIFICATION_GROUP = NotificationGroup("Maven Search", NotificationDisplayType.BALLOON, true)
  }

  val searchManager = SearchManager(project)

  fun errorNotify(content: String): Notification {
    val notification: Notification = NOTIFICATION_GROUP.createNotification(content, NotificationType.ERROR)
    notification.notify(project)
    return notification
  }
}