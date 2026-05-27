package com.example.ui.components

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ComponentType
import com.example.data.WatchComponent
import java.io.File
import java.util.Calendar

// Helper to determine text to show based on system time or preset demo values
@Composable
fun getFormattedComponentText(
    component: WatchComponent,
    mockSteps: Int,
    mockBattery: Int,
    mockHeartRate: Int,
    mockCalories: Int,
    mockTemp: Int
): String {
    return when (component.type) {
        ComponentType.TIME -> {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
            val min = calendar.get(Calendar.MINUTE).toString().padStart(2, '0')
            if (component.textFormat.contains("SS") || component.textFormat.lowercase().contains("с")) {
                val sec = calendar.get(Calendar.SECOND).toString().padStart(2, '0')
                "$hour:$min:$sec"
            } else {
                "$hour:$min"
            }
        }
        ComponentType.DATE -> {
            val daysRu = listOf("ВС", "ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ")
            val monthsRu = listOf("ЯНВ", "ФЕВ", "МАР", "АПР", "МАЙ", "ИЮН", "ИЮЛ", "АВГ", "СЕН", "ОКТ", "НОЯ", "ДЕК")
            val calendar = Calendar.getInstance()
            val dayOfWeek = daysRu[calendar.get(Calendar.DAY_OF_WEEK) - 1]
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            val month = monthsRu[calendar.get(Calendar.MONTH)]
            "$dayOfWeek, $dayOfMonth $month"
        }
        ComponentType.STEPS -> {
            val prefix = if (component.textFormat.isNotEmpty()) "${component.textFormat} " else ""
            "$prefix$mockSteps"
        }
        ComponentType.CALORIES -> {
            val suffix = if (component.textFormat.isNotEmpty()) " ${component.textFormat}" else " ккал"
            "$mockCalories$suffix"
        }
        ComponentType.HEART_RATE -> {
            "$mockHeartRate уд/м"
        }
        ComponentType.BATTERY -> {
            "$mockBattery%"
        }
        ComponentType.WEATHER -> {
            "$mockTemp°C"
        }
        ComponentType.LABEL_SHORTCUT -> {
            component.textFormat.ifEmpty { "Старт" }
        }
        ComponentType.CUSTOM_TEXT -> {
            component.textFormat.ifEmpty { "WATCH" }
        }
    }
}

@Composable
fun getComponentIcon(type: ComponentType): @Composable (() -> Unit)? {
    return when (type) {
        ComponentType.STEPS -> { { Icon(Icons.Default.Star, contentDescription = "Шаги", tint = Color(0xFF30D158), modifier = Modifier.size(16.dp)) } }
        ComponentType.CALORIES -> { { Icon(Icons.Default.Star, contentDescription = "Калории", tint = Color(0xFFFF9500), modifier = Modifier.size(16.dp)) } }
        ComponentType.HEART_RATE -> { { Icon(Icons.Default.Favorite, contentDescription = "Пульс", tint = Color(0xFFFF453A), modifier = Modifier.size(16.dp)) } }
        ComponentType.BATTERY -> { { Icon(Icons.Default.Info, contentDescription = "Заряд", tint = Color(0xFFFFD60A), modifier = Modifier.size(16.dp)) } }
        ComponentType.WEATHER -> { { Icon(Icons.Default.Star, contentDescription = "Погода", tint = Color(0xFF64D2FF), modifier = Modifier.size(16.dp)) } }
        ComponentType.LABEL_SHORTCUT -> { { Icon(Icons.Default.PlayArrow, contentDescription = "Ярлык", tint = Color.White, modifier = Modifier.size(14.dp)) } }
        else -> null
    }
}

// Generate fontFamily safely from asset or internal storage
fun loadTypefaceSafely(fontName: String, path: String?): FontFamily {
    if (fontName == "Custom" && !path.isNullOrEmpty()) {
        val file = File(path)
        if (file.exists()) {
            return try {
                val tf = Typeface.createFromFile(file)
                FontFamily(tf)
            } catch (e: Exception) {
                FontFamily.Default
            }
        }
    }
    return when (fontName) {
        "Digital" -> FontFamily.Monospace
        "Tech" -> FontFamily.Monospace
        "Space" -> FontFamily.SansSerif
        else -> FontFamily.Default
    }
}

@Composable
fun WatchCanvas(
    modifier: Modifier = Modifier,
    resolutionWidth: Int,
    resolutionHeight: Int,
    isPill: Boolean,
    backgroundColor: Int,
    components: List<WatchComponent>,
    selectedComponentId: String?,
    onSelectComponent: (String) -> Unit,
    onMoveComponent: (String, Float, Float) -> Unit, // dx, dy in watchface space
    mockSteps: Int,
    mockBattery: Int,
    mockHeartRate: Int,
    mockCalories: Int,
    mockTemp: Int
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Compute layout dimensions for screen rendering preview scale
        val maxCanvasWidth = maxWidth * 0.85f
        val maxCanvasHeight = maxHeight * 0.80f

        val ratioWidth = maxCanvasWidth.value / resolutionWidth.toFloat()
        val ratioHeight = maxCanvasHeight.value / resolutionHeight.toFloat()
        val canvasScale = minOf(ratioWidth, ratioHeight)

        val renderedWidthDp = (resolutionWidth * canvasScale).dp
        val renderedHeightDp = (resolutionHeight * canvasScale).dp

        // Outer watch hardware bezel simulator
        val watchBezelShape = if (isPill) RoundedCornerShape(percent = 50) else RoundedCornerShape(32.dp)
        
        Box(
            modifier = Modifier
                .width(renderedWidthDp + 28.dp)
                .height(renderedHeightDp + 28.dp)
                .shadow(16.dp, watchBezelShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF2C2C2E), Color(0xFF1C1C1E))
                    ),
                    shape = watchBezelShape
                )
                .border(2.5.dp, Color(0xFF48484A), watchBezelShape)
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            // Actual watch face screen viewport (Canvas)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(watchBezelShape)
                    .clipToBounds()
                    .background(Color(backgroundColor))
            ) {
                // Background grid lines in editor (for technical precision layout feel)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            // Tap background to clear selection if wanted - or simple click catcher
                        }
                ) {
                    // Predefined subtle dot grid guide lines
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        repeat(5) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                repeat(5) {
                                    Box(
                                        modifier = Modifier
                                            .size(2.dp)
                                            .background(Color.White.copy(0.08f), RoundedCornerShape(1.dp))
                                    )
                                }
                            }
                        }
                    }
                }

                // Render each visible watch component
                components.forEach { comp ->
                    if (comp.isVisible) {
                        key(comp.id) {
                            val isSelected = selectedComponentId == comp.id
                            val textValue = getFormattedComponentText(
                                comp, mockSteps, mockBattery, mockHeartRate, mockCalories, mockTemp
                            )
                            val icon = getComponentIcon(comp.type)
                            val itemFontFamily = loadTypefaceSafely(comp.fontName, comp.customFontPath)

                            // Translate relative coordinates back and forth based on canvas scale factor
                            val itemX = (comp.x * canvasScale).dp
                            val itemY = (comp.y * canvasScale).dp
                            val itemFontSize = (comp.size * canvasScale).sp

                            Box(
                                modifier = Modifier
                                    .absoluteOffset(x = itemX, y = itemY)
                                    .pointerInput(comp.id) {
                                        detectDragGestures(
                                            onDragStart = {
                                                onSelectComponent(comp.id)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                // Convert drag displacement on display into coordinate adjustment values relative to template resolution
                                                val dx = dragAmount.x / canvasScale
                                                val dy = dragAmount.y / canvasScale
                                                onMoveComponent(comp.id, dx, dy)
                                            }
                                        )
                                    }
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        onSelectComponent(comp.id)
                                    }
                                    // Draw a beautiful glowing border if component is selected for editing
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(if (isSelected) 4.dp else 0.dp)
                            ) {
                                // Widget outer container (handles custom backgrounds of widgets/shortcuts if colored)
                                val widgetShape = RoundedCornerShape((comp.customLabelRadius * canvasScale).dp)
                                Row(
                                    modifier = Modifier
                                        .background(
                                            color = comp.customLabelBackground?.let { Color(it) } ?: Color.Transparent,
                                            shape = widgetShape
                                        )
                                        .padding(
                                            horizontal = if (comp.customLabelBackground != null) (comp.customLabelPadding * canvasScale).dp else 0.dp,
                                            vertical = if (comp.customLabelBackground != null) (comp.customLabelPadding * 0.5f * canvasScale).dp else 0.dp
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (comp.iconVisible && icon != null) {
                                        Box(modifier = Modifier.padding(end = 4.dp)) {
                                            icon()
                                        }
                                    }
                                    Text(
                                        text = textValue,
                                        color = Color(comp.color),
                                        fontSize = itemFontSize,
                                        fontFamily = itemFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Render custom watch screen boundary resolution indicators (subtle)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color.White.copy(0.12f), watchBezelShape)
                ) {
                    Text(
                        text = "${resolutionWidth}x${resolutionHeight}",
                        color = Color.White.copy(0.35f),
                        fontSize = 11.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                    )
                }
            }
        }
    }
}
