package com.github.grual.testgeneratorplugin.actions

import com.github.grual.testgeneratorplugin.MessagesBundle
import com.github.grual.testgeneratorplugin.components.settings.SettingsDialog
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ShowSettingsDialogAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        SettingsDialog().show()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.text = MessagesBundle.message("action.showSettings.title")
        e.presentation.description = MessagesBundle.message("action.showSettings.desc")
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}