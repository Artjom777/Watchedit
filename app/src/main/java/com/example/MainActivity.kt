package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.components.*
import com.example.viewmodel.WatchFaceViewModel
import com.example.viewmodel.WatchFaceViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Init databases and providers
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = WatchFaceRepository(database.watchFaceDao())
        val factory = WatchFaceViewModelFactory(repository)

        setContent {
            // High-contrasting beautiful Dark Space theme for a PC-analogue editing feel
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF0A84FF), // Indigo / iOS neon blue
                    secondary = Color(0xFF30D158), // Emerald green
                    tertiary = Color(0xFFBF5AF2), // Radiant magenta
                    background = Color(0xFF0F0F14), // Luxury obsidian
                    surface = Color(0xFF1C1C24), // Slate card UI
                    surfaceVariant = Color(0xFF2C2C35), // Hover state highlights
                    onBackground = Color(0xFFF2F2F7),
                    onSurface = Color(0xFFFFFFFF)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WatchFaceStudioApp(factory = factory)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchFaceStudioApp(
    factory: WatchFaceViewModelFactory,
    viewModel: WatchFaceViewModel = viewModel(factory = factory)
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Observe core view states
    val currentProject by viewModel.currentProject.collectAsStateWithLifecycle()
    val components by viewModel.components.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedComponentId.collectAsStateWithLifecycle()
    val customFonts by viewModel.customFonts.collectAsStateWithLifecycle()
    val projectsList by viewModel.allProjects.collectAsStateWithLifecycle()

    // Mock preview values states (to change values under Preview and watch design stretch)
    val mockSteps by viewModel.userMockSteps.collectAsStateWithLifecycle()
    val mockBattery by viewModel.userMockBattery.collectAsStateWithLifecycle()
    val mockHeartRate by viewModel.userMockHeartRate.collectAsStateWithLifecycle()
    val mockCalories by viewModel.userMockCalories.collectAsStateWithLifecycle()
    val mockTemp by viewModel.userMockTemp.collectAsStateWithLifecycle()

    // Bottom Tab Selector (Mobile Navigation: Workspace, Properties, Projects)
    var currentTab by remember { mutableStateOf(0) } // 0 = Canvas & Joystick, 1 = Elements Properties, 2 = Projects/Resolution

    // Automatically trigger ticker updates for real-time clock preview!
    var clockTicker by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        viewModel.loadCustomFonts(context)
        while (true) {
            delay(1000)
            clockTicker = System.currentTimeMillis()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "WatchFace Studio",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                actions = {
                    // EXPORT ZIP BUNDLE TO PUBLIC DOWNLOADS FOLDER (PC Analogue highlight feature!)
                    Button(
                        onClick = { viewModel.exportProjectToDownloads(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.padding(end = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Экспорт циферблата", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Экспорт", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(0.95f)
                )
            )
        },
        bottomBar = {
            // Beautiful interactive M3 navigation tabs bar
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Build, contentDescription = "Конструктор") },
                    label = { Text("Конструктор", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Edit, contentDescription = "Параметры") },
                    label = { Text("Параметры", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Шаблоны & Проекты") },
                    label = { Text("Проекты", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { innerPadding ->
        // Responsive Layout Divider: If on tablet (horizontal/wide) we display side-by-side. 
        // Otherwise on portable phones we divide vertical space cleanly.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            Color(0xFF030305)
                        )
                    )
                )
        ) {
            val isWideScreen = maxWidth > 680.dp

            if (isWideScreen) {
                // Large Tablet/Desktop View Style
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Column: Watch face center canvas layout viewport
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🖥️ РЕДАКТОР ЦИФЕРБЛАТА (ПЕРЕТАСКИВАЙТЕ ЧАСТИ)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        currentProject?.let { proj ->
                            // Render watch canvas in the center window
                            WatchCanvas(
                                resolutionWidth = proj.resolutionWidth,
                                resolutionHeight = proj.resolutionHeight,
                                isPill = proj.isPill,
                                backgroundColor = proj.backgroundColor,
                                components = components,
                                selectedComponentId = selectedId,
                                onSelectComponent = { viewModel.updateSelectedComponent(it) },
                                onMoveComponent = { id, dx, dy -> viewModel.nudgeComponent(id, dx, dy) },
                                mockSteps = mockSteps,
                                mockBattery = mockBattery,
                                mockHeartRate = mockHeartRate,
                                mockCalories = mockCalories,
                                mockTemp = mockTemp,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Bottom telemetry editor presets (to adjust state metrics in live preview)
                        TelemetryModifierWidget(
                            steps = mockSteps,
                            onStepsChange = { viewModel.userMockSteps.value = it },
                            battery = mockBattery,
                            onBatteryChange = { viewModel.userMockBattery.value = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    VerticalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))

                    // Right Column: Split control panels
                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                    ) {
                        TabRow(
                            selectedTabIndex = currentTab,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Tab(selected = currentTab == 0 || currentTab == 1, onClick = { currentTab = 1 }) {
                                Text("Элементы", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                            }
                            Tab(selected = currentTab == 2, onClick = { currentTab = 2 }) {
                                Text("Проекты и Разрешения", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            if (currentTab == 0 || currentTab == 1) {
                                PropertiesPanel(
                                    components = components,
                                    selectedId = selectedId,
                                    customFonts = customFonts,
                                    onComponentChanged = { viewModel.updateComponent(it) },
                                    onAddNewComponent = { type, label -> viewModel.addNewComponent(type, label) },
                                    onRemoveComponent = { viewModel.removeComponent(it) },
                                    onCustomFontSelected = { viewModel.addCustomFontFromUri(context, it) }
                                )
                            } else {
                                ProjectManager(
                                    projects = projectsList,
                                    currentProject = currentProject,
                                    onLoadProject = { viewModel.loadProject(it) },
                                    onCreateProject = { name, preset ->
                                        coroutineScope.launch {
                                            viewModel.createDefaultProject(name, preset)
                                        }
                                    },
                                    onDuplicateProject = { viewModel.duplicateProject(context, it) },
                                    onDeleteProject = { viewModel.deleteProject(context, it) },
                                    onUpdateResolution = { w, h -> viewModel.updateResolution(w, h) },
                                    onUpdateBackgroundColor = { viewModel.updateBackgroundColor(it) }
                                )
                            }
                        }
                    }
                }
            } else {
                // Mobile Portrait Space Optimizing Columns Layout
                Column(modifier = Modifier.fillMaxSize()) {
                    // Mobile static Top area: Live Preview Watchface constructor viewport (always visible!)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.05f)
                            .background(Color.Black.copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        currentProject?.let { proj ->
                            WatchCanvas(
                                resolutionWidth = proj.resolutionWidth,
                                resolutionHeight = proj.resolutionHeight,
                                isPill = proj.isPill,
                                backgroundColor = proj.backgroundColor,
                                components = components,
                                selectedComponentId = selectedId,
                                onSelectComponent = { viewModel.updateSelectedComponent(it) },
                                onMoveComponent = { id, dx, dy -> viewModel.nudgeComponent(id, dx, dy) },
                                mockSteps = mockSteps,
                                mockBattery = mockBattery,
                                mockHeartRate = mockHeartRate,
                                mockCalories = mockCalories,
                                mockTemp = mockTemp
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))

                    // Mobile dynamic Bottom pane area controlled via the tabs selector
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.1f)
                    ) {
                        when (currentTab) {
                            0 -> {
                                // Workspace helper: shows selected element's name and quick move joysticks nudgers
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.2f))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🎯 Конструктор холста",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        AssistChip(
                                            onClick = {},
                                            label = { Text("Перетаскивайте пальцем") },
                                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                        )
                                    }

                                    TelemetryModifierWidget(
                                        steps = mockSteps,
                                        onStepsChange = { viewModel.userMockSteps.value = it },
                                        battery = mockBattery,
                                        onBatteryChange = { viewModel.userMockBattery.value = it }
                                    )

                                    if (selectedId != null && components.any { it.id == selectedId }) {
                                        val activeItem = components.first { it.id == selectedId }
                                        Text(
                                            text = "Выбран: ${activeItem.name} • Координаты: X:${activeItem.x.toInt()} Y:${activeItem.y.toInt()}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        
                                        // Simple rapid joystick controls
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(onClick = { viewModel.nudgeComponent(activeItem.id, -5f, 0f) }) {
                                                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null)
                                            }
                                            IconButton(onClick = { viewModel.nudgeComponent(activeItem.id, 0f, -5f) }) {
                                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                                            }
                                            IconButton(onClick = { viewModel.nudgeComponent(activeItem.id, 0f, 5f) }) {
                                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                                            }
                                            IconButton(onClick = { viewModel.nudgeComponent(activeItem.id, 5f, 0f) }) {
                                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                                            }
                                            Spacer(Modifier.width(16.dp))
                                            TextButton(onClick = { currentTab = 1 }) {
                                                Text("Подробнее...", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surface.copy(0.4f), RoundedCornerShape(12.dp))
                                                .padding(20.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Коснитесь любой детали на циферблате вверху для быстрого редактирования или перетащите её!",
                                                fontSize = 12.sp,
                                                color = Color.Gray,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                            1 -> {
                                PropertiesPanel(
                                    components = components,
                                    selectedId = selectedId,
                                    customFonts = customFonts,
                                    onComponentChanged = { viewModel.updateComponent(it) },
                                    onAddNewComponent = { type, label -> viewModel.addNewComponent(type, label) },
                                    onRemoveComponent = { viewModel.removeComponent(it) },
                                    onCustomFontSelected = { viewModel.addCustomFontFromUri(context, it) }
                                )
                            }
                            2 -> {
                                ProjectManager(
                                    projects = projectsList,
                                    currentProject = currentProject,
                                    onLoadProject = { viewModel.loadProject(it) },
                                    onCreateProject = { name, preset ->
                                        coroutineScope.launch {
                                            viewModel.createDefaultProject(name, preset)
                                        }
                                    },
                                    onDuplicateProject = { viewModel.duplicateProject(context, it) },
                                    onDeleteProject = { viewModel.deleteProject(context, it) },
                                    onUpdateResolution = { w, h -> viewModel.updateResolution(w, h) },
                                    onUpdateBackgroundColor = { viewModel.updateBackgroundColor(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Compact widget helper underneath the preview screen to mock steps/battery stats triggers
@Composable
fun TelemetryModifierWidget(
    steps: Int,
    onStepsChange: (Int) -> Unit,
    battery: Int,
    onBatteryChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(0.6f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⚡ Симулятор значений датчиков (Слайдеры для теста):",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Шаги: $steps", fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    Slider(
                        value = steps.toFloat(),
                        onValueChange = { onStepsChange(it.toInt()) },
                        valueRange = 0f..20000f,
                        modifier = Modifier.height(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Батарея: $battery%", fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    Slider(
                        value = battery.toFloat(),
                        onValueChange = { onBatteryChange(it.toInt()) },
                        valueRange = 0f..100f,
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
        }
    }
}
