package com.example.sensor

import androidx.compose.ui.graphics.Color

enum class HeatmapMetric(
    val titleRu: String,
    val unit: String,
    val descriptionRu: String,
    val defaultMin: Float,
    val defaultMax: Float
) {
    MAGNETIC_FIELD(
        titleRu = "Магнитное поле (EMF)",
        unit = "µT",
        descriptionRu = "Обнаружение магнитных аномалий, проводки и металла",
        defaultMin = 20f,
        defaultMax = 120f
    ),
    ACCELEROMETER(
        titleRu = "Вибрации / G-Force",
        unit = "м/с²",
        descriptionRu = "Ускорение, удары, шаги и тряска",
        defaultMin = 9.8f,
        defaultMax = 25f
    ),
    GYROSCOPE(
        titleRu = "Угловая скорость",
        unit = "рад/с",
        descriptionRu = "Интенсивность вращения и поворотов",
        defaultMin = 0f,
        defaultMax = 6f
    ),
    AMBIENT_LIGHT(
        titleRu = "Освещенность",
        unit = "Lux",
        descriptionRu = "Карта теней и источников света",
        defaultMin = 0f,
        defaultMax = 1000f
    ),
    BAROMETRIC_PRESSURE(
        titleRu = "Атмосферное давление",
        unit = "hPa",
        descriptionRu = "Микро-перепады высоты и давления воздуха",
        defaultMin = 980f,
        defaultMax = 1040f
    ),
    GPS_SPEED(
        titleRu = "GPS Скорость",
        unit = "км/ч",
        descriptionRu = "Скорость пространственного перемещения",
        defaultMin = 0f,
        defaultMax = 40f
    ),
    COMPOSITE_STRESS(
        titleRu = "Комплексный индекс",
        unit = "баллов",
        descriptionRu = "Суммарный индекс движения и электромагнитных полей",
        defaultMin = 0f,
        defaultMax = 100f
    );

    fun formatValue(value: Float): String {
        return when (this) {
            BAROMETRIC_PRESSURE -> String.format("%.1f %s", value, unit)
            AMBIENT_LIGHT -> String.format("%.0f %s", value, unit)
            MAGNETIC_FIELD -> String.format("%.1f %s", value, unit)
            GPS_SPEED -> String.format("%.1f %s", value, unit)
            else -> String.format("%.2f %s", value, unit)
        }
    }
}

enum class Render3DMode(val titleRu: String) {
    TERRAIN_SURFACE("3D Рельефная сетка"),
    VOXEL_COLUMNS("3D Столбцы (Воксели)"),
    POINT_CLOUD("3D Точечное облако"),
    CONTOUR_2D("2D Тепловой срез")
}

enum class ColorPalette(val titleRu: String, val colors: List<Color>) {
    CYBER_NEON(
        titleRu = "Кибернеон",
        colors = listOf(
            Color(0xFF0D1B2A), // Deep Navy
            Color(0xFF00B4D8), // Cyan
            Color(0xFF06D6A0), // Emerald
            Color(0xFFFFD166), // Yellow
            Color(0xFFEF476F), // Pink / Coral
            Color(0xFFFF0054)  // Hot Red
        )
    ),
    THERMAL_FLIR(
        titleRu = "Тепловизор FLIR",
        colors = listOf(
            Color(0xFF03045E), // Deep Blue
            Color(0xFF5A189A), // Purple
            Color(0xFFE85D04), // Orange
            Color(0xFFFFBA08), // Yellow
            Color(0xFFFFFFFF)  // White Hot
        )
    ),
    ACID_MATRIX(
        titleRu = "Биохазард",
        colors = listOf(
            Color(0xFF0A0F0D),
            Color(0xFF1B4965),
            Color(0xFF2EC4B6),
            Color(0xFF70E000),
            Color(0xFFFFFF3F)
        )
    ),
    PLASMA(
        titleRu = "Плазма",
        colors = listOf(
            Color(0xFF1A0033),
            Color(0xFF7209B7),
            Color(0xFFF72585),
            Color(0xFFFF9E00),
            Color(0xFFFFF01F)
        )
    )
}
