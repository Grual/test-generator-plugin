package com.github.grual.testgeneratorplugin.util

fun StringBuilder.appendWithBreak(value: String?): StringBuilder {
    val appendValue = value ?: return this
    this.append(appendValue)
    this.append("\n")
    return this
}