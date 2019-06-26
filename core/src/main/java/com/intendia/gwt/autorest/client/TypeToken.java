package com.intendia.gwt.autorest.client;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * A "supertype" token capable of representing any Java type.
 * 
 * While the purpose of this class is primarily to be instantiated by AutoRest itself and then just used by the user code, it is a public API.<br><br>
 *  
 * Based on ideas from <a href="http://gafter.blogspot.com/2006/12/super-type-tokens.html">http://gafter.blogspot.com/2006/12/super-type-tokens.html</a>,
 * as well as on the implementation of the same notion in various Java libraries (Guava's TypeToken, Jackson's TypeReference, Guice's TypeLiteral, 
 * Gson's TypeToken, Apache Commons TypeLiteral - too many to enumerate all of those here).
 * 
 * Unlike all those other implementations however, this {@link TypeToken} is capable of operating in a GWT/J2CL environment as well, where Java reflection 
 * - and therefore, the {@link Type} class on which all other implementations are based of - is not available. 
 * 
 * How to use:
 * <li> For simple, non-parameterized types like {@link String}, {@link Integer} or non-parameterized Java Beans:<br>
 * <ul><li><code>TypeToken.of(String.class)</code></ul>
 * <li> For parameterized types, like List&lt;String&gt;, in JavaSE/Android environments:<br>
 * <ul><li><code>new TypeToken&lt;List&lt;String&gt;&gt;() {}</code><br></ul>
 * Please note that the above statement creates an anonymous inner class - a necessary precondition for the type to be captured correctly.
 * <li> For parameterized types, like List&lt;String&gt;, in GWT/J2CL environments:<br>
 * <ul><li><code>new TypeToken&lt;List&lt;String&gt;&gt;(List.class, TypeToken.of(String)) {}</code></ul>
 * 
 * A more advanced example with a multiple-nesting of a parameterized type like List&lt;Map&lt;Integer, String&gt;&gt;:<br>
 * <li> In JavaSE/Android environments:<br>
 * <ul><li><code>new TypeToken&lt;List&lt;Map&lt;Integer, String&gt;&gt;() {}</code><br></ul>
 * <li> In GWT/J2CL environments:<br>
 * <ul><li><code>new TypeToken&lt;List&lt;Map&lt;Integer, String&gt;&gt;(List.class, new TypeToken&lt;Map&lt;Integer, String&gt;&gt;(Map.class, TypeToken.of(Integer.class), TypeToken.of(String.class)) {}) {}</code></ul>
 * <br>
 * 
 * The syntax for GWT/J2CL is much more verbose and requires the captured type not only to be written in the type parameter of the type token (<code>TypeToken&lt;...&gt;</code>) 
 * but to be also explicitly enumerated as a pair of a raw class type reference (i.e. "<code>List.class</code>") plus a chain of nested type token instantiations describing all the 
 * instantiations of the type parameters of the raw type for the concrete type we are trying to capture with the type token. This verbosity is unavoidable, because GWT/J2CL is missing Java reflection, 
 * which in turn prohibits the {@link TypeToken} instance from "introspecting" itself and figuring out the type automatically.
 * <br><br>
 * Nevertheless, the concept of a type token is very useful in these environments too, as it allows generified interfaces like the following (from AutoRest <code>RequestVisitor</code>):<br>
 * <li><code><T> ResourceVisitor param(String key, T data, TypeToken<T> typeToken)</code><br>
 * Without the presence of the {@link TypeToken} parameter, the above method signature deteriorates to:<br> 
 * <li><code><T> ResourceVisitor param(String key, Object data)</code><br><br>
 * 
 * Last but not least, the presence of {@link TypeToken} in the above method signatures allows AutoRest to easily interoperate with 3rd party libraries which e.g. need to know the proper type 
 * so as to deserialize the (JSON) payload returned from the server. 
 */
public class TypeToken<T> implements Comparable<TypeToken<T>> {
	private Class<? super T> rawType;
	private TypeToken<?>[] typeArguments;
	
	@GwtIncompatible
    private Type runtimeType;
	
	public static <T> TypeToken<T> of(Class<T> type) {
	    return new TypeToken<T>(type, new TypeToken<?>[0]);
	}

	/**
	 * Allows the construction of a {@link TypeToken} instance based on a provided Java Reflection {@link Type} instance. Not type safe.  
	 */
    @GwtIncompatible
    public static TypeToken<?> of(Type type) {
        return new TypeToken<Object>(type);
    }

    /**
     * Use this constructor only in code that needs to be compatible with GWtT/J2CL. For JavaSE/Android-only code, {@link TypeToken#TypeToken()} quite a bit less verbose.
     */
    protected TypeToken(Class<? super T> rawType, TypeToken<?>... typeArguments) {
    	if (rawType != null && rawType.isArray()) {
    		// User provided the array directly as a raw class
    		// Normalize to the standard type token representation of arrays, where the raw type is null, and the array component type is in the type arguments   
    		if (typeArguments.length > 0)
    			throw new IllegalArgumentException("To create a type token for an array, either pass the non-generic array class instance as the raw type and keep the type argumetns empty, or pass null as raw type and provide a single type argument for the component type of the (possibly generic) array");
    		
    		typeArguments = new TypeToken<?>[] {TypeToken.of(rawType.getComponentType())};
    		rawType = null;
    	}
    	
    	this.rawType = rawType;
    	this.typeArguments = typeArguments;
    }

	/**
	 * Less verbose alternative to {@link TypeToken#TypeToken(Class, TypeToken...)}. Only available in JavaSE/Android.  
	 */
    @GwtIncompatible
    protected TypeToken() {
    	initialize();
    }

    @GwtIncompatible
    private TypeToken(Type type) {
    	this.runtimeType = type;
    	initialize();
    }
    
    /**
     * Return the raw type represented by this {@link TypeToken} instance. E.g.:<br>
     * <li>When called on <code>TypeToken&lt;String&gt;</code> it will return <code>String.class</code> 
     * <li>When called on <code>TypeToken&lt;List&lt;String&gt;&gt;</code> it will return <code>List.class</code><br><rr>
     * 
     * For arrays, this method will return null.
     */
    public final Class<? super T> getRawType() {
    	return rawType;
    }

    /**
     * Return the type tokens corresponding to the type arguments of the parameterized type represented by this type token. If the type is not parameterized, 
     * an empty array is returned. For example:<br>
     * <li>When called on <code>TypeToken&lt;String&gt;</code> an empty array will be returned 
     * <li>When called on <code>TypeToken&lt;List&lt;String&gt;&gt;</code> a single-element array with a type token <code>TypeToken&lt;String&gt;</code> 
     * <li>When called on <code>TypeToken&lt;String[]&gt;</code> a single-element array with a type token <code>TypeToken&lt;String&gt;</code> will be returned as well 
     */
    public final TypeToken<?>[] getTypeArguments() {
		return typeArguments;
	}
    
    /**
     * A JavaSE/Android-only method that returns the underlying Java Reflection {@link Type} instance. 
     */
    @GwtIncompatible
    public final synchronized Type getType() {
    	initialize(); // Call initialize() because runtimeType might not be populated yet in case the (only) GWT-compatible constructor was used 
    	return runtimeType;
    }
    
    /**
     * The only reason we define this method (and require implementation
     * of <code>Comparable</code>) is to prevent constructing a
     * reference without type information.
     */
    @Override
    public final int compareTo(TypeToken<T> o) {
    	return 0;
    }


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((rawType == null) ? 0 : rawType.hashCode());
		result = prime * result + Arrays.hashCode(typeArguments);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof TypeToken<?>))
			return false;
		TypeToken<?> other = (TypeToken<?>) obj;
		if (rawType == null) {
			if (other.rawType != null)
				return false;
		} else if (!rawType.equals(other.rawType))
			return false;
		if (!Arrays.equals(typeArguments, other.typeArguments))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return  "TypeToken<" + stringify() + ">";
	}

	public final String stringify() {
		StringBuilder buf = new StringBuilder();

		stringify(buf);
		
		return buf.toString();
	}
	
	private void stringify(StringBuilder buf) {
		if (getRawType() != null) {
			buf.append(getRawType().getName());
			
			if (typeArguments.length > 0) {
				buf.append('<');
				
				for (int i = 0; i < typeArguments.length; i++) {
					if(i > 0)
						buf.append(", ");
					
					typeArguments[i].stringify(buf);
				}
				
				buf.append('>');
			}
		} else {
			typeArguments[0].stringify(buf);
			
			buf.append("[]");
		}
	}
	
    @SuppressWarnings("unchecked")
	@GwtIncompatible
    private void initialize() {
    	if (runtimeType == null) {
    		if (getClass() == TypeToken.class) {
    			// The type token was created with a raw type only
    			// Assign the raw type to the runtime type
    			if (rawType != null)
    				runtimeType = rawType;
    			else
    				// Array
    				runtimeType = Array.newInstance(typeArguments[0].getRawType(), 0).getClass();
    		} else {
    			// The type token was created via inheritance and is likely not representing the raw type
    			// Extract the actual type using the "supertype" token trick
    			
	            Type superClass = getClass().getGenericSuperclass();
	            
	            if (superClass instanceof Class<?>) { // Should never happen
	                throw new IllegalArgumentException("Internal error: TypeReference constructed without actual type information");
	            }
	            
	            runtimeType = ((ParameterizedType) superClass).getActualTypeArguments()[0];
    		}
    	}

    	if (typeArguments == null) {
	    	if (runtimeType instanceof GenericArrayType) {
		    	rawType = null;
		    	typeArguments = new TypeToken<?>[] {new TypeToken<Object>(((GenericArrayType)runtimeType).getGenericComponentType())};
	    	} else {
		    	rawType = (Class<? super T>)ReflectionTypeUtils.getRawType(runtimeType);

		    	typeArguments = Arrays.stream(ReflectionTypeUtils.getActualTypeArguments(runtimeType))
		    		.map(TypeToken::new)
		    		.toArray(TypeToken<?>[]::new);

		    	if (rawType.isArray()) {
		    		// Normalize to the canonical representtion of an array
		    		if (typeArguments.length == 0)
		    			typeArguments = new TypeToken<?>[] {new TypeToken<Object>(rawType.getComponentType())};
		    		
		    		rawType = null;
		    	}
	    	}
    	}
    }
}
