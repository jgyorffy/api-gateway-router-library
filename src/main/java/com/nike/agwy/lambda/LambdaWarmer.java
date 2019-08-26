package com.nike.agwy.lambda;

import com.amazonaws.regions.Regions;
import com.amazonaws.serverless.proxy.model.ApiGatewayRequestIdentity;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyRequestContext;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvocationType;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

final class LambdaWarmer {
    private final static Logger log = LoggerFactory.getLogger(LambdaWarmer.class);
    private static final int MAX_WAIT_TIME_MS = 10_000;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    LambdaWarmer() {
    }

    static String makeRequestObject(int totalToWarm, int indexNumber) throws JsonProcessingException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("CALL_NUMBER", Integer.toString(totalToWarm));
        pathParams.put("INDEX_NUMBER", Integer.toString(indexNumber));
        AwsProxyRequest request = new AwsProxyRequest();
        request.setPath("*");
        request.setPathParameters(pathParams);
        AwsProxyRequestContext requestContext = new AwsProxyRequestContext();
        requestContext.setPath("*");
        ApiGatewayRequestIdentity apiGatewayRequestIdentity = new ApiGatewayRequestIdentity();
        apiGatewayRequestIdentity.setCaller("_lambda_warmer_");
        requestContext.setIdentity(apiGatewayRequestIdentity);
        request.setRequestContext(requestContext);
        return objectMapper.writeValueAsString(request);
    }

    public static boolean isWarmerCall(AwsProxyRequest request) {
        return Optional.ofNullable(request)
                .map(AwsProxyRequest::getRequestContext)
                .map(AwsProxyRequestContext::getIdentity)
                .map(ApiGatewayRequestIdentity::getCaller)
                .map(c -> "_lambda_warmer_".equals(c))
                .orElse(false);
    }

    public static void handleWarmRequest(AwsProxyRequest request, String funcName) {

        final int totalToWarm = Optional.ofNullable(request)
                .map(AwsProxyRequest::getPathParameters)
                .map(m -> m.get("CALL_NUMBER"))
                .map(n -> Try.of(() -> Integer.parseInt(n))
                        .onFailure(ex -> log.error("Number format failed: CALL_NUMBER", ex))
                        .getOrElse(0))
                .orElse(0);

        final int indexNumber = Optional.ofNullable(request)
                .map(AwsProxyRequest::getPathParameters)
                .map(m -> m.get("INDEX_NUMBER"))
                .map(n -> Try.of(() -> Integer.parseInt(n))
                        .onFailure(ex -> log.error("Number format failed: INDEX_NUMBER", ex))
                        .getOrElse(-1))
                .orElse(-1);

        final int sleepTimeMs = Optional.ofNullable(request)
                .map(AwsProxyRequest::getPathParameters)
                .map(m -> m.get("SLEEP_MS"))
                .map(n -> Try.of(() -> Integer.parseInt(n))
                        .onFailure(ex -> log.error("Number format failed: SLEEP_MS", ex))
                        .getOrElse(5_000))
                .orElse(5_000);

        if (indexNumber < 0 && totalToWarm > 0) {
            String regionName = System.getenv("AWS_REGION");
            log.debug("Using region: {} and function: {}", regionName, funcName);
            IntStream.range(0, totalToWarm).forEach(
                    index -> Try.of(() -> invokeSameLambdaWithCallNumber(totalToWarm, index, funcName, makeClient(regionName)))
                            .onFailure(ex -> log.error("Error invoking warming lambda", ex)).getOrElse(-1)
            );
            log.info("Executed {} lambdas for warming", totalToWarm);
        } else if (indexNumber >= 0 && totalToWarm > 0 && sleepTimeMs < MAX_WAIT_TIME_MS) {
            Try.of(() -> lambdaWait(sleepTimeMs)).onFailure(ex -> log.error("Lambda wait failed", ex));
            log.info("Warmed lambda index {} of total {} waiting {} ms", indexNumber, totalToWarm, sleepTimeMs);
        }
    }

    static int lambdaWait(int ms) throws InterruptedException {
        Thread.sleep(ms);
        return 0;
    }

    static int invokeSameLambdaWithCallNumber(int totalToWarm, int indexNumber, String funcName, AWSLambda awsLambda) throws Exception {
        log.debug("Calling self with index {} of total {}", indexNumber, totalToWarm);
        InvokeRequest req = new InvokeRequest()
                .withInvocationType(InvocationType.Event)
                .withFunctionName(funcName)
                .withPayload(makeRequestObject(totalToWarm, indexNumber));
        InvokeResult result = awsLambda.invoke(req);
        return result.getStatusCode();
    }

    static AWSLambda makeClient(String regionName) {
        Regions region = Regions.fromName(regionName);
        AWSLambdaClientBuilder builder = AWSLambdaClientBuilder.standard()
                .withRegion(region);
        AWSLambda client = builder.build();
        return client;
    }
}
