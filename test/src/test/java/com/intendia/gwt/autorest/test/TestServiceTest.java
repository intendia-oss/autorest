package com.intendia.gwt.autorest.test;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestServiceTest {

    @Test
    public void verify_test_service_has_been_processed() {
        assertNotNull(TestService_RestServiceModel.class);
    }
}