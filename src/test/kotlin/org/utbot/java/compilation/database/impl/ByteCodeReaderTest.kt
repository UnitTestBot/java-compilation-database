package org.utbot.java.compilation.database.impl

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.utbot.java.compilation.database.ApiLevel
import org.utbot.java.compilation.database.compilationDatabase
import org.utbot.java.compilation.database.impl.fs.asByteCodeLocation
import org.utbot.java.compilation.database.impl.tree.ClassTree


class ByteCodeReaderTest : LibrariesMixin {

    @Test
    fun `read byte-code benchmark`() {
        val lib = guavaLib
        benchmark(20, "read bytecode") {
            runBlocking {
                lib.asByteCodeLocation(ApiLevel.ASM8).loader()!!.load(ClassTree())
            }
        }
    }

    @Test
    fun `read all classpath byte-code benchmark`() {
        val jars = allJars
        benchmark(10, "reading libraries bytecode") {
            runBlocking {
                val db = compilationDatabase {
                    apiLevel = ApiLevel.ASM8
                    useJavaHomeJRE()
                }
                db.load(jars)
            }
        }
    }

    @Test
    fun `read jre libraries  benchmark`() {
        benchmark(10, "reading libraries bytecode") {
            runBlocking {
                compilationDatabase {
                    apiLevel = ApiLevel.ASM8
                    useJavaHomeJRE()
                }
            }
        }
    }

    private fun benchmark(repeats: Int = 10, name: String, action: () -> Unit) {
        // warmup
        repeat(repeats / 2) {
            action()
        }

        // let's count
        repeat(repeats) {
            val start = System.currentTimeMillis()
            action()
            val end = System.currentTimeMillis()
            println("$it: $name took: ${end - start}ms")
            Thread.sleep(1_000)
        }

    }

}