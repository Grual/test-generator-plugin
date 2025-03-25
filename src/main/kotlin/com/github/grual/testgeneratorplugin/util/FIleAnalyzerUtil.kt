import com.github.grual.testgeneratorplugin.util.findAllRegexMatches
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

data class RESTMethodInfo(val method: String, val path: String, val pathParams: List<String>)

data class MethodInfo(
    val name: String,
    val returnType: String?,
    val annotations: List<String>,
    val parameters: List<Pair<String, String>>, // (parameter name, parameter type)
    val restMethodInfo: RESTMethodInfo
)

data class ClassInfo(
    val qualifiedName: String,
    val fields: List<Pair<String, String>>, // (field name, fully qualified field type)
    val methods: List<MethodInfo>,
    val imports: List<String>, // List of fully qualified imported classes
    val packageName: String,
    val requestMappingPath: String
)

fun analyzePsiFile(psiFile: PsiFile): ClassInfo {
    val imports = mutableListOf<String>()
    if (psiFile is PsiJavaFile) {
        imports.addAll(psiFile.importList?.allImportStatements?.mapNotNull { it.text } ?: emptyList())
    }

    val psiClass = PsiTreeUtil.findChildrenOfType(psiFile, PsiClass::class.java).first()
    val fields = psiClass.fields.map { it.name to it.type.presentableText }

    val methods = psiClass.methods.filter { it.hasModifierProperty(PsiModifier.PUBLIC) }
        .map { Pair(it, analyzeRESTMethod(it)) }
        .filter { it.second != null }
        .map { methodPair ->
        MethodInfo(
            name = methodPair.first.name,
            returnType = methodPair.first.returnType?.presentableText,
            annotations = methodPair.first.modifierList.annotations.mapNotNull { it.qualifiedName },
            parameters = methodPair.first.parameterList.parameters.map { it.name to it.type.presentableText },
            restMethodInfo = methodPair.second!!
        )
    }

    return ClassInfo(
        psiClass.qualifiedName!!,
        fields,
        methods,
        imports,
        (psiFile as PsiJavaFile).packageName,
        extractRequestMappingValue(psiClass)
            ?: throw IllegalArgumentException("class ${psiClass.qualifiedName!!} does not have a RequestMapping path!")
    )
}

// todo maybe make this conditional on whether or not we use spring or smth
fun analyzeRESTMethod(method: PsiMethod): RESTMethodInfo? {
    val annotation = method.getAnnotation("org.springframework.web.bind.annotation.GetMapping")
        ?: method.getAnnotation("org.springframework.web.bind.annotation.PostMapping")
        ?: method.getAnnotation("org.springframework.web.bind.annotation.PutMapping")
        ?: method.getAnnotation("org.springframework.web.bind.annotation.DeleteMapping")
        ?: return null

    val path = extractPathAttribute(annotation) ?: ""
    return RESTMethodInfo(
        annotation.text.replace(Regex("@|Mapping.*"), "").lowercase(),
        path,
        findAllRegexMatches(path, "\\{(\\w+)}")
    )
}

private fun extractRequestMappingValue(psiClass: PsiClass): String? {
    val annotation = psiClass.getAnnotation("org.springframework.web.bind.annotation.RequestMapping") ?: return null
    return extractValueAttribute(annotation)
}

private fun extractValueAttribute(annotation: PsiAnnotation): String? {
    return extractStringValue(annotation.findAttributeValue("value"))
}

private fun extractPathAttribute(annotation: PsiAnnotation): String? {
    return extractValueAttribute(annotation)
        ?: extractStringValue(annotation.findAttributeValue("path"))
}

private fun extractStringValue(expression: PsiAnnotationMemberValue?): String? {
    return when (expression) {
        is PsiLiteralExpression -> expression.value as? String
        is PsiArrayInitializerMemberValue -> expression.initializers.firstNotNullOfOrNull {
            (it as? PsiLiteralExpression)?.value as? String
        } // Handle multiple paths, but take the first one
        else -> null
    }
}