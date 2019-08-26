package com.datapark.agwy.lambda;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Try;

import java.util.Set;

public interface RouteHandler<T, U, C> {

    void setRouteHandler(String resource, RouteHandler.HTTPMethod httpMethod, ObjectMapper objectMapper,
                         RouteFunction rfnc, Set<ExceptionHandlerInfo> efncs);


    T HandleRequest(U request, C context);

    default <T> String serializeObject(T object, ObjectMapper mapper) {
        return Try.of(() -> mapper.writeValueAsString(object))
                .getOrElseThrow((ex) -> new RuntimeException("Error writing object as string", ex));
    }

    enum HTTPMethod {
        GET,
        POST,
        PUT,
        DELETE,
        HEAD,
        PATCH
    }

    @FunctionalInterface
    interface RouteFunction {
        LambdaResponse apply(LambdaRequest n) throws Exception;
    }

    @FunctionalInterface
    interface ExceptionHandler {
        LambdaResponse apply(Throwable ex);
    }

    interface ExceptionHandlerInfo<T> extends ExceptionHandler {
        Class<T> getExceptionClass();
    }

}
