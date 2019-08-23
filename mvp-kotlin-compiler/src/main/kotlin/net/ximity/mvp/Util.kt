package net.ximity.mvp

import com.squareup.javapoet.JavaFile
import com.squareup.kotlinpoet.FileSpec
import net.ximity.annotation.MvpContract
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
    private var messager: Messager? = null
    private var filer: Filer? = null
    private var typeUtil: Types? = null
    private var elementUtil: Elements? = null

    fun init(environment: ProcessingEnvironment) {
        messager = environment.messager
        filer = environment.filer
        typeUtil = environment.typeUtils
        elementUtil = environment.elementUtils
    }

    internal fun error(message: String) {
        messager?.printMessage(Diagnostic.Kind.ERROR, message)
    }

    internal fun warn(message: String) {
        messager?.printMessage(Diagnostic.Kind.WARNING, message)
    }

    internal fun note(message: String) {
        messager?.printMessage(Diagnostic.Kind.NOTE, message)
    }

    internal fun isEmpty(collection: Collection<*>?): Boolean {
        return collection == null || collection.isEmpty()
    }

    internal fun isEmpty(value: String?): Boolean {
        return value == null || value.isEmpty()
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
        if (shouldLog && !isEmpty(generatedFile)) note("Generating $generatedFile...\n")
        try {
            file.writeTo(filer)
            if (shouldLog && !isEmpty(generatedFile)) note("Generated $generatedFile\n")
        } catch (e: Exception) {
            if (shouldLog && !isEmpty(generatedFile)) warn("Unable to generate file for $generatedFile!\n")
        }
    }

    fun writeFile(filesSpec: FileSpec, shouldLog: Boolean) {
        if (shouldLog && !isEmpty(filesSpec.name)) note("Generating ${filesSpec.name}...\n")
        try {
            filer?.also(filesSpec::writeTo)
            if (shouldLog && !isEmpty(filesSpec.name)) note("Generated ${filesSpec.name}\n")
        } catch (e: Exception) {
            if (shouldLog && !isEmpty(filesSpec.name)) warn("Unable to generate file for ${filesSpec.name}!\n")
        }
    }
}
