package com.github.grual.testgeneratorplugin.actions

import com.github.grual.testgeneratorplugin.MessagesBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile

class GenerateTestActionInEditorPopup : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val virtualFile: VirtualFile = e.getData(CommonDataKeys.PSI_FILE)?.virtualFile ?: return

        Messages.showMessageDialog(project,
                "You right-clicked in the file: ${virtualFile.name}",
                "Custom Action",
                Messages.getInformationIcon()
        )
    }

    override fun update(e: AnActionEvent) {
        val virtualFile: VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = virtualFile != null && !virtualFile.isDirectory
        e.presentation.text = MessagesBundle.message("action.title")
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}