package com.github.grual.testgeneratorplugin.services

import analyzePsiFile
import com.github.grual.testgeneratorplugin.MessagesBundle
import com.github.grual.testgeneratorplugin.components.settings.TestGeneratorSettings
import com.github.grual.testgeneratorplugin.components.settings.TestGeneratorState
import com.github.grual.testgeneratorplugin.util.appendWithBreak
import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
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

        createFileContent(project, originalFile, newFile)
    }

    private fun createFileContent(project: Project, virtualFile: VirtualFile, newFile: File) {
        val psiFile = getPsiFileForVirtualFile(project, virtualFile) ?: return

        val classInfo = analyzePsiFile(psiFile)

        val fileContent = StringBuilder()

        fileContent.appendWithBreak("package ${classInfo.packageName};")
        classInfo.imports.forEach(fileContent::appendWithBreak)
        fileContent.appendWithBreak("import org.junit.jupiter.api.Test;")
        fileContent.appendWithBreak("import org.springframework.beans.factory.annotation.Autowired;")

        fileContent.appendWithBreak("public class ${newFile.nameWithoutExtension} { ")
        classInfo.fields.forEach {
            fileContent.appendWithBreak("@Autowired")
            fileContent.appendWithBreak("private final ${it.second} ${it.first};")
        }

        classInfo.methods.forEach {
            fileContent.appendWithBreak("@Test")
            fileContent.append("public ${it.returnType} ${it.name}(")
            it.parameters.forEachIndexed { index, parameter ->
                fileContent.append("${parameter.second} ${parameter.first}")
                fileContent.append(if (index != it.parameters.size - 1) ", " else null)
            }
            fileContent.appendWithBreak(") {")
            fileContent.appendWithBreak(if (it.returnType?.lowercase().equals("void")) null else "return null;")
            fileContent.appendWithBreak("}")
        }

        fileContent.appendWithBreak("}")
        newFile.writeText(fileContent.toString())

        reloadAndFormatFile(project, newFile)
    }

    private fun reloadAndFormatFile(project: Project, file: File) {
        ApplicationManager.getApplication().invokeLater {
            val virtualFile: VirtualFile =
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file) ?: return@invokeLater
            virtualFile.refresh(false, false)
            formatAndOptimizeImports(project, virtualFile)
        }
    }

    private fun formatAndOptimizeImports(project: Project, file: VirtualFile) {
        val psiFile = getPsiFileForVirtualFile(project, file) ?: return
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: return
        PsiDocumentManager.getInstance(project).commitDocument(document)
        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(psiFile)
            OptimizeImportsProcessor(project, psiFile).run()
        }
    }

    private fun getPsiFileForVirtualFile(project: Project, virtualFile: VirtualFile): PsiFile? {
        return PsiManager.getInstance(project).findFile(virtualFile)
    }
}
