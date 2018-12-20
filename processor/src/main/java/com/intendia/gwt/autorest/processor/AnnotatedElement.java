package com.intendia.gwt.autorest.processor;

import static com.google.auto.common.MoreTypes.asElement;

import java.lang.annotation.Annotation;
import java.util.Objects;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;


/**
 * A minimal abstraction of an annotated element (a class, method or method parameter).
 * 
 * {@link AnnotationProcessor} works with this abstraction only, which allows it to target the 
 * javax.lang.model-based elements
 */
public class AnnotatedElement {
	private String simpleName;
	private Element jlmElement;
	private TypeMirror jlmType;

	public AnnotatedElement(String simpleName, Element jlmElement, TypeMirror jlmType) {
		this.simpleName = simpleName;
		this.jlmElement = jlmElement;
		this.jlmType = jlmType;
	}
	
	public Element getJlmElement() {
		return jlmElement;
	}
	
	public TypeMirror getJlmType() {
		return jlmType;
	}
	
	public String getSimpleName() {
		return simpleName;
	}

	public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
		return jlmElement.getAnnotation(annotationClass);
	}
	
	public <T extends Annotation> T getAnnotationOverAnnotations(Class<T> annotationClass) {
		return jlmElement.getAnnotationMirrors().stream()
			.map(a -> asElement(a.getAnnotationType()).getAnnotation(annotationClass))
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
	}
}
