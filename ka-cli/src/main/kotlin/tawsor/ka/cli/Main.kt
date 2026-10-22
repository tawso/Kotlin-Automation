package tawsor.ka.cli

import kotlinx.coroutines.runBlocking
import tawsor.ka.core.PipelineResult
import tawsor.ka.core.pipeline
import tawsor.ka.tasks.gitBranch
import tawsor.ka.tasks.sh
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    println(
        """
        ====================================================
               Kotlin Automation (KA) Engine CLI
        ====================================================
        """.trimIndent()
    )

    if (args.isEmpty() || args[0] in listOf("-h", "--help", "help")) {
        printUsage()
        return
    }

    when (val command = args[0]) {
        "version" -> {
            println("Kotlin Automation v0.1.0 (JVM 21)")
        }
        "run-demo" -> {
            println("Running demo pipeline...")
            runBlocking {
                val demoPipeline = pipeline("CLI Demo") {
                    step("Git Info") {
                        val branch = gitBranch()
                        log("Current branch detected: $branch")
                    }
                    step("System check") {
                        val javaVer = sh("java -version")
                        log("Java check:\n$javaVer")
                    }
                }

                val result = demoPipeline.execute()
                if (result is PipelineResult.Failure) {
                    System.err.println("Demo pipeline failed!")
                    exitProcess(1)
                }
            }
        }
        else -> {
            System.err.println("Unknown command: '$command'")
            printUsage()
            exitProcess(1)
        }
    }
}

private fun printUsage() {
    println(
        """
        Usage: ka-cli <command> [options]

        Commands:
          version      Display current KA version
          run-demo     Execute the built-in demo automation pipeline
          help         Show this help message
        """.trimIndent()
    )
}

