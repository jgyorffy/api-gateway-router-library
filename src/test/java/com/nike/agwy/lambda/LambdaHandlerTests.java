package com.nike.agwy.lambda;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nike.agwy.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("Integration")
public class LambdaHandlerTests {
    private static final ObjectMapper objectMapper = new ObjectMapper();


    @Test
    public void routeHandlerHappyWithAWSParams() throws Exception {

        Map<String, String> pathParam = new HashMap<>();

        LambdaHandler lambdaHandler = new LambdaHandler() {
            @Override
            protected void loadControllers(Router router) {
                router.addController(routeHandler -> routeHandler.setRouteHandler("/pathvar/{operand1}/{operand2}", RouteHandler.HTTPMethod.GET, objectMapper, (request) -> {
                    pathParam.put("operand1", request.getResourceParamAsString("operand1"));
                    pathParam.put("operand2", request.getResourceParamAsString("operand2"));
                    return LambdaResponse.builder()
                            .returnCode(200)
                            .headers(new HashMap<>())
                            .responseObject(new TestModel("1234", "USA")).build();
                }, getErrorHandlers()));
            }
        };

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        lambdaHandler.handleRequest(TestUtils.getFileIO("mock/agwy-search-with-path-params.json"), bout, null);
        AwsProxyResponse response = TestUtils.deserialize(bout);
        assertEquals(response.getStatusCode(), 200);
        assertTrue(response.getBody().contains("USA"));
        assertTrue(response.getBody().contains("1234"));
        assertEquals(pathParam.get("operand1"), "user");
        assertEquals(pathParam.get("operand2"), "test");
    }

    @Test
    public void routeHandlerHappyWithAWSParams2() throws Exception {

        Map<String, String> pathParam = new HashMap<>();

        LambdaHandler lambdaHandler = new LambdaHandler() {
            @Override
            protected void loadControllers(Router router) {
                router.addController(routeHandler -> routeHandler.setRouteHandler("/store/stores/v2/{id}", RouteHandler.HTTPMethod.PUT, objectMapper, (request) -> {
                    pathParam.put("id", request.getResourceParamAsString("id"));
                    return LambdaResponse.builder()
                            .returnCode(200)
                            .responseObject(new TestModel("1234", "USA")).build();
                }, getErrorHandlers()));
            }
        };

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        lambdaHandler.handleRequest(TestUtils.getFileIO("mock/agwy-search-with-path-params2.json"), bout, null);
        AwsProxyResponse response = TestUtils.deserialize(bout);
        assertEquals(response.getStatusCode(), 200);
        assertTrue(response.getBody().contains("USA"));
        assertTrue(response.getBody().contains("1234"));
        assertEquals(pathParam.get("id"), "1117cf87-a6ea-4bba-9f2b-5599a3a62342");
    }

    @Test
    public void routeHandlerSearch() throws Exception {

        Map<String, String> pathParam = new HashMap<>();

        LambdaHandler lambdaHandler = new LambdaHandler() {
            @Override
            protected void loadControllers(Router router) {
                router.addController(routeHandler -> routeHandler.setRouteHandler("/store/store_views/v1", RouteHandler.HTTPMethod.GET, objectMapper, (request) -> {
                    pathParam.put("search", request.getResourceParamAsString("search"));
                    return LambdaResponse.builder()
                            .returnCode(200)
                            .responseObject(new TestModel("1234", "USA")).build();
                }, getErrorHandlers()));
            }
        };

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        lambdaHandler.handleRequest(TestUtils.getFileIO("mock/agwy-search-store.json"), bout, null);
        AwsProxyResponse response = TestUtils.deserialize(bout);
        assertEquals(response.getStatusCode(), 200);
        assertTrue(response.getBody().contains("USA"));
        assertTrue(response.getBody().contains("1234"));
        assertEquals(pathParam.get("search"), "address.country==CHL");
    }

    @Test
    public void routeHandlerHappyWithParsedParams() throws Exception {

        Map<String, String> pathParam = new HashMap<>();

        LambdaHandler lambdaHandler = new LambdaHandler() {
            @Override
            protected void loadControllers(Router router) {
                router.addController(routeHandler -> routeHandler.setRouteHandler("/store/store_views/v1/{id}", RouteHandler.HTTPMethod.GET, objectMapper, (request) -> {
                    pathParam.put("id", request.getResourceParamAsString("id"));
                    return LambdaResponse.builder()
                            .returnCode(200)
                            .responseObject(new TestModel("1234", "USA")).build();
                }, getErrorHandlers()));

                router.addController(routeHandler -> routeHandler.setRouteHandler("/store/store_views/v1/about", RouteHandler.HTTPMethod.GET, objectMapper, (request) -> {
                    pathParam.put("id", request.getResourceParamAsString("bla bla"));
                    return LambdaResponse.builder()
                            .returnCode(200)
                            .responseObject("Howdee folks").build();
                }, getErrorHandlers()));
            }
        };

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        lambdaHandler.handleRequest(TestUtils.getFileIO("mock/agwy-get-with-id.json"), bout, null);
        AwsProxyResponse response = TestUtils.deserialize(bout);
        assertEquals(response.getStatusCode(), 200);
        assertTrue(response.getBody().contains("USA"));
        assertTrue(response.getBody().contains("1234"));
        assertEquals(pathParam.get("id"), "c5e2db2b-dbd5-41fe-91ce-91d774f1e9d5");
    }


    RouteHandler.ExceptionHandlerInfo getErrorHandler(Class clazz, RouteHandler.ExceptionHandler handler) {
        return new RouteHandler.ExceptionHandlerInfo() {
            @Override
            public LambdaResponse apply(Throwable ex) {
                return handler.apply(ex);
            }

            @Override
            public Class getExceptionClass() {
                return clazz;
            }
        };
    }

    Set<RouteHandler.ExceptionHandlerInfo> getErrorHandlers() {
        Set<RouteHandler.ExceptionHandlerInfo> setOfHanders = new HashSet<>();

        setOfHanders.add(getErrorHandler(MyTestException.class, (ex) -> LambdaResponse.builder()
                .returnCode(418)
                .responseObject("Oh Snap").build()));

        setOfHanders.add(getErrorHandler(Exception.class, (ex) -> LambdaResponse.builder()
                .returnCode(400)
                .responseObject("Oh Snap").build()));


        return setOfHanders;
    }

    Set<RouteHandler.ExceptionHandlerInfo> getErrorHandlersNoDefault() {
        Set<RouteHandler.ExceptionHandlerInfo> setOfHanders = new HashSet<>();

        setOfHanders.add(getErrorHandler(MyTestException.class, (ex) -> LambdaResponse.builder()
                .returnCode(418)
                .responseObject("Oh Snap").build()));


        return setOfHanders;
    }

    @Test
    public void routeHandlerSpecificException() throws Exception {

        LambdaHandler lambdaHandler = new LambdaHandler() {
            @Override
            protected void loadControllers(Router router) {
                router.addController(routeHandler -> routeHandler.setRouteHandler("/pathvar/{operand1}/{operand2}", RouteHandler.HTTPMethod.GET, objectMapper, (request) -> {
                    throw new MyTestException("Oh Snap");
                }, getErrorHandlers()));
            }
        };

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        lambdaHandler.handleRequest(TestUtils.getFileIO("mock/agwy-search-with-path-params.json"), bout, null);
        AwsProxyResponse response = TestUtils.deserialize(bout);
        assertEquals(response.getStatusCode(), 418);
        assertTrue(response.getBody().contains("Oh Snap"));
    }

    @Test
    public void routeHandlerGenericException() throws Exception {

        LambdaHandler lambdaHandler = new LambdaHandler() {
            @Override
            protected void loadControllers(Router router) {
                router.addController(routeHandler -> routeHandler.setRouteHandler("/pathvar/{operand1}/{operand2}", RouteHandler.HTTPMethod.GET, objectMapper, (request) -> {
                    throw new Exception("Oh Snap");
                }, getErrorHandlers()));

            }
        };

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        lambdaHandler.handleRequest(TestUtils.getFileIO("mock/agwy-search-with-path-params.json"), bout, null);
        AwsProxyResponse response = TestUtils.deserialize(bout);
        assertEquals(response.getStatusCode(), 400);
        assertTrue(response.getBody().contains("Oh Snap"));
    }

    @Test
    public void routeHandlerDefaultException() throws Exception {
        LambdaHandler lambdaHandler = new LambdaHandler() {
            @Override
            protected void loadControllers(Router router) {
                router.addController(routeHandler -> routeHandler.setRouteHandler("/pathvar/{operand1}/{operand2}", RouteHandler.HTTPMethod.GET, objectMapper, (request) -> {
                    throw new IllegalArgumentException("Oh Snap");
                }, getErrorHandlersNoDefault()));
            }
        };

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        lambdaHandler.handleRequest(TestUtils.getFileIO("mock/agwy-search-with-path-params.json"), bout, null);
        AwsProxyResponse response = TestUtils.deserialize(bout);
        assertEquals(response.getStatusCode(), 500);
        assertTrue(response.getBody().contains("Server Error"));
    }

    @Test
    public void routeHandlerResourceNotFound() throws Exception {

        LambdaHandler lambdaHandler = new LambdaHandler() {
            @Override
            protected void loadControllers(Router router) {
                router.addController(routeHandler -> routeHandler.setRouteHandler("/foo/yuk", RouteHandler.HTTPMethod.GET, objectMapper, (request) -> {
                    throw new MyTestException("Oh Snap");
                }, getErrorHandlers()));
            }
        };

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        lambdaHandler.handleRequest(TestUtils.getFileIO("mock/agwy-search-with-path-params.json"), bout, null);
        AwsProxyResponse response = TestUtils.deserialize(bout);
        assertEquals(response.getStatusCode(), 404);
        assertTrue(response.getBody().contains("Resource Not found"));
    }

    @Test
    public void routeHandlerMethodNotFound() throws Exception {

        LambdaHandler lambdaHandler = new LambdaHandler() {
            @Override
            protected void loadControllers(Router router) {
                router.addController(routeHandler -> routeHandler.setRouteHandler("/foo/yuk/foomanchu", RouteHandler.HTTPMethod.PUT, objectMapper, (request) -> {
                    throw new MyTestException("Oh Snap");
                }, getErrorHandlers()));
            }
        };

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        lambdaHandler.handleRequest(TestUtils.getFileIO("mock/agwy-search-with-path-params.json"), bout, null);
        AwsProxyResponse response = TestUtils.deserialize(bout);
        assertEquals(response.getStatusCode(), 404);
        assertTrue(response.getBody().contains("Resource Not found"));
    }

    @Test
    public void deserializeOK() {

        AwsProxyRequest request = new AwsProxyRequest();
        request.setBody("{\"id\": \"12345\",\"country\": \"USA\"}");

        LambdaRequest r = new LambdaRequest(request, objectMapper);

        TestModel t = r.getBodyAsObject(TestModel.class);

        assertEquals(t.getCountry(), "USA");
    }

    @Test
    public void deserializeFailed() {

        Assertions.assertThrows(ValidationException.class, () -> {
            AwsProxyRequest request = new AwsProxyRequest();
            request.setBody("{\"id\": \"12345\",\"country\": \"US\"}");

            LambdaRequest r = new LambdaRequest(request, objectMapper);

            r.getBodyAsObject(TestModel.class);
        });

    }

    @Test
    public void getParamsTest() throws Exception {
        InputStream inputStream = TestUtils.getFileIO("mock/agwy-search-with-path-params.json");
        LambdaRequest request = new LambdaRequest(objectMapper.readValue(inputStream, AwsProxyRequest.class), objectMapper);
        String val = request.getResourceParamAsString("operand1");
        assertEquals("user", val);
    }

    @Test
    public void getQueryStringTest() throws Exception {
        InputStream inputStream = TestUtils.getFileIO("mock/agwy-search-with-multivalues.json");
        LambdaRequest request = new LambdaRequest(objectMapper.readValue(inputStream, AwsProxyRequest.class), objectMapper);
        String val = request.getResourceParamAsString("search");
        assertEquals("storeNumber==6405", val);
    }

    @Test
    public void getQueryStringMultiTest1() throws Exception {
        InputStream inputStream = TestUtils.getFileIO("mock/agwy-search-with-multivalues.json");
        LambdaRequest request = new LambdaRequest(objectMapper.readValue(inputStream, AwsProxyRequest.class), objectMapper);
        Map<String, List<String>> val = request.getResourceMultiValueMap();
        assertEquals("storeNumber==6405", val.get("search").get(0));
    }

    @Test
    public void getPathStringMultiTest2() throws Exception {
        InputStream inputStream = TestUtils.getFileIO("mock/agwy-search-with-multivalues.json");
        LambdaRequest request = new LambdaRequest(objectMapper.readValue(inputStream, AwsProxyRequest.class), objectMapper);
        Map<String, String[]> val = request.getResourceMultiValueArray();
        assertEquals("storeNumber==6405", val.get("search")[0]);
    }

    @Test
    public void getPathParamTest2() throws Exception {
        InputStream inputStream = TestUtils.getFileIO("mock/agwy-search-with-multivalues.json");
        LambdaRequest request = new LambdaRequest(objectMapper.readValue(inputStream, AwsProxyRequest.class), objectMapper);
        List<String> val = request.getResourceParamAsList("test");
        assertEquals(val.size(), 5);
    }

    @Test
    public void getPathParamTest3() throws Exception {
        InputStream inputStream = TestUtils.getFileIO("mock/agwy-search-with-multivalues.json");
        LambdaRequest request = new LambdaRequest(objectMapper.readValue(inputStream, AwsProxyRequest.class), objectMapper);
        int val = request.getResourceParamAsIntOrDefault("test2", -1);
        assertEquals(val, -1);
    }

    @Test
    public void getPathParamTest4() throws Exception {
        InputStream inputStream = TestUtils.getFileIO("mock/agwy-search-with-multivalues.json");
        LambdaRequest request = new LambdaRequest(objectMapper.readValue(inputStream, AwsProxyRequest.class), objectMapper);
        double val = request.getResourceParamAsDoubleOrDefault("test2", -1.0D);
        assertEquals(val, 1.2);
    }

    @Test
    public void getPathParamTest5() throws Exception {
        InputStream inputStream = TestUtils.getFileIO("mock/agwy-search-with-multivalues.json");
        LambdaRequest request = new LambdaRequest(objectMapper.readValue(inputStream, AwsProxyRequest.class), objectMapper);
        long val = request.getResourceParamAsLongOrDefault("test3", 1L);
        assertEquals(val, 50);
    }

    @Test
    public void getPathParamTest6() throws Exception {
        InputStream inputStream = TestUtils.getFileIO("mock/agwy-search-with-multivalues.json");
        LambdaRequest request = new LambdaRequest(objectMapper.readValue(inputStream, AwsProxyRequest.class), objectMapper);
        int val = request.getResourceParamAsIntOrDefault("test3", 1);
        assertEquals(val, 50);
    }

    @Test
    public void getHttpMethod() throws Exception {
        InputStream inputStream = TestUtils.getFileIO("mock/agwy-search-with-multivalues.json");
        LambdaRequest request = new LambdaRequest(objectMapper.readValue(inputStream, AwsProxyRequest.class), objectMapper);
        String val = request.getHttpMethod();
        assertEquals(val, "GET");
    }

    @Test
    public void getHeaderValue() throws Exception {
        InputStream inputStream = TestUtils.getFileIO("mock/agwy-search-with-multivalues.json");
        LambdaRequest request = new LambdaRequest(objectMapper.readValue(inputStream, AwsProxyRequest.class), objectMapper);
        String val = request.getHeaderValue("cache-control").get();
        assertEquals(val, "no-cache");
    }

    @Test
    public void testgetResource() throws Exception {
        InputStream inputStream = TestUtils.getFileIO("mock/agwy-search-with-multivalues.json");
        LambdaRequest request = new LambdaRequest(objectMapper.readValue(inputStream, AwsProxyRequest.class), objectMapper);
        String s = request.getResource();
        assertEquals("/store/store_views/v1", s);
    }

    @Test
    public void testgetResourceParamAsListOrDefault() throws Exception {
        List<String> input = new ArrayList<>();
        input.add("foo");
        InputStream inputStream = TestUtils.getFileIO("mock/agwy-search-with-multivalues.json");
        LambdaRequest request = new LambdaRequest(objectMapper.readValue(inputStream, AwsProxyRequest.class), objectMapper);
        List<String> ret = request.getResourceParamAsListOrDefault("foo", input);
        assertEquals(1, ret.size());
        assertEquals(input, ret);
    }

    @Test
    public void testgetResourceParamAsStringOrDefault() throws Exception {
        InputStream inputStream = TestUtils.getFileIO("mock/agwy-search-with-multivalues.json");
        LambdaRequest request = new LambdaRequest(objectMapper.readValue(inputStream, AwsProxyRequest.class), objectMapper);
        String ret = request.getResourceParamAsStringOrDefault("foo", "text");
        assertEquals("text", ret);
    }

}
