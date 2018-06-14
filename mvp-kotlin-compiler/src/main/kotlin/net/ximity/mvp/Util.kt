package net.ximity.mvp

import com.squareup.javapoet.JavaFile
import com.squareup.kotlinpoet.FileSpec
import net.ximity.annotation.MvpContract
import java.io.File
import java.io.IOException
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic

/**
 * Annotation processing utility class methods
 *
 * @author by Emarc Magtanong on 2017/08/16.
 */
object Util {
    const val OUTPUT_FLAG = "mvpDebugLogs"
    private const val generatedKotlin = "kapt.kotlin.generated"
    private var messager: Messager? = null
    private var filer: Filer? = null
    private var typeUtil: Types? = null
    private var elementUtil: Elements? = null
    private var generatedDirectory: File? = null

    fun init(environment: ProcessingEnvironment) {
        messager = environment.messager
        filer = environment.filer
        typeUtil = environment.typeUtils
        elementUtil = environment.elementUtils

        val generatedDirectoryPath: String = environment.options[generatedKotlin]
                ?.replace("kaptKotlin", "kapt") ?: ""
        if (isEmpty(generatedDirectoryPath)) {
            error("Can't find the target directory for generated Kotlin files.")
        }

        generatedDirectory = File(generatedDirectoryPath)
        if (generatedDirectory?.parentFile?.exists() == false) {
            generatedDirectory?.parentFile?.mkdirs()
        }
    }

    internal fun error(message: String) {
        messager!!.printMessage(Diagnostic.Kind.ERROR, message)
    }

    internal fun warn(message: String) {
        messager!!.printMessage(Diagnostic.Kind.WARNING, message)
    }

    internal fun note(message: String) {
        messager!!.printMessage(Diagnostic.Kind.NOTE, message)
    }

    internal fun isEmpty(collection: Collection<*>?): Boolean {
        return collection == null || collection.isEmpty()
    }

    internal fun isEmpty(value: String?): Boolean {
        return value == null || value.isEmpty()
    }

    internal fun getGeneratedDirectory(): File? {
        return generatedDirectory
    }

    internal fun getView(element: TypeElement): TypeElement? {
        try {
            element.getAnnotation(MvpContract::class.java).view
        } catch (e: MirroredTypeException) {
            return typeUtil!!.asElement(e.typeMirror) as TypeElement
        }

        return null
    }

    internal fun getPresenter(element: TypeElement): TypeElement? {
        try {
            element.getAnnotation(MvpContract::class.java).presenter
        } catch (e: MirroredTypeException) {
            return typeUtil!!.asElement(e.typeMirror) as TypeElement
        }

        return null
    }

    internal fun asElement(mirror: TypeMirror): TypeElement {
        return typeUtil!!.asElement(mirror) as TypeElement
    }

    internal fun isSubTypePresenter(type: TypeMirror): Boolean {
        val viewPresenter = elementUtil!!.getTypeElement("net.ximity.mvp.contract.MvpPresenter").asType()
        return typeUtil!!.isSubtype(type, viewPresenter)
    }

    fun writeJavaFile(file: JavaFile, generatedFile: String, shouldLog: Boolean) {
        if (shouldLog && !isEmpty(generatedFile)) Util.note("Generating $generatedFile...")
        try {
            file.writeTo(filer)
            if (shouldLog && !isEmpty(generatedFile)) Util.note("Generated $generatedFile")
        } catch (e: IOException) {
            if (shouldLog && !isEmpty(generatedFile)) Util.warn("Unable to generate file for $generatedFile!")
        }
    }
}

fun FileSpec.writeFile(shouldLog: Boolean) {
    try {
        this.writeTo(Util.getGeneratedDirectory()!!)
        if (shouldLog && !Util.isEmpty(this.name)) Util.note("Generated ${this.name}")
    } catch (e: Exception) {
        if (shouldLog && !Util.isEmpty(this.name)) Util.warn("Unable to generate file for ${this.name}!")
    }
}
