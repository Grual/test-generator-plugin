package com.github.grual.testgeneratorplugin.components.settings

class TestGeneratorState {
    var baseTestClass: String? = null
    var allowActionForFilesEndingIn: List<String> = mutableListOf("WebApi.java") // TODO empty list
}