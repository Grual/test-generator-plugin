package com.github.grual.testgeneratorplugin.components.settings

import com.github.grual.testgeneratorplugin.MessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.util.preferredWidth
import com.intellij.uiDesigner.core.AbstractLayout
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.SwingConstants
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class SettingsDialog : DialogWrapper(false) {
    private val settings: TestGeneratorState = TestGeneratorSettings.getInstance().state!!

    private val panel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
    private val baseTestClassField = createTextFieldWithHint(MessagesBundle.message("settings.baseTestClass.hint"))
    private val testClassNameSuffixField =
        createTextFieldWithHint(MessagesBundle.message("settings.testClassNameSuffix.hint"))
    private val allowedFileNameEndingsField =
        createTextFieldWithHint(MessagesBundle.message("settings.allowActionForFilesEndingIn.hint"))
    private val useMockMvcCheckBox = createCheckBoxWithToolTip("useMockVc")
    private val checkBaseClassForAutowiresCheckBox = createCheckBoxWithToolTip("checkBaseClassForAutowires")
    private val checkBaseClassForMocksCheckBox = createCheckBoxWithToolTip("checkBaseClassForMocks")

    init {
        init()
        title = MessagesBundle.message("settings.title")

        panel.preferredWidth = 800

        checkBaseClassForMocksCheckBox.addItemListener { validateTextField() }
        checkBaseClassForAutowiresCheckBox.addItemListener { validateTextField() }

        baseTestClassField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = validateTextField()
            override fun removeUpdate(e: DocumentEvent?) = validateTextField()
            override fun changedUpdate(e: DocumentEvent?) = validateTextField()
        })

        setInitialValues()
    }

    override fun createCenterPanel(): JComponent {
        panel.add(createTextFieldPanel())
        panel.add(createCheckBoxPanel(useMockMvcCheckBox, "useMockVc"))
        panel.add(createCheckBoxPanel(checkBaseClassForAutowiresCheckBox, "checkBaseClassForAutowires"))
        panel.add(createCheckBoxPanel(checkBaseClassForMocksCheckBox, "checkBaseClassForMocks"))

        return panel
    }

    override fun doOKAction() {
        val state = TestGeneratorState().apply {
            settingsInitiallySet = true
            baseTestClass = baseTestClassField.text.trim()
            testClassNameSuffix = testClassNameSuffixField.text.trim()
            allowActionForFilesEndingIn = allowedFileNameEndingsField.text.split(",").map { it.trim() }.toList()
            useMockVc = useMockMvcCheckBox.isSelected
            checkBaseClassForAutowires = checkBaseClassForAutowiresCheckBox.isSelected
            checkBaseClassForMocks = checkBaseClassForMocksCheckBox.isSelected
        }
        TestGeneratorSettings.getInstance().loadState(state)
        super.doOKAction()
    }

    private fun createTextFieldPanel(): JPanel {
        val gb = GridBag()
            .setDefaultInsets(JBUI.insets(0, 0, AbstractLayout.DEFAULT_VGAP, AbstractLayout.DEFAULT_HGAP))
            .setDefaultWeightX(1.0)
            .setDefaultFill(GridBagConstraints.HORIZONTAL)
        return JPanel(GridBagLayout()).apply {
            add(createLabelWithHelpIcon("baseTestClass"), gb.nextLine().next().weightx(0.0))
            add(baseTestClassField, gb.next().weightx(1.0))
            add(createLabelWithHelpIcon("testClassNameSuffix"), gb.nextLine().next().weightx(0.0))
            add(testClassNameSuffixField, gb.next().weightx(1.0))
            add(createLabelWithHelpIcon("allowActionForFilesEndingIn"), gb.nextLine().next().weightx(0.0))
            add(allowedFileNameEndingsField, gb.next().weightx(1.0))
        }
    }

    private fun createLabelWithHelpIcon(propertyName: String): JBLabel {
        return JBLabel(MessagesBundle.message("settings.$propertyName")).apply {
            componentStyle = UIUtil.ComponentStyle.SMALL
            fontColor = UIUtil.FontColor.BRIGHTER
            border = JBUI.Borders.empty(0, 5, 2, 0)
            icon = AllIcons.General.ContextHelp
            toolTipText = MessagesBundle.message("settings.$propertyName.desc")
            horizontalTextPosition = SwingConstants.LEFT
        }
    }

    private fun createCheckBoxWithToolTip(propertyName: String): JCheckBox {
        return JCheckBox().apply {
            toolTipText = MessagesBundle.message("settings.$propertyName.desc")
        }
    }

    private fun createCheckBoxPanel(checkBox: JCheckBox, propertyName: String): JPanel {
        val label = createLabelWithHelpIcon(propertyName)
        label.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                checkBox.isSelected = !checkBox.isSelected
            }
        })

        return JPanel().apply {
            layout = FlowLayout(FlowLayout.LEFT)
            add(checkBox)
            add(label)
        }
    }

    private fun createTextFieldWithHint(hint: String): JBTextField {
        return JBTextField().apply { emptyText.text = hint }
    }

    private fun validateTextField() {
        if ((checkBaseClassForMocksCheckBox.isSelected || checkBaseClassForAutowiresCheckBox.isSelected) && baseTestClassField.text.isEmpty()) {
            baseTestClassField.border = JBUI.Borders.customLine(JBColor.RED, 1)
            baseTestClassField.toolTipText = MessagesBundle.message("settings.error.baseTestClassRequired")
            super.setOKActionEnabled(false)
        } else {
            baseTestClassField.border = JTextField().border
            baseTestClassField.toolTipText = null
            super.setOKActionEnabled(true)
        }
    }

    private fun setInitialValues() {
        baseTestClassField.text = settings.baseTestClass
        testClassNameSuffixField.text = settings.testClassNameSuffix
        allowedFileNameEndingsField.text = settings.allowActionForFilesEndingIn.reduce { acc, s -> "$acc,$s" }
        useMockMvcCheckBox.isSelected = settings.useMockVc
        checkBaseClassForAutowiresCheckBox.isSelected = settings.checkBaseClassForAutowires
        checkBaseClassForMocksCheckBox.isSelected = settings.checkBaseClassForMocks
    }
}
