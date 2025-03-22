package com.github.grual.testgeneratorplugin.services

import com.github.grual.testgeneratorplugin.MessagesBundle
import com.github.grual.testgeneratorplugin.components.settings.TestGeneratorSettings
import com.github.grual.testgeneratorplugin.components.settings.TestGeneratorState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

class FileCreatorService {
    private val settings: TestGeneratorState = TestGeneratorSettings.getInstance().state!!

    fun createFile(project: Project, originalFile: VirtualFile) {
        val newFileName = originalFile.name.replace(".java", settings.testClassNameSuffix + ".java")
        var newFilePath = originalFile.path.replace("/" + settings.mainPackageBaseName + "/", "/" + settings.testPackageBaseName + "/")
        newFilePath = newFilePath.replace(originalFile.name, newFileName)

        val newFile = File(newFilePath)
        newFile.parentFile.mkdirs()

        if (!newFile.createNewFile()) {
            val userChoice = Messages.showOkCancelDialog(project, MessagesBundle.getMessage("fileExistsDialog.message", newFile.path),
                    MessagesBundle.getMessage("fileExistsDialog.title"),
                    MessagesBundle.getMessage("common.override"),
                    MessagesBundle.getMessage("common.cancel"),
                    Messages.getWarningIcon())
            if (userChoice == Messages.CANCEL) return
        }

        createFileContent(originalFile, newFile)
    }

    private fun createFileContent(virtualFile: VirtualFile, newFile: File) {
        // TODO "finish this"
        newFile.writeText("i created this")
        reloadFile(newFile)
    }

    private fun reloadFile(file: File) {
        ApplicationManager.getApplication().invokeLater {
            val virtualFile: VirtualFile? = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
            virtualFile?.refresh(false, false)
        }
    }
}
