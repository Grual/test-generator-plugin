package com.github.grual.testgeneratorplugin.services

import ClassInfo
import MethodInfo
import ParameterAndFieldInfo
import analyzePsiFile
import com.github.grual.testgeneratorplugin.MessagesBundle
import com.github.grual.testgeneratorplugin.components.settings.TestGeneratorSettings
import com.github.grual.testgeneratorplugin.components.settings.TestGeneratorState
import com.github.grual.testgeneratorplugin.`object`.Observer
import com.github.grual.testgeneratorplugin.util.JUNIT_TEST_ANNOTATION
import com.github.grual.testgeneratorplugin.util.MOCK_MVC_DELETE
import com.github.grual.testgeneratorplugin.util.MOCK_MVC_GET
import com.github.grual.testgeneratorplugin.util.MOCK_MVC_POST
import com.github.grual.testgeneratorplugin.util.MOCK_MVC_PUT
import com.github.grual.testgeneratorplugin.util.MOCK_MVC_STATUS
import com.github.grual.testgeneratorplugin.util.SPRING_AUTOWIRED_ANNOTATION
import com.github.grual.testgeneratorplugin.util.SPRING_REQUEST_BODY_ANNOTATION
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

class FileCreatorService : Observer {
    private var settings: TestGeneratorState = TestGeneratorSettings.getInstance().state!!

    init {
        TestGeneratorSettings.addObserver(this)
    }

    fun createFile(project: Project, originalFile: VirtualFile) {
        val newFileName = originalFile.name.replace(".java", settings.testClassNameSuffix + ".java")
        var newFilePath = originalFile.path.replace("/main/", "/test/")
        newFilePath = newFilePath.replace(originalFile.name, newFileName)

        val newFile = File(newFilePath)
        newFile.parentFile.mkdirs()

        if (!askForOverride(project, newFile)) return

        val psiFile = getPsiFileForVirtualFile(project, originalFile)
            ?: throw IllegalStateException("Could not get psi file for virtual file ${originalFile.name}")
        val classInfo = analyzePsiFile(psiFile)

        createFileContent(project, classInfo, newFile)
        createFixture(project, classInfo, newFile.path.replace(settings.testClassNameSuffix + ".java", "Fixture.java"))
    }

    /* ======================= TEST CLASS =======================*/

    private fun createFileContent(project: Project, classInfo: ClassInfo, newFile: File) {
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
        fileContent.appendWithBreak("import $JUNIT_TEST_ANNOTATION;")
        fileContent.appendWithBreak("import $SPRING_AUTOWIRED_ANNOTATION;")

        // TODO add import for base test class

        if (settings.useMockVc) {
            fileContent.appendWithBreak("import static $MOCK_MVC_GET;")
            fileContent.appendWithBreak("import static $MOCK_MVC_POST;")
            fileContent.appendWithBreak("import static $MOCK_MVC_DELETE;")
            fileContent.appendWithBreak("import static $MOCK_MVC_PUT;")
            fileContent.appendWithBreak("import static $MOCK_MVC_STATUS;")
        }
    }

    private fun addClassDeclarationAndMembers(
        fileContent: StringBuilder, className: String,
        requestMappingPath: String, fields: List<ParameterAndFieldInfo>
    ) {
        fileContent.append("public class $className ")
        fileContent.appendWithBreak(if (settings.baseTestClass != null) "extends ${settings.baseTestClass} {" else "{")
        fileContent.appendWithBreak(if (settings.useMockVc) "private static final String BASE_PATH = \"${requestMappingPath}\";" else null)
        // TODO maybe add mockMvc as mock or whatever in this class if not in base class
        fields.forEach { // TODO implement check for mock and autowire
            fileContent.appendWithBreak("@Autowired")
            fileContent.appendWithBreak("private final ${it.type} ${it.name};")
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
        if (!settings.useMockVc) return

        val restInfo = method.restMethodInfo
        val pathParams = restInfo.pathParams.map { it.replace(Regex("[{}]"), "") }
        val httpMethod = restInfo.method
        val interpolatedPath = "BASE_PATH + ${generatePathExpression(restInfo.path, pathParams)}".replace("+ \"\"", "")

        pathParams.forEach {
            fileContent.appendWithBreak(
                "final var ${it.camelToSnakeCase().uppercase()} = null; // TODO set value or delete"
            )
        }

        fileContent.appendWithBreak(if (pathParams.isNotEmpty()) "" else null)
        fileContent.append(if (httpMethod == "get") "String contentAsString = " else "")
        fileContent.append("mockMvc.perform(${httpMethod}(")
        fileContent.append(interpolatedPath)
        fileContent.appendWithBreak("))")
        fileContent.appendWithBreak(if (httpMethod == "post" || httpMethod == "put") ".contentType(MediaType.APPLICATION_JSON)" else null)
        fileContent.appendWithBreak(if (httpMethod == "post" || httpMethod == "put") ".content(/* TODO provide value */)" else null)
        fileContent.append(".andExpect(status().isOk())")
        fileContent.appendWithBreak(if (httpMethod == "get") "\n.andReturn().getResponse().getContentAsString();" else ";")
    }

    private fun generatePathExpression(path: String, pathParams: List<String>): String {
        var result = path
        for (param in pathParams) {
            result = result.replace("{$param}", "\" + ${param.camelToSnakeCase().uppercase()} + \"")
        }
        return "\"$result\"".replace(" + \"\"", "").replace("\"\" + ", "")
    }

    /* ======================= FIXTURE =======================*/

    private fun createFixture(project: Project, classInfo: ClassInfo, path: String) {
        if (!settings.generateFixture) return

        val requiredFixtureTypes = getFixtureTypes(classInfo)

        if (requiredFixtureTypes.isEmpty()) return

        val fixtureFile = File(path)
        if (!askForOverride(project, fixtureFile)) return

        val fileContent = StringBuilder()
        addFixtureImportsAndClassDef(
            fileContent,
            classInfo.packageName,
            fixtureFile.nameWithoutExtension,
            classInfo.imports
        )

        // TODO fill fixture class

        finalizeFile(fileContent)

        fixtureFile.writeText(fileContent.toString())

        reloadAndFormatFile(project, fixtureFile)
    }

    private fun getFixtureTypes(classInfo: ClassInfo): List<String> {
        return classInfo.methods.flatMap {
            it.parameters
                .filter { param -> param.annotations.contains(SPRING_REQUEST_BODY_ANNOTATION) }
                .map { param -> param.type }
        }.toList()
    }

    private fun addFixtureImportsAndClassDef(
        fileContent: StringBuilder,
        packageName: String,
        className: String,
        imports: List<String>
    ) {
        fileContent.appendWithBreak("package ${packageName};")
        imports.forEach(fileContent::appendWithBreak)
        fileContent.appendWithBreak("public abstract class $className {")
    }

    /* ======================= GENERAL =======================*/

    private fun finalizeFile(fileContent: StringBuilder) {
        fileContent.appendWithBreak("}")
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

    /**
     * Checks if a given file exists. If it does a confirmation dialog is shown asking the user if the file
     * should be overridden or not.
     *
     * @param project the current [Project]
     * @param file the file whose existence should be checked
     * @return `true` if the file does not exist or the user allows the override; `false` if the file
     * exists and the user does not allow the override
     */
    private fun askForOverride(project: Project, file: File): Boolean {
        if (!file.createNewFile()) {
            val userChoice = Messages.showOkCancelDialog(
                project, MessagesBundle.getMessage("fileExistsDialog.message", file.path),
                MessagesBundle.getMessage("fileExistsDialog.title"),
                MessagesBundle.getMessage("common.override"),
                MessagesBundle.getMessage("common.cancel"),
                Messages.getWarningIcon()
            )
            return userChoice == Messages.OK
        }
        return true
    }

    /* ======================= OTHER =======================*/

    override fun update() {
        settings = TestGeneratorSettings.getInstance().state!!
    }
}
