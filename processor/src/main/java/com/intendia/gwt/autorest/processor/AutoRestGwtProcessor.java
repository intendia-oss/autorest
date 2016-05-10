package com.intendia.gwt.autorest.processor;

import static com.google.auto.common.MoreTypes.asElement;
import static java.util.Collections.singleton;
import static java.util.Optional.ofNullable;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.HEAD;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;

import com.google.common.base.Throwables;
import com.intendia.gwt.autorest.client.AutoRestGwt;
import com.intendia.gwt.autorest.client.ResourceBuilder;
import com.intendia.gwt.autorest.client.RestServiceProxy;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic.Kind;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

public class AutoRestGwtProcessor extends AbstractProcessor {
    private Set<String> HTTP_METHODS = Stream.of(GET, POST, PUT, DELETE, HEAD, OPTIONS).collect(Collectors.toSet());

    @Override public Set<String> getSupportedOptions() { return singleton("debug"); }

    @Override public Set<String> getSupportedAnnotationTypes() { return singleton(AutoRestGwt.class.getName()); }

    @Override public SourceVersion getSupportedSourceVersion() { return SourceVersion.latestSupported(); }

    @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) return false;
        roundEnv.getElementsAnnotatedWith(AutoRestGwt.class).stream()
                .filter(e -> e.getKind().isInterface() && e instanceof TypeElement).map(e -> (TypeElement) e)
                .forEach(restService -> {
                    try {
                        processRestService(restService);
                    } catch (Exception e) {
                        // We don't allow exceptions of any kind to propagate to the compiler
                        error("uncaught exception processing rest service " + restService + ": " + e + "\n"
                                + Throwables.getStackTraceAsString(e));
                    }
                });
        return true;
    }

    private void processRestService(TypeElement restService) throws Exception {
        String rsPath = restService.getAnnotation(Path.class).value();

        ClassName rsName = ClassName.get(restService);
        log("rest service interface: " + rsName);

        ClassName proxyName = ClassName.get(rsName.packageName(), rsName.simpleName() + "_RestServiceProxy");
        log("rest service proxy: " + proxyName);

        TypeSpec.Builder proxyTypeBuilder = TypeSpec.classBuilder(proxyName.simpleName())
                .addOriginatingElement(restService)
                .addModifiers(Modifier.PUBLIC)
                .superclass(RestServiceProxy.class)
                .addSuperinterface(TypeName.get(restService.asType()));

        proxyTypeBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(ParameterizedTypeName.get(Supplier.class, ResourceBuilder.class), "factory")
                .addStatement("super(() -> $L.get().path($S))", "factory", rsPath)
                .build());

        List<ExecutableElement> methods = restService.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.METHOD && e instanceof ExecutableElement)
                .map(e -> (ExecutableElement) e)
                .filter(method -> !(method.getModifiers().contains(STATIC) || method.isDefault()))
                .collect(Collectors.toList());

        Set<String> methodImports = new HashSet<>();
        for (ExecutableElement method : methods) {
            String methodName = method.getSimpleName().toString();

            if (isIncompatible(method)) {
                proxyTypeBuilder.addMethod(MethodSpec.overriding(method)
                        .addStatement("throw new $T(\"$L\")", UnsupportedOperationException.class, methodName)
                        .build());
                continue;
            }

            CodeBlock.Builder builder = CodeBlock.builder().add("$[return factory.get()");
            {
                // resolve paths
                builder.add(".path($L)", Arrays
                        .stream(ofNullable(method.getAnnotation(Path.class)).map(Path::value).orElse("").split("/"))
                        .filter(s -> !s.isEmpty()).map(path -> !path.startsWith("{") ? "\"" + path + "\"" : method
                                .getParameters().stream()
                                .filter(a -> ofNullable(a.getAnnotation(PathParam.class)).map(PathParam::value)
                                        .map(v -> path.equals("{" + v + "}")).orElse(false))
                                .findFirst().map(VariableElement::getSimpleName).map(Object::toString)
                                .orElse("null /* path param " + path + " does not match any argument! */"))
                        .collect(Collectors.joining(", ")));
                // query params
                method.getParameters().stream()
                        .filter(p -> p.getAnnotation(QueryParam.class) != null)
                        .forEach(p -> builder.add(".param($S, $L)",
                                p.getAnnotation(QueryParam.class).value(), p.getSimpleName())
                        );
                // method type
                builder.add(".method($L)", methodImport(methodImports, method.getAnnotationMirrors().stream()
                        .map(a -> asElement(a.getAnnotationType()).getAnnotation(HttpMethod.class))
                        .filter(a -> a != null).map(HttpMethod::value).findFirst().orElse(GET)));
                // data
                method.getParameters().stream().filter(this::isParam).findFirst()
                        .ifPresent(data -> builder.add(".data($L)", data.getSimpleName()));
            }
            builder.add(".build($T.class);\n$]",
                    processingEnv.getTypeUtils().erasure(method.getReturnType()));

            proxyTypeBuilder.addMethod(MethodSpec.overriding(method).addCode(builder.build()).build());
        }

        Filer filer = processingEnv.getFiler();
        JavaFile.Builder file = JavaFile.builder(rsName.packageName(), proxyTypeBuilder.build());
        for (String methodImport : methodImports) file.addStaticImport(HttpMethod.class, methodImport);
        file.build().writeTo(filer);
    }

    private String methodImport(Set<String> methodImports, String method) {
        if (HTTP_METHODS.contains(method)) {
            methodImports.add(method); return method;
        } else {
            return "\"" + method + "\"";
        }
    }

    public boolean isParam(VariableElement a) {
        return a.getAnnotation(CookieParam.class) == null
                && a.getAnnotation(FormParam.class) == null
                && a.getAnnotation(HeaderParam.class) == null
                && a.getAnnotation(MatrixParam.class) == null
                && a.getAnnotation(PathParam.class) == null
                && a.getAnnotation(QueryParam.class) == null;
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

    private void error(String msg) {
        processingEnv.getMessager().printMessage(Kind.ERROR, msg);
    }
}
