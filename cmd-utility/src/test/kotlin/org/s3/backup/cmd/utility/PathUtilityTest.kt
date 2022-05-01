package org.s3.backup.cmd.utility

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class PathUtilityTest {

    @Test
    fun resolvePath() {
        val homeDir = System.getProperty("user.home")
        assertEquals(PathUtility.workDirectory(), PathUtility.resolvePath(".").also { println(it) })
        assertEquals(
            PathUtility.resolvePath("${PathUtility.workDirectory()}/.."),
            PathUtility.resolvePath("..").also { println(it) }
        )
        assertEquals("/", PathUtility.resolvePath("/").also { println(it) })
        assertEquals(homeDir, PathUtility.resolvePath("~").also { println(it) })
    }

    @Test
    fun currentLocation() {
        assertTrue { PathUtility.workDirectory().also { println(it) }.contains("s3-backup") }
        assertTrue { PathUtility.workDirectory("test").also { println(it) }.endsWith("test") }
    }
}
