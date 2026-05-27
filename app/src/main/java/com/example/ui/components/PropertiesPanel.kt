package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ComponentType
import com.example.data.WatchComponent

// Color schemes presets
val PRESET_COLORS = listOf(
    0xFFFFFFFF.toInt(), // White
    0xFF8E8E93.toInt(), // Slate Gray
    0xFFFF453A.toInt(), // Crimson / Ruby Red
    0xFFFF9500.toInt(), // Orange
    0xFFFFD60A.toInt(), // Sunshine Yellow
    0xFF30D158.toInt(), // Mint Green
    0xFF64D2FF.toInt(), // Sky Blue
    0xFF0A84FF.toInt(), // Cobalt Blue
    0xFFBF5AF2.toInt(), // Amethyst Purple
    0xFFFF2D55.toInt(), // Pink / Rose
)

val PRESET_WIDGET_ALPHA = listOf(
    0x00000000,          // Transparent
    0x22FFFFFF,          // Dark Glass 13%
    0x44FFFFFF,          // Glass 26%
    0x66000000,          // Darkened Glass 40%
    0x2230D158,          // Green Glass 13%
    0x22FF453A,          // Red Glass 13%
    0x220A84FF,          // Blue Glass 13%
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PropertiesPanel(
    modifier: Modifier = Modifier,
    components: List<WatchComponent>,
    selectedId: String?,
    customFonts: List<Pair<String, String>>,
    onComponentChanged: (WatchComponent) -> Unit,
    onAddNewComponent: (ComponentType, String) -> Unit,
    onRemoveComponent: (String) -> Unit,
    onCustomFontSelected: (Uri) -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val currentSelected = components.find { it.id == selectedId }

    // Font picking storage activity registry
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onCustomFontSelected(uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.35f))
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SECTION 1: ADD NEW COMPONENTS ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "🛠️ Добавить элемент",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val addTypes = listOf(
                        Triple(ComponentType.STEPS, "🚶 Шаги", "steps"),
                        Triple(ComponentType.CALORIES, "🔥 Калории", "calories"),
                        Triple(ComponentType.HEART_RATE, "❤️ Пульс", "heart_rate"),
                        Triple(ComponentType.BATTERY, "🔋 Заряд", "battery"),
                        Triple(ComponentType.WEATHER, "⛅ Погода", "weather"),
                        Triple(ComponentType.LABEL_SHORTCUT, "🎯 Ярлык", "shortcut"),
                        Triple(ComponentType.CUSTOM_TEXT, "✏️ Текст", "custom_text")
                    )

                    addTypes.forEach { (type, label, prefix) ->
                        // Only add if it doesn't exist yet, or allow duplicate labels/shortcuts/custom texts
                        val alreadyExists = components.any { it.type == type } && type != ComponentType.LABEL_SHORTCUT && type != ComponentType.CUSTOM_TEXT

                        InputChip(
                            selected = false,
                            onClick = {
                                val userLabel = when (type) {
                                    ComponentType.LABEL_SHORTCUT -> "Музыка"
                                    ComponentType.CUSTOM_TEXT -> "RE-FIT"
                                    else -> label.substring(2)
                                }
                                onAddNewComponent(type, userLabel)
                            },
                            label = { Text(label, fontSize = 12.sp) },
                            enabled = !alreadyExists,
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
            }
        }

        // --- SECTION 2: EDIT SELECTED COMPONENT ---
        if (currentSelected != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Параметры элемента",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                            Text(
                                text = currentSelected.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Delete current item
                        IconButton(
                            onClick = { onRemoveComponent(currentSelected.id) },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(0.4f),
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить элемент")
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(0.08f))

                    // 1. POSITION CONTROLLER (FINE COORDINATE ADJUSTMENTS)
                    Text(
                        text = "📍 Позиционирование (Координаты)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    // Compact directional keyboard nudge buttons (like real PC watch designers layout!)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Up
                            IconButton(
                                onClick = { onComponentChanged(currentSelected.copy(y = currentSelected.y - 1f)) },
                                modifier = Modifier.size(36.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors()
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Вверх")
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                // Left
                                IconButton(
                                    onClick = { onComponentChanged(currentSelected.copy(x = currentSelected.x - 1f)) },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors()
                                ) {
                                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Влево")
                                }

                                // Center coordinates stats indicator
                                Box(
                                    modifier = Modifier
                                        .size(width = 54.dp, height = 36.dp)
                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(0.5f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                   ) {
                                    Text(
                                        text = "X: ${currentSelected.x.toInt()}\nY: ${currentSelected.y.toInt()}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 13.sp
                                    )
                                }

                                // Right
                                IconButton(
                                    onClick = { onComponentChanged(currentSelected.copy(x = currentSelected.x + 1f)) },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors()
                                ) {
                                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Вправо")
                                }
                            }

                            // Down
                            IconButton(
                                onClick = { onComponentChanged(currentSelected.copy(y = currentSelected.y + 1f)) },
                                modifier = Modifier.size(36.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors()
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Вниз")
                            }
                        }
                    }

                    // Precise drag coordinates input indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Смещение X", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Slider(
                                value = currentSelected.x,
                                onValueChange = { onComponentChanged(currentSelected.copy(x = it)) },
                                valueRange = 0f..400f,
                                modifier = Modifier.height(28.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Смещение Y", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Slider(
                                value = currentSelected.y,
                                onValueChange = { onComponentChanged(currentSelected.copy(y = it)) },
                                valueRange = 0f..500f,
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }

                    // 2. FONT SIZE SLIDER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("📐 Размер шрифта: ${currentSelected.size.toInt()}sp", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        IconButton(onClick = { onComponentChanged(currentSelected.copy(size = currentSelected.size + 1f)) }) {
                            Icon(Icons.Default.Add, contentDescription = "Увеличить")
                        }
                    }
                    Slider(
                        value = currentSelected.size,
                        onValueChange = { onComponentChanged(currentSelected.copy(size = it)) },
                        valueRange = 8f..72f
                    )

                    // 3. COLOR SELECTION PRESETS
                    Text("🎨 Цвет текста", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(PRESET_COLORS) { colorInt ->
                            val color = Color(colorInt)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (currentSelected.color == colorInt) 3.dp else 1.dp,
                                        color = if (currentSelected.color == colorInt) MaterialTheme.colorScheme.primary else Color.White.copy(0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onComponentChanged(currentSelected.copy(color = colorInt))
                                    }
                            )
                        }
                    }

                    // 4. FONTS SELECTOR WITH CUSTOM LOADING
                    Text("🔤 Шрифты (в т.ч. Свои)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    val standardFonts = listOf("Default", "Digital", "Tech", "Space")
                    
                    Text("Системные пресеты:", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        standardFonts.forEach { fontName ->
                            FilterChip(
                                selected = currentSelected.fontName == fontName,
                                onClick = { onComponentChanged(currentSelected.copy(fontName = fontName)) },
                                label = { Text(fontName) }
                            )
                        }
                    }

                    // Loading of custom files (PC Analogue requirement!)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ваши загруженные шрифты:", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        Button(
                            onClick = { filePickerLauncher.launch("font/*") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.filledTonalButtonColors()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Загрузить .TTF", fontSize = 11.sp)
                        }
                    }

                    if (customFonts.isEmpty()) {
                        Text(
                            text = "Нет загруженных .ttf/.otf файлов в приложении. Нажмите кнопку выше.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            lineHeight = 14.sp
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            customFonts.forEach { (fileName, targetPath) ->
                                val isSelected = currentSelected.fontName == "Custom" && currentSelected.customFontPath == targetPath
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        onComponentChanged(
                                            currentSelected.copy(
                                                fontName = "Custom",
                                                customFontPath = targetPath
                                            )
                                        )
                                    },
                                    label = { Text(fileName) }
                                )
                            }
                        }
                    }

                    // 5. CUSTOM LABELS BACKDROPS (PILLS & CIRCLES AND PADDING - PC Analogue feature requested!)
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(0.08f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📐 Фон ярлыка / Счётчика", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Switch(
                            checked = currentSelected.customLabelBackground != null,
                            onCheckedChange = { enabled ->
                                val defaultBg = if (enabled) 0x22FFFFFF else null
                                onComponentChanged(currentSelected.copy(customLabelBackground = defaultBg))
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = currentSelected.customLabelBackground != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Цвет и Прозрачность подложки:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            // Background opacity presets grid
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(PRESET_WIDGET_ALPHA) { bgInt ->
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(bgInt).copy(1f)) // Show solid preview
                                            .border(
                                                width = if (currentSelected.customLabelBackground == bgInt) 3.dp else 1.dp,
                                                color = if (currentSelected.customLabelBackground == bgInt) MaterialTheme.colorScheme.primary else Color.White.copy(0.3f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                onComponentChanged(currentSelected.copy(customLabelBackground = bgInt))
                                            }
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            // Padding sliders
                            Text("Внутренний отступ (Padding): ${currentSelected.customLabelPadding.toInt()}dp", fontSize = 11.sp)
                            Slider(
                                value = currentSelected.customLabelPadding,
                                onValueChange = { onComponentChanged(currentSelected.copy(customLabelPadding = it)) },
                                valueRange = 2f..24f
                            )

                            // Rounded Corner radius sliders
                            Text("Радиус скругления (Corner Radius): ${currentSelected.customLabelRadius.toInt()}dp", fontSize = 11.sp)
                            Slider(
                                value = currentSelected.customLabelRadius,
                                onValueChange = { onComponentChanged(currentSelected.copy(customLabelRadius = it)) },
                                valueRange = 0f..32f
                            )
                        }
                    }

                    // 6. EXTRA WIDGET OPTIONS (FORMATING AND ICONS)
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(0.08f))
                    Text("⚙️ Дополнительные опции:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Отображать иконку", fontSize = 12.sp)
                        Checkbox(
                            checked = currentSelected.iconVisible,
                            onCheckedChange = { onComponentChanged(currentSelected.copy(iconVisible = it)) }
                        )
                    }

                    // Custom text formatting
                    Text(
                        text = when (currentSelected.type) {
                            ComponentType.LABEL_SHORTCUT -> "Текст ярлыка:"
                            ComponentType.CUSTOM_TEXT -> "Текст бренда:"
                            ComponentType.TIME -> "Тип (напишите SS для показа секунд):"
                            else -> "Префикс / Текстовый ярлык:"
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = currentSelected.textFormat,
                        onValueChange = { onComponentChanged(currentSelected.copy(textFormat = it)) },
                        placeholder = { Text("Например: ШАГИ или SS") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        } else {
            // Emptystate indicator properties panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(0.6f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Выберите элемент на холсте или списках, чтобы редактировать его положение, цвета, шрифты и фоновые подложки.",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
