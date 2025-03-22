package com.github.grual.testgeneratorplugin.actions

import com.github.grual.testgeneratorplugin.MessagesBundle
import com.github.grual.testgeneratorplugin.components.settings.TestGeneratorSettings
import com.github.grual.testgeneratorplugin.components.settings.TestGeneratorState
import com.github.grual.testgeneratorplugin.services.FileCreatorService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class GenerateTestAction : AnAction() {
    private val settings: TestGeneratorState = TestGeneratorSettings.getInstance().state!!
    private val fileCreatorService = FileCreatorService()
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val fileFromExplorer: VirtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        fileCreatorService.createFile(project, fileFromExplorer)
    }

    override fun update(e: AnActionEvent) {
        val virtualFile: VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)

        e.presentation.isEnabledAndVisible = virtualFile != null && !virtualFile.isDirectory
                && settings.allowActionForFilesEndingIn.any { virtualFile.name.endsWith(it) } == true
        e.presentation.text = MessagesBundle.message("action.title")
        println(virtualFile?.name)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}