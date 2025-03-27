package com.github.grual.testgeneratorplugin.actions

import com.github.grual.testgeneratorplugin.MessagesBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup

class TestGeneratorActionGroup : DefaultActionGroup() {
    override fun update(e: AnActionEvent) {
        e.presentation.text = MessagesBundle.message("action.actionGroup.title")
        e.presentation.isEnabledAndVisible = true
        e.presentation.description = MessagesBundle.message("action.actionGroup.desc")
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}