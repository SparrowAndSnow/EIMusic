package com.eimsound.eimusic.ui

/**
 * Nerd Font / Font Awesome icons used in the terminal UI.
 *
 * Keep every icon single-cell wide: mixing double-width emoji with these glyphs
 * breaks header/list alignment. Terminals without a Nerd Font may render these as
 * placeholders; an ASCII fallback is tracked in TODO.md.
 */
enum class Icon(val sym: String) {
    Previous("\uF048"),
    Play("\uF04B"),
    Pause("\uF04C"),
    Stop("\uF04D"),
    Next("\uF051"),
    Repeat("\uF01E"),
    Shuffle("\uF074"),
    VolumeOff("\uF026"),
    VolumeLow("\uF027"),
    VolumeHigh("\uF028"),
    User("\uF007"),
    Queue("\uF03A"),

    Search("\uF002"),
    Settings("\uF013"),
    Help("\uF059"),
    Moon("\uF186"),
    Sun("\uF185"),
    Together("\uF0C0"),
}
