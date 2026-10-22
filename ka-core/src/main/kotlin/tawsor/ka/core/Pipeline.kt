package tawsor.ka.core

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime

sealed class StepExecution {
    data class Single(val step: Step) : StepExecution()
    data class Parallel(val name: String, val steps: List<Step>) : StepExecution()
}

data class Step(
    val name: String,
    val retries: Int = 0,
    val retryDelay: Duration = 500.milliseconds,
    val action: suspend (PipelineContext) -> Unit
)

sealed class PipelineResult {
    data class Success(val name: String, val duration: Duration) : PipelineResult()
    data class Failure(val name: String, val failedStep: String, val cause: Throwable, val duration: Duration) : PipelineResult()
}

class Pipeline(
    val name: String,
    private val executions: List<StepExecution>,
    private val successHook: (suspend (PipelineContext) -> Unit)? = null,
    private val failureHook: (suspend (PipelineContext, Throwable) -> Unit)? = null
) {
    suspend fun execute(context: PipelineContext = PipelineContext()): PipelineResult {
        context.logInfo("Starting pipeline: '$name'")
        var failedStepName: String? = null
        var stepFailure: Throwable? = null

        val duration = measureTime {
            for (exec in executions) {
                try {
                    when (exec) {
                        is StepExecution.Single -> executeStep(exec.step, context)
                        is StepExecution.Parallel -> executeParallel(exec, context)
                    }
                } catch (t: Throwable) {
                    failedStepName = when (exec) {
                        is StepExecution.Single -> exec.step.name
                        is StepExecution.Parallel -> exec.name
                    }
                    stepFailure = t
                    break
                }
            }
        }

        return if (stepFailure != null) {
            context.logError("Pipeline '$name' FAILED at step '$failedStepName' in $duration. Error: ${stepFailure.message}")
            try {
                failureHook?.invoke(context, stepFailure)
            } catch (hookEx: Throwable) {
                context.logError("Error executing onFailure hook: ${hookEx.message}")
            }
            PipelineResult.Failure(name, failedStepName ?: "unknown", stepFailure, duration)
        } else {
            context.logSuccess("Pipeline '$name' COMPLETED successfully in $duration")
            try {
                successHook?.invoke(context)
            } catch (hookEx: Throwable) {
                context.logError("Error executing onSuccess hook: ${hookEx.message}")
            }
            PipelineResult.Success(name, duration)
        }
    }

    private suspend fun executeStep(step: Step, context: PipelineContext) {
        var attemptsLeft = step.retries + 1
        var attempt = 1
        var lastError: Throwable? = null

        while (attemptsLeft > 0) {
            try {
                context.log("Running step [${step.name}] (attempt $attempt/${step.retries + 1})...")
                val stepDuration = measureTime {
                    step.action(context)
                }
                context.log("Step [${step.name}] finished in $stepDuration")
                return
            } catch (e: Throwable) {
                lastError = e
                attemptsLeft--
                if (attemptsLeft > 0) {
                    context.logWarn("Step [${step.name}] failed: ${e.message}. Retrying in ${step.retryDelay}...")
                    delay(step.retryDelay)
                    attempt++
                }
            }
        }

        throw lastError ?: RuntimeException("Step ${step.name} failed with unknown error")
    }

    private suspend fun executeParallel(parallel: StepExecution.Parallel, context: PipelineContext) = coroutineScope {
        context.log("Running parallel block [${parallel.name}] with ${parallel.steps.size} steps...")
        parallel.steps.map { step ->
            async {
                executeStep(step, context)
            }
        }.awaitAll()
    }
}
