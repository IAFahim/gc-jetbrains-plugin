package com.github.iafahim.gccopy

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(name = "GcCopySettings", storages = [Storage("gc-copy.xml")])
class GcSettings : PersistentStateComponent<GcSettings.Stored> {

    data class Stored(var command: String = DEFAULT_COMMAND)

    private var stored = Stored()

    override fun getState(): Stored = stored

    override fun loadState(state: Stored) {
        stored = state
    }

    var command: String
        get() = stored.command.ifBlank { DEFAULT_COMMAND }
        set(value) {
            stored.command = value
        }

    companion object {
        const val DEFAULT_COMMAND: String = "gc -e cs -z \\\\ \\n\\n"

        fun getInstance(): GcSettings =
            ApplicationManager.getApplication().getService(GcSettings::class.java)
    }
}
