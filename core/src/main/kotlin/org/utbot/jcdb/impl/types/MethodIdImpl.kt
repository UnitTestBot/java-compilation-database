package org.utbot.jcdb.impl.types

import org.objectweb.asm.tree.MethodNode
import org.utbot.jcdb.api.ClassId
import org.utbot.jcdb.api.MethodId
import org.utbot.jcdb.api.MethodResolution
import org.utbot.jcdb.api.throwClassNotFound
import org.utbot.jcdb.impl.ClassIdService
import org.utbot.jcdb.impl.signature.MethodSignature
import org.utbot.jcdb.impl.tree.ClassNode

class MethodIdImpl(
    private val methodInfo: MethodInfo,
    private val classNode: ClassNode,
    override val classId: ClassId,
    private val classIdService: ClassIdService
) : MethodId {

    override val name: String get() = methodInfo.name
    override suspend fun access() = methodInfo.access

    private val lazyParameters by lazy(LazyThreadSafetyMode.NONE) {
        methodInfo.parameters.map {
            classIdService.toClassId(it)  ?: throw org.utbot.jcdb.api.NoClassInClasspathException(it)
        }
    }
    private val lazyAnnotations by lazy(LazyThreadSafetyMode.NONE) {
        methodInfo.annotations.map {
            val className = it.className
            classIdService.toClassId(className) ?: className.throwClassNotFound()
        }
    }

    override suspend fun signature(): MethodResolution {
        return MethodSignature.of(methodInfo.signature, classId.classpath)
    }

    override suspend fun returnType() = classIdService.toClassId(methodInfo.returnType) ?: methodInfo.returnType.throwClassNotFound()

    override suspend fun parameters() = lazyParameters

    override suspend fun annotations() = lazyAnnotations

    override suspend fun description() = methodInfo.desc

    override suspend fun readBody(): MethodNode? {
        val location = classId.location
        if (location?.isChanged() == true) {
            return null
        }
        return classNode.source.loadMethod(name, methodInfo.desc)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is MethodIdImpl) {
            return false
        }
        return other.name == name && classId == other.classId && methodInfo.desc == other.methodInfo.desc
    }

    override fun hashCode(): Int {
        return 31 * classId.hashCode() + name.hashCode()
    }


}