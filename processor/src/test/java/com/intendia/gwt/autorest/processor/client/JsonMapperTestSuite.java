package com.intendia.gwt.autorest.processor.client;

import com.google.gwt.junit.tools.GWTTestSuite;
import junit.framework.Test;
import junit.framework.TestSuite;

public class JsonMapperTestSuite extends GWTTestSuite {

    public static Test suite() {
        TestSuite testSuite = new TestSuite("Tests for autorest-gwt-processor");
        testSuite.addTestSuite(JsonMapperGeneratorTest.class);
        return testSuite;
    }
}
