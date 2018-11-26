package com.intendia.gwt.autorest.client;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.Test;

public class TypeTokenTest {
	public static final java.util.List<java.util.Map<java.lang.String, java.lang.Integer[]>>[] TEST_FIELD = null;
	
    @Test 
    public void simpleTest() {
    	assertThat(TypeToken.of(String.class).stringify(), equalTo("java.lang.String"));
    	assertThat(new TypeToken<Integer>(Integer.class) {}.stringify(), equalTo("java.lang.Integer"));
    }

    @Test 
    public void arrayTest() {
    	assertThat(TypeToken.of(String[].class).stringify(), equalTo("java.lang.String[]"));
    }
    
    @Test 
    public void genericTest() {
    	assertThat(
    		new TypeToken<List<Map<String, Integer[]>>>(
	    			List.class, 
	    			new TypeToken<Map<String, Integer>>(Map.class, TypeToken.of(String.class), TypeToken.of(Integer[].class)))
    			.stringify(), 
    		equalTo("java.util.List<java.util.Map<java.lang.String, java.lang.Integer[]>>"));
    }

    @Test 
    public void genericTestArray() {
    	assertThat(
    		new TypeToken<List<Map<String, Integer[]>>[]>(
    			null,
   				new TypeToken<List<Map<String, Integer[]>>>(
	    			List.class, 
	    			new TypeToken<Map<String, Integer>>(Map.class, TypeToken.of(String.class), TypeToken.of(Integer[].class))))
    			.stringify(), 
    		equalTo("java.util.List<java.util.Map<java.lang.String, java.lang.Integer[]>>[]"));
    }
    
    @Test 
    public void genericJRETest() {
    	assertThat(
    		new TypeToken<List<Map<String, Integer[]>>>() {}
    			.stringify(), 
    		equalTo("java.util.List<java.util.Map<java.lang.String, java.lang.Integer[]>>"));

    	assertThat(
    		new TypeToken<List<Map<String, Integer[]>>>(
    			List.class, 
    			new TypeToken<Map<String, Integer>>(Map.class, TypeToken.of(String.class), TypeToken.of(Integer[].class))),
    		equalTo(new TypeToken<List<Map<String, Integer[]>>>() {}));
    	
    	try {
			assertThat(
				TypeToken.of(TypeTokenTest.class.getDeclaredField("TEST_FIELD").getGenericType()).stringify(),
				equalTo("java.util.List<java.util.Map<java.lang.String, java.lang.Integer[]>>[]"));
		} catch (NoSuchFieldException | SecurityException e) {
			throw new RuntimeException(e);
		}
    }
}
