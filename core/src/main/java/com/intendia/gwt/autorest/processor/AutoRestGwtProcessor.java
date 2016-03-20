package com.intendia.gwt.autorest.processor;

import static com.google.auto.common.MoreElements.getAnnotationMirror;
import static com.google.auto.common.MoreTypes.asElement;
import static com.google.auto.common.MoreTypes.isTypeOf;
import static java.util.Collections.singleton;
import static java.util.Optional.ofNullable;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.common.base.Throwables;
import com.intendia.gwt.autorest.client.AutoRestGwt;
import com.intendia.gwt.autorest.client.Dispatcher;
import com.intendia.gwt.autorest.client.Resource;
import com.intendia.gwt.autorest.client.RestServiceProxy;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
        List<TypeElement> elements = roundEnv.getElementsAnnotatedWith(AutoRestGwt.class).stream()
                .filter(e -> e.getKind().isInterface() && e instanceof TypeElement)
                .map(e -> (TypeElement) e).collect(Collectors.toList());

        log(annotations.toString());
        log(elements.toString());

        for (TypeElement restService : elements) {
            //noinspection OptionalGetWithoutIsPresent
            AnnotationMirror annotation = getAnnotationMirror(restService, AutoRestGwt.class).get();

            String rsPath = restService.getAnnotation(Path.class).value();
            String rsConsumes = ofNullable(restService.getAnnotation(Consumes.class))
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

            List<ExecutableElement> methods = restService.getEnclosedElements().stream()
                    .filter(e -> e.getKind() == ElementKind.METHOD && e instanceof ExecutableElement)
                    .map(e -> (ExecutableElement) e)
                    .filter(method -> !(method.getModifiers().contains(STATIC) || method.isDefault()))
                    .collect(Collectors.toList());

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

                String methodPath = ofNullable(method.getAnnotation(Path.class)).map(Path::value).orElse("");
                String resolvedPath = Arrays.stream(methodPath.split("/")).filter(s -> !s.isEmpty()).map(subPath -> {
                    if (subPath.startsWith("{")) {
                        String pathParamName = subPath.substring(1, subPath.length() - 1);
                        return method.getParameters().stream()
                                .filter(a -> ofNullable(a.getAnnotation(PathParam.class)).map(PathParam::value)
                                        .map(pathParamName::equals).orElse(false))
                                .findFirst().map(a -> a.getSimpleName().toString())
                                .orElse("\"path param '" + pathParamName + "\" does not match any param!");
                    } else {
                        return "\"" + subPath + "\"";
                    }
                }).collect(Collectors.joining(", "));

                final String N = ""; // separator between each element
                CodeBlock.Builder builder = CodeBlock.builder();
                builder.add("$[return resolve($L)", resolvedPath);
                {
                    // query params
                    method.getParameters().stream()
                            .filter(p -> p.getAnnotation(QueryParam.class) != null)
                            .forEach(p -> builder.add(".param($S, $L)" + N,
                                    p.getAnnotation(QueryParam.class).value(), p.getSimpleName())
                            );
                    // method type
                    builder.add(".method($S)" + N, method.getAnnotationMirrors().stream()
                            .map(a -> asElement(a.getAnnotationType()).getAnnotation(HttpMethod.class))
                            .filter(a -> a != null).map(HttpMethod::value).findFirst().orElse("GET"));
                    // accept
                    String accept = ofNullable(method.getAnnotation(Consumes.class))
                            .map(a -> a.value().length == 0 ? "*/*" : a.value()[0])
                            .orElse(rsConsumes);
                    if (!accept.equals("*/*")) builder.add(".accept($S)" + N, accept);
                    // data
                    method.getParameters().stream()
                            .filter(a -> a.getAnnotation(QueryParam.class) == null
                                    || a.getAnnotation(PathParam.class) == null)
                            .findFirst().ifPresent(data -> builder.add(".data($L)" + N, data.getSimpleName()));

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
}
