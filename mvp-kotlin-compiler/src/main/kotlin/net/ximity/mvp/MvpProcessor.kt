package net.ximity.mvp

import com.google.auto.common.MoreElements.getPackage
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.kotlinpoet.*
import net.ximity.annotation.MvpContract
import net.ximity.annotation.MvpMainComponent
import net.ximity.annotation.MvpScope
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.inject.Inject
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror

@SupportedAnnotationTypes(
        "net.ximity.annotation.MvpMainComponent",
        "net.ximity.annotation.MvpContract"
)
class MvpProcessor : AbstractProcessor() {

    private val templatePackageName = "net.ximity.mvp.template"
    private val contractPackageName = "net.ximity.mvp.contract"
    private var halt = false
    private val bindings = ArrayList<Binding>()

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    @Synchronized
    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        Util.init(processingEnv)
    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (!processAnnotations(roundEnv)) {
            return halt
        }

        if (roundEnv.processingOver()) {
            halt = true
        }

        return halt
    }

    private fun processAnnotations(roundEnv: RoundEnvironment): Boolean {
        return processMvpModules(roundEnv) &&
                processMvpSubcomponents(roundEnv) &&
                processMainComponent(roundEnv)
    }

    private fun processMvpModules(roundEnv: RoundEnvironment): Boolean {
        val elements = roundEnv.getElementsAnnotatedWith(MvpContract::class.java)

        if (Util.isEmpty(elements)) {
            return true
        }

        for (element in elements) {
            if (element.kind != ElementKind.INTERFACE) {
                Util.error(MvpContract::class.simpleName + " can only be used for interfaces!")
                return false
            }

            if (!generateMvpModule(element as TypeElement)) {
                return false
            }
        }

        return true
    }

    private fun generateMvpModule(element: TypeElement): Boolean {
        val packageName = getPackage(element).toString()
        val contract = element.getAnnotation(MvpContract::class.java)

        val view = Util.getView(element)
        val viewImplements = view?.interfaces as List<TypeMirror>
        val presenter = Util.getPresenter(element)
        val presenterImplements = presenter?.interfaces as List<TypeMirror>
        presenter.getAnnotation(MvpScope::class.java)
                ?: Util.error("${presenter.simpleName} does not have a ${MvpScope::class.java.simpleName} scope!!!")
                        .also { return false }
        val moduleClassName = if (Util.isEmpty(contract.module)) {
            "${element.simpleName}Module"
        } else {
            contract.module
        }

        if (viewImplements.isEmpty()) {
            Util.error("${view.simpleName} does not implement ${element.simpleName} view contract! (Does not implement any interfaces)")
            return false
        }

        if (presenterImplements.isEmpty()) {
            Util.error("${presenter.simpleName} does not implement ${element.simpleName} presenter contract!")
            return false
        }

        val viewInterface = viewImplements.stream()
                .map(Util::asElement)
                .filter { element.enclosedElements.contains(it) }
                .findFirst()

        if (!viewInterface.isPresent) {
            Util.error("${view.simpleName} does not implement ${element.simpleName} view contract!")
            return false
        }

        val presenterInterface = presenterImplements.stream()
                .map(Util::asElement)
                .filter { element.enclosedElements.contains(it) }
                .findFirst()

        if (!presenterInterface.isPresent) {
            Util.error("${presenter.simpleName} does not implement  ${element.simpleName} presenter contract!")
            return false
        }

        val isViewPresenter = presenterImplements.stream()
                .map(Util::asElement)
                .map { it.interfaces }
                .flatMap { it.stream() }
                .anyMatch(Util::isSubTypePresenter)

        val moduleFunctions = ArrayList<FunSpec>()

        moduleFunctions.add(FunSpec.builder("provides${element.simpleName}View")
                .addAnnotation(MvpScope::class)
                .addAnnotation(ClassName("dagger", "Provides"))
                .addModifiers(KModifier.INTERNAL)
                .returns(viewInterface.get().asClassName())
                .addStatement("return view")
                .build())

        moduleFunctions.add(FunSpec.builder("provides${element.simpleName}Presenter")
                .addAnnotation(MvpScope::class)
                .addAnnotation(ClassName("dagger", "Provides"))
                .addModifiers(KModifier.INTERNAL)
                .addParameter(ParameterSpec.builder("impl", presenter.asClassName())
                        .build())
                .returns(presenterInterface.get().asClassName())
                .addStatement("return impl")
                .build())

        if (isViewPresenter) {
            moduleFunctions.add(FunSpec.builder("provides${element.simpleName}MvpPresenter")
                    .addAnnotation(MvpScope::class)
                    .addAnnotation(ClassName("dagger", "Provides"))
                    .addModifiers(KModifier.INTERNAL)
                    .addParameter(ParameterSpec.builder("impl", presenter.asClassName())
                            .build())
                    .returns(ClassName(contractPackageName, "MvpPresenter"))
                    .addStatement("return impl")
                    .build())
        }

        FileSpec.builder(packageName, moduleClassName)
                .addAnnotation(AnnotationSpec.builder(JvmName::class)
                        .addMember("%S", moduleClassName)
                        .build())
                .addType(TypeSpec.classBuilder(moduleClassName)
                        .addAnnotation(ClassName("dagger", "Module"))
                        .primaryConstructor(FunSpec.constructorBuilder()
                                .addParameter(ParameterSpec.builder("view", viewInterface.get().asClassName())
                                        .addModifiers(KModifier.PRIVATE)
                                        .build())
                                .build())
                        .addProperty(PropertySpec.builder("view", viewInterface.get().asClassName())
                                .initializer("view")
                                .addModifiers(KModifier.PRIVATE)
                                .build())
                        .addFunctions(moduleFunctions)
                        .build())

                .build()
                .writeFile()

        return true
    }

    private fun processMvpSubcomponents(roundEnv: RoundEnvironment): Boolean {
        val elements = roundEnv.getElementsAnnotatedWith(MvpContract::class.java)

        if (Util.isEmpty(elements)) {
            return true
        }

        for (element in elements) {
            if (element.kind != ElementKind.INTERFACE) {
                Util.error("${MvpContract::class.simpleName} can ony be used for interfaces!")
                return false
            }

            if (!generateMvpComponent(element as TypeElement)) {
                return false
            }
        }

        return true
    }

    private fun generateMvpComponent(element: TypeElement): Boolean {
        val packageName = getPackage(element).toString()
        val contract = element.getAnnotation(MvpContract::class.java)

        val componentName = if (Util.isEmpty(contract.subcomponent))
            "${element.simpleName}Component"
        else
            contract.subcomponent
        val moduleName = if (Util.isEmpty(contract.module))
            "${element.simpleName}Module"
        else
            contract.module

        val view = Util.getView(element)

        if (view == null) {
            Util.error("Missing view for $contract!!!")
            return false
        }

        FileSpec.builder(packageName, componentName)
                .addAnnotation(AnnotationSpec.builder(JvmName::class)
                        .addMember("%S", componentName)
                        .build())
                .addType(TypeSpec.interfaceBuilder(componentName)
                        .addAnnotation(MvpScope::class)
                        .addAnnotation(AnnotationSpec.builder(ClassName("dagger", "Subcomponent"))
                                .addMember("%L = [%T::class]", "modules", ClassName(packageName, moduleName))
                                .build())
                        .addFunction(FunSpec.builder("bind")
                                .addParameter("view", view.asClassName())
                                .addModifiers(KModifier.ABSTRACT)
                                .build())
                        .build())
                .build()
                .writeFile()

        bindings.add(Binding(packageName, componentName, moduleName))
        return true
    }

    private fun processMainComponent(roundEnv: RoundEnvironment): Boolean {
        val elements = roundEnv.getElementsAnnotatedWith(MvpMainComponent::class.java)

        if (Util.isEmpty(elements)) {
            return true
        }

        if (elements.size > 1) {
            Util.error("Only one component can be annotated with ${MvpMainComponent::class.simpleName}!")
        }

        for (element in elements) {
            if (element.kind != ElementKind.INTERFACE &&
                    element.kind != ElementKind.CLASS &&
                    !element.modifiers.contains(Modifier.ABSTRACT)) {
                Util.error("${MvpMainComponent::class.simpleName} can only be used for interfaces and abstract classes!")
                return false
            }

            if (!generateBaseComponents(element as TypeElement)) {
                return false
            }

            if (!generateBaseViews(element)) {
                return false
            }

            if (!generateBaseComponent(element)) {
                return false
            }
        }

        return true
    }

    private fun generateBaseComponents(element: TypeElement): Boolean {
        val templateApplication = ClassName(templatePackageName, "MvpApplication")
        val mainComponent = ClassName.bestGuess(element.toString())
        FileSpec.builder(templatePackageName, "BaseMvpApplication")
                .addType(TypeSpec.classBuilder("BaseMvpApplication")
                        .addModifiers(KModifier.ABSTRACT)
                        .superclass(ParameterizedTypeName.get(templateApplication, mainComponent))
                        .build())
                .build()
                .writeFile()

        val templateActivity = ClassName(templatePackageName, "MvpActivity")
        FileSpec.builder(templatePackageName, "BaseMvpActivity")
                .addType(TypeSpec.classBuilder("BaseMvpActivity")
                        .addModifiers(KModifier.ABSTRACT)
                        .superclass(ParameterizedTypeName.get(templateActivity, mainComponent))
                        .build())
                .build()
                .writeFile()

        val templateFragment = ClassName(templatePackageName, "MvpFragment")
        FileSpec.builder(templatePackageName, "BaseMvpFragment")
                .addType(TypeSpec.classBuilder("BaseMvpFragment")
                        .addModifiers(KModifier.ABSTRACT)
                        .superclass(ParameterizedTypeName.get(templateFragment, mainComponent))
                        .build())
                .build()
                .writeFile()

        val templateDialog = ClassName(templatePackageName, "MvpDialog")
        FileSpec.builder(templatePackageName, "BaseMvpDialog")
                .addType(TypeSpec.classBuilder("BaseMvpDialog")
                        .addModifiers(KModifier.ABSTRACT)
                        .superclass(ParameterizedTypeName.get(templateDialog, mainComponent))
                        .build())
                .build()
                .writeFile()

        val templateReceiver = ClassName(templatePackageName, "MvpBroadcastReceiver")
        FileSpec.builder(templatePackageName, "BaseMvpBroadcastReceiver")
                .addType(TypeSpec.classBuilder("BaseMvpBroadcastReceiver")
                        .addModifiers(KModifier.ABSTRACT)
                        .superclass(ParameterizedTypeName.get(templateReceiver, mainComponent))
                        .build())
                .build()
                .writeFile()

        val templateService = ClassName(templatePackageName, "MvpService")
        FileSpec.builder(templatePackageName, "BaseMvpService")
                .addType(TypeSpec.classBuilder("BaseMvpService")
                        .addModifiers(KModifier.ABSTRACT)
                        .superclass(ParameterizedTypeName.get(templateService, mainComponent))
                        .build())
                .build()
                .writeFile()

        return true
    }

    private fun generateBaseViews(element: TypeElement): Boolean {
        val mainComponent = ClassName.bestGuess(element.toString())
        val activityMethods = ArrayList<FunSpec>()

        activityMethods.add(FunSpec.builder("injectPresenter")
                .addModifiers(KModifier.INTERNAL)
                .addParameter(ParameterSpec.builder("viewPresenter", ClassName(contractPackageName, "MvpPresenter"))
                        .build())
                .addStatement("presenter = viewPresenter")
                .addAnnotation(Inject::class)
                .build())

        activityMethods.add(FunSpec.builder("onCreate")
                .addParameter(ParameterSpec.builder("savedInstanceState", ClassName("android.os", "Bundle")
                        .asNullable())
                        .build())
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("super.onCreate(savedInstanceState)")
                .addStatement("presenter.create(savedInstanceState)")
                .build())

        activityMethods.add(FunSpec.builder("onStart")
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("super.onStart()")
                .addStatement("presenter.start()")
                .build())

        activityMethods.add(FunSpec.builder("onSaveInstanceState")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter(ParameterSpec.builder("outState", ClassName("android.os", "Bundle")
                        .asNonNullable())
                        .build())
                .addStatement("super.onSaveInstanceState(outState)")
                .addStatement("presenter.saveState(outState)")
                .build())

        activityMethods.add(FunSpec.builder("onPause")
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("super.onPause()")
                .addStatement("presenter.pause()")
                .build())

        activityMethods.add(FunSpec.builder("onStop")
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("super.onStop()")
                .addStatement("presenter.stop()")
                .build())

        activityMethods.add(FunSpec.builder("onDestroy")
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("super.onDestroy()")
                .addStatement("presenter.destroy()")
                .build())

        val baseActivity = ClassName(templatePackageName, "MvpActivity")
        FileSpec.builder(templatePackageName, "ActivityView")
                .addType(TypeSpec.classBuilder("ActivityView")
                        .addModifiers(KModifier.ABSTRACT)
                        .superclass(ParameterizedTypeName.get(baseActivity, mainComponent))
                        .addProperty(PropertySpec.varBuilder("presenter", ClassName(contractPackageName, "MvpPresenter"))
                                .addModifiers(KModifier.PRIVATE, KModifier.LATEINIT)
                                .build())
                        .addFunctions(activityMethods)
                        .build())
                .build()
                .writeFile()

        val fragmentMethods = ArrayList<FunSpec>()

        fragmentMethods.add(FunSpec.builder("injectPresenter")
                .addModifiers(KModifier.INTERNAL)
                .addParameter(ParameterSpec.builder("viewPresenter", ClassName(contractPackageName, "MvpPresenter"))
                        .build())
                .addStatement("presenter = viewPresenter")
                .addAnnotation(Inject::class)
                .build())

        fragmentMethods.add(FunSpec.builder("onViewCreated")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter(ParameterSpec.builder("view", ClassName("android.view", "View")
                        .asNonNullable())
                        .build())
                .addParameter(ParameterSpec.builder("savedInstanceState", ClassName("android.os", "Bundle")
                        .asNullable())
                        .build())
                .addStatement("super.onViewCreated(view, savedInstanceState)")
                .addStatement("presenter.create(savedInstanceState)")
                .build())

        fragmentMethods.add(FunSpec.builder("onStart")
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("super.onStart()")
                .addStatement("presenter.start()")
                .build())

        fragmentMethods.add(FunSpec.builder("onSaveInstanceState")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter(ParameterSpec.builder("outState", ClassName("android.os", "Bundle")
                        .asNonNullable())
                        .build())
                .addStatement("super.onSaveInstanceState(outState)")
                .addStatement("presenter.saveState(outState)")
                .build())

        fragmentMethods.add(FunSpec.builder("onPause")
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("super.onPause()")
                .addStatement("presenter.pause()")
                .build())

        fragmentMethods.add(FunSpec.builder("onStop")
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("super.onStop()")
                .addStatement("presenter.stop()")
                .build())

        fragmentMethods.add(FunSpec.builder("onDestroy")
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("super.onDestroy()")
                .addStatement("presenter.destroy()")
                .build())

        val baseFragment = ClassName(templatePackageName, "MvpFragment")
        FileSpec.builder(templatePackageName, "FragmentView")
                .addType(TypeSpec.classBuilder("FragmentView")
                        .addModifiers(KModifier.ABSTRACT)
                        .superclass(ParameterizedTypeName.get(baseFragment, mainComponent))
                        .addProperty(PropertySpec.varBuilder("presenter", ClassName(contractPackageName, "MvpPresenter"))
                                .addModifiers(KModifier.PRIVATE, KModifier.LATEINIT)
                                .build())
                        .addFunctions(fragmentMethods)
                        .build())
                .build()
                .writeFile()

        val baseDialog = ClassName(templatePackageName, "MvpDialog")
        FileSpec.builder(templatePackageName, "DialogView")
                .addType(TypeSpec.classBuilder("DialogView")
                        .addModifiers(KModifier.ABSTRACT)
                        .superclass(ParameterizedTypeName.get(baseDialog, mainComponent))
                        .addProperty(PropertySpec.varBuilder("presenter", ClassName(contractPackageName, "MvpPresenter"))
                                .addModifiers(KModifier.PRIVATE, KModifier.LATEINIT)
                                .build())
                        .addFunctions(fragmentMethods)
                        .build())
                .build()
                .writeFile()

        return true
    }

    private fun generateBaseComponent(element: TypeElement): Boolean {
        val component = element.getAnnotation(MvpMainComponent::class.java)
        val componentName = component.name

        val bindingBuilder = com.squareup.javapoet.TypeSpec.interfaceBuilder(componentName)
                .addModifiers(Modifier.PUBLIC)

        bindings.forEach {
            bindingBuilder.addMethod(MethodSpec.methodBuilder("add")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(com.squareup.javapoet.ClassName.get(it.packageName, it.moduleName), "module")
                    .returns(com.squareup.javapoet.ClassName.get(it.packageName, it.componentName))
                    .build())
        }

        bindingBuilder.build()
                .let {
                    JavaFile.builder(getPackage(element).qualifiedName.toString(), it)
                            .build()
                }.let { Util.writeJavaFile(it, componentName) }

//        val bindingBuilder = TypeSpec.interfaceBuilder(componentName)
//        bindings.forEach {
//            bindingBuilder.addFunction(FunSpec.builder("add")
//                    .addModifiers(KModifier.ABSTRACT)
//                    .addParameter("module", ClassName(it.packageName, it.moduleName))
//                    .returns(ClassName(it.packageName, it.componentName))
//                    .build())
//        }
//
//        val packageName = getPackage(element).qualifiedName.toString()
//        FileSpec.builder(packageName, componentName)
//                .addAnnotation(AnnotationSpec.builder(JvmName::class)
//                        .addMember("%S", componentName)
//                        .build())
//                .addType(bindingBuilder.build())
//                .build()
//                .writeFile()
        return true
    }
}
