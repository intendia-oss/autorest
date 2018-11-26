package com.intendia.gwt.autorest.client;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.intendia.gwt.autorest.client.AnnotationProcessor.AnnotatedElement;
import com.intendia.gwt.autorest.client.AnnotationProcessor.ParamInfo;

/**
 * A simple {@link Proxy}-based alternative to generating source for JAX-RS proxies via AutorestGwtProcessor.
 * Since GWT/J2CL is supporting reflection, this factory can only be used with JavaSE/Android.
 */
public class AutorestProxy {
	private static Object[] EMPTY = new Object[0];
			
    private AutorestProxy() {} // Namespace
	
    private interface ParamVisitor {
    	<T> void visit(String key, T value, TypeToken<T> typeToken);
    }
    
    private interface UnnamedParamVisitor {
    	<T> void visit(T value, TypeToken<T> typeToken);
    }
    
	public static <T> T create(Class<T> restService, ResourceVisitor.Supplier path) {
		return create(Thread.currentThread().getContextClassLoader(), restService, path);
	}
	
	public static <T> T create(ClassLoader classLoader, Class<T> restService, ResourceVisitor.Supplier path) {
        Stream<? extends Method> methods = Arrays.stream(restService.getMethods())
            .filter(method -> !((method.getModifiers()&java.lang.reflect.Modifier.STATIC) != 0 || method.isDefault()));

        Map<Method, BiFunction<Object, Object[], Object>> restServiceMethodProxies = methods
        	.map(method -> new SimpleEntry<>(method, createMethodProxy(method, path)))
        	.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        
        return restService.cast(Proxy.newProxyInstance(
        	classLoader, 
        	new Class<?>[] {restService}, 
        	(proxy, method, args) -> restServiceMethodProxies.get(method).apply(proxy, args)));
	}
	
	private static BiFunction<Object, Object[], Object> createMethodProxy(Method method, ResourceVisitor.Supplier path) {
		Class<?> restService = method.getDeclaringClass();

		String name = method.getName();
		Parameter[] parameters = method.getParameters();

		// Handle the non-JAX-RS methods first
		if (name.equals("toString") && parameters.length == 0)
			return (proxy, args) -> proxy.getClass().getName() + "::Proxy";
		else if (name.equals("hashCode") && parameters.length == 0)
			return (proxy, args) -> 0;
		else if (name.equals("equals") && parameters.length == 1 && parameters[0].getType() == Object.class)
			return (proxy, args) -> proxy.equals(args[0]);
					
		// For each JAX-RS method, prepare a closure of precomputed data that will be used during the method invocation
		// The data is obtained by analyzing the JAX-RS-annotated rest API interface
			
		AnnotatedElement restServiceAnnotatedElement = new JLRAnnotatedElement(restService.getSimpleName(), restService, null);
		
		AnnotationProcessor restServiceProcessor = new AnnotationProcessor(restServiceAnnotatedElement);
		
        Object[] restServicePaths = restServiceProcessor.getPaths(Stream.empty()).toArray(Object[]::new);
        String[] restServiceProduces = restServiceProcessor.getProduces().toArray(String[]::new);
        String[] restServiceConsumes = restServiceProcessor.getConsumes().toArray(String[]::new);

        AnnotatedElement methodAnnotatedElement = new JLRAnnotatedElement(method.getName(), method, method.getGenericReturnType());
		
		AnnotationProcessor methodProcessor = new AnnotationProcessor(methodAnnotatedElement);
		
		Supplier<Stream<? extends Entry<Integer, AnnotatedElement>>> parametersFactory = () -> 
			IntStream
				.range(0, parameters.length)
				.mapToObj(index -> new SimpleEntry<>(
					index, 
					new JLRAnnotatedElement(parameters[index].getName(), parameters[index], parameters[index].getParameterizedType())));
		
		String httpMethod = methodProcessor.getHttpMethod();
		
		Object[] paths = methodProcessor.getPaths(parametersFactory.get()).toArray(Object[]::new);
		
		String[] produces = methodProcessor.getProduces(restServiceProduces).toArray(String[]::new);
		String[] consumes = methodProcessor.getConsumes(restServiceConsumes).toArray(String[]::new);

		ParamInfo[] queryParams = methodProcessor.getQueryParams(parametersFactory.get()).toArray(ParamInfo[]::new);
		ParamInfo[] headerParams = methodProcessor.getHeaderParams(parametersFactory.get()).toArray(ParamInfo[]::new);
		ParamInfo[] formParams = methodProcessor.getFormParams(parametersFactory.get()).toArray(ParamInfo[]::new);
		
		ParamInfo data = methodProcessor.getData(parametersFactory.get()).orElse(null);
		
		return (proxy, args) -> {
			ResourceVisitor visitor = path.get();

			visitor
				.path(restServicePaths)
				.method(httpMethod);
			
			accept(paths, args, visitor);

			visitor
				.produces(produces)
				.consumes(consumes);
			
			if (args == null)
				args = EMPTY;
			
			accept(queryParams, args, visitor::param);
			accept(headerParams, args, visitor::header);
			accept(formParams, args, visitor::form);
			
			if(data != null)
				accept(data, args, visitor::data, createTypeToken(data.getAnnotatedElement()));

			return visitor.as(createTypeToken(methodAnnotatedElement));
		};
	}
	
	private static void accept(Object[] paths, Object[] args, ResourceVisitor visitor) {
		Arrays.stream(paths).forEach(path -> {
			if (path instanceof ParamInfo) {
				ParamInfo paramInfo = (ParamInfo)path;
				accept(paramInfo, args, (UnnamedParamVisitor)visitor::path, createTypeToken(paramInfo.getAnnotatedElement()));
			} else
				visitor.path(path);
		});
	}
	
	private static void accept(ParamInfo[] params, Object[] args, ParamVisitor paramVisitor) {
		Arrays.stream(params).forEach(paramInfo -> 
			accept(paramInfo, args, paramVisitor, createTypeToken(paramInfo.getAnnotatedElement())));
	}

	private static <T> void accept(ParamInfo paramInfo, Object[] args, ParamVisitor paramVisitor, TypeToken<T> typeToken) {
		@SuppressWarnings("unchecked")
		T arg = (T)args[paramInfo.getJavaArgumentIndex()];
		
		paramVisitor.visit(paramInfo.getName(), arg, typeToken);
	}

	private static <T> void accept(ParamInfo paramInfo, Object[] args, UnnamedParamVisitor paramVisitor, TypeToken<T> typeToken) {
		@SuppressWarnings("unchecked")
		T arg = (T)args[paramInfo.getJavaArgumentIndex()];
		
		paramVisitor.visit(arg, typeToken);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static <T> TypeToken<T> createTypeToken(AnnotatedElement annotatedElement) {
		JLRAnnotatedElement jlrAnnotatedElement = (JLRAnnotatedElement)annotatedElement;
		
		return (TypeToken)TypeToken.of(jlrAnnotatedElement.getJlrType());
	}
}
