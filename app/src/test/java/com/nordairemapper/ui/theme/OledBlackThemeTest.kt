package com.nordairemapper.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class OledBlackThemeTest {

    @Test
    fun withOledBlackBackground_usesPureBlackAndKeepsSurface() {
        val base = androidx.compose.material3.darkColorScheme(
            background = BackgroundDark,
            surface = SurfaceDark,
        )

        val oled = base.withOledBlackBackground()

        assertEquals(Color.Black, oled.background)
        assertEquals(SurfaceDark, oled.surface)
    }
}
