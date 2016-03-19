package com.intendia.gwt.autorest.processor;

import static com.google.auto.common.MoreElements.getAnnotationMirror;
import static com.google.auto.common.MoreTypes.asElement;
import static com.google.auto.common.MoreTypes.isTypeOf;
import static com.google.common.collect.FluentIterable.from;
import static java.util.Collections.singleton;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.js.JsType;
import com.intendia.gwt.autorest.client.AutoRestGwt;
import com.intendia.gwt.autorest.client.AutoRestGwt.TypeMap;
import com.intendia.gwt.autorest.client.Dispatcher;
import com.intendia.gwt.autorest.client.Resource;
import com.intendia.gwt.autorest.client.RestServiceProxy;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import javax.ws.rs.Consumes;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import rx.Observable;
import rx.Single;

public class AutoRestGwtProcessor extends AbstractProcessor {
    private Set<? extends TypeElement> annotations;
    private RoundEnvironment roundEnv;

    @Override public Set<String> getSupportedAnnotationTypes() { return singleton(AutoRestGwt.class.getName()); }

    @Override public SourceVersion getSupportedSourceVersion() { return SourceVersion.latestSupported(); }

    @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) return false;

        this.annotations = annotations;
        this.roundEnv = roundEnv;
        try {
            processAnnotations();
        } catch (Exception e) {
            // We don't allow exceptions of any kind to propagate to the compiler
            fatalError(Throwables.getStackTraceAsString(e));
        }
        return true;
    }

    private void processAnnotations() throws Exception {
        List<? extends TypeElement> elements = from(roundEnv.getElementsAnnotatedWith(AutoRestGwt.class))
                .filter(TypeElement.class)
                .filter(input -> input.getKind().isInterface())
                .toList();

        log(annotations.toString());
        log(elements.toString());

        Map<TypeMirror, TypeMirror> typeMap = new HashMap<>();

        for (TypeElement restService : elements) {
            AnnotationMirror annotation = getAnnotationMirror(restService, AutoRestGwt.class).get();

            TypeMap[] types = restService.getAnnotation(AutoRestGwt.class).types();
            for (TypeMap type : types) typeMap.put(typeMap_type(type), typeMap_with(type));
            Function<TypeMirror, TypeMirror> typeMapper = t -> typeMap.getOrDefault(t, t);

            String rsPath = restService.getAnnotation(Path.class).value();
            String rsConsumes = Optional.ofNullable(restService.getAnnotation(Consumes.class))
                    .map(a -> a.value().length == 0 ? "*/*" : a.value()[0])
                    .orElse("*/*");

            ClassName serviceName = ClassName.get(restService);
            log("rest service interface: " + serviceName);

            ClassName adapterName = ClassName
                    .get(serviceName.packageName(), serviceName.simpleName() + "_RestServiceProxy");
            log("rest service proxy: " + adapterName);

            TypeSpec.Builder adapterBuilder = TypeSpec.classBuilder(adapterName.simpleName())
                    .addOriginatingElement(restService)
                    .addModifiers(Modifier.PUBLIC)
                    .superclass(RestServiceProxy.class)
                    .addSuperinterface(TypeName.get(restService.asType()));

            adapterBuilder.addMethod(MethodSpec.constructorBuilder()
                    .addModifiers(PUBLIC)
                    .addParameter(Resource.class, "resource")
                    .addParameter(Dispatcher.class, "dispatcher")
                    .addStatement("super($L, $L, $S)", "resource", "dispatcher", rsPath)
                    .build());

            FluentIterable<? extends ExecutableElement> methods = from(restService.getEnclosedElements())
                    .filter(element -> element.getKind() == ElementKind.METHOD)
                    .filter(ExecutableElement.class)
                    .filter(method -> !(method.getModifiers().contains(STATIC) || method.isDefault()));
            for (ExecutableElement method : methods) {
                String methodName = method.getSimpleName().toString();

                if (isIncompatible(method)) {
                    adapterBuilder.addMethod(MethodSpec.overriding(method)
                            .addStatement("throw new $T(\"$L\")", UnsupportedOperationException.class, methodName)
                            .build());
                    continue;
                }

                boolean isObservable = isTypeOf(Observable.class, method.getReturnType());
                boolean isSingle = isTypeOf(Single.class, method.getReturnType());
                if (!isObservable && !isSingle) {
                    error("Observable<T> return type required", method, annotation);
                    continue;
                }

                String mPath = Optional.ofNullable(method.getAnnotation(Path.class))
                        .map(Path::value)
                        .orElse("");

                CodeBlock.Builder builder = CodeBlock.builder();
                builder.add("return resource($S)$[\n", mPath);
                {
                    // query params
                    method.getParameters().stream()
                            .filter(p -> p.getAnnotation(QueryParam.class) != null)
                            .forEach(p -> builder.add(".param($S, $L)\n",
                                    p.getAnnotation(QueryParam.class).value(), p.getSimpleName())
                            );
                    // method type
                    builder.add(".method($S)\n", from(method.getAnnotationMirrors())
                            .transform(a -> asElement(a.getAnnotationType()).getAnnotation(HttpMethod.class))
                            .filter(Predicates.notNull()).transform(HttpMethod::value).first().or("GET"));
                    // accept
                    String accept = Optional.ofNullable(method.getAnnotation(Consumes.class))
                            .map(a -> a.value().length == 0 ? "*/*" : a.value()[0])
                            .orElse(rsConsumes);
                    if (!accept.equals("*/*")) builder.add(".accept($S)\n", accept);
                    // data
                    method.getParameters().stream()
                            .filter(a -> a.getAnnotation(QueryParam.class) == null
                                    || a.getAnnotation(PathParam.class) == null)
                            .findFirst().ifPresent(data -> builder.add(".data($L)\n", data.getSimpleName()));

                }

                builder.add("." + (isObservable ? "observe" : "single") + "(dispatcher());\n$]");
                adapterBuilder.addMethod(MethodSpec.overriding(method).addCode(builder.build()).build());

            }

            Filer filer = processingEnv.getFiler();
            JavaFile.builder(serviceName.packageName(), adapterBuilder.build()).build().writeTo(filer);
        }
    }

    private boolean isIncompatible(ExecutableElement method) {
        return method.getAnnotationMirrors().stream().anyMatch(this::isIncompatible);
    }

    private boolean isIncompatible(AnnotationMirror a) {
        return a.getAnnotationType().toString().endsWith("GwtIncompatible");
    }

    private boolean isOverlay(TypeMirror T) {
        // Void
        TypeMirror vd = processingEnv.getElementUtils().getTypeElement(Void.class.getName()).asType();
        // JavaScriptObject
        TypeMirror js = processingEnv.getElementUtils().getTypeElement(JavaScriptObject.class.getName()).asType();
        return processingEnv.getTypeUtils().isSubtype(T, js)
                || T.getAnnotationsByType(JsType.class).length > 0
                || processingEnv.getTypeUtils().isSameType(T, vd);
    }

    private Iterable<AnnotationSpec> transformAnnotations(List<? extends AnnotationMirror> annotationMirrors) {
        return from(annotationMirrors)
                .filter(input -> !isTypeOf(AutoRestGwt.class, input.getAnnotationType()))
                .transform(AnnotationSpec::get);
    }

    private void log(String msg) {
        if (processingEnv.getOptions().containsKey("debug")) {
            processingEnv.getMessager().printMessage(Kind.NOTE, msg);
        }
    }

    private void error(String msg, Element element, AnnotationMirror annotation) {
        processingEnv.getMessager().printMessage(Kind.ERROR, msg, element, annotation);
    }

    private void fatalError(String msg) {
        processingEnv.getMessager().printMessage(Kind.ERROR, "FATAL ERROR: " + msg);
    }

    private TypeMirror typeMap_type(TypeMap annotation) {
        try {
            annotation.type();
            throw new RuntimeException("unreachable");
        } catch (MirroredTypeException exception) {
            return exception.getTypeMirror();
        }
    }

    private TypeMirror typeMap_with(TypeMap annotation) {
        try {
            annotation.with();
            throw new RuntimeException("unreachable");
        } catch (MirroredTypeException exception) {
            return exception.getTypeMirror();
        }
    }
}
