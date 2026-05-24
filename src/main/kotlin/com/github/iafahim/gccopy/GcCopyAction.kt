package com.github.iafahim.gccopy

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.util.concurrent.TimeUnit

class GcCopyAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        event.presentation.isVisible = true
        event.presentation.isEnabled = targetDirectory(event) != null
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        val directory = targetDirectory(event) ?: return
        val command = resolveCommand()
        object : Task.Backgroundable(project, "Running gc in ${directory.name}", true) {
            private var outcome: Outcome = Outcome.Failure(directory, "did not run")

            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                outcome = execute(command, directory)
            }

            override fun onSuccess() {
                announce(project, outcome)
            }
        }.queue()
    }

    private fun resolveCommand(): String =
        runCatching { GcSettings.getInstance().command }.getOrDefault(GcSettings.DEFAULT_COMMAND)

    private fun targetDirectory(event: AnActionEvent): File? {
        val selected: VirtualFile? =
            event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.firstOrNull()
                ?: event.getData(CommonDataKeys.VIRTUAL_FILE)
        val anchor = selected ?: return projectBaseDirectory(event)
        val folder = if (anchor.isDirectory) anchor else anchor.parent ?: return null
        return File(folder.path)
    }

    private fun projectBaseDirectory(event: AnActionEvent): File? =
        event.project?.basePath?.let(::File)

    private fun execute(command: String, directory: File): Outcome {
        val home = System.getProperty("user.home")
        val pathPrefix = "$home/.local/bin:$home/bin:/usr/local/bin"
        val script = "export PATH=\"$pathPrefix:\$PATH\"; $command"
        val process = ProcessBuilder("bash", "-lc", script)
            .directory(directory)
            .redirectErrorStream(true)
            .start()
        val transcript = process.inputStream.bufferedReader().readText().trim()
        val finished = process.waitFor(120, TimeUnit.SECONDS)
        return when {
            !finished -> {
                process.destroyForcibly()
                Outcome.Failure(directory, "command timed out after 120s")
            }
            process.exitValue() == 0 -> Outcome.Success(directory)
            else -> Outcome.Failure(directory, transcript.ifBlank { "exit code ${process.exitValue()}" })
        }
    }

    private fun announce(project: Project?, outcome: Outcome) {
        val group = NotificationGroupManager.getInstance().getNotificationGroup("gc.notifications")
        val notification = when (outcome) {
            is Outcome.Success -> group.createNotification(
                "Copied ${outcome.directory.name} to clipboard",
                NotificationType.INFORMATION,
            )
            is Outcome.Failure -> group.createNotification(
                "gc failed in ${outcome.directory.name}",
                outcome.detail,
                NotificationType.ERROR,
            )
        }
        notification.notify(project)
    }

    private sealed interface Outcome {
        data class Success(val directory: File) : Outcome
        data class Failure(val directory: File, val detail: String) : Outcome
    }
}
