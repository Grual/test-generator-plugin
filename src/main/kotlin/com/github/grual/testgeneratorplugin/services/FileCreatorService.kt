package com.github.grual.testgeneratorplugin.services

import ai.grazie.utils.capitalize
import com.github.grual.testgeneratorplugin.MessagesBundle
import com.github.grual.testgeneratorplugin.components.settings.TestGeneratorSettings
import com.github.grual.testgeneratorplugin.components.settings.TestGeneratorState
import com.github.grual.testgeneratorplugin.`object`.Observer
import com.github.grual.testgeneratorplugin.util.ClassInfo
import com.github.grual.testgeneratorplugin.util.JUNIT_TEST_ANNOTATION
import com.github.grual.testgeneratorplugin.util.MOCK_MVC_DELETE
import com.github.grual.testgeneratorplugin.util.MOCK_MVC_GET
import com.github.grual.testgeneratorplugin.util.MOCK_MVC_POST
import com.github.grual.testgeneratorplugin.util.MOCK_MVC_PUT
import com.github.grual.testgeneratorplugin.util.MOCK_MVC_STATUS
import com.github.grual.testgeneratorplugin.util.MethodInfo
import com.github.grual.testgeneratorplugin.util.NOT_NULL_ANNOTATION
import com.github.grual.testgeneratorplugin.util.NO_VALUE_PARAMETER
import com.github.grual.testgeneratorplugin.util.ParameterAndFieldInfo
import com.github.grual.testgeneratorplugin.util.SETTER_ANNOTATION
import com.github.grual.testgeneratorplugin.util.SPRING_AUTOWIRED_ANNOTATION
import com.github.grual.testgeneratorplugin.util.SPRING_REQUEST_BODY_ANNOTATION
import com.github.grual.testgeneratorplugin.util.analyzePsiFile
import com.github.grual.testgeneratorplugin.util.appendWithBreak
import com.github.grual.testgeneratorplugin.util.camelToSnakeCase
import com.github.grual.testgeneratorplugin.util.decapitalize
import com.github.grual.testgeneratorplugin.util.hasBuilderMethod
import com.github.grual.testgeneratorplugin.util.prettyPrintMethodHead
import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiModifier
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import mu.KotlinLogging
import java.io.File

class FileCreatorService : Observer {
    private val log = KotlinLogging.logger {}
    private var settings: TestGeneratorState = TestGeneratorSettings.getInstance().state!!

    init {
        TestGeneratorSettings.addObserver(this)
    }

    fun createFile(project: Project, originalFile: VirtualFile) {
        val newFileName = originalFile.name.replace(".java", settings.testClassNameSuffix + ".java")
        val newFilePath = originalFile.path.replace("/main/", "/test/").replace(originalFile.name, newFileName)
        val newFile = File(newFilePath)
        newFile.parentFile.mkdirs()

        if (!askForOverride(project, newFile)) return

        val psiFile = getPsiFileForVirtualFile(project, originalFile)
            ?: throw IllegalStateException("Could not get psi file for virtual file ${originalFile.name}")
        val classInfo = analyzePsiFile(psiFile)

        val fixture = createFixture(
            project,
            classInfo,
            newFile.path.replace(settings.testClassNameSuffix + ".java", "Fixture.java")
        )
        createTestClass(project, classInfo, newFile, fixture)
    }

    /* ======================= TEST CLASS =======================*/

    private fun createTestClass(project: Project, classInfo: ClassInfo, newFile: File, fixture: File?) {
        val fileContent = StringBuilder()

        addPackageAndImports(fileContent, classInfo.packageName, classInfo.imports, fixture, project)
        addClassDeclarationAndMembers(
            fileContent,
            newFile.nameWithoutExtension,
            classInfo.requestMappingPath,
            classInfo.fields
        )
        addMethods(fileContent, classInfo.methods, fixture)
        finalizeFile(fileContent)

        newFile.writeText(fileContent.toString())

        reloadAndFormatFile(project, newFile)
    }

    private fun addPackageAndImports(
        fileContent: StringBuilder,
        packageName: String,
        imports: List<String>,
        fixture: File?,
        project: Project
    ) {
        fileContent.appendWithBreak("package ${packageName};")
        imports.forEach(fileContent::appendWithBreak)
        fileContent.appendWithBreak("import $JUNIT_TEST_ANNOTATION;")
        fileContent.appendWithBreak("import $SPRING_AUTOWIRED_ANNOTATION;")

        // Fixture imports
        val fixturePsiClass =
            PsiTreeUtil.findChildrenOfType(getPsiFileForFile(fixture, project), PsiClass::class.java).first()
        fixturePsiClass.methods.forEach {
            fileContent.appendWithBreak("import static ${fixturePsiClass.qualifiedName}.${it.name};")
        }

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

    private fun addMethods(fileContent: StringBuilder, methods: List<MethodInfo>, fixture: File?) {
        methods.forEach {
            fileContent.appendWithBreak("@Test")
            fileContent.appendWithBreak("public void ${it.name}() {")
            addMethodBody(fileContent, it, fixture)
            fileContent.appendWithBreak("}")
        }
    }

    private fun addMethodBody(fileContent: StringBuilder, method: MethodInfo, fixture: File?) {
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
        fileContent.appendWithBreak(
            if (httpMethod == "post" || httpMethod == "put") ".content(" +
                    addFixtureCall(restInfo.requestBodyType, fixture)
                    + ")"
            else null
        )
        fileContent.append(".andExpect(status().isOk())")
        fileContent.appendWithBreak(if (httpMethod == "get") "\n.andReturn().getResponse().getContentAsString();" else ";")
    }

    private fun addFixtureCall(requestBodyType: String?, fixture: File?): String {
        if (requestBodyType == null || fixture == null) {
            return NO_VALUE_PARAMETER
        }

        val fixtureLine = fixture.useLines { lines ->
            lines.find { it.contains("public static $requestBodyType") }
        } ?: return NO_VALUE_PARAMETER

        return fixtureLine.replace("public static $requestBodyType", "").replace(Regex("[ {]"), "")
    }

    private fun generatePathExpression(path: String, pathParams: List<String>): String {
        var result = path
        for (param in pathParams) {
            result = result.replace("{$param}", "\" + ${param.camelToSnakeCase().uppercase()} + \"")
        }
        return "\"$result\"".replace(" + \"\"", "").replace("\"\" + ", "")
    }

    /* ======================= FIXTURE =======================*/

    private fun createFixture(project: Project, classInfo: ClassInfo, path: String): File? {
        if (!settings.generateFixture) return null

        val requiredFixtureTypes = getFixtureTypes(classInfo)

        if (requiredFixtureTypes.isEmpty()) return null

        val requiredFixtureTypesWithClass =
            requiredFixtureTypes.map { Pair(it, getPsiClassFromClassName(project, it.fullyQualifiedName)) }

        val fixtureFile = File(path)
        if (!askForOverride(project, fixtureFile)) return null

        val fileContent = StringBuilder()
        addFixtureImportsAndClassDef(
            fileContent,
            classInfo.packageName,
            fixtureFile.nameWithoutExtension,
            classInfo.imports
        )

        requiredFixtureTypesWithClass.flatMap { getStaticFinalFields(it.first, it.second.fields) }
            .sorted()
            .forEach(fileContent::appendWithBreak)

        requiredFixtureTypesWithClass.forEach {
            createFixtureMethod(fileContent, it.first, it.second)
        }

        finalizeFile(fileContent)

        fixtureFile.writeText(fileContent.toString())

        reloadAndFormatFile(project, fixtureFile)

        return fixtureFile
    }

    // TODO scan the class the fixture is of for base classes and include their fields in the instantiation
    private fun createFixtureMethod(fileContent: StringBuilder, fieldInfo: ParameterAndFieldInfo, javaClass: PsiClass) {
        fileContent.appendWithBreak("public static ${fieldInfo.type} ${fieldInfo.type.decapitalize()}() {")
        if (settings.fixturesUseBuilder && javaClass.hasBuilderMethod()) {
            log.debug { "[Fixture ${javaClass.name}]: using builder to instantiate fixture" }
            fileContent.appendWithBreak("return ${fieldInfo.type}.builder()")
            javaClass.fields.forEach {
                fileContent.appendWithBreak(".${it.name}(${generateTestPropertyName(fieldInfo.type, it.name)})")
            }
            fileContent.appendWithBreak(".build();")
        } else {
            log.debug { "[Fixture ${javaClass.name}]: using constructor to instantiate fixture" }
            if (javaClass.hasModifierProperty(PsiModifier.ABSTRACT)) {
                log.debug { "[Fixture ${javaClass.name}]: trying to instantiate abstract class; returning null" }
                fileContent.appendWithBreak("null; // TODO provide value \n}")
                return
            }

            val constructors = javaClass.constructors
            // check if the class has no constructors or an explicitly defined non-private no-args constructor
            if (constructors.isEmpty()
                || constructors.any { it.parameterList.parameters.isEmpty() && !it.hasModifierProperty(PsiModifier.PRIVATE) }
            ) {
                log.debug {
                    "[Fixture ${javaClass.name}]: class '${javaClass.name}' has no explicit non-empty constructor or has an explicitly defined " +
                            "non-private no-args constructor. Using no-args constructor for instantiation"
                }
                fileContent.appendWithBreak("${fieldInfo.type} obj = new ${fieldInfo.type}();")
                val setterFields = javaClass.fields.filter {
                    javaClass.findMethodsByName("set${it.name.capitalize()}", true).isNotEmpty()
                            || (settings.usesLombok && it.hasAnnotation(SETTER_ANNOTATION)) // TODO add checking for setter annotation on class
                }
                setterFields.forEach {
                    fileContent.append("obj.set${it.name.capitalize()}(")
                    fileContent.appendWithBreak("${generateTestPropertyName(fieldInfo.type, it.name)});")
                }
                fileContent.appendWithBreak("return obj;")
            } else {
                constructors.sortByDescending { it.parameterList.parameters.size }
                val chosenCtor = constructors.first()
                log.debug {
                    "[Fixture ${javaClass.name}]: using the following constructor for instantiation: ${
                        prettyPrintMethodHead(
                            chosenCtor
                        )
                    }"
                }
                fileContent.appendWithBreak("return new ${fieldInfo.type}(")
                chosenCtor.parameterList.parameters.forEach {
                    fileContent.appendWithBreak("${generateTestPropertyName(fieldInfo.type, it.name)},")
                }
                fileContent.delete(fileContent.length - 2, fileContent.length) // remove last linebreak an comma
                fileContent.appendWithBreak(");")
            }
        }
        fileContent.appendWithBreak("}")
    }

    private fun getStaticFinalFields(
        fieldInfo: ParameterAndFieldInfo,
        fields: Array<PsiField>
    ): List<String> {
        val relevantFields =
            if (settings.fixturesUseOnlyNotNullFields) fields.filter { it.hasAnnotation(NOT_NULL_ANNOTATION) }.toList()
            else fields.toList()
        val staticFinalFields = ArrayList<String>()
        relevantFields.forEach {
            val sb = StringBuilder()
            sb.append("public static final ${it.type.presentableText} ")
            sb.append(generateTestPropertyName(fieldInfo.type, it.name))
            sb.append(" = ${generateTestValueType(it.type.presentableText)};")
            staticFinalFields.add(sb.toString())
        }
        return staticFinalFields
    }

    private fun getFixtureTypes(classInfo: ClassInfo): List<ParameterAndFieldInfo> {
        return classInfo.methods.flatMap {
            it.parameters
                .filter { param ->
                    param.annotations.contains(SPRING_REQUEST_BODY_ANNOTATION)
                            && param.type.contains("DTO", false)
                }
        }.distinctBy { it.fullyQualifiedName }
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

    private fun generateTestValueType(type: String): String {
        return when {
            type == "Integer" || type == "int" -> "42"
            type == "Long" || type == "long" -> "69L"
            type == "Boolean" || type == "boolean" -> "false"
            type == "String" -> "\"this is a test string\""
            type == "BigDecimal" -> "BigDecimal.valueOf(1.23)"
            type == "UUID" -> "UUID.randomUUID()"
            type.startsWith("List<", false) -> "List.of()"
            type.endsWith("DTO") -> "null /* TODO add value */"
            else -> {
                log.debug { "No default value defined for type '$type', using `null`" }
                return "null"
            }
        }
    }

    private fun generateTestPropertyName(type: String, originalName: String): String {
        return "${type}_${originalName.camelToSnakeCase().uppercase()}_TEST"// TODO maybe make _TEST suffix configurable
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

    private fun getPsiClassFromClassName(project: Project, fullyQualifiedName: String): PsiClass {
        val psiFacade = JavaPsiFacade.getInstance(project)
        val scope = GlobalSearchScope.allScope(project)
        return psiFacade.findClass(fullyQualifiedName, scope)
            ?: throw IllegalArgumentException("No java class found for classname '${fullyQualifiedName}'")
    }

    private fun getPsiFileForFile(file: File?, project: Project): PsiFile? {
        val virtualFile = file?.let { LocalFileSystem.getInstance().findFileByIoFile(it) }
        return virtualFile?.let { PsiManager.getInstance(project).findFile(it) }
    }

    /* ======================= OTHER =======================*/

    override fun update() {
        settings = TestGeneratorSettings.getInstance().state!!
    }
}
