package com.intendia.gwt.autorest.processor;

import com.intendia.gwt.autorest.client.mapper.MappersRegistry;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.Set;

public class MappersConfiguratorGenerator {
    private final Set<String> mappers;

    public MappersConfiguratorGenerator(Set<String> mapper) {
        this.mappers = mapper;
    }

    public TypeSpec generate() {
        MethodSpec configureMethod = MethodSpec.methodBuilder("configure")
                .addModifiers(Modifier.PUBLIC)
                .addCode(configureBody())
                .build();

        return TypeSpec.classBuilder("MappersConfigurator_Generated")
                .addModifiers(Modifier.PUBLIC)
                .addMethod(configureMethod)
                .build();
    }

    private CodeBlock configureBody() {
        CodeBlock.Builder builder = CodeBlock.builder();
        mappers.forEach(bean -> {
            ClassName beanName = ClassName.bestGuess(bean);
            builder.addStatement("$T.register($T.class.getCanonicalName(), new $T())",
                    MappersRegistry.class, beanName, ClassName.bestGuess(beanName.simpleName() + "JsonMapper"));
        });
        return builder.build();
    }
}
