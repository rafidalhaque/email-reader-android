package com.rafidalhaque.emailreader

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                EmlReaderScreen()
            }
        }
    }
}

@Composable
private fun EmlReaderScreen() {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var parseResult by remember { mutableStateOf<Result<EmlMessage>?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            selectedUri = uri
            parseResult = uri?.let {
                runCatching {
                    context.contentResolver.openInputStream(it)?.use { input ->
                        val content = BufferedReader(InputStreamReader(input)).readText()
                        EmlParser.parse(content)
                    } ?: error("Unable to read selected file.")
                }
            }
        }
    )

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = { filePicker.launch(arrayOf("message/rfc822", "text/plain", "*/*")) }) {
                Text("Choose .eml file")
            }

            selectedUri?.let {
                Text(
                    text = "File: $it",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            parseResult?.fold(
                onSuccess = { message ->
                    ParsedEmailCard(message = message)
                },
                onFailure = { throwable ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = throwable.message ?: "Failed to parse file.",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun ParsedEmailCard(message: EmlMessage) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HeaderRow("Subject", message.subject)
            HeaderRow("From", message.from)
            HeaderRow("To", message.to)
            HeaderRow("Date", message.date)
            Text("Body", style = MaterialTheme.typography.titleSmall)
            Text(message.body.ifBlank { "(No readable body found)" })
        }
    }
}

@Composable
private fun HeaderRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value.ifBlank { "-" },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
