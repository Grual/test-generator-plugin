import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiModifier
import com.intellij.psi.util.PsiTreeUtil

data class MethodInfo(
    val name: String,
    val returnType: String?,
    val annotations: List<String>,
    val parameters: List<Pair<String, String>> // (parameter name, parameter type)
)

data class ClassInfo(
    val qualifiedName: String,
    val fields: List<Pair<String, String>>, // (field name, fully qualified field type)
    val methods: List<MethodInfo>,
    val imports: List<String>, // List of fully qualified imported classes
    val packageName: String
)

fun analyzePsiFile(psiFile: PsiFile): ClassInfo {
    val imports = mutableListOf<String>()
    if (psiFile is PsiJavaFile) {
        imports.addAll(psiFile.importList?.allImportStatements?.mapNotNull { it.text } ?: emptyList())
    }

    val psiClass = PsiTreeUtil.findChildrenOfType(psiFile, PsiClass::class.java).first()
    val fields = psiClass.fields.map { it.name to it.type.presentableText }

    val methods = psiClass.methods.filter { it.hasModifierProperty(PsiModifier.PUBLIC) }.map { method ->
        MethodInfo(
            name = method.name,
            returnType = method.returnType?.presentableText,
            annotations = method.modifierList.annotations.mapNotNull { it.qualifiedName },
            parameters = method.parameterList.parameters.map { it.name to it.type.presentableText }
        )
    }

    return ClassInfo(psiClass.qualifiedName!!, fields, methods, imports, (psiFile as PsiJavaFile).packageName)
}
