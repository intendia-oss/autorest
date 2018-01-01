package com.intendia.gwt.autorest.processor;

import com.intendia.gwt.autorest.client.JacksonMapper;
import com.intendia.gwt.autorest.client.JsonMapper;
import com.progressoft.brix.domino.gwtjackson.DefaultJsonDeserializationContext;
import com.progressoft.brix.domino.gwtjackson.JsonDeserializationContext;
import com.progressoft.brix.domino.gwtjackson.ObjectMapper;
import com.progressoft.brix.domino.gwtjackson.annotation.JSONMapper;
import com.progressoft.brix.domino.gwtjackson.deser.array.ArrayJsonDeserializer;
import com.progressoft.brix.domino.gwtjackson.stream.JsonReader;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;

import static javax.lang.model.element.Modifier.*;

public class JsonMapperGenerator {
    private ClassName beanName;
    private String mapperName;
    private String jsonMapperName;

    public JsonMapperGenerator(ClassName beanName) {
        this.beanName = beanName;
        this.mapperName = beanName.simpleName() + "Mapper";
        this.jsonMapperName = beanName.simpleName() + "JsonMapper";
    }

    public TypeSpec generate() {

        ParameterizedTypeName jacksonMapper = ParameterizedTypeName.get(ClassName.get(ObjectMapper.class), beanName);
        FieldSpec instance = FieldSpec.builder(ClassName.bestGuess(mapperName), "INSTANCE")
                .addModifiers(PUBLIC, STATIC, FINAL)
                .initializer("new $T()", ClassName.bestGuess(jsonMapperName + "_" + mapperName + "Impl"))
                .build();
        TypeSpec objectMapper = TypeSpec.interfaceBuilder(mapperName)
                .addModifiers(PUBLIC)
                .addSuperinterface(jacksonMapper)
                .addAnnotation(JSONMapper.class)
                .addField(instance)
                .build();


        return TypeSpec.classBuilder(jsonMapperName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(JacksonMapper.class)
                .addSuperinterface(JsonMapper.class)
                .addType(objectMapper)
                .addMethod(readMethod())
                .addMethod(readAsArrayMethod())
                .addMethod(writeMethod())
                .build();

    }

    private MethodSpec writeMethod() {
        TypeVariableName typeVariable = TypeVariableName.get("T");
        return MethodSpec.methodBuilder("write")
                .addModifiers(PUBLIC)
                .addAnnotation(Override.class)
                .addTypeVariable(typeVariable)
                .returns(String.class)
                .addParameter(typeVariable, "value")
                .addStatement("return $T.INSTANCE.write(($T) value)", ClassName.bestGuess(mapperName), beanName)
                .build();
    }

    private MethodSpec readAsArrayMethod() {
        TypeVariableName typeVariable = TypeVariableName.get("T");
        return MethodSpec.methodBuilder("readAsArray")
                .addParameter(String.class, "json")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addTypeVariable(typeVariable)
                .returns(typeVariable)
                .addCode(readAsArrayBody())
                .build();
    }

    private CodeBlock readAsArrayBody() {
        return CodeBlock.builder()
                .addStatement("$T deserializer = ArrayJsonDeserializer" +
                                ".newInstance(new " + "$T(), $T[]::new)",
                        ParameterizedTypeName.get(ClassName.get(ArrayJsonDeserializer.class),
                                beanName),
                        ClassName.bestGuess(beanName.simpleName() + "BeanJsonDeserializerImpl"),
                        beanName)
                .addStatement("$T context = $T.builder().build()", JsonDeserializationContext.class, DefaultJsonDeserializationContext.class)
                .addStatement("$T reader = context.newJsonReader(json)", JsonReader.class)
                .addStatement("return (T) deserializer.deserialize(reader, context)")
                .build();
    }

    private MethodSpec readMethod() {
        TypeVariableName type = TypeVariableName.get("T");
        return MethodSpec.methodBuilder("read")
                .addParameter(String.class, "json")
                .addModifiers(PUBLIC)
                .addTypeVariable(type)
                .returns(type)
                .addAnnotation(Override.class)
                .addCode(readBody()).build();
    }

    private CodeBlock readBody() {
        return CodeBlock.builder()
                .addStatement("return (T) $T.INSTANCE.read(json)"
                        , ClassName.bestGuess(mapperName))
                .build();
    }
}
