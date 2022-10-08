package com.delitx.huffmancoding

import android.Manifest
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalOverScrollConfiguration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.delitx.huffmancoding.ui.bar_chart.BarChart
import com.delitx.huffmancoding.ui.bar_chart.BarChartData
import com.delitx.huffmancoding.ui.huffman_coding.getCharOccurRate
import com.delitx.huffmancoding.ui.huffman_coding.toHuffmanMap
import com.delitx.huffmancoding.ui.theme.HuffmanCodingTheme
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {

    private val permissionsToGrant = listOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private var onPermissionReceived: () -> Unit = {}

    private val permissionRequester =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (!permissions.any { !it.value }) {
                onPermissionReceived()
                onPermissionReceived = {}
            }
        }

    private var onFileSelected: (Uri) -> Unit = {}
    private val fileSelector =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    onFileSelected(uri)
                    onFileSelected = {}
                } catch (e: IllegalArgumentException) {
                }
            }
        }

    private var onFileSaveSelected: (Uri) -> Unit = {}
    private val fileSaver =
        registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
            if (uri != null) {
                try {
                    onFileSaveSelected(uri)
                    onFileSaveSelected = {}
                } catch (e: Throwable) {
                    val ex = e
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HuffmanCodingTheme {
                MainUI()
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun MainUI() {
        CompositionLocalProvider(
            LocalOverScrollConfiguration provides null
        ) {
            var text by rememberSaveable { mutableStateOf("") }
            var encodedText by rememberSaveable { mutableStateOf("") }
            var encodingCodes by rememberSaveable { mutableStateOf<Map<Char, String>>(mapOf()) }
            var probabilities by rememberSaveable {
                mutableStateOf<List<Pair<Char, Int>>>(
                    listOf()
                )
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(1f)
                    .padding(horizontal = 20.dp)
            ) {
                item {
                    TextField(value = text, onValueChange = { text = it })
                    Button(onClick = {
                        whenPermissionsGranted {
                            selectFile { uri ->
                                text = readFile(uri)
                            }
                        }
                    }) {
                        Text(text = "Load text from file")
                    }
                    Text(text = "Start length:${text.toBinary().length}")
                    Text(text = "Start binary:" + text.toBinary())
                    Button(onClick = {
                        val map = text.toHuffmanMap()
                        encodedText = map.encodeText(text.toList())
                        encodingCodes = map.codesTable
                        probabilities = text.getCharOccurRate()
                    }) {
                        Text(text = "Encode")
                    }
                    Text(text = "Encoded length:${encodedText.length}")
                    Text(text = "Encoded text:$encodedText")
                    Text(text = "Encode code table:")
                }
                items(encodingCodes.toList()) { (symbol, code) ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "\'$symbol\'",
                            modifier = Modifier.fillParentMaxWidth(0.5f)
                        )
                        Text(text = code)
                    }
                }
                item {
                    if (probabilities.isNotEmpty()) {
                        BarChart(
                            barChartData = BarChartData(
                                probabilities.map {
                                    BarChartData.Bar(
                                        value = it.second.toFloat(),
                                        color = Color.Cyan,
                                        label = "\'${it.first}\'"
                                    )
                                }
                            ),
                            modifier = Modifier
                                .height(300.dp)
                                .fillParentMaxHeight(1f)
                        )
                        Button(onClick = {
                            whenPermissionsGranted {
                                saveFile { uri ->
                                    writeFile(
                                        uri,
                                        """
                                        Encoded text: $encodedText
                                        Encoding codes: $encodingCodes
                                        """.trimIndent()
                                    )
                                }
                            }
                        }) {
                            Text(text = "Save result to file")
                        }
                    }
                }
            }
        }
    }

    private fun writeFile(uri: Uri, content: String) {
        contentResolver.openFileDescriptor(uri, "w")
            ?.use { descriptor ->
                FileOutputStream(descriptor.fileDescriptor).use { inputStream ->
                    inputStream.write(content.toByteArray())
                }
            }
    }

    private fun readFile(uri: Uri): String {
        val stringBuilder = StringBuilder()
        contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    stringBuilder.append('\n')
                    line = reader.readLine()
                }
            }
        }
        return stringBuilder.toString()
    }

    private fun saveFile(action: (Uri) -> Unit) {
        onFileSaveSelected = action
        fileSaver.launch("${System.currentTimeMillis()}.txt")
    }

    private fun selectFile(action: (Uri) -> Unit) {
        onFileSelected = action
        fileSelector.launch(arrayOf("text/plain"))
    }

    private fun whenPermissionsGranted(action: () -> Unit) {
        onPermissionReceived = action
        permissionRequester.launch(permissionsToGrant.toTypedArray())
    }
}

fun String.toBinary(): String {
    val resultBuilder = StringBuilder()
    for (char in this) {
        resultBuilder.append(Integer.toBinaryString(char.code))
    }
    return resultBuilder.toString()
}
