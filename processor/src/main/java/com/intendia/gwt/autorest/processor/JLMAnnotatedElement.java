package com.intendia.gwt.autorest.processor;

import static com.google.auto.common.MoreTypes.asElement;

import java.lang.annotation.Annotation;
import java.util.Objects;

import javax.lang.model.element.Element;

import com.intendia.gwt.autorest.client.AnnotationProcessor.AnnotatedElement;

/**
 * An {@link AnnotatedElement} implementation which delegates to {@link Element}.
 */
public class JLMAnnotatedElement implements AnnotatedElement {
	private String simpleName;
	private Element jlmElement;

	public JLMAnnotatedElement(String simpleName, Element jlmElement) {
		this.simpleName = simpleName;
		this.jlmElement = jlmElement;
	}
	
	public Element getJlmElement() {
		return jlmElement;
	}
	
	@Override
	public String getSimpleName() {
		return simpleName;
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
		return jlmElement.getAnnotation(annotationClass);
	}
	
	@Override
	public <T extends Annotation> T getAnnotationOverAnnotations(Class<T> annotationClass) {
		return jlmElement.getAnnotationMirrors().stream()
			.map(a -> asElement(a.getAnnotationType()).getAnnotation(annotationClass))
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
	}
}
