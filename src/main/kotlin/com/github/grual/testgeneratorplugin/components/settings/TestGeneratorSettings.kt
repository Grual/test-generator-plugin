package com.github.grual.testgeneratorplugin.components.settings

import com.github.grual.testgeneratorplugin.`object`.Observer
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@State(
        name = "TestGeneratorSetting",
        storages = [Storage("test-generator-plugin-settings.xml")]
)
class TestGeneratorSettings : PersistentStateComponent<TestGeneratorState> {
    private val observers = mutableListOf<Observer>()
    private var pluginState = TestGeneratorState()

    override fun getState(): TestGeneratorState {
        return pluginState
    }

    override fun loadState(state: TestGeneratorState) {
        pluginState = state
        notifyObservers()
    }

    private fun notifyObservers() {
        observers.forEach(Observer::update)
    }

    companion object {
        @JvmStatic
        fun getInstance(): PersistentStateComponent<TestGeneratorState> {
            return service<TestGeneratorSettings>()
        }

        @JvmStatic
        fun addObserver(observer: Observer) {
            service<TestGeneratorSettings>().observers.add(observer)
        }
    }
}