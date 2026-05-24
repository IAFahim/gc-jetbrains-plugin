package com.github.iafahim.gccopy

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.util.concurrent.TimeUnit

class GcCopyAction : AnAction() {

    private val notificationGroupId = "gc.notifications"
    private val pathPrefix = System.getProperty("user.home").let { home ->
        "$home/.local/bin:$home/bin:/usr/local/bin"
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        // Bulletproof: Always show the action, but only enable if we can find a context
        event.presentation.isVisible = true
        
        val directory = resolveTargetDirectory(event)
        event.presentation.isEnabled = true // Always allow it to be clicked
        
        if (directory != null) {
            event.presentation.text = "Copy with gc (${directory.name})"
        } else {
            event.presentation.text = "Copy with gc"
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        val directory = resolveTargetDirectory(event) ?: return
        val invocation = GcSettings.getInstance().command

        object : Task.Backgroundable(project, "Running gc context copy", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = "Executing gc in ${directory.absolutePath}..."
                
                val result = executeGc(directory, invocation)
                
                ApplicationManager.getApplication().invokeLater {
                    showNotification(project, result)
                }
            }
        }.queue()
    }

    private fun resolveTargetDirectory(event: AnActionEvent): File? {
        // 1. Try selected files/folders
        val virtualFiles = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (!virtualFiles.isNullOrEmpty()) {
            val first = virtualFiles[0]
            val folder = if (first.isDirectory) first else first.parent
            if (folder != null) return File(folder.path)
        }

        // 2. Try the file open in the editor
        val editorFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
        if (editorFile != null) {
            val folder = if (editorFile.isDirectory) editorFile else editorFile.parent
            if (folder != null) return File(folder.path)
        }

        // 3. Absolute fallback: Project root
        val project = event.project
        if (project != null && project.basePath != null) {
            return File(project.basePath!!)
        }

        return null
    }

    private fun executeGc(directory: File, invocation: String): GcResult {
        return try {
            val command = "export PATH=\"$pathPrefix:\$PATH\"; $invocation"
            val process = ProcessBuilder("bash", "-lc", command)
                .directory(directory)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText().trim()
            val finished = process.waitFor(60, TimeUnit.SECONDS)
            
            if (!finished) {
                process.destroyForcibly()
                GcResult(false, directory, "Process timed out")
            } else if (process.exitValue() == 0) {
                GcResult(true, directory, "Successfully copied to clipboard")
            } else {
                GcResult(false, directory, output.ifBlank { "Exit code ${process.exitValue()}" })
            }
        } catch (e: Exception) {
            GcResult(false, directory, e.message ?: "Unknown execution error")
        }
    }

    private fun showNotification(project: Project?, result: GcResult) {
        val group = NotificationGroupManager.getInstance().getNotificationGroup(notificationGroupId)
        val type = if (result.success) NotificationType.INFORMATION else NotificationType.ERROR
        val title = if (result.success) "gc Copy Success" else "gc Copy Failed"
        
        group.createNotification(title, result.message, type).notify(project)
    }

    private data class GcResult(val success: Boolean, val directory: File, val message: String)
}
