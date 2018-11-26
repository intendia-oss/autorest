package com.intendia.gwt.autorest.client;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Objects;

import com.intendia.gwt.autorest.client.AnnotationProcessor.AnnotatedElement;

/**
 * An {@link AnnotatedElement} implementation which delegates to {@link java.lang.reflect.AnnotatedElement}.
 */
public class JLRAnnotatedElement implements AnnotatedElement {
	private String simpleName;
	private java.lang.reflect.AnnotatedElement jlrAnnotatedElement;
	private java.lang.reflect.Type jlrType;

	public JLRAnnotatedElement(String simpleName, java.lang.reflect.AnnotatedElement jlrAnnotatedElement, java.lang.reflect.Type jlrType) {
		this.simpleName = simpleName;
		this.jlrAnnotatedElement = jlrAnnotatedElement;
		this.jlrType = jlrType;
	}
	
	public java.lang.reflect.AnnotatedElement getJlrAnnotatedElement() {
		return jlrAnnotatedElement;
	}
	
	public java.lang.reflect.Type getJlrType() {
		return jlrType;
	}
	
	@Override
	public String getSimpleName() {
		return simpleName;
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
		return jlrAnnotatedElement.getAnnotation(annotationClass);
	}
	
	@Override
	public <T extends Annotation> T getAnnotationOverAnnotations(Class<T> annotationClass) {
		return Arrays.stream(jlrAnnotatedElement.getAnnotations())
			.map(Annotation::annotationType)
			.map(at -> at.getAnnotation(annotationClass))
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
	}
}
