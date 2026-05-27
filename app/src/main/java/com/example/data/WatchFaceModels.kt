package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

// Watch presets definition
data class WatchPreset(
    val name: String,
    val category: String, // "Redmi Watch" or "Smart Band / Mi Fitness" or "Custom"
    val width: Int,
    val height: Int,
    val isPill: Boolean = false // Is it dynamic pill shape like bands or rectangular?
)

object Presets {
    val list = listOf(
        WatchPreset("Redmi Watch 5", "Redmi Watch", 390, 450, isPill = false),
        WatchPreset("Redmi Watch 5 Active", "Redmi Watch", 320, 385, isPill = false),
        WatchPreset("Redmi Watch 5 Lite", "Redmi Watch", 390, 450, isPill = false),
        WatchPreset("Mi Fitness Band 8", "Smart Band / Mi Fitness", 192, 490, isPill = true),
        WatchPreset("Mi Fitness Band 9", "Smart Band / Mi Fitness", 192, 490, isPill = true),
        WatchPreset("Mi Fitness Band 10", "Smart Band / Mi Fitness", 206, 494, isPill = true),
        WatchPreset("Custom Resolution", "Custom", 320, 320, isPill = false)
    )
}

enum class ComponentType {
    TIME,
    DATE,
    STEPS,
    CALORIES,
    HEART_RATE,
    BATTERY,
    WEATHER,
    LABEL_SHORTCUT,
    CUSTOM_TEXT
}

data class WatchComponent(
    val id: String,
    val type: ComponentType,
    val name: String,
    var x: Float,          // X position (relative to canvas pixels, e.g. 0 to width)
    var y: Float,          // Y position (relative to canvas pixels, e.g. 0 to height)
    var size: Float = 24f, // Font size or main dimension
    var color: Int = 0xFFFFFFFF.toInt(), // ARGB
    var fontName: String = "Default", // "Default", "Digital", "Tech", "Space", "Custom"
    var customFontPath: String? = null, // Stored internal file path for loaded TTF
    var isVisible: Boolean = true,
    var customLabelBackground: Int? = null, // ARGB for widget pill highlight
    var customLabelRadius: Float = 12f,
    var customLabelPadding: Float = 8f,
    var iconVisible: Boolean = true,
    var textFormat: String = "" // Optional custom text prefix/suffix support
)

@Entity(tableName = "watchfaces")
data class WatchFaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String,
    val watchModel: String,
    val resolutionWidth: Int,
    val resolutionHeight: Int,
    val isPill: Boolean,
    val backgroundColor: Int = 0xFF000000.toInt(),
    val backgroundImageUri: String? = null,
    val componentsJson: String, // Serialized list of WatchComponent
    val lastModified: Long = System.currentTimeMillis()
)

object WatchFaceConverter {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, WatchComponent::class.java)
    private val adapter = moshi.adapter<List<WatchComponent>>(listType)

    fun jsonToComponents(json: String?): List<WatchComponent> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun componentsToJson(components: List<WatchComponent>): String {
        return adapter.toJson(components)
    }

    fun createDefaultComponents(width: Int, height: Int): List<WatchComponent> {
        return listOf(
            WatchComponent(
                id = "time",
                type = ComponentType.TIME,
                name = "Time (ЧЧ:ММ)",
                x = (width * 0.15f),
                y = (height * 0.25f),
                size = 48f,
                color = 0xFFFFFFFF.toInt(),
                fontName = "Digital"
            ),
            WatchComponent(
                id = "date",
                type = ComponentType.DATE,
                name = "Date (Дата)",
                x = (width * 0.25f),
                y = (height * 0.15f),
                size = 18f,
                color = 0xFF8E8E93.toInt()
            ),
            WatchComponent(
                id = "steps",
                type = ComponentType.STEPS,
                name = "Steps (Шаги)",
                x = (width * 0.15f),
                y = (height * 0.45f),
                size = 18f,
                color = 0xFF30D158.toInt(),
                customLabelBackground = 0x2230D158,
                customLabelRadius = 16f
            ),
            WatchComponent(
                id = "battery",
                type = ComponentType.BATTERY,
                name = "Battery (Батарея)",
                x = (width * 0.15f),
                y = (height * 0.60f),
                size = 16f,
                color = 0xFFFFD60A.toInt(),
                customLabelBackground = 0x22FFD60A,
                customLabelRadius = 16f
            ),
            WatchComponent(
                id = "heart_rate",
                type = ComponentType.HEART_RATE,
                name = "Heart Rate (Пульс)",
                x = (width * 0.15f),
                y = (height * 0.72f),
                size = 16f,
                color = 0xFFFF453A.toInt()
            ),
            WatchComponent(
                id = "weather",
                type = ComponentType.WEATHER,
                name = "Weather (Погода)",
                x = (width * 0.55f),
                y = (height * 0.72f),
                size = 16f,
                color = 0xFF64D2FF.toInt()
            )
        )
    }
}
