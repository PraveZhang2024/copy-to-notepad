package com.github.pravezhang2024.copytonotepad.actions

import com.github.pravezhang2024.copytonotepad.MyBundle
import com.github.pravezhang2024.copytonotepad.copyout.CopyOutNotifier
import com.github.pravezhang2024.copytonotepad.copyout.CopyOutResult
import com.github.pravezhang2024.copytonotepad.copyout.CopyOutService
import com.github.pravezhang2024.copytonotepad.copyout.CopyOutTextResolver
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction

class CopyOutAction : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        event.presentation.isEnabledAndVisible =
            event.place != ActionPlaces.PROJECT_VIEW_POPUP || file?.isDirectory != true
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        when (val result = CopyOutTextResolver.resolve(event)) {
            is CopyOutResult.Text -> CopyOutService.getInstance().export(result.content, project)
            CopyOutResult.Unreadable -> CopyOutNotifier.warn(project, MyBundle.message("copyOut.unreadable"))
            CopyOutResult.NotImplemented -> CopyOutNotifier.warn(project, MyBundle.message("copyOut.notImplemented"))
            CopyOutResult.Empty -> CopyOutNotifier.warn(project, MyBundle.message("copyOut.empty"))
        }
    }
}
