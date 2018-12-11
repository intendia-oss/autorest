package com.intendia.gwt.autorest.client;

import static java.util.Optional.ofNullable;
import static javax.ws.rs.HttpMethod.GET;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * A utility class capable of processing a type annotated with JAX-RS annotations and extracting the REST paths, 
 * HTTP method type, as well as data, query, header and form parameters' information. 
 */
public class AnnotationProcessor {
	/**
	 * A minimal abstraction of an annotated element (a class, method or method parameter).
	 * 
	 * {@link AnnotationProcessor} works with this abstraction only, which allows it to target the 
	 * java.lang.reflection-based representations of a class, method and method parameters as well as their 
	 * javax.lang.model-based analogs.
	 */
	public interface AnnotatedElement {
		String getSimpleName();
		
    	<T extends Annotation> T getAnnotationOverAnnotations(Class<T> annotationClass);
    	<T extends Annotation> T getAnnotation(Class<T> annotationClass);
    }

	public static class ParamInfo {
		private String name;
		private AnnotatedElement annotatedElement;
		
		private int javaArgumentIndex;
		private String javaArgumentName;
		
		public ParamInfo(String name, AnnotatedElement annotatedElement, int javaArgumentIndex, String javaArgumentName) {
			this.name = name;
			this.annotatedElement = annotatedElement;
			this.javaArgumentIndex = javaArgumentIndex;
			this.javaArgumentName = javaArgumentName;
		}
		
		public String getName() {
			return name;
		}
		
		public AnnotatedElement getAnnotatedElement() {
			return annotatedElement;
		}
		
		public int getJavaArgumentIndex() {
			return javaArgumentIndex;
		}
		
		public String getJavaArgumentName() {
			return javaArgumentName;
		}
	}
	
    private AnnotatedElement annotatedElement;

    public AnnotationProcessor(AnnotatedElement annotatedElement) {
		this.annotatedElement = annotatedElement;
	}
	
    public String getHttpMethod() {
    	return ofNullable(annotatedElement.getAnnotationOverAnnotations(HttpMethod.class))
			.map(a -> ((HttpMethod)a).value())
			.orElse(GET);
    }
    
    public Stream<Object> getPaths(Stream<? extends Entry<Integer, AnnotatedElement>> parameters) {
    	List<? extends Entry<Integer, AnnotatedElement>> paramList = parameters.collect(Collectors.toList());

    	return Arrays.stream(
	    		ofNullable(annotatedElement.getAnnotation(Path.class))
	    			.map(Path::value)
	    			.orElse("")
	    			.split("/"))
			.filter(s -> !s.isEmpty())
			.map(path -> !path.startsWith("{")? 
				(Object)path: 
				paramList.stream()
					.filter(entry -> ofNullable(entry.getValue().getAnnotation(PathParam.class))
						.map(PathParam::value)
						.map(v -> path.equals("{" + v + "}"))
						.orElse(false))
					.findFirst()
		    		.map(entry -> {
		    			int javaArgumentIndex = entry.getKey();
		    			AnnotatedElement annotatedElement = entry.getValue();
		    			
		    			return new ParamInfo(
		    				null,
		    				annotatedElement,
		    				javaArgumentIndex,
		    				annotatedElement.getSimpleName());
		    		})
					.orElseThrow(() -> new IllegalArgumentException("Unknown path parameter: " + path)));
    }
    
    public Stream<String> getProduces(String... produces) {
    	return Arrays.stream(
    		ofNullable(annotatedElement.getAnnotation(Produces.class))
    			.map(Produces::value)
    			.orElse(produces));
    }
    
    public Stream<String> getConsumes(String... consumes) {
    	return Arrays.stream(
			ofNullable(annotatedElement.getAnnotation(Consumes.class))
				.map(Consumes::value)
				.orElse(consumes));
    }

	public Stream<ParamInfo> getQueryParams(Stream<? extends Entry<Integer, AnnotatedElement>> parameters) {
    	return getParams(QueryParam.class, QueryParam::value, parameters);
    }
    
	public Stream<ParamInfo> getHeaderParams(Stream<? extends Entry<Integer, AnnotatedElement>> parameters) {
    	return getParams(HeaderParam.class, HeaderParam::value, parameters);
    }
	
	public Stream<ParamInfo> getFormParams(Stream<? extends Entry<Integer, AnnotatedElement>> parameters) {
    	return getParams(FormParam.class, FormParam::value, parameters);
    }
    
    public Optional<ParamInfo> getData(Stream<? extends Entry<Integer, AnnotatedElement>> parameters) {
    	return parameters
    		.filter(entry -> !isParam(entry.getValue()))
    		.map(entry -> {
    			int javaArgumentIndex = entry.getKey();
    			AnnotatedElement annotatedElement = entry.getValue();
    			
    			return new ParamInfo(
    				null,
    				annotatedElement,
    				javaArgumentIndex,
    				annotatedElement.getSimpleName());
    		})
    		.findFirst();
    }

	private <T extends Annotation> Stream<ParamInfo> getParams(
			Class<T> paramAnnotationClass,
			Function<T, String> paramNameAnnotationExtractor,
			Stream<? extends Entry<Integer, AnnotatedElement>> parameters) {
    	return parameters
    		.filter(entry -> entry.getValue().getAnnotation(paramAnnotationClass) != null)
    		.map(entry -> {
    			int javaArgumentIndex = entry.getKey();
    			AnnotatedElement annotatedElement = entry.getValue();
    			String nameFromAnnotation = paramNameAnnotationExtractor.apply(entry.getValue().getAnnotation(paramAnnotationClass));
    			
    			return new ParamInfo(
    				nameFromAnnotation != null? nameFromAnnotation: annotatedElement.getSimpleName(),
    				annotatedElement,
    				javaArgumentIndex,
    				annotatedElement.getSimpleName());
    		});
    }
    
    private boolean isParam(AnnotatedElement annotatedElement) {
        return annotatedElement.getAnnotation(CookieParam.class) != null
            || annotatedElement.getAnnotation(FormParam.class) != null
            || annotatedElement.getAnnotation(HeaderParam.class) != null
            || annotatedElement.getAnnotation(MatrixParam.class) != null
            || annotatedElement.getAnnotation(PathParam.class) != null
            || annotatedElement.getAnnotation(QueryParam.class) != null;
    }
}
