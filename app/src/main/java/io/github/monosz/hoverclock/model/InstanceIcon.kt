package io.github.monosz.hoverclock.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.DirectionsTransit
import androidx.compose.material.icons.outlined.EmojiFoodBeverage
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VideogameAsset
import androidx.compose.ui.graphics.vector.ImageVector

enum class InstanceIcon {
    Schedule,
    Hourglass,
    Timer,
    Bedtime,
    Game,
    Brush,
    Music,
    Food,
    Drink,
    Fitness,
    Medication,
    Plant,
    Transport,
    ;

    fun imageVector(): ImageVector =
        when (this) {
            Schedule -> Icons.Outlined.Schedule
            Hourglass -> Icons.Outlined.HourglassEmpty
            Timer -> Icons.Outlined.Timer
            Bedtime -> Icons.Outlined.Bedtime
            Game -> Icons.Outlined.VideogameAsset
            Brush -> Icons.Outlined.Brush
            Music -> Icons.Outlined.MusicNote
            Food -> Icons.Outlined.Fastfood
            Drink -> Icons.Outlined.EmojiFoodBeverage
            Fitness -> Icons.Outlined.FitnessCenter
            Medication -> Icons.Outlined.Medication
            Plant -> Icons.Outlined.Grass
            Transport -> Icons.Outlined.DirectionsTransit
//        else -> Icons.Outlined.DirectionsTransit
        }

    companion object {
        val all: List<InstanceIcon> = entries

        fun defaultFor(mode: TimeMode): InstanceIcon =
            when (mode) {
                TimeMode.Clock -> Schedule
                TimeMode.Stopwatch -> Hourglass
                TimeMode.Timer -> Timer
            }

        fun fromName(
            name: String,
            fallbackMode: TimeMode,
        ): InstanceIcon = entries.firstOrNull { it.name == name } ?: defaultFor(fallbackMode)

        fun isValidName(name: String): Boolean = entries.any { it.name == name }
    }
}
