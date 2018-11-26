package com.intendia.gwt.autorest.client;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * A set of private utility methods used by {@link TypeToken} in JavaSE/Android environments.
 */
@GwtIncompatible
class ReflectionTypeUtils {
	private ReflectionTypeUtils() {}

    public static Type[] getActualTypeArguments(Type type) {
    	if (type instanceof ParameterizedType)
    		return ((ParameterizedType) type).getActualTypeArguments();
    	else 
    		return new Type[0];
    }
    
    public static Class<?> getRawType(Type type) {
        // For wildcard or type variable, the first bound determines the runtime type.
        return getRawTypes(type).iterator().next();
    }

    private static Collection<Class<?>> getRawTypes(Type type) {
        if (type instanceof Class<?>) {
          return Collections.<Class<?>>singleton((Class<?>) type);
        } else if (type instanceof ParameterizedType) {
          ParameterizedType parameterizedType = (ParameterizedType) type;
          // JDK implementation declares getRawType() to return Class<?>: http://goo.gl/YzaEd
          return Collections.<Class<?>>singleton((Class<?>) parameterizedType.getRawType());
        } else if (type instanceof GenericArrayType) {
          GenericArrayType genericArrayType = (GenericArrayType) type;
          return Collections.<Class<?>>singleton(getArrayClass(getRawType(genericArrayType.getGenericComponentType())));
        } else if (type instanceof TypeVariable) {
          return getRawTypes(((TypeVariable<?>) type).getBounds());
        } else if (type instanceof WildcardType) {
          return getRawTypes(((WildcardType) type).getUpperBounds());
        } else {
          throw new AssertionError(type + " unsupported");
        }
      }

    /** Returns the {@code Class} object of arrays with {@code componentType}. */
    private static Class<?> getArrayClass(Class<?> componentType) {
      return Array.newInstance(componentType, 0).getClass();
    }

    private static Collection<Class<?>> getRawTypes(Type[] types) {
    	return Arrays.stream(types)
    		.flatMap(type -> getRawTypes(type).stream())
    		.collect(Collectors.toList());
    }
}
