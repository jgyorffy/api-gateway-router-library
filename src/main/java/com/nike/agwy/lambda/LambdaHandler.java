package com.nike.agwy.lambda;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.Headers;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Try;
import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public abstract class LambdaHandler implements RequestStreamHandler {
    private final static Logger log = LoggerFactory.getLogger(LambdaHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Router router = new Router(new RouteHandlerAWSLambda());
    private UUID uuid;

    public LambdaHandler() {
        uuid = UUID.randomUUID();
        loadControllers(router);
        router.loadControllers();
    }


    void sendError(String errMsg, Throwable ex, OutputStream outputStream) {
        log.error(errMsg, ex);
        Try.run(() -> objectMapper.writeValue(outputStream, createErrorResponse(500, ex)))
                .onFailure(e -> log.debug("Error writing error response to the output stream", ex))
                .getOrElseThrow(() -> new RuntimeException("Error writing object as string"));
    }

    protected abstract void loadControllers(Router router);

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {
        //lambda instance identifier for logging
        ThreadContext.put("lid", uuid.toString());

        Try.of(() -> objectMapper.readValue(inputStream, AwsProxyRequest.class))
                .onFailure(ex -> sendError("Error reading AWS Proxy Request Object", ex, outputStream))
                .andThenTry((request) -> objectMapper.writeValue(outputStream, router.getRouteHandler().HandleRequest(request, context)))
                .onFailure(ex -> sendError("Error writing error response to the output stream", ex, outputStream))
                .andFinally(() -> {
                    Try.run(outputStream::flush)
                            .onFailure(ex -> log.debug("flush failed", ex));
                    Try.run(outputStream::close)
                            .onFailure(ex -> log.debug("close failed", ex));
                });
    }

    AwsProxyResponse createErrorResponse(int errCode, Throwable ex) {
        return new AwsProxyResponse(errCode, new Headers(),
                router.getRouteHandler().serializeObject(new ErrorResponse(ex.getMessage()), objectMapper));
    }
}
