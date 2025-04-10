package com.github.grual.testgeneratorplugin.util

import com.intellij.psi.PsiMethod

fun findAllRegexMatches(input: String, pattern: String): List<String> {
    return Regex(pattern).findAll(input).map { it.value }.toList()
}

fun prettyPrintMethodHead(methodName: PsiMethod): String {
    return methodName.text.split("{").first().replace("[\n\\s+]", "").replace(",", ", ")
}