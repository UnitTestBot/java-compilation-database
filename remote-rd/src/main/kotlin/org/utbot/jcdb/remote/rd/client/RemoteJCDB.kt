package org.utbot.jcdb.remote.rd.client

import com.jetbrains.rd.framework.IdKind
import com.jetbrains.rd.framework.Identities
import com.jetbrains.rd.framework.Protocol
import com.jetbrains.rd.framework.SocketWire
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.threading.SingleThreadScheduler
import org.utbot.jcdb.api.ByteCodeLocation
import org.utbot.jcdb.api.ClasspathSet
import org.utbot.jcdb.api.JCDB
import org.utbot.jcdb.api.LocationScope
import org.utbot.jcdb.impl.fs.asByteCodeLocation
import org.utbot.jcdb.remote.rd.*
import java.io.File

class RemoteJCDB(port: Int) : JCDB {

    private val lifetimeDef = Lifetime.Eternal.createNested()
    private val scheduler = SingleThreadScheduler(lifetimeDef.lifetime, "rd-scheduler")

    private val clientProtocol = Protocol(
        "rd-client-$port",
        serializers,
        Identities(IdKind.Client),
        scheduler,
        SocketWire.Client(lifetimeDef.lifetime, scheduler, port),
        lifetimeDef.lifetime
    )

    private val getClasspath = GetClasspathResource().clientCall(clientProtocol)
    private val getClass = GetClassResource().clientCall(clientProtocol)
    private val closeClasspath = CloseClasspathResource().clientCall(clientProtocol)
    private val getSubClasses = GetSubClassesResource().clientCall(clientProtocol)
    private val callIndex = CallIndexResource().clientCall(clientProtocol)
    private val stopServer = StopServerResource().clientCall(clientProtocol)

    private val getId = GetGlobalIdResource().clientCall(clientProtocol)
    private val getName = GetGlobalNameResource().clientCall(clientProtocol)
    private val loadLocations = LoadLocationsResource().clientCall(clientProtocol)

    init {
        scheduler.flush()
    }

    override val globalIdStore = RemoteGlobalIdsStore(getName, getId)

    override suspend fun classpathSet(dirOrJars: List<File>): ClasspathSet {
        val resp = getClasspath.startSuspending(GetClasspathReq(dirOrJars.map { it.absolutePath }.sorted()))
        return RemoteClasspathSet(
            resp.key,
            locations = resp.locations.mapIndexed { index, path ->
                File(path).asByteCodeLocation(isRuntime = resp.scopes.get(index) == LocationScope.RUNTIME)
            },
            db = this,
            close = closeClasspath,
            getClass = getClass,
            getSubClasses = getSubClasses,
            callIndex = callIndex
        )
    }

    override suspend fun load(dirOrJar: File): JCDB = apply {
        loadLocations.startSuspending(GetClasspathReq(listOf(dirOrJar.absolutePath)))
    }

    override suspend fun load(dirOrJars: List<File>): JCDB = apply {
        loadLocations.startSuspending(GetClasspathReq(dirOrJars.map { it.absolutePath }))
    }

    override suspend fun loadLocations(locations: List<ByteCodeLocation>): JCDB = apply {
        loadLocations.startSuspending(
            GetClasspathReq(locations.map { it.path })
        )
    }

    override suspend fun refresh() {
    }

    override fun watchFileSystemChanges() = this

    override suspend fun awaitBackgroundJobs() {
    }

    override fun close() {
        scheduler.queue {
            stopServer.start(lifetimeDef, Unit)
        }
        scheduler.flush()
        lifetimeDef.terminate()
    }
}


