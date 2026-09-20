package com.github.pravezhang2024.copytonotepad.copyout

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object CopyOutNotifier {
    private const val GROUP_ID = "CopyOut"

    fun warn(project: Project?, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(message, NotificationType.WARNING)
            .notify(project)
    }
}
