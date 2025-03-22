package com.github.grual.testgeneratorplugin.components.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@State(
        name = "TestGeneratorSetting",
        storages = [Storage("test-generator-plugin-settings.xml")]
)
class TestGeneratorSettings : PersistentStateComponent<TestGeneratorState> {
    private var pluginState = TestGeneratorState()

    override fun getState(): TestGeneratorState {
        return pluginState
    }

    override fun loadState(state: TestGeneratorState) {
        pluginState = state
    }

    companion object {
        @JvmStatic
        fun getInstance(): PersistentStateComponent<TestGeneratorState> {
            return service<TestGeneratorSettings>()
        }
    }
}