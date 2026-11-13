package com.example.chemsandbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ChemSandApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChemSandApp() {
    val glView = remember { SandGLView(LocalContext.current) }
    var gravity by remember { mutableStateOf(1f) }
    var reaction by remember { mutableStateOf(1f) }
    var ambient by remember { mutableStateOf(20f) }
    var brush by remember { mutableStateOf(8) }
    var paused by remember { mutableStateOf(false) }
    var showTemp by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(Element.SAND) }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold(topBar = { TopAppBar(title = { Text("Хим-Песочница") }) }) { pad ->
            Column(Modifier.padding(pad).fillMaxSize().background(Color(0xFF0d0d12))) {
                AndroidView(
                    factory = { glView },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    update = { v ->
                        v.sim.gravity = gravity; v.sim.reactionRate = reaction
                        v.sim.ambient = ambient; v.sim.paused = paused
                        v.sim.showTemperature = showTemp
                        v.brushRadius = brush; v.brushElement = selected
                    }
                )
                Controls(gravity, reaction, ambient, brush, paused, showTemp,
                    { gravity = it }, { reaction = it }, { ambient = it }, { brush = it },
                    { paused = !paused }, { showTemp = !showTemp }, { glView.sim.clear() })
                Palette(selected) { selected = it }
            }
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>,
                      fmt: (Float) -> String, onValue: (Float) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text("$label: ${fmt(value)}", color = Color.White)
        Slider(value = value, onValueChange = onValue, valueRange = range)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@OptIn(ExperimentalMaterial3Api::class)
@OptIn(ExperimentalMaterial3Api::class)
private fun Controls(gravity: Float, reaction: Float, ambient: Float, brush: Int,
                     paused: Boolean, showTemp: Boolean,
                     onG: (Float) -> Unit, onR: (Float) -> Unit, onA: (Float) -> Unit,
                     onB: (Float) -> Unit, onPause: () -> Unit, onTemp: () -> Unit, onClear: () -> Unit) {
    Column(Modifier.padding(top = 8.dp)) {
        SliderRow("Гравитация", gravity, 0f..2f, { "%.2f".format(it) }, onG)
        SliderRow("Скорость реакций", reaction, 0f..2f, { "%.2f".format(it) }, onR)
        SliderRow("Температура среды", ambient, -50f..1200f, { "${it.toInt()} °C" }, onA)
        SliderRow("Размер кисти", brush.toFloat(), 1f..30f, { it.toInt().toString() }, onB)
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onPause) { Text(if (paused) "▶ Старт" else "⏸ Пауза") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onClear) { Text("🗑 Очистить") }
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = showTemp, onClick = onTemp, label = { Text("🌡 Тепловизор") })
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@OptIn(ExperimentalMaterial3Api::class)
@OptIn(ExperimentalMaterial3Api::class)
private fun Palette(selected: Element, onPick: (Element) -> Unit) {
    val names = mapOf(
        Element.SAND to "Песок", Element.WATER to "Вода", Element.ICE to "Лёд",
        Element.STONE to "Камень", Element.METAL to "Металл", Element.WOOD to "Дерево",
        Element.OIL to "Нефть", Element.PLANT to "Растение", Element.FIRE to "Огонь",
        Element.LAVA to "Лава", Element.ACID to "Кислота", Element.EMPTY to "Ластик"
    )
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(12.dp)) {
        names.forEach { (e, name) ->
            Surface(
                onClick = { onPick(e) },
                shape = RoundedCornerShape(12.dp),
                color = Color(e.color).copy(alpha = if (e == selected) 1f else 0.45f),
                modifier = Modifier.padding(end = 8.dp)
                    .border(2.dp, if (e == selected) Color.White else Color.Transparent, RoundedCornerShape(12.dp))
            ) {
                Text(name, Modifier.padding(horizontal = 14.dp, vertical = 10.dp), color = Color.White)
            }
        }
    }
}
