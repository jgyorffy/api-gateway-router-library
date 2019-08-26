package com.datapark.agwy.lambda;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyRequestContext;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AWSLambdaTests {


    @Test
    public void testCommonRoutesWithGet()  {
        RouteHandlerAWSLambda routeHandlerAWSLambda = getRouteHandler();

        AwsProxyResponse awsProxyResponse = routeHandlerAWSLambda.HandleRequest(makeRequest(RouteHandler.HTTPMethod.GET, "/v/v2"), null);
        assertEquals(awsProxyResponse.getStatusCode(), 200);

        awsProxyResponse = routeHandlerAWSLambda.HandleRequest(makeRequest(RouteHandler.HTTPMethod.GET, "/v/v2/12345"), null);
        assertEquals(awsProxyResponse.getStatusCode(), 200);
        //echo back the parsed param in the body
        assertEquals(awsProxyResponse.getBody(), "\"12345\"");
    }


    @Test
    public void testCommonRoutesWithPut()  {
        RouteHandlerAWSLambda routeHandlerAWSLambda = getRouteHandler();

        AwsProxyResponse awsProxyResponse = routeHandlerAWSLambda.HandleRequest(makeRequest(RouteHandler.HTTPMethod.PUT, "/v/v2"), null);
        assertEquals(awsProxyResponse.getStatusCode(), 200);

        awsProxyResponse = routeHandlerAWSLambda.HandleRequest(makeRequest(RouteHandler.HTTPMethod.PUT, "/v/v2/12345"), null);
        assertEquals(awsProxyResponse.getStatusCode(), 200);
        //echo back the parsed param in the body
        assertEquals(awsProxyResponse.getBody(), "\"12345\"");
    }

    @Test
    public void testCommonRoutesWithPost()   {
        RouteHandlerAWSLambda routeHandlerAWSLambda = getRouteHandler();
        AwsProxyResponse awsProxyResponse = routeHandlerAWSLambda.HandleRequest(makeRequest(RouteHandler.HTTPMethod.POST, "/v/v2"), null);
        assertEquals(awsProxyResponse.getStatusCode(), 200);
    }

    @Test
    public void testCommonRoutesWithDelete() {
        RouteHandlerAWSLambda routeHandlerAWSLambda = getRouteHandler();

        AwsProxyResponse awsProxyResponse = routeHandlerAWSLambda.HandleRequest(makeRequest(RouteHandler.HTTPMethod.DELETE, "/v/v2"), null);
        assertEquals(awsProxyResponse.getStatusCode(), 200);

        awsProxyResponse = routeHandlerAWSLambda.HandleRequest(makeRequest(RouteHandler.HTTPMethod.DELETE, "/v/v2/12345"), null);
        assertEquals(awsProxyResponse.getStatusCode(), 200);
        //echo back the parsed param in the body
        assertEquals(awsProxyResponse.getBody(), "\"12345\"");
    }

    @Test
    public void testCommonRoutesWithHead()   {
        RouteHandlerAWSLambda routeHandlerAWSLambda = getRouteHandler();
        AwsProxyResponse awsProxyResponse = routeHandlerAWSLambda.HandleRequest(makeRequest(RouteHandler.HTTPMethod.HEAD, "/v/v2"), null);
        assertEquals(awsProxyResponse.getStatusCode(), 200);
    }

    @Test
    public void testCommonRoutesWithPatch()   {
        RouteHandlerAWSLambda routeHandlerAWSLambda = getRouteHandler();
        AwsProxyResponse awsProxyResponse = routeHandlerAWSLambda.HandleRequest(makeRequest(RouteHandler.HTTPMethod.PATCH, "/v/v2"), null);
        assertEquals(awsProxyResponse.getStatusCode(), 200);
    }

    @Test
    public void testCommonRoutesWith404()   {
        RouteHandlerAWSLambda routeHandlerAWSLambda = getRouteHandler();
        routeHandlerAWSLambda.clearState();
        AwsProxyResponse awsProxyResponse = routeHandlerAWSLambda.HandleRequest(makeRequest(RouteHandler.HTTPMethod.PATCH, "/v/v2"), null);
        assertEquals(awsProxyResponse.getStatusCode(), 404);
    }

    @Test
    public void testCommonRoutesWith500()   {
        RouteHandlerAWSLambda routeHandlerAWSLambda = new RouteHandlerAWSLambda();

        routeHandlerAWSLambda.setRouteHandler("/v/v2", RouteHandler.HTTPMethod.GET, null,
                (r) -> {throw new RuntimeException("howdee");}, null);
        AwsProxyResponse awsProxyResponse = routeHandlerAWSLambda.HandleRequest(makeRequest(RouteHandler.HTTPMethod.GET, "/v/v2"), null);
        assertEquals(awsProxyResponse.getStatusCode(), 500);
    }

    @Test
    public void testRouteHandlerError() {

        LambdaHandler lambdaHandler = new LambdaHandler() {
            @Override
            protected void loadControllers(Router router) {

            }
        };
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        lambdaHandler.sendError("error", new IllegalArgumentException("crap"), byteArrayOutputStream);
        String s = new String(byteArrayOutputStream.toByteArray());
        assertEquals("{\"statusCode\":500,\"multiValueHeaders\":{},\"body\":\"{\\\"message\\\":\\\"crap\\\"}\",\"isBase64Encoded\":false}", s);
    }

        //echo back the parsed param in the body
    private RouteHandlerAWSLambda getRouteHandler() {
        RouteHandlerAWSLambda routeHandlerAWSLambda = new RouteHandlerAWSLambda();

        routeHandlerAWSLambda.setRouteHandler("/v/v2", RouteHandler.HTTPMethod.GET, null,
                (r) -> LambdaResponse.builder().returnCode(200).build(), null);
        routeHandlerAWSLambda.setRouteHandler("/v/v2", RouteHandler.HTTPMethod.PUT, null,
                (r) -> LambdaResponse.builder().returnCode(200).build(), null);
        routeHandlerAWSLambda.setRouteHandler("/v/v2", RouteHandler.HTTPMethod.POST, null,
                (r) -> LambdaResponse.builder().returnCode(200).build(), null);
        routeHandlerAWSLambda.setRouteHandler("/v/v2", RouteHandler.HTTPMethod.DELETE, null,
                (r) -> LambdaResponse.builder().returnCode(200).build(), null);
        routeHandlerAWSLambda.setRouteHandler("/v/v2", RouteHandler.HTTPMethod.HEAD, null,
                (r) -> LambdaResponse.builder().returnCode(200).build(), null);
        routeHandlerAWSLambda.setRouteHandler("/v/v2", RouteHandler.HTTPMethod.PATCH, null,
                (r) -> LambdaResponse.builder().returnCode(200).build(), null);


        routeHandlerAWSLambda.setRouteHandler("/v/v2/{param1}", RouteHandler.HTTPMethod.GET, null,
                (r) -> LambdaResponse.builder().returnCode(200).responseObject(r.getResourceParamAsString("param1")).build(), null);

        routeHandlerAWSLambda.setRouteHandler("/v/v2/{param1}", RouteHandler.HTTPMethod.PUT, null,
                (r) -> LambdaResponse.builder().returnCode(200).responseObject(r.getResourceParamAsString("param1")).build(), null);

        routeHandlerAWSLambda.setRouteHandler("/v/v2/{param1}", RouteHandler.HTTPMethod.DELETE, null,
                (r) -> LambdaResponse.builder().returnCode(200).responseObject(r.getResourceParamAsString("param1")).build(), null);

        routeHandlerAWSLambda.setRouteHandler("/v/v2/blabla", RouteHandler.HTTPMethod.PUT, null,
                (r) -> LambdaResponse.builder().returnCode(200).responseObject(r.getResourceParamAsString("param1")).build(), null);

        return routeHandlerAWSLambda;
    }

    private AwsProxyRequest makeRequest(RouteHandler.HTTPMethod method, String path) {
        AwsProxyRequest awsProxyRequest = new AwsProxyRequest();
        awsProxyRequest.setPath(path);
        AwsProxyRequestContext ctx = new AwsProxyRequestContext();
        ctx.setHttpMethod(method.name());
        awsProxyRequest.setRequestContext(ctx);
        return awsProxyRequest;
    }
}
