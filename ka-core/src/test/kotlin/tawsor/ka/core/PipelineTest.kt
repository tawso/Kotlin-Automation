package tawsor.ka.core

import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PipelineTest {

    @Test
    fun testSequentialPipelineSuccess() = runTest {
        val p = pipeline("test-pipe") {
            step("step1") {
                set("counter", 1)
            }
            step("step2") {
                val current: Int = get("counter") ?: 0
                set("counter", current + 1)
            }
        }

        val ctx = PipelineContext()
        val result = p.execute(ctx)

        assertTrue(result is PipelineResult.Success)
        assertEquals(2, ctx.get<Int>("counter"))
    }

    @Test
    fun testStepRetry() = runTest {
        val attempts = AtomicInteger(0)
        val p = pipeline("retry-pipe") {
            step("retry-step", retries = 2) {
                if (attempts.incrementAndGet() < 2) {
                    throw IllegalStateException("Temporary error")
                }
            }
        }

        val result = p.execute()
        assertTrue(result is PipelineResult.Success)
        assertEquals(2, attempts.get())
    }

    @Test
    fun testParallelSteps() = runTest {
        val p = pipeline("parallel-pipe") {
            parallel("par-block") {
                step("p1") {
                    set("p1_done", true)
                }
                step("p2") {
                    set("p2_done", true)
                }
            }
        }

        val ctx = PipelineContext()
        val result = p.execute(ctx)

        assertTrue(result is PipelineResult.Success)
        assertTrue(ctx.get<Boolean>("p1_done") == true)
        assertTrue(ctx.get<Boolean>("p2_done") == true)
    }

    @Test
    fun testLifecycleHooks() = runTest {
        val successCalled = AtomicBoolean(false)
        val failureCalled = AtomicBoolean(false)

        val successPipe = pipeline("success-pipe") {
            step("ok") {}
            onSuccess { successCalled.set(true) }
            onFailure { _, _ -> failureCalled.set(true) }
        }
        successPipe.execute()
        assertTrue(successCalled.get())
        assertTrue(!failureCalled.get())

        successCalled.set(false)
        val failPipe = pipeline("fail-pipe") {
            step("failing") { throw RuntimeException("Boom") }
            onSuccess { successCalled.set(true) }
            onFailure { _, _ -> failureCalled.set(true) }
        }
        failPipe.execute()
        assertTrue(!successCalled.get())
        assertTrue(failureCalled.get())
    }
}
