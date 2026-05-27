package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Presets
import com.example.data.WatchFaceEntity
import com.example.data.WatchPreset

@Composable
fun ProjectManager(
    modifier: Modifier = Modifier,
    projects: List<WatchFaceEntity>,
    currentProject: WatchFaceEntity?,
    onLoadProject: (WatchFaceEntity) -> Unit,
    onCreateProject: (String, WatchPreset) -> Unit,
    onDuplicateProject: (WatchFaceEntity) -> Unit,
    onDeleteProject: (Int) -> Unit,
    onUpdateResolution: (Int, Int) -> Unit,
    onUpdateBackgroundColor: (Int) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showNewProjectDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf(Presets.list.first()) }

    var customWidthStr by remember { mutableStateOf("320") }
    var customHeightStr by remember { mutableStateOf("320") }
    var customIsPill by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.2f))
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📁 Проекты циферблатов",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Button(
                onClick = { 
                    newProjectName = "Циферблат ${projects.size + 1}"
                    showNewProjectDialog = true 
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Новый", fontSize = 13.sp)
            }
        }

        // --- CURRENT EDITING PROJECT ACTIONS ---
        if (currentProject != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "✏️ Текущий проект",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.8f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentProject.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${currentProject.watchModel} • ${currentProject.resolutionWidth}x${currentProject.resolutionHeight}px",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f)
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.12f))

                    // BACKGROUND PALETTE FOR CHOSEN WATCHFACE (PC Analogue requirement!)
                    Text(
                        text = "🌌 Цвет фона экрана",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    val bgPresets = listOf(
                        0xFF000000.toInt(), // Black
                        0xFF121212.toInt(), // Deep Gray
                        0xFF0F1E36.toInt(), // Deep Blue
                        0xFF1A1F1C.toInt(), // Space Green
                        0xFF2A1010.toInt(), // Deep Burgundy
                        0xFF1F102A.toInt(), // Cyber Purple
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        bgPresets.forEach { colorInt ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(colorInt))
                                    .border(
                                        width = if (currentProject.backgroundColor == colorInt) 2.5.dp else 1.dp,
                                        color = if (currentProject.backgroundColor == colorInt) MaterialTheme.colorScheme.primary else Color.White.copy(0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        onUpdateBackgroundColor(colorInt)
                                    }
                            )
                        }
                    }

                    // MANUAL RESOLUTION CHANGE (Requested: "с выбором разрешения пикселей вручную")
                    Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.12f))
                    Text(
                        text = "📐 Изменить разрешение вручную:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    var editWidth by remember(currentProject.id) { mutableStateOf(currentProject.resolutionWidth.toString()) }
                    var editHeight by remember(currentProject.id) { mutableStateOf(currentProject.resolutionHeight.toString()) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = editWidth,
                            onValueChange = { editWidth = it },
                            label = { Text("Ширина (W)", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = editHeight,
                            onValueChange = { editHeight = it },
                            label = { Text("Высота (H)", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                val w = editWidth.toIntOrNull() ?: currentProject.resolutionWidth
                                val h = editHeight.toIntOrNull() ?: currentProject.resolutionHeight
                                onUpdateResolution(w, h)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("ОК", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // --- PROJECTS ARCHIVE LIST ---
        Text(
            text = "📁 Ваши сохраненные дизайны:",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        if (projects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Нет созданных проектов. Нажмите кнопку 'Новый' вверху.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Group and list all projects
            projects.forEach { proj ->
                val isCurrent = proj.id == currentProject?.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLoadProject(proj) }
                        .border(
                            width = if (isCurrent) 2.dp else 0.dp,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = proj.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Unspecified
                            )
                            Text(
                                text = "${proj.category} • ${proj.watchModel}\n${proj.resolutionWidth}x${proj.resolutionHeight} px",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 13.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Copy draft icon
                            IconButton(
                                onClick = { onDuplicateProject(proj) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Дублировать", modifier = Modifier.size(18.dp))
                            }

                            // Delete icon (only allow delete if not the only one left)
                            IconButton(
                                onClick = { onDeleteProject(proj.id) },
                                modifier = Modifier.size(36.dp),
                                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG: CREATE NEW PROJECT ---
    if (showNewProjectDialog) {
        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            title = { Text("Новый циферблат", fontWeight = FontWeight.Bold) },
            text = {
                val presetScroll = rememberScrollState()
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(presetScroll)
                ) {
                    OutlinedTextField(
                        value = newProjectName,
                        onValueChange = { newProjectName = it },
                        label = { Text("Название проекта") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text(
                        text = "Выберите шаблон часов:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Group by categories
                    val presetsByCategory = Presets.list.groupBy { it.category }
                    presetsByCategory.forEach { (catName, presetList) ->
                        Text(
                            text = "🏷️ $catName",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        presetList.forEach { preset ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPreset = preset
                                        if (preset.name == "Custom Resolution") {
                                            customWidthStr = "320"
                                            customHeightStr = "320"
                                        }
                                    }
                                    .border(
                                        width = if (selectedPreset.name == preset.name) 1.5.dp else 1.dp,
                                        color = if (selectedPreset.name == preset.name) MaterialTheme.colorScheme.primary else Color.LightGray.copy(0.4f),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedPreset.name == preset.name) MaterialTheme.colorScheme.primaryContainer.copy(0.4f) else Color.Transparent
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(preset.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Разрешение: ${preset.width}x${preset.height} px", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    if (preset.isPill) {
                                        AssistChip(onClick = {}, label = { Text("Вытянутый", fontSize = 9.sp) })
                                    }
                                }
                            }
                        }
                    }

                    // Fine grain settings for manual custom templates (Requested!)
                    if (selectedPreset.name == "Custom Resolution") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.2f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Настройка кастомного разрешения:", fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = customWidthStr,
                                        onValueChange = { customWidthStr = it },
                                        label = { Text("Ширина (px)", fontSize = 10.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = customHeightStr,
                                        onValueChange = { customHeightStr = it },
                                        label = { Text("Высота (px)", fontSize = 10.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Форма браслета (овальный/Pill)", fontSize = 11.sp)
                                    Switch(
                                        checked = customIsPill,
                                        onCheckedChange = { customIsPill = it }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalPreset = if (selectedPreset.name == "Custom Resolution") {
                            val w = customWidthStr.toIntOrNull() ?: 320
                            val h = customHeightStr.toIntOrNull() ?: 320
                            WatchPreset(
                                name = "Кастомные часы",
                                category = "Custom",
                                width = w,
                                height = h,
                                isPill = customIsPill
                            )
                        } else {
                            selectedPreset
                        }

                        onCreateProject(
                            newProjectName.ifEmpty { "Мой циферблат" },
                            finalPreset
                        )
                        showNewProjectDialog = false
                    }
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
