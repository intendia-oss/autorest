package com.intendia.gwt.autorest.processor;

import static com.google.auto.common.MoreTypes.isTypeOf;
import static com.squareup.javapoet.ClassName.bestGuess;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static java.util.stream.Collectors.toSet;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.auto.common.MoreTypes;
import com.google.common.base.Throwables;
import com.intendia.gwt.autorest.client.AutoRestGwt;
import com.progressoft.brix.domino.gwtjackson.ObjectMapper;
import com.progressoft.brix.domino.gwtjackson.annotation.JSONMapper;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic;
import jsinterop.annotations.JsType;
import rx.Observable;
import rx.Single;

public class JsonMapperProcessor extends AbstractProcessor {
    private Set<String> beansMapper = new HashSet<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) return false;
        for (Element element : roundEnv.getElementsAnnotatedWith(AutoRestGwt.class)) {
            if (!(element.getKind().isInterface() && element instanceof TypeElement)) continue;
            TypeElement restService = (TypeElement) element;
            try {
                List<ExecutableElement> methods = restService.getEnclosedElements().stream()
                        .filter(e -> e.getKind() == METHOD && !isIncompatible(e) && e instanceof ExecutableElement)
                        .map(e -> (ExecutableElement) e)
                        .filter(m -> !m.getModifiers().contains(STATIC) && !m.isDefault())
                        .collect(Collectors.toList());

                for (ExecutableElement method : methods) {
                    DeclaredType returnType = MoreTypes.asDeclared(method.getReturnType());
                    if (isJsType(returnType) || (!isObservable(returnType) && !isSingle(returnType))) continue;
                    TypeElement returnElement = asTypeElement(returnType);
                    if (isInterface(returnElement) || isTypeOf(Void.class, returnElement.asType())) continue;
                    if (!beansMapper.add(returnElement.getQualifiedName().toString())) continue;
                    ClassName name = ClassName.get(returnElement);
                    String mapperName = name.simpleName() + "Mapper";
                    String mapperImplName = (name.simpleName() + "JsonMapper") + "_" + mapperName + "Impl";
                    JavaFile.builder(ClassName.get(restService).packageName(), interfaceBuilder(mapperName)
                            .addAnnotation(JSONMapper.class).addModifiers(PUBLIC)
                            .addSuperinterface(ParameterizedTypeName.get(ClassName.get(ObjectMapper.class), name))
                            .addField(FieldSpec.builder(bestGuess(mapperName), "INSTANCE")
                                    .addModifiers(PUBLIC, STATIC, FINAL)
                                    .initializer("new $T()", bestGuess(mapperImplName))
                                    .build())
                            .build()
                    ).build().writeTo(processingEnv.getFiler());
                }
            } catch (Exception e) {
                error("error generating json mappers" + restService + ": " + e + "\n"
                        + Throwables.getStackTraceAsString(e));
            }
        }

//        for (Element configuration : roundEnv.getElementsAnnotatedWith(JacksonConfiguration.class)) {
//            try {
//                TypeSpec generate = TypeSpec.classBuilder("MappersConfigurator_Generated").addModifiers(PUBLIC)
//                        .addMethod(methodBuilder("configure").addModifiers(PUBLIC)
//                                .addCode(beansMapper.stream().map(ClassName::bestGuess).map(m ->
//                                        CodeBlock.of("$T.register($T.class.getCanonicalName(), new $T());",
//                                                MappersRegistry.class, m, bestGuess(m.simpleName() + "JsonMapper")))
//                                        .reduce(builder().build(), (a, b) -> builder().add(a).add(b).build()))
//                                .build())
//                        .build();
//                JavaFile.builder(packageOf(configuration), generate).build().writeTo(processingEnv.getFiler());
//            } catch (IOException e) {
//                error("error generating mappers configurator " + e.getMessage());
//            }
//        }

        return false;
    }

    private String packageOf(Element configuration) {
        return processingEnv.getElementUtils().getPackageOf(configuration).getQualifiedName().toString();
    }

    private boolean isJsType(DeclaredType declaredType) {
        return MoreTypes.asTypeElement(declaredType.getTypeArguments().get(0)).getAnnotationMirrors().stream()
                .anyMatch(o -> o.getAnnotationType().toString().endsWith(JsType.class.getCanonicalName()));
    }

    private boolean isInterface(TypeElement typeElement) {
        return typeElement.getKind().isInterface();
    }

    private TypeElement asTypeElement(DeclaredType declaredType) {
        return MoreTypes.asTypeElement(declaredType.getTypeArguments().get(0));
    }

    private boolean isSingle(DeclaredType declaredType) {
        return isTypeOf(Single.class, declaredType);
    }

    private boolean isObservable(DeclaredType declaredType) {
        return isTypeOf(Observable.class, declaredType);
    }

    private boolean isIncompatible(Element method) {
        return method.getAnnotationMirrors().stream()
                .anyMatch(a -> a.getAnnotationType().toString().endsWith("GwtIncompatible"));
    }

    private void error(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Stream.of(AutoRestGwt.class).map(Class::getCanonicalName).collect(toSet());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
