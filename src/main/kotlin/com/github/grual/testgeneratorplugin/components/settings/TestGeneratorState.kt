package com.github.grual.testgeneratorplugin.components.settings

class TestGeneratorState {
    var settingsInitiallySet: Boolean = false
    var baseTestClass: String? = null
    var testClassNameSuffix: String = "IT"
    var allowActionForFilesEndingIn: List<String> = mutableListOf("WebApi.java", "WebApiController.java") // TODO empty list
    var useMockVc: Boolean = true // TODO set to false
    var checkBaseClassForAutowires: Boolean = true
    var checkBaseClassForMocks: Boolean = true
    // TODO add generateBefore/afterEach
}