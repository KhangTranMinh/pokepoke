package com.khangtm.pokepoke.core.testing

/** Robolectric qualifiers for screenshot tests: `@Config(qualifiers = ScreenshotDevices.PHONE)`. */
object ScreenshotDevices {
    /** Default phone (docs/design/build.md §7). */
    const val PHONE = "w411dp-h891dp-xxhdpi"

    /** Tablet / unfolded foldable (width >= 841dp). */
    const val TABLET = "w841dp-h701dp-xhdpi"
}
