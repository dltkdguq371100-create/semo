package com.semo.memo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.semo.memo.ui.SecondaryText
import com.semo.memo.ui.SemoTheme
import kotlinx.coroutines.launch

/** Dialog-themed entry point launched from the home screen widget. */
class QuickInputActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SemoTheme {
                QuickInputContent(
                    onCancel = { finish() },
                    onSave = { text -> saveAndFinish(text) },
                )
            }
        }
    }

    private fun saveAndFinish(text: String) {
        val app = application as SemoApplication
        lifecycleScope.launch {
            runCatching { app.repository.createMemo(text) }
            finish()
        }
    }
}

@Composable
private fun QuickInputContent(onCancel: () -> Unit, onSave: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val requester = remember { FocusRequester() }
    LaunchedEffect(Unit) { requester.requestFocus() }
    Surface {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("빠른 메모", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().focusRequester(requester),
                placeholder = { Text("메모 입력", color = SecondaryText) },
                minLines = 2,
                maxLines = 6,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onCancel) { Text("취소") }
                Button(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text("저장") }
            }
        }
    }
}
