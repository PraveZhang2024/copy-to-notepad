package com.github.pravezhang2024.copytonotepad.copyout

import com.github.pravezhang2024.copytonotepad.MyBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@Service(Service.Level.APP)
class CopyOutService {
    @Volatile
    private var cachedLauncher: Path? = null

    fun ensureLauncher() {
        if (!SystemInfo.isWindows) {
            return
        }
        extractLauncher()
    }

    fun export(text: String, project: Project?) {
        try {
            if (!SystemInfo.isWindows) {
                CopyOutNotifier.warn(project, MyBundle.message("copyOut.windowsOnly"))
                return
            }
            val propertiesFile = writeTempText(text)
            val exe = extractLauncher()
            ProcessBuilder(exe.toAbsolutePath().toString(), propertiesFile.toAbsolutePath().toString())
                .directory(exe.parent.toFile())
                .start()
        } catch (error: MissingLauncherException) {
            thisLogger().warn("CopyOut failed", error)
            CopyOutNotifier.warn(project, MyBundle.message("copyOut.missingLauncher"))
        } catch (error: Exception) {
            thisLogger().warn("CopyOut failed", error)
            CopyOutNotifier.warn(project, MyBundle.message("copyOut.launchFailed"))
        }
    }

    private fun writeTempText(text: String): Path {
        return try {
            val file = Files.createTempFile("copyout-", ".properties")
            Files.writeString(file, text, StandardCharsets.UTF_8)
            file
        } catch (error: Exception) {
            throw IllegalStateException(MyBundle.message("copyOut.writeFailed"), error)
        }
    }

    private fun extractLauncher(): Path {
        cachedLauncher?.takeIf { Files.isRegularFile(it) }?.let { return it }
        synchronized(this) {
            cachedLauncher?.takeIf { Files.isRegularFile(it) }?.let { return it }
            val dir = Path.of(System.getProperty("java.io.tmpdir"), "copy-to-notepad")
            Files.createDirectories(dir)
            val exe = dir.resolve("VBCopyO.exe")
            val resourceUrl = javaClass.classLoader.getResource(LAUNCHER_RESOURCE)
                ?: throw MissingLauncherException()
            val resourceSize = runCatching { resourceUrl.openConnection().contentLengthLong }.getOrDefault(-1L)
            if (Files.isRegularFile(exe) && resourceSize > 0 && Files.size(exe) == resourceSize) {
                cachedLauncher = exe
                return exe
            }
            resourceUrl.openStream().use { input ->
                Files.copy(input, exe, StandardCopyOption.REPLACE_EXISTING)
            }
            cachedLauncher = exe
            return exe
        }
    }

    companion object {
        const val LAUNCHER_RESOURCE = "VBCopyO.exe"

        fun getInstance(): CopyOutService =
            ApplicationManager.getApplication().getService(CopyOutService::class.java)
    }
}

private class MissingLauncherException :
    IllegalStateException("VBCopyO.exe is missing from plugin resources: src/main/resources/VBCopyO.exe")
