package com.example.pdf2word

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pdf2word.util.rememberPdfPicker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Pdf2WordTheme {
                AppScreen()
            }
        }
    }
}

@Composable
fun AppScreen() {
    val vm: ConversionViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val pickPdf = rememberPdfPicker(
        onPicked = { file -> vm.convert(file.absolutePath, "output.docx") },
        onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
    )

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("PDF 转 Word", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = pickPdf) { Text("选择 PDF") }

            when (val s = state) {
                is UiState.Idle -> Text(
                    "请选择一个 PDF 文件",
                    style = MaterialTheme.typography.bodyMedium
                )
                is UiState.Loading -> {
                    CircularProgressIndicator()
                    Text(s.msg)
                }
                is UiState.Success -> {
                    Text("已保存到：")
                    Text(s.outputFile.absolutePath, style = MaterialTheme.typography.bodySmall)
                }
                is UiState.Error -> {
                    Text(s.msg, color = Color.Red)
                }
            }
        }
    }
}

@Composable
fun Pdf2WordTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        content = content
    )
}
