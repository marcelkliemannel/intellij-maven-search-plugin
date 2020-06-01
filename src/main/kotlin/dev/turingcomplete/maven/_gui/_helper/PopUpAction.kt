package dev.turingcomplete.maven._gui._helper

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.LayeredIcon
import com.intellij.ui.scale.JBUIScale
import javax.swing.Icon
import javax.swing.JComponent

class PopUpAction<T>(private val items: Map<String, T>,
                     private val icon: Icon,
                     text: String?,
                     private val createActionForItem: (String, T) -> AnAction) : DumbAwareAction(), CustomComponentAction {

  private val switchContextGroup: DefaultActionGroup
  private val actionButton: ActionButton = ActionButton(this, templatePresentation, ActionPlaces.UNKNOWN, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE)

  companion object {
    fun <T> forToggleAction(items: Map<String, T>,
                            icon: Icon,
                            text: String?,
                            isItemSelected: (T) -> Boolean,
                            setSelectedItem: (T) -> Unit): PopUpAction<T> {

      return PopUpAction(items, icon, text) { title, item ->
        object : DumbAwareToggleAction(title) {
          override fun isSelected(e: AnActionEvent) = isItemSelected(item)

          override fun setSelected(e: AnActionEvent, state: Boolean) {
            setSelectedItem(item)
          }
        }
      }
    }
  }

  init {
    templatePresentation.icon = JBUIScale.scaleIcon(LayeredIcon(2)).apply {
      setIcon(icon, 0)
      setIcon(AllIcons.General.Dropdown, 1, 3, 0)
    }
    templatePresentation.text = text
    templatePresentation.description = "sdfasdf"

    switchContextGroup = DefaultActionGroup({ "Foo" }, listOf()).apply {
      isPopup = true
      items.forEach { (title, item) -> add(createActionForItem(title, item)) }
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    if (e.getData(PlatformDataKeys.CONTEXT_COMPONENT) == null) return
    val listPopup = JBPopupFactory.getInstance().createActionGroupPopup(null, switchContextGroup, e.dataContext, true, null, 10)
    listPopup.showUnderneathOf(actionButton)
  }

  override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
    return actionButton
  }
}