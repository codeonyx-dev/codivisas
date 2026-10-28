package com.codeonyx.bcvaqui.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Evita regresiones del crash de "Acerca de": painterResource no soporta adaptive-icons (mipmap). */
class ResourceUsageGuardTest {
    @Test
    fun noPainterResourceWithMipmap() {
        val root = listOf(File("src/main/java"), File("app/src/main/java")).first { it.exists() }
        val bad = root.walkTopDown().filter { it.extension == "kt" }
            .filter { Regex("""painterResource\(\s*(id\s*=\s*)?R\.mipmap""").containsMatchIn(it.readText()) }
            .map { it.name }.toList()
        assertTrue("painterResource(R.mipmap…) crashea con adaptive icons: $bad", bad.isEmpty())
    }
}
