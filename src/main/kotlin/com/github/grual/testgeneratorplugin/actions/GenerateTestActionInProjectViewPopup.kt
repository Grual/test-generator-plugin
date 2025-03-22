package com.github.grual.testgeneratorplugin.actions

import com.github.grual.testgeneratorplugin.components.settings.TestGeneratorSettings
import com.github.grual.testgeneratorplugin.components.settings.TestGeneratorState
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile

class GenerateTestActionInProjectViewPopup : AnAction() {
    private val settings: TestGeneratorState? = TestGeneratorSettings.getInstance().state

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return

        val fileFromExplorer: VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)
        if (fileFromExplorer != null) {
            Messages.showMessageDialog(project,
                    "You right-clicked on the file: ${fileFromExplorer.name}",
                    "Custom Action",
                    Messages.getInformationIcon()
            )
        }
    }

    override fun update(e: AnActionEvent) {
        val virtualFile: VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)

        e.presentation.isEnabledAndVisible = virtualFile != null && !virtualFile.isDirectory
                && settings?.allowActionForFilesEndingIn?.any { virtualFile.name.endsWith(it) } == true
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}