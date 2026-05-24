package com.github.iafahim.gccopy

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class GcSettingsConfigurable : Configurable {

    private val commandField = JBTextField()

    override fun getDisplayName(): String = "gc Copy"

    override fun createComponent(): JComponent =
        FormBuilder.createFormBuilder()
            .addLabeledComponent(
                JBLabel("Command (run by bash in the selected folder):"),
                commandField,
                true,
            )
            .addComponentFillVertically(JPanel(), 0)
            .panel

    override fun isModified(): Boolean =
        commandField.text != GcSettings.getInstance().command

    override fun apply() {
        GcSettings.getInstance().command = commandField.text
    }

    override fun reset() {
        commandField.text = GcSettings.getInstance().command
    }
}
