package org.utbot.jcdb.impl.performance

import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.*
import org.utbot.jcdb.api.JCDB
import org.utbot.jcdb.impl.LibrariesMixin
import org.utbot.jcdb.jcdb
import java.nio.file.Files
import java.util.*
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@Fork(0)
@Warmup(iterations = 2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.MILLISECONDS)
class RestoreDBBenchmark : LibrariesMixin {

    companion object {
        private val jdbcLocation = Files.createTempDirectory("jdbc-${UUID.randomUUID()}").toFile().absolutePath
    }

    var db: JCDB? = null

    @Setup
    fun setup() {
        val tempDb = newDB()
        tempDb.close()
    }

    @Benchmark
    fun restore() {
        db = newDB()
    }

    @TearDown(Level.Iteration)
    fun clean() {
        db?.close()
        db = null
    }

    private fun newDB(): JCDB {
        return runBlocking {
            jcdb {
                persistent {
                    location = jdbcLocation
                }
                predefinedDirOrJars = allClasspath
                useProcessJavaRuntime()
            }.also {
                it.awaitBackgroundJobs()
            }
        }
    }

}

fun main() {
    val test = RestoreDBBenchmark()
    test.setup()
    repeat(3) {
        println("iteration $it")
        val start = System.currentTimeMillis()
        test.restore()
        println("took ${System.currentTimeMillis() - start}ms")
        test.clean()
    }
}