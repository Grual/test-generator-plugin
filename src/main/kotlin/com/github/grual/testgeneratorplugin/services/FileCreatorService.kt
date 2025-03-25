package com.github.grual.testgeneratorplugin.services

import MethodInfo
import analyzePsiFile
import com.github.grual.testgeneratorplugin.MessagesBundle
import com.github.grual.testgeneratorplugin.components.settings.TestGeneratorSettings
import com.github.grual.testgeneratorplugin.components.settings.TestGeneratorState
import com.github.grual.testgeneratorplugin.util.appendWithBreak
import com.github.grual.testgeneratorplugin.util.camelToSnakeCase
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

        addPackageAndImports(fileContent, classInfo.packageName, classInfo.imports)
        addClassDeclarationAndMembers(
            fileContent,
            newFile.nameWithoutExtension,
            classInfo.requestMappingPath,
            classInfo.fields
        )
        addMethods(fileContent, classInfo.methods)
        finalizeFile(fileContent)

        newFile.writeText(fileContent.toString())

        reloadAndFormatFile(project, newFile)
    }

    private fun addPackageAndImports(fileContent: StringBuilder, packageName: String, imports: List<String>) {
        fileContent.appendWithBreak("package ${packageName};")
        imports.forEach(fileContent::appendWithBreak)
        fileContent.appendWithBreak("import org.junit.jupiter.api.Test;")
        fileContent.appendWithBreak("import org.springframework.beans.factory.annotation.Autowired;")

        // TODO add import for base test class

        if (settings.useMockVc) {
            fileContent.appendWithBreak("import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;")
            fileContent.appendWithBreak("import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;")
            fileContent.appendWithBreak("import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;")
            fileContent.appendWithBreak("import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;")
            fileContent.appendWithBreak("import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;")
        }
    }

    private fun addClassDeclarationAndMembers(
        fileContent: StringBuilder, className: String,
        requestMappingPath: String, fields: List<Pair<String, String>>
    ) {
        fileContent.append("public class $className ")
        fileContent.appendWithBreak(if (settings.baseTestClass != null) "extends ${settings.baseTestClass} {" else "{")
        fileContent.appendWithBreak(if (settings.useMockVc) "private static final String BASE_PATH = \"${requestMappingPath}\";" else null)
        // TODO maybe add mockMvc as mock or whatever in this class if not in base class
        fields.forEach { // TODO implement check for mock and autowire
            fileContent.appendWithBreak("@Autowired")
            fileContent.appendWithBreak("private final ${it.second} ${it.first};")
        }
    }

    private fun addMethods(fileContent: StringBuilder, methods: List<MethodInfo>) {
        methods.forEach {
            fileContent.appendWithBreak("@Test")
            fileContent.appendWithBreak("public void ${it.name}() {")
            addMethodBody(fileContent, it)
            fileContent.appendWithBreak("}")
        }
    }

    private fun addMethodBody(fileContent: StringBuilder, method: MethodInfo) {
        if (!settings.useMockVc) {
            fileContent.appendWithBreak("\t\t// arrange\n")
            fileContent.appendWithBreak("\t\t// act\n")
            fileContent.appendWithBreak("\t\t// assert\n")

            return
        }

        val restInfo = method.restMethodInfo
        val pathParams = restInfo.pathParams.map { it.replace(Regex("[{}]"), "") }
        val httpMethod = restInfo.method
        val interpolatedPath = "BASE_PATH + ${generatePathExpression(restInfo.path, pathParams)}".replace("+ \"\"", "")

        fileContent.appendWithBreak("\t\t// arrange\n")

        pathParams.forEach {
            fileContent.appendWithBreak(
                "final var ${it.camelToSnakeCase().uppercase()} = null; // TODO set value or delete"
            )
        }

        fileContent.appendWithBreak("\n\t\t// act\n")
        fileContent.appendWithBreak(if (pathParams.isNotEmpty()) "" else null)
        fileContent.append(if (httpMethod == "get") "String contentAsString = " else "")
        fileContent.append("mockMvc.perform(${httpMethod}(")
        fileContent.append(interpolatedPath)
        fileContent.appendWithBreak("))")
        fileContent.appendWithBreak(if (httpMethod == "post" || httpMethod == "put") ".contentType(MediaType.APPLICATION_JSON)" else null)
        fileContent.appendWithBreak(if (httpMethod == "post" || httpMethod == "put") ".content(/* TODO provide value */)" else null)
        fileContent.append(".andExpect(status().isOk())")
        fileContent.appendWithBreak(if (httpMethod == "get") "\n.andReturn().getResponse().getContentAsString();" else ";")

        fileContent.appendWithBreak("\n\t\t// assert\n")
    }

    private fun finalizeFile(fileContent: StringBuilder) {
        fileContent.appendWithBreak("}")
    }

    private fun generatePathExpression(path: String, pathParams: List<String>): String {
        var result = path
        for (param in pathParams) {
            result = result.replace("{$param}", "\" + ${param.camelToSnakeCase().uppercase()} + \"")
        }
        return "\"$result\"".replace(" + \"\"", "").replace("\"\" + ", "")
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
