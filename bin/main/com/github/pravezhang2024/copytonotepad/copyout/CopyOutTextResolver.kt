package com.github.pravezhang2024.copytonotepad.copyout

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.frontend.view.TerminalView
import com.intellij.terminal.frontend.view.activeOutputModel
import java.awt.Component
import java.awt.datatransfer.DataFlavor
import javax.swing.SwingUtilities

object CopyOutTextResolver {
    private const val MAX_FILE_BYTES = 32L * 1024 * 1024
    private val CLASSIC_SELECTED_TEXT_KEY = DataKey.create<String>("SelectedText")

    fun resolve(event: AnActionEvent): CopyOutResult {
        if (isProjectViewPopup(event)) {
            val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return CopyOutResult.Empty
            if (file.isDirectory) {
                return CopyOutResult.NotImplemented
            }
            return readVirtualFile(file)
        }

        if (isTerminalContext(event)) {
            val selected = terminalSelectedText(event)
            return if (selected.isNullOrEmpty()) CopyOutResult.Empty else CopyOutResult.Text(selected)
        }

        val editorText = selectedText(event.getData(CommonDataKeys.EDITOR))
        if (!editorText.isNullOrEmpty()) {
            return CopyOutResult.Text(editorText)
        }

        val clipboard = clipboardText()
        return if (clipboard.isNullOrEmpty()) CopyOutResult.Empty else CopyOutResult.Text(clipboard)
    }

    fun readVirtualFile(file: VirtualFile): CopyOutResult {
        if (file.fileType.isBinary || file.length > MAX_FILE_BYTES) {
            return CopyOutResult.Unreadable
        }
        return try {
            val text = ReadAction.compute<String, Exception> { VfsUtil.loadText(file) }
            if (text.contains('\u0000')) CopyOutResult.Unreadable else CopyOutResult.Text(text)
        } catch (_: Exception) {
            CopyOutResult.Unreadable
        }
    }

    private fun isProjectViewPopup(event: AnActionEvent): Boolean =
        event.place == ActionPlaces.PROJECT_VIEW_POPUP

    private fun clipboardText(): String? {
        val contents = CopyPasteManager.getInstance().contents ?: return null
        if (!contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            return null
        }
        return runCatching { contents.getTransferData(DataFlavor.stringFlavor) as? String }.getOrNull()
    }

    private fun isTerminalContext(event: AnActionEvent): Boolean {
        if (event.place.contains("Terminal", ignoreCase = true)) {
            return true
        }
        if (findTerminalView(event) != null) {
            return true
        }
        if (isInsideTerminalToolWindow(event)) {
            return true
        }
        val editor = editorFromComponent(event) ?: event.getData(CommonDataKeys.EDITOR)
        return editor != null && isTerminalEditor(editor)
    }

    @Suppress("UnstableApiUsage")
    private fun terminalSelectedText(event: AnActionEvent): String? {
        findTerminalView(event)?.let { view ->
            selectedTextFromReworkedTerminal(view)
        }?.let { return it }

        editorFromComponent(event)?.let { selectedText(it) }?.let { return it }

        event.getData(CommonDataKeys.EDITOR)
            ?.takeIf { isTerminalEditor(it) }
            ?.let { selectedText(it) }
            ?.let { return it }

        return classicTerminalSelection(event)
    }

    @Suppress("UnstableApiUsage")
    private fun selectedTextFromReworkedTerminal(view: TerminalView): String? {
        val selection = view.textSelectionModel.selection ?: return null
        return view.activeOutputModel()
            .getText(selection.startOffset, selection.endOffset)
            .toString()
            .takeIf { it.isNotEmpty() }
    }

    @Suppress("UnstableApiUsage")
    private fun findTerminalView(event: AnActionEvent): TerminalView? {
        for (context in dataContexts(event)) {
            (context.getData(TerminalView.DATA_KEY) as? TerminalView)?.let { return it }
        }
        return null
    }

    private fun dataContexts(event: AnActionEvent): List<DataContext> {
        val contexts = LinkedHashSet<DataContext>()
        contexts += event.dataContext
        contextComponent(event)?.let { component ->
            contexts += DataManager.getInstance().getDataContext(component)
        }
        return contexts.toList()
    }

    private fun editorFromComponent(event: AnActionEvent): Editor? {
        val component = contextComponent(event) ?: return null
        return DataManager.getInstance().getDataContext(component).getData(CommonDataKeys.EDITOR)
    }

    private fun contextComponent(event: AnActionEvent): Component? =
        event.getData(PlatformDataKeys.CONTEXT_COMPONENT) ?: event.inputEvent?.component

    private fun isInsideTerminalToolWindow(event: AnActionEvent): Boolean {
        val project = event.project ?: return false
        val component = contextComponent(event) ?: return false
        val terminalWindow = ToolWindowManager.getInstance(project).getToolWindow("Terminal") ?: return false
        if (SwingUtilities.isDescendingFrom(component, terminalWindow.component)) {
            return true
        }
        var current: Component? = component
        while (current != null) {
            if (current.javaClass.name.contains("terminal", ignoreCase = true)) {
                return true
            }
            current = current.parent
        }
        return false
    }

    private fun selectedText(editor: Editor?): String? =
        editor?.selectionModel?.getSelectedText(true)?.takeIf { it.isNotEmpty() }

    private fun isTerminalEditor(editor: Editor): Boolean {
        val classNames = listOf(
            "org.jetbrains.plugins.terminal.util.TerminalDataContextUtils",
            "org.jetbrains.plugins.terminal.exp.TerminalDataContextUtils",
            "com.intellij.terminal.frontend.view.impl.TerminalDataContextUtils",
        )
        val methodNames = listOf(
            "isOutputModelEditor",
            "isPromptEditor",
            "isAlternateBufferEditor",
            "isReworkedTerminalEditor",
        )
        for (className in classNames) {
            val clazz = runCatching { Class.forName(className) }.getOrNull() ?: continue
            for (methodName in methodNames) {
                val method = clazz.methods.firstOrNull { candidate ->
                    candidate.name == methodName &&
                        candidate.parameterCount == 1 &&
                        candidate.returnType == java.lang.Boolean.TYPE
                } ?: continue
                if (runCatching { method.invoke(null, editor) as Boolean }.getOrNull() == true) {
                    return true
                }
            }
        }
        return false
    }

    private fun classicTerminalSelection(event: AnActionEvent): String? {
        for (context in dataContexts(event)) {
            context.getData(CLASSIC_SELECTED_TEXT_KEY)?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        val classNames = listOf(
            "com.intellij.terminal.JBTerminalWidget",
            "org.jetbrains.plugins.terminal.JBTerminalWidget",
        )
        for (className in classNames) {
            val clazz = runCatching { Class.forName(className) }.getOrNull() ?: continue
            val field = clazz.fields.firstOrNull { it.name.contains("SELECTED_TEXT") } ?: continue
            val key = runCatching { field.get(null) as? DataKey<*> }.getOrNull() ?: continue
            for (context in dataContexts(event)) {
                val value = context.getData(key) as? String
                if (!value.isNullOrEmpty()) {
                    return value
                }
            }
        }
        return null
    }
}
