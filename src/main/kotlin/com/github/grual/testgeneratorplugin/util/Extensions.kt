package com.github.grual.testgeneratorplugin.util

fun StringBuilder.appendWithBreak(value: String?): StringBuilder {
    val appendValue = value ?: return this
    this.append(appendValue)
    this.append("\n")
    return this
}

fun String.camelToSnakeCase(): String {
    return this.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
}