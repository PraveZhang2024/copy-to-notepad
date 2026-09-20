package com.github.pravezhang2024.copytonotepad

import com.github.pravezhang2024.copytonotepad.copyout.CopyOutService
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.Constraints
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.util.concurrent.atomic.AtomicBoolean

class CopyOutActionRegistrar : ProjectActivity {
    override suspend fun execute(project: Project) {
        ApplicationManager.getApplication().invokeLater {
            registerOnce()
            CopyOutService.getInstance().ensureLauncher()
        }
    }

    companion object {
        private val registered = AtomicBoolean(false)

        private val terminalGroupIds = listOf(
            "Terminal.ReworkedTerminalContextMenu",
            "Terminal.OutputContextMenu",
            "Terminal.PromptContextMenu",
        )

        private fun registerOnce() {
            if (!registered.compareAndSet(false, true)) {
                return
            }
            val actionManager = ActionManager.getInstance()
            val action = actionManager.getAction("CopyOut.CopyOutAction") ?: return
            for (groupId in terminalGroupIds) {
                val group = actionManager.getAction(groupId) as? DefaultActionGroup ?: continue
                if (group.getChildActionsOrStubs().none { it === action }) {
                    group.add(action, Constraints.FIRST)
                }
            }
        }
    }
}
