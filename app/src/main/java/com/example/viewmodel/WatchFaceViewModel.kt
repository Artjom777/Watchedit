package com.example.viewmodel

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class WatchFaceViewModel(private val repository: WatchFaceRepository) : ViewModel() {

    // List of all user projects
    val allProjects: StateFlow<List<WatchFaceEntity>> = repository.allWatchFaces
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current active project we are editing
    private val _currentProject = MutableStateFlow<WatchFaceEntity?>(null)
    val currentProject: StateFlow<WatchFaceEntity?> = _currentProject.asStateFlow()

    // Loaded list of components for real-time manipulation
    private val _components = MutableStateFlow<List<WatchComponent>>(emptyList())
    val components: StateFlow<List<WatchComponent>> = _components.asStateFlow()

    // Selected component ID in the editor workspace
    private val _selectedComponentId = MutableStateFlow<String?>(null)
    val selectedComponentId: StateFlow<String?> = _selectedComponentId.asStateFlow()

    // Available Custom Fonts loaded from local files
    private val _customFonts = MutableStateFlow<List<Pair<String, String>>>(emptyList()) // Name to Absolute Path
    val customFonts: StateFlow<List<Pair<String, String>>> = _customFonts.asStateFlow()

    // Mock states for watchface rendering preview
    val userMockSteps = MutableStateFlow(8420)
    val userMockBattery = MutableStateFlow(78)
    val userMockHeartRate = MutableStateFlow(76)
    val userMockCalories = MutableStateFlow(320)
    val userMockTemp = MutableStateFlow(23)

    init {
        // Build initial project if none exist on DB flow arrival
        viewModelScope.launch {
            allProjects.collect { list ->
                if (list.isEmpty() && _currentProject.value == null) {
                    createDefaultProject("My Redmi Watch Face", Presets.list.first())
                } else if (_currentProject.value == null && list.isNotEmpty()) {
                    loadProject(list.first())
                }
            }
        }
    }

    fun loadProject(project: WatchFaceEntity) {
        _currentProject.value = project
        _components.value = WatchFaceConverter.jsonToComponents(project.componentsJson)
        _selectedComponentId.value = _components.value.firstOrNull()?.id
    }

    suspend fun createDefaultProject(name: String, preset: WatchPreset) {
        val defaultComponents = WatchFaceConverter.createDefaultComponents(preset.width, preset.height)
        val entity = WatchFaceEntity(
            name = name,
            category = preset.category,
            watchModel = preset.name,
            resolutionWidth = preset.width,
            resolutionHeight = preset.height,
            isPill = preset.isPill,
            componentsJson = WatchFaceConverter.componentsToJson(defaultComponents),
            backgroundColor = 0xFF000000.toInt()
        )
        val id = repository.save(entity)
        val created = entity.copy(id = id.toInt())
        loadProject(created)
    }

    fun duplicateProject(context: Context, project: WatchFaceEntity) {
        viewModelScope.launch {
            val duplicate = project.copy(
                id = 0,
                name = "${project.name} (Copy)",
                lastModified = System.currentTimeMillis()
            )
            repository.save(duplicate)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Проект скопирован", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateResolution(width: Int, height: Int) {
        val curr = _currentProject.value ?: return
        val updated = curr.copy(
            resolutionWidth = width,
            resolutionHeight = height,
            lastModified = System.currentTimeMillis()
        )
        _currentProject.value = updated
        saveCurrentProject()
    }

    fun deleteProject(context: Context, id: Int) {
        viewModelScope.launch {
            repository.delete(id)
            val left = allProjects.value.filter { it.id != id }
            if (left.isNotEmpty()) {
                loadProject(left.first())
            } else {
                createDefaultProject("My Smart Watchface", Presets.list.first())
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Проект удален", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateSelectedComponent(id: String?) {
        _selectedComponentId.value = id
    }

    fun updateComponent(updated: WatchComponent) {
        val list = _components.value.map {
            if (it.id == updated.id) updated else it
        }
        _components.value = list
        saveCurrentProject()
    }

    // Canvas background modifier
    fun updateBackgroundColor(color: Int) {
        val curr = _currentProject.value ?: return
        val updated = curr.copy(
            backgroundColor = color,
            lastModified = System.currentTimeMillis()
        )
        _currentProject.value = updated
        saveCurrentProject()
    }

    fun updateBackgroundImage(uriString: String?) {
        val curr = _currentProject.value ?: return
        val updated = curr.copy(
            backgroundImageUri = uriString,
            lastModified = System.currentTimeMillis()
        )
        _currentProject.value = updated
        saveCurrentProject()
    }

    // Nudging position
    fun nudgeComponent(id: String, dx: Float, dy: Float) {
        val list = _components.value.map {
            if (it.id == id) {
                it.copy(x = it.x + dx, y = it.y + dy)
            } else it
        }
        _components.value = list
        saveCurrentProject()
    }

    // Add elements or customization
    fun addNewComponent(type: ComponentType, name: String) {
        val curr = _currentProject.value ?: return
        val newId = "${type.name.lowercase()}_${System.currentTimeMillis()}"
        val component = WatchComponent(
            id = newId,
            type = type,
            name = name,
            x = (curr.resolutionWidth / 3f),
            y = (curr.resolutionHeight / 2f),
            size = 20f,
            color = 0xFFFFFFFF.toInt()
        )
        _components.value = _components.value + component
        _selectedComponentId.value = newId
        saveCurrentProject()
    }

    fun removeComponent(id: String) {
        val list = _components.value.filterNot { it.id == id }
        _components.value = list
        if (_selectedComponentId.value == id) {
            _selectedComponentId.value = list.firstOrNull()?.id
        }
        saveCurrentProject()
    }

    fun loadCustomFonts(context: Context) {
        viewModelScope.launch {
            val destinationFolder = File(context.filesDir, "custom_fonts")
            if (destinationFolder.exists()) {
                val files = destinationFolder.listFiles() ?: emptyArray()
                val list = files.map { it.name to it.absolutePath }
                _customFonts.value = list
            }
        }
    }

    fun addCustomFontFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                var name = "font_${System.currentTimeMillis()}.ttf"
                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            val displayName = cursor.getString(nameIndex)
                            if (displayName.endsWith(".ttf") || displayName.endsWith(".otf")) {
                                name = displayName
                            }
                        }
                    }
                }

                val destinationFolder = File(context.filesDir, "custom_fonts")
                if (!destinationFolder.exists()) {
                    destinationFolder.mkdirs()
                }
                val targetFile = File(destinationFolder, name)
                resolver.openInputStream(uri)?.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // Reload fonts
                loadCustomFonts(context)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Шрифт $name успешно импортирован!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Ошибка импорта шрифта: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun saveCurrentProject() {
        val curr = _currentProject.value ?: return
        val currentJson = WatchFaceConverter.componentsToJson(_components.value)
        val updated = curr.copy(
            componentsJson = currentJson,
            lastModified = System.currentTimeMillis()
        )
        _currentProject.value = updated
        viewModelScope.launch {
            repository.save(updated)
        }
    }

    // Export function: Compiles watchface layout and instructions to public Downloads as ZIP
    fun exportProjectToDownloads(context: Context) {
        val project = _currentProject.value ?: return
        val filename = "watchface_${project.name.replace(" ", "_")}_bundle.zip"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Prepare content bytes
                val byteArrOutput = ByteArrayOutputStream()
                val zipOutput = ZipOutputStream(byteArrOutput)

                // 1. Save config JSON
                val configEntry = ZipEntry("watchface.json")
                zipOutput.putNextEntry(configEntry)
                val jsonBytes = WatchFaceConverter.componentsToJson(_components.value).toByteArray()
                zipOutput.write(jsonBytes)
                zipOutput.closeEntry()

                // 2. Save models text meta
                val metaEntry = ZipEntry("manifest.txt")
                zipOutput.putNextEntry(metaEntry)
                val metaContent = """
                    PROJECT NAME: ${project.name}
                    MODEL: ${project.watchModel}
                    RESOLUTION: ${project.resolutionWidth} x ${project.resolutionHeight}
                    CATEGORY: ${project.category}
                    DEVICE PILL SCREEN: ${project.isPill}
                    BACKGROUND COLOR ARGB: ${Integer.toHexString(project.backgroundColor)}
                    COMPONENTS COUNT: ${_components.value.size}
                    CREATED WITH: WatchFace Studio for Android
                """.trimIndent().toByteArray()
                zipOutput.write(metaContent)
                zipOutput.closeEntry()

                // 3. Save instructions (README.txt) in Russian as requested for local watchface tool loading instruction
                val instructionsEntry = ZipEntry("README_RU.txt")
                zipOutput.putNextEntry(instructionsEntry)
                val instructions = """
                    ИНСТРУКЦИЯ ПО УСТАНОВКЕ ЦИФЕРБЛАТА
                    ----------------------------------
                    Модель часов: ${project.watchModel} (разрешение ${project.resolutionWidth}x${project.resolutionHeight})

                    Этот ZIP-архив содержит структуру циферблата для импорта в кастомные прошивки или утилиты прошивки.
                    
                    Способ 1 (через Mi Fitness mod и сторонние редакторы):
                    1. Распакуйте файлы из этого архива.
                    2. Используйте приложение "Mi Band / Watch Face Installer" или кастомные утилиты разгрузки.
                    3. Поместите "watchface.json" в рабочую папку конструктора на ПК или в аналогичную папку Android-инсталлятора.
                    4. Скомпилируйте циферблат кнопкой упаковки (Pack).
                    5. Установите полученный .bin / .mpk файл на ваши смарт-часы Redmi / Mi Band через "Mi Fitness" (в режиме разработчика).
                    
                    Способ 2 (Кастомные циферблаты):
                    - Загрузите сконфигурированные ресурсы в сторонние приложения, такие как "Notify for Xiaomi" или "Mi Band Tools" для прямой заливки.
                    
                    Создано на Android в приложении "WatchFace Studio"!
                """.trimIndent().toByteArray(Charsets.UTF_8)
                zipOutput.write(instructions)
                zipOutput.closeEntry()

                zipOutput.close()

                val zipBytes = byteArrOutput.toByteArray()

                // Write to downloads directory using standard MediaStore API
                val resolver = context.contentResolver
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { output ->
                            output.write(zipBytes)
                        }
                    } else {
                        throw Exception("Failed to create MediaStore entry in Downloads")
                    }
                } else {
                    val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!path.exists()) {
                        path.mkdirs()
                    }
                    val targetFile = File(path, filename)
                    FileOutputStream(targetFile).use { fos ->
                        fos.write(zipBytes)
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Успешно экспортировано! Проверьте папку Загрузки/ (Downloads) -> $filename",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Ошибка экспорта: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

class WatchFaceViewModelFactory(private val repository: WatchFaceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WatchFaceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WatchFaceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
