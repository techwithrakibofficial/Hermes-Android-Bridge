package io.github.techwithrakibofficial.hermesbridge

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Only explicit in-app button presses can call these actions in milestone 1. */
class BridgeAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        connectedState.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) {
            instance = null
            connectedState.value = false
        }
        super.onDestroy()
    }

    companion object {
        private var instance: BridgeAccessibilityService? = null
        private val connectedState = MutableStateFlow(false)
        val connected = connectedState.asStateFlow()

        // No arbitrary integer action or remotely callable interface is exposed.
        fun home(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_HOME) ?: false
        fun back(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_BACK) ?: false
    }
}
