package com.github.grual.testgeneratorplugin.components.settings

class TestGeneratorState {
    var baseTestClass: String? = null
    var mainPackageBaseName: String = "main"
    var testPackageBaseName: String = "test"
    var testClassNameSuffix: String = "IT"
    var allowActionForFilesEndingIn: List<String> = mutableListOf("WebApi.java", "WebApiController.java") // TODO empty list
    var useMockVc: Boolean = true // TODO set to false
    var checkBaseClassForAutowires: Boolean = true
    var checkBaseClassForMocks: Boolean = true
    // TODO add generateBefore/afterEach
}