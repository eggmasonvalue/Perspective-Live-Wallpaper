package com.perspectivelive.wallpaper.data

/**
 * Constants for Day Counter modes.
 */
object DayCounterMode {
    const val STATIC = "STATIC"
    const val NO_TOMORROW = "NO_TOMORROW"
    const val VS_YESTERDAY = "VS_YESTERDAY"
    const val THIS_YEAR = "THIS_YEAR"
    const val THIS_YEAR_WEEKS = "THIS_YEAR_WEEKS"

    fun isThisYear(mode: String): Boolean = mode == THIS_YEAR || mode == THIS_YEAR_WEEKS

    fun isDynamic(mode: String): Boolean =
        mode == NO_TOMORROW || mode == VS_YESTERDAY || isThisYear(mode)
}
