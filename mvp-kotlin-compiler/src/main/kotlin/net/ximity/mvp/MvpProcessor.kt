package net.ximity.mvp

import com.google.auto.common.MoreElements.getPackage
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import net.ximity.annotation.MvpContract
import net.ximity.annotation.MvpMainComponent
import net.ximity.annotation.MvpScope
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror

@SupportedAnnotationTypes(
        "net.ximity.annotation.MvpMainComponent",
        "net.ximity.annotation.MvpContract"
)
@SupportedOptions(
        Util.OUTPUT_FLAG,
        "kapt.kotlin.generated"
)
class MvpProcessor : AbstractProcessor() {

    private val templatePackageName = "net.ximity.mvp.template"
    private val contractPackageName = "net.ximity.mvp.contract"
    private var halt = false
    private var shouldLog = false
    private val bindings = ArrayList<Binding>()

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    @Synchronized
    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        Util.init(processingEnv)
        val debugLogs = processingEnv.options[Util.OUTPUT_FLAG]
        shouldLog = debugLogs?.toBoolean() == true
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

        val moduleFunctions = ArrayList<MethodSpec>()

        moduleFunctions.add(MethodSpec.methodBuilder("providesView")
                .addAnnotation(MvpScope::class.java)
                .addAnnotation(com.squareup.javapoet.ClassName.get("dagger", "Provides"))
                .returns(com.squareup.javapoet.ClassName.get(viewInterface.get()))
                .addStatement("return view")
                .build())

        moduleFunctions.add(MethodSpec.methodBuilder("providesPresenter")
                .addAnnotation(MvpScope::class.java)
                .addAnnotation(com.squareup.javapoet.ClassName.get("dagger", "Provides"))
                .addParameter(com.squareup.javapoet.ClassName.get(presenter), "impl")
                .returns(com.squareup.javapoet.ClassName.get(presenterInterface.get()))
                .addStatement("return impl")
                .build())

        if (isViewPresenter) {
            moduleFunctions.add(MethodSpec.methodBuilder("providesMvpPresenter")
                    .addAnnotation(MvpScope::class.java)
                    .addAnnotation(com.squareup.javapoet.ClassName.get("dagger", "Provides"))
                    .addParameter(com.squareup.javapoet.ClassName.get(presenter), "impl")
                    .returns(com.squareup.javapoet.ClassName.get(contractPackageName, "MvpPresenter"))
                    .addStatement("return impl")
                    .build())
        }

        Util.writeJavaFile(JavaFile.builder(packageName, com.squareup.javapoet.TypeSpec.classBuilder(moduleClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(com.squareup.javapoet.ClassName.get("dagger", "Module"))
                .addField(FieldSpec.builder(com.squareup.javapoet.ClassName.get(viewInterface.get()), "view")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addParameter(com.squareup.javapoet.ParameterSpec.builder(com.squareup.javapoet.ClassName.get(viewInterface.get()), "view")
                                .addAnnotation(com.squareup.javapoet.ClassName.get("androidx.annotation", "NonNull"))
                                .build())
                        .addModifiers(Modifier.PUBLIC)
                        .addStatement("this.\$N = \$N", "view", "view")
                        .build())
                .addMethods(moduleFunctions)
                .build())
                .build(), moduleClassName, shouldLog)

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

        val mvpBindings = com.squareup.javapoet.TypeSpec.interfaceBuilder(componentName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(MvpScope::class.java)
                .addAnnotation(AnnotationSpec.builder(com.squareup.javapoet.ClassName.get("dagger", "Subcomponent"))
                        .addMember("modules", "\$N.class", moduleName)
                        .build())
                .addMethod(MethodSpec.methodBuilder("bind")
                        .addAnnotation(AnnotationSpec.builder(com.squareup.javapoet.ClassName.get("androidx.annotation", "CheckResult"))
                                .addMember("suggest", "\"#bindPresenter(net.ximity.mvp.contract.MvpPresenter)\"")
                                .build())
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(com.squareup.javapoet.ClassName.get(view), view.simpleName.toString())
                        .returns(com.squareup.javapoet.ClassName.get(view))
                        .build())
                .build()

        Util.writeJavaFile(JavaFile.builder(packageName, mvpBindings)
                .build(), componentName, shouldLog)

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
        Util.writeFile(FileSpec.builder(templatePackageName, "BaseMvpApplication")
                .addType(TypeSpec.classBuilder("BaseMvpApplication")
                        .addModifiers(KModifier.ABSTRACT)
                        .superclass(templateApplication.parameterizedBy(mainComponent))
                        .build())
                .build(), shouldLog)

        val templateActivity = ClassName(templatePackageName, "MvpActivity")
        Util.writeFile(FileSpec.builder(templatePackageName, "BaseMvpActivity")
                .addType(TypeSpec.classBuilder("BaseMvpActivity")
                        .addModifiers(KModifier.ABSTRACT)
                        .superclass(templateActivity.parameterizedBy(mainComponent))
                        .build())
                .build(), shouldLog)

        val templateFragment = ClassName(templatePackageName, "MvpFragment")
        Util.writeFile(FileSpec.builder(templatePackageName, "BaseMvpFragment")
                .addType(TypeSpec.classBuilder("BaseMvpFragment")
                        .addModifiers(KModifier.ABSTRACT)
                        .superclass(templateFragment.parameterizedBy(mainComponent))
                        .build())
                .build(), shouldLog)

        val templateDialog = ClassName(templatePackageName, "MvpDialog")
        Util.writeFile(FileSpec.builder(templatePackageName, "BaseMvpDialog")
                .addType(TypeSpec.classBuilder("BaseMvpDialog")
                        .addModifiers(KModifier.ABSTRACT)
                        .superclass(templateDialog.parameterizedBy(mainComponent))
                        .build())
                .build(), shouldLog)

        val templateReceiver = ClassName(templatePackageName, "MvpBroadcastReceiver")
        Util.writeFile(FileSpec.builder(templatePackageName, "BaseMvpBroadcastReceiver")
                .addType(TypeSpec.classBuilder("BaseMvpBroadcastReceiver")
                        .addModifiers(KModifier.ABSTRACT)
                        .superclass(templateReceiver.parameterizedBy(mainComponent))
                        .build())
                .build(), shouldLog)

        val templateService = ClassName(templatePackageName, "MvpService")
        Util.writeFile(FileSpec.builder(templatePackageName, "BaseMvpService")
                .addType(TypeSpec.classBuilder("BaseMvpService")
                        .addModifiers(KModifier.ABSTRACT)
                        .superclass(templateService.parameterizedBy(mainComponent))
                        .build())
                .build(), shouldLog)

        return true
    }

    private fun generateBaseViews(element: TypeElement): Boolean {
        val mainComponent = ClassName.bestGuess(element.toString())
        val activityMethods = ArrayList<FunSpec>()

        activityMethods.add(FunSpec.builder("bindPresenter")
                .addParameter(ParameterSpec.builder("viewPresenter", TypeVariableName("P"))
                        .build())
                .addStatement("this.viewPresenter = viewPresenter")
                .build())

        activityMethods.add(FunSpec.builder("onCreate")
                .addParameter(ParameterSpec.builder("savedInstanceState", ClassName("android.os", "Bundle")
                        .copy(nullable = true))
                        .build())
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("super.onCreate(savedInstanceState)")
                .addStatement("viewPresenter.create(savedInstanceState)")
                .build())

        activityMethods.add(FunSpec.builder("onStart")
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("super.onStart()")
                .addStatement("viewPresenter.start()")
                .build())

        activityMethods.add(FunSpec.builder("onResume")
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("super.onResume()")
                .addStatement("viewPresenter.resume()")
                .build())

        activityMethods.add(FunSpec.builder("onSaveInstanceState")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter(ParameterSpec.builder("outState", ClassName("android.os", "Bundle"))
                        .build())
                .addStatement("super.onSaveInstanceState(outState)")
                .addStatement("viewPresenter.saveState(outState)")
                .build())

        activityMethods.add(FunSpec.builder("onPause")
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("super.onPause()")
                .addStatement("viewPresenter.pause()")
                .build())

        activityMethods.add(FunSpec.builder("onStop")
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("super.onStop()")
                .addStatement("viewPresenter.stop()")
                .build())

        activityMethods.add(FunSpec.builder("onDestroy")
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("super.onDestroy()")
                .addStatement("viewPresenter.destroy()")
                .build())

        val baseActivity = ClassName(templatePackageName, "MvpActivity")
        Util.writeFile(FileSpec.builder(templatePackageName, "ActivityView")
                .addType(TypeSpec.classBuilder("ActivityView")
                        .addTypeVariable(TypeVariableName.invoke("P", ClassName(contractPackageName, "MvpPresenter")
                                .parameterizedBy(WildcardTypeName.producerOf(ClassName(contractPackageName, "MvpView")))))
                        .addModifiers(KModifier.ABSTRACT)
                        .superclass(baseActivity.parameterizedBy(mainComponent))
                        .addProperty(PropertySpec.builder("viewPresenter", TypeVariableName.invoke("P"))
                                .mutable()
                                .addModifiers(KModifier.PRIVATE, KModifier.LATEINIT)
                                .build())
                        .addFunctions(activityMethods)
                        .build())
                .build(), shouldLog)

        val fragmentMethods = ArrayList<FunSpec>()

        fragmentMethods.add(FunSpec.builder("bindPresenter")
                .addParameter(ParameterSpec.builder("viewPresenter", TypeVariableName("P"))
                        .build())
                .addStatement("this.viewPresenter = viewPresenter")
                .build())

        fragmentMethods.add(FunSpec.builder("onViewCreated")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter(ParameterSpec.builder("view", ClassName("android.view", "View"))
                        .build())
                .addParameter(ParameterSpec.builder("savedInstanceState", ClassName("android.os", "Bundle")
                        .copy(nullable = true))
                        .build())
                .addStatement("super.onViewCreated(view, savedInstanceState)")
                .addStatement("viewPresenter.create(savedInstanceState)")
                .build())

        fragmentMethods.add(FunSpec.builder("onStart")
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("super.onStart()")
                .addStatement("viewPresenter.start()")
                .build())

        fragmentMethods.add(FunSpec.builder("onResume")
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("super.onResume()")
                .addStatement("viewPresenter.resume()")
                .build())

        fragmentMethods.add(FunSpec.builder("onSaveInstanceState")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter(ParameterSpec.builder("outState", ClassName("android.os", "Bundle"))
                        .build())
                .addStatement("super.onSaveInstanceState(outState)")
                .addStatement("viewPresenter.saveState(outState)")
                .build())

        fragmentMethods.add(FunSpec.builder("onPause")
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("super.onPause()")
                .addStatement("viewPresenter.pause()")
                .build())

        fragmentMethods.add(FunSpec.builder("onStop")
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("super.onStop()")
                .addStatement("viewPresenter.stop()")
                .build())

        fragmentMethods.add(FunSpec.builder("onDestroy")
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("super.onDestroy()")
                .addStatement("viewPresenter.destroy()")
                .build())

        val baseFragment = ClassName(templatePackageName, "MvpFragment")
        Util.writeFile(FileSpec.builder(templatePackageName, "FragmentView")
                .addType(TypeSpec.classBuilder("FragmentView")
                        .addTypeVariable(TypeVariableName.invoke("P", ClassName(contractPackageName, "MvpPresenter")
                                .parameterizedBy(WildcardTypeName.producerOf(ClassName(contractPackageName, "MvpView")))))
                        .addModifiers(KModifier.ABSTRACT)
                        .superclass(baseFragment.parameterizedBy(mainComponent))
                        .addProperty(PropertySpec.builder("viewPresenter", TypeVariableName("P"))
                                .mutable()
                                .addModifiers(KModifier.PRIVATE, KModifier.LATEINIT)
                                .build())
                        .addFunctions(fragmentMethods)
                        .build())
                .build(), shouldLog)

        val baseDialog = ClassName(templatePackageName, "MvpDialog")
        Util.writeFile(FileSpec.builder(templatePackageName, "DialogView")
                .addType(TypeSpec.classBuilder("DialogView")
                        .addTypeVariable(TypeVariableName.invoke("P", ClassName(contractPackageName, "MvpPresenter")
                                .parameterizedBy(WildcardTypeName.producerOf(ClassName(contractPackageName, "MvpView")))))
                        .addModifiers(KModifier.ABSTRACT)
                        .superclass(baseDialog.parameterizedBy(mainComponent))
                        .addProperty(PropertySpec.builder("viewPresenter", TypeVariableName("P"))
                                .mutable()
                                .addModifiers(KModifier.PRIVATE, KModifier.LATEINIT)
                                .build())
                        .addFunctions(fragmentMethods)
                        .build())
                .build(), shouldLog)

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
                }.let { Util.writeJavaFile(it, componentName, shouldLog) }

        return true
    }
}
