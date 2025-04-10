package com.github.grual.testgeneratorplugin.components.settings

class TestGeneratorState {
    // TODO maybe make substates that group settings that belong together
    var settingsInitiallySet: Boolean = false
    var baseTestClass: String? = null
    var testClassNameSuffix: String = "IT"
    var allowActionForFilesEndingIn: List<String> = mutableListOf("WebApi.java", "WebApiController.java") // TODO empty list
    var usesLombok: Boolean = false
    var useMockVc: Boolean = true // TODO set to false
    var checkBaseClassForAutowires: Boolean = true
    var checkBaseClassForMocks: Boolean = true
    var generateFixture: Boolean = false
    var fixturesUseBuilder: Boolean = false
    var fixturesUseOnlyNotNullFields: Boolean = false
    // TODO add generateBefore/afterEach
}