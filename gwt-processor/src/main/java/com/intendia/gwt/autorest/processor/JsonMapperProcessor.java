package com.intendia.gwt.autorest.processor;

import com.google.auto.common.MoreTypes;
import com.google.auto.service.AutoService;
import com.google.common.base.Throwables;
import com.intendia.gwt.autorest.client.AutoRestGwt;
import com.intendia.gwt.autorest.client.JacksonConfiguration;
import com.intendia.gwt.autorest.client.JacksonMapper;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import jsinterop.annotations.JsType;
import rx.Observable;
import rx.Single;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.auto.common.MoreTypes.isTypeOf;
import static java.util.Collections.singleton;
import static javax.lang.model.element.Modifier.STATIC;

@AutoService(Processor.class)
public class JsonMapperProcessor extends AbstractProcessor {
    private Set<String> beansMapper = new HashSet<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) return false;
        roundEnv.getElementsAnnotatedWith(AutoRestGwt.class).stream()
                .filter(e -> e.getKind().isInterface() && e instanceof TypeElement).map(e -> (TypeElement) e)
                .forEach(restService -> {
                    try {
                        generateJsonMappers(restService);
                    } catch (Exception e) {
                        error("uncaught exception processing rest service " + restService + ": " + e + "\n"
                                + Throwables.getStackTraceAsString(e));
                    }
                });

        roundEnv.getElementsAnnotatedWith(JacksonConfiguration.class)
                .forEach(configuration -> generateMappersConfigurator(beansMapper, packageOf(configuration)));

        return false;
    }

    private String packageOf(Element configuration) {
        return processingEnv.getElementUtils().getPackageOf(configuration).getQualifiedName().toString();
    }

    private void generateMappersConfigurator(Set<String> mappers, String packageName) {
        TypeSpec generate = new MappersConfiguratorGenerator(mappers).generate();
        try {
            JavaFile.builder(packageName, generate).build().writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            error("Error while generating mappers configurator " + e.getMessage());
        }
    }

    private void generateJsonMappers(TypeElement restService) {
        List<ExecutableElement> methods = restService.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.METHOD && e instanceof ExecutableElement)
                .map(e -> (ExecutableElement) e)
                .filter(method -> !(method.getModifiers().contains(STATIC) || method.isDefault()))
                .filter(method -> !isIncompatible(method).isPresent())
                .collect(Collectors.toList());

        ClassName rsName = ClassName.get(restService);

        methods.forEach(method -> generateMapperIfRequired(method, rsName.packageName()));
    }

    private void generateMapperIfRequired(ExecutableElement method, String packageName) {
        DeclaredType returnType = MoreTypes.asDeclared(method.getReturnType());
        Stream.of(returnType)
                .filter(declaredType -> !isJsType(declaredType))
                .filter(declaredType -> isObservable(declaredType) || isSingle(declaredType))
                .map(this::asTypeElement)
                .filter(typeElement -> !isInterface(typeElement))
                .filter(typeElement -> !isTypeOf(Void.class, typeElement.asType()))
                .forEach(typeElement -> generateJsonMapper(packageName, typeElement));
    }

    private boolean isJsType(DeclaredType declaredType) {
        TypeElement beanType = MoreTypes.asTypeElement(declaredType.getTypeArguments().get(0));
        return beanType.getAnnotationMirrors()
                .stream()
                .anyMatch(o -> o.getAnnotationType().toString().endsWith(JsType.class.getCanonicalName()));
    }

    private void generateJsonMapper(String packageName, TypeElement typeElement) {
        if (!beansMapper.contains(typeElement.getQualifiedName().toString())) {
            beansMapper.add(typeElement.getQualifiedName().toString());
            try {
                JavaFile.builder(packageName,
                        new JsonMapperGenerator(ClassName.get(typeElement)).generate())
                        .build()
                        .writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                error("Error while generating json mapper\n"
                        + Throwables.getStackTraceAsString(e));
            }
        }
    }

    private boolean isInterface(TypeElement typeElement) {
        return typeElement.getKind().isInterface();
    }

    private TypeElement asTypeElement(DeclaredType declaredType) {
        TypeMirror beanType = declaredType.getTypeArguments().get(0);
        return MoreTypes.asTypeElement(beanType);
    }

    private boolean isSingle(DeclaredType declaredType) {
        return isTypeOf(Single.class, declaredType);
    }

    private boolean isObservable(DeclaredType declaredType) {
        return isTypeOf(Observable.class, declaredType);
    }

    private Optional<? extends AnnotationMirror> isIncompatible(ExecutableElement method) {
        return method.getAnnotationMirrors().stream().filter(this::isIncompatible).findAny();
    }

    private boolean isIncompatible(AnnotationMirror a) {
        return a.getAnnotationType().toString().endsWith("GwtIncompatible");
    }

    private void error(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
    }

    @Override
    public Set<String> getSupportedOptions() {
        return singleton("debug");
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<>(Arrays.asList(AutoRestGwt.class.getCanonicalName(),
                JacksonMapper.class.getCanonicalName(),
                JacksonConfiguration.class.getCanonicalName()));
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
