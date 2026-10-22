package tawsor.ka.core

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@DslMarker
annotation class KaDsl

@KaDsl
class ParallelBlockBuilder(val name: String) {
    private val steps = mutableListOf<Step>()

    fun step(
        name: String,
        retries: Int = 0,
        retryDelay: Duration = 500.milliseconds,
        action: suspend PipelineContext.() -> Unit
    ) {
        steps.add(Step(name, retries, retryDelay) { ctx -> ctx.action() })
    }

    internal fun build(): List<Step> = steps.toList()
}

@KaDsl
class PipelineBuilder(val name: String) {
    private val executions = mutableListOf<StepExecution>()

    fun step(
        name: String,
        retries: Int = 0,
        retryDelay: Duration = 500.milliseconds,
        action: suspend PipelineContext.() -> Unit
    ) {
        executions.add(StepExecution.Single(Step(name, retries, retryDelay) { ctx -> ctx.action() }))
    }

    fun parallel(name: String, block: ParallelBlockBuilder.() -> Unit) {
        val builder = ParallelBlockBuilder(name)
        builder.block()
        executions.add(StepExecution.Parallel(name, builder.build()))
    }

    fun build(): Pipeline = Pipeline(name, executions)
}

fun pipeline(name: String, block: PipelineBuilder.() -> Unit): Pipeline {
    val builder = PipelineBuilder(name)
    builder.block()
    return builder.build()
}

