package com.github.grual.testgeneratorplugin.util

import com.github.grual.testgeneratorplugin.components.settings.TestGeneratorSettings
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifier

fun StringBuilder.appendWithBreak(value: String?): StringBuilder {
    val appendValue = value ?: return this
    this.append(appendValue)
    this.append("\n")
    return this
}

fun String.camelToSnakeCase(): String {
    return this.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
}

fun String.decapitalize(): String {
    return this.replaceFirstChar { it.lowercase() }
}

fun PsiClass.hasBuilderMethod(): Boolean {
    return this.methods.any { it.name == "builder" && it.hasModifierProperty(PsiModifier.STATIC) }
            || (TestGeneratorSettings.getInstance().state!!.usesLombok
            && (this.hasAnnotation(BUILDER_ANNOTATION) || this.hasAnnotation(SUPER_BUILDER_ANNOTATION)))
}