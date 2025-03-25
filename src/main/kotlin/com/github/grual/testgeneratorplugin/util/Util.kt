package com.github.grual.testgeneratorplugin.util

fun findAllRegexMatches(input: String, pattern: String): List<String> {
    return Regex(pattern).findAll(input).map { it.value }.toList()
}
