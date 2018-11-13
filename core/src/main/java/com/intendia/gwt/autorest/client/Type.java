package com.intendia.gwt.autorest.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Recursive structure to model Java type meta-data
 * associated with corresponding parameter or method
 * return result.
 */
public class Type {
	private final boolean array;
	private final List<Type> typeParameters;
	private final Class<?> clazz;
	
	private Type(Class<?> clazz) {
		this(clazz, Collections.<Type>emptyList(), false);
	}
	
	private Type(Class<?> clazz, List<Type> typeParameters, boolean isArray) {
		this.clazz = clazz;
		this.typeParameters = typeParameters;
		this.array = isArray;
	}
	
	public boolean isArray() {
		return array;
	}
	
	public boolean isGeneric() {
		return !typeParameters.isEmpty(); 
	}
	
	public boolean isDefined() {
		return clazz == null; 
	}
	
	public static Type of(Class<?> clazz) {
		return new Type(clazz);
	}
	
	public static Type array(Type type) {
		return new Type(type.clazz, type.typeParameters, true);
	}
	
	public static Type undefined() {
		return new Type(null);
	}
	
	public Type typeParam(Type type) {
		List<Type> typeParams = new ArrayList<>(this.typeParameters);
		typeParams.add(type);
		return new Type(this.clazz, typeParams, type.array);
	}
	
	public Class<?> getClazz() {
		return clazz;
	}
	
	public List<Type> getTypeParams() {
		return Collections.unmodifiableList(typeParameters);
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(clazz.getSimpleName());
		
		if (!typeParameters.isEmpty()) {
			result.append("<");
			for(Type t: typeParameters) {
				result.append(t.toString());
				result.append(", ");
			}
			result.delete(result.length()-2, result.length());
			result.append(">");
		}
		
		return result.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (array ? 1231 : 1237);
		result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
		result = prime * result + ((typeParameters == null) ? 0 : typeParameters.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		
		if(!(other instanceof Type))
			return false;
		
		Type otype = (Type)other;
		if (array != otype.array)
			return false;
		
		if (clazz == null) {
			if (otype.clazz != null)
				return false;
		} else {
			if (!clazz.equals(otype.clazz))
				return false;
		}
		
		if (!typeParameters.equals(otype.typeParameters))
			return false;
		
		return true;
	}
}
