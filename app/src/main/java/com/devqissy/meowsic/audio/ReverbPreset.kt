package com.devqissy.meowsic.audio

import androidx.annotation.StringRes
import com.devqissy.meowsic.R

enum class ReverbPreset(val id: String, @StringRes val labelRes: Int) {
    NONE("no_reverb", R.string.no_reverb),
    STUDIO("studio", R.string.studio),
    SMALL_ROOM("small_room", R.string.small_room),
    MEDIUM_ROOM("medium_room", R.string.medium_room),
    LARGE_ROOM("large_room", R.string.large_room),
    CONCERT_HALL("concert_hall", R.string.concert_hall),
    LARGE_HALL("large_hall", R.string.large_hall),
    OPEN_SPACE("open_space", R.string.open_space),
    CAVE("cave", R.string.cave);

    companion object {
        fun fromId(id: String?): ReverbPreset =
            entries.firstOrNull { it.id == id } ?: NONE

        const val DEFAULT_ENABLED_ID = "medium_room"
    }
}