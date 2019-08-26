package com.datapark.agwy.lambda;

import com.amazonaws.serverless.proxy.model.ApiGatewayRequestIdentity;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyRequestContext;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.Headers;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

final class RouteHandlerAWSLambda implements RouteHandler<AwsProxyResponse, AwsProxyRequest, Context> {
    private final static Logger log = LoggerFactory.getLogger(RouteHandlerAWSLambda.class);
    private final EnumMap<RouteHandler.HTTPMethod, Map<String, RouteFunctionHolder>> mapVerbToListOfMappers = new EnumMap<>(HTTPMethod.class);
    private final ObjectMapper defaultObjectMapper = new ObjectMapper();
    private final EnumMap<RouteHandler.HTTPMethod, List<RouteFunctionHolder>> mapVerbToListOfHolders = new EnumMap<>(HTTPMethod.class);

    RouteHandlerAWSLambda() {
    }

    void clearState() {
        mapVerbToListOfMappers.clear();
        mapVerbToListOfHolders.clear();
    }

    public void setRouteHandler(String resource, RouteHandler.HTTPMethod httpMethod, ObjectMapper objectMapper,
                                RouteFunction fnc, Set<ExceptionHandlerInfo> efncs) {
        Map<String, RouteFunctionHolder> mapOfHolders;
        List<RouteFunctionHolder> listOfRouterHolders;
        if (mapVerbToListOfMappers.containsKey(httpMethod)) {
            mapOfHolders = mapVerbToListOfMappers.get(httpMethod);
            listOfRouterHolders = mapVerbToListOfHolders.get(httpMethod);
        } else {
            mapOfHolders = new HashMap<>();
            listOfRouterHolders = new ArrayList<>();
            mapVerbToListOfMappers.put(httpMethod, mapOfHolders);
            mapVerbToListOfHolders.put(httpMethod, listOfRouterHolders);
        }

        RouteFunctionHolder rfh = new RouteFunctionHolder(fnc, objectMapper, ResourceParamMatcher.getPathMatch(resource), efncs);
        mapOfHolders.put(resource, rfh);
        listOfRouterHolders.add(rfh);
    }

    private String getSafePath(String path) {
        path = Optional.ofNullable(path).orElse("");
        return path.substring(0, path.length() - (path.length() > 1 && path.endsWith("/") ? 1 : 0));
    }


    public AwsProxyResponse HandleRequest(AwsProxyRequest request, Context context) {
        ObjectMapper objectMapper = defaultObjectMapper;
        Set<ExceptionHandlerInfo> setOfExceptionHandlerInfos = new HashSet<>();
        final AuditLogger auditLogger = new AuditLogger(request);

        try {
            final String path = getSafePath(request.getPath());

            if (log.isDebugEnabled()) {
                log.debug("Lambda received: {}", defaultObjectMapper.writeValueAsString(request));
            }

            if (LambdaWarmer.isWarmerCall(request)) {
                LambdaWarmer.handleWarmRequest(request, Optional.ofNullable(context).map(Context::getFunctionName).orElse(""));
                return new AwsProxyResponse(200, null, "OK");
            }

            Map<String, RouteFunctionHolder> mapOfHolders = Optional.ofNullable(request).map(r -> mapVerbToListOfMappers.get(HTTPMethod.valueOf(r.getRequestContext().getHttpMethod())))
                    .orElse(null);

            List<RouteFunctionHolder> listOfHolders = Optional.ofNullable(request).map(r -> mapVerbToListOfHolders.get(HTTPMethod.valueOf(r.getRequestContext().getHttpMethod())))
                    .orElse(null);


            RouteFunctionHolder fncHolder = Optional.ofNullable(mapOfHolders).filter(m -> m.containsKey(path))
                    .map(m -> m.get(path))
                    .orElse(Optional.ofNullable(listOfHolders).map(m -> m.stream().filter(h -> h.evaluate(path)).findFirst().orElse(null)).orElse(null));


            Optional.ofNullable(fncHolder).orElseThrow(() -> new NotFoundException("Method not found in route map: " + path));

            Optional.ofNullable(fncHolder.mapParams).filter(f -> !f.isEmpty()).ifPresent((p) -> request.setPathParameters(p));

            objectMapper = Optional.ofNullable(fncHolder.jsonMapper).orElse(defaultObjectMapper);
            setOfExceptionHandlerInfos = fncHolder.efncs;

            AwsProxyResponse response = createAWSResponse(fncHolder.handler.apply(new LambdaRequest(request,
                    objectMapper)), objectMapper);

            if (log.isDebugEnabled()) {
                log.debug("Lambda sent: {}", defaultObjectMapper.writeValueAsString(response));
            }

            return response;

        } catch (NotFoundException ex) {
            log.debug("Route error", ex);
            return createErrorResponse(404, "Resource Not found", objectMapper);
        } catch (Throwable ex) {
            //controller should log error
            log.debug("Caught error", ex);
            final ObjectMapper o = objectMapper;

            return Optional.ofNullable(findExceptionHandler(setOfExceptionHandlerInfos, ex)).map(h -> Try.of(() -> h.apply(ex)).map(hh -> createAWSResponse(hh, o))
                    .onFailure(exc -> log.error("Failed calling controller exception handler", exc)).getOrElse(createErrorResponse(500, "Server Error", o)))
                    .orElse(createErrorResponse(500, "Server Error", o));
        } finally {
            auditLogger.log();
        }
    }

    private ExceptionHandlerInfo findExceptionHandler(Set<ExceptionHandlerInfo> setOfExceptionHandlerInfos, Throwable ex) {
        Set<ExceptionHandlerInfo> setOfExceptionHandlerInfoChecked = Optional.ofNullable(setOfExceptionHandlerInfos).orElse(new HashSet<>());
        ExceptionHandlerInfo exceptionHandlerInfo = setOfExceptionHandlerInfoChecked.stream().filter(f -> f.getExceptionClass().equals(ex.getClass())).findFirst().orElse(null);
        ExceptionHandlerInfo defaultHandler = setOfExceptionHandlerInfoChecked.stream().filter(f -> f.getExceptionClass().equals(Exception.class)).findFirst().orElse(null);
        return Optional.ofNullable(exceptionHandlerInfo).orElse(defaultHandler);
    }

    private AwsProxyResponse createAWSResponse(LambdaResponse response, ObjectMapper objectMapper) {
        Headers headers = new Headers();
        Map<String, String> m = Optional.ofNullable(response.getHeaders()).orElse(new HashMap<String, String>());
        m.entrySet().stream().forEach(e -> headers.putSingle(e.getKey(), e.getValue()));
        return new AwsProxyResponse(response.getReturnCode(), headers,
                serializeObject(response.getResponseObject(), objectMapper));
    }

    private AwsProxyResponse createErrorResponse(int errCode, String message, ObjectMapper objectMapper) {
        return new AwsProxyResponse(errCode, new Headers(),
                serializeObject(new ErrorResponse(message), objectMapper));
    }

    private static class RouteFunctionHolder {
        private final RouteFunction handler;
        private final ObjectMapper jsonMapper;
        private final Set<ExceptionHandlerInfo> efncs;
        private final Function<String, Tuple2<Boolean, Map<String, String>>> fncParser;
        private Map<String, String> mapParams = new HashMap<>();

        RouteFunctionHolder(RouteFunction handler, ObjectMapper jsonMapper,
                            Function<String, Tuple2<Boolean, Map<String, String>>> fncParser,
                            Set<ExceptionHandlerInfo> efncs) {
            this.handler = handler;
            this.jsonMapper = jsonMapper;
            this.fncParser = fncParser;
            this.efncs = efncs;
        }

        boolean evaluate(String resource) {
            Tuple2<Boolean, Map<String, String>> t = fncParser.apply(resource);
            mapParams = t._2;
            return t._1;
        }
    }

    private class AuditLogger {
        private final double startTime;
        private final AwsProxyRequest request;

        AuditLogger(AwsProxyRequest request) {
            startTime = System.nanoTime();
            this.request = request;
            String traceId = Optional.ofNullable(request.getMultiValueHeaders()).map(h -> h.get("X-B3-TraceId")).filter(l -> !l.isEmpty()).map(l -> l.get(0))
                    .orElse(Optional.ofNullable(request.getMultiValueHeaders()).map(h -> h.get("X-Amzn-Trace-Id")).filter(l -> !l.isEmpty()).map(l -> l.get(0)).orElse(""));

            ThreadContext.put("traceId", traceId);
        }

        void log() {
            String ip = Optional.ofNullable(request.getRequestContext()).map(AwsProxyRequestContext::getIdentity)
                    .map(ApiGatewayRequestIdentity::getSourceIp).orElse("unknown");
            log.info("Lambda Request handled {}-> {} in {} ms from source {}", request.getRequestContext().getHttpMethod(),
                    getSafePath(request.getPath()),
                    ((System.nanoTime() - startTime) / 1_000_000), ip);
        }
    }
}
