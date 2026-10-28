package io.github.techwithrakibofficial.hermesbridge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A device-verification target with no network, send, delete or call action. */
class ControlTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var text by rememberSaveable { mutableStateOf("") }
            var count by rememberSaveable { mutableIntStateOf(0) }
            var checked by rememberSaveable { mutableStateOf(false) }
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text("Hermes control test", style = MaterialTheme.typography.headlineMedium)
                        Text("এখানে শুধু এই test screen-এর নমুনা text ও counter বদলাবে।")
                        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Test message") }, modifier = Modifier.fillMaxWidth())
                        Text("Draft: $text")
                        Button(onClick = { count++ }) { Text("Increase test counter") }
                        Text("Counter: $count")
                        Row { Checkbox(checked = checked, onCheckedChange = { checked = it }); Text("Test option: $checked") }
                        repeat(12) { Text("Scroll sample ${it + 1}", modifier = Modifier.padding(vertical = 12.dp)) }
                        Text("End of test screen")
                        OutlinedButton(onClick = { finish() }) { Text("Close test screen") }
                    }
                }
            }
        }
    }
}
