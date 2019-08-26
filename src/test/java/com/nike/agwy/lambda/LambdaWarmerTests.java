package com.nike.agwy.lambda;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LambdaWarmerTests {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testRequestObjectSerialization() throws Exception {

        String toSend = LambdaWarmer.makeRequestObject(10, 1);

        AwsProxyRequest request = objectMapper.readValue(toSend, AwsProxyRequest.class);

        assertEquals("10", request.getPathParameters().get("CALL_NUMBER"));

        assertEquals("1", request.getPathParameters().get("INDEX_NUMBER"));

        assertEquals("_lambda_warmer_", request.getRequestContext().getIdentity().getCaller());

    }

    @Test
    public void testRequestParam() throws Exception {

        String toSend = LambdaWarmer.makeRequestObject(10, 1);

        AwsProxyRequest request = objectMapper.readValue(toSend, AwsProxyRequest.class);

        assertTrue(LambdaWarmer.isWarmerCall(request));
    }

    @Test
    public void testCallLambda() throws Exception {

        AWSLambda mockLamba = mock(AWSLambda.class);

        InvokeResult invokeResult = new InvokeResult();
        invokeResult.setStatusCode(200);
        when(mockLamba.invoke(any(InvokeRequest.class))).thenReturn(invokeResult);

        LambdaWarmer.makeClient("us-east-1");

        int ret = LambdaWarmer.invokeSameLambdaWithCallNumber(10, 1, "foo", mockLamba);

        assertEquals(200, ret);
    }

    @Test
    public void testSleep() throws Exception {

        int ret = LambdaWarmer.lambdaWait(1);

        assertEquals(0, ret);
    }


    @Test
    public void testCallOthers() throws Exception {

        String toSend = LambdaWarmer.makeRequestObject(2, -1);

        AwsProxyRequest request = objectMapper.readValue(toSend, AwsProxyRequest.class);

        request.getPathParameters().put("INDEX_NUMBER", "a");
        request.getPathParameters().put("SLEEP_MS", "b");

        LambdaWarmer.handleWarmRequest(request, "foo");
    }

    @Test
    public void testRespondToCall() throws Exception {

        String toSend = LambdaWarmer.makeRequestObject(2, -1);

        AwsProxyRequest request = objectMapper.readValue(toSend, AwsProxyRequest.class);

        request.getPathParameters().put("INDEX_NUMBER", "0");
        request.getPathParameters().put("SLEEP_MS", "1");

        LambdaWarmer.handleWarmRequest(request, "foo");
    }


}
