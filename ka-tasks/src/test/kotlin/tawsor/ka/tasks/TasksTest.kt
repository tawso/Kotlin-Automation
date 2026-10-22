package tawsor.ka.tasks

import kotlinx.coroutines.test.runTest
import tawsor.ka.core.PipelineContext
import tawsor.ka.core.pipeline
import kotlin.test.Test
import kotlin.test.assertTrue

class TasksTest {
    @Test
    fun testShellEcho() = runTest {
        val p = pipeline("test-shell") {
            step("echo") {
                val output = sh("echo hello_ka")
                assertTrue(output.contains("hello_ka"))
            }
        }
        val result = p.execute()
        assertTrue(result is tawsor.ka.core.PipelineResult.Success)
    }
}

