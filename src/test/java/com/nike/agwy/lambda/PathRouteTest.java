package com.nike.agwy.lambda;

import io.vavr.Tuple2;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class PathRouteTest {

    @Test
    public void testRoot() {
        Tuple2<Boolean, Map<String, String>> result = ResourceParamMatcher.getPathMatch("/").apply("/");
        boolean matched = result._1;
        Map<String, String> captured = result._2;
        assertFalse(matched);
        assertEquals(captured.size(), 0);
    }

    @Test
    public void testCompileToRegexp() {
        Tuple2<Boolean, Map<String, String>> result = ResourceParamMatcher.getPathMatch("/{test }").apply("/1234567890");
        assertTrue(result._1);
        assertEquals(result._2.size(), 1);
        assertEquals(result._2.get("test"), "1234567890");
    }

    @Test
    public void testMatch1() {
        Tuple2<Boolean, Map<String, String>> result = ResourceParamMatcher.getPathMatch("foo/yuk/{id}").apply("foo/yuk/5963");
        assertTrue(result._1);
        assertEquals(result._2.size(), 1);
        assertEquals(result._2.get("id"), "5963");
    }

    @Test
    public void testMatch2() {
        Tuple2<Boolean, Map<String, String>> result = ResourceParamMatcher.getPathMatch("/{lat}/{lng}").apply("/137.555/64.222");
        assertTrue(result._1);
        assertEquals(result._2.size(), 2);
        assertEquals(result._2.get("lat"), "137.555");
        assertEquals(result._2.get("lng"), "64.222");
    }
}