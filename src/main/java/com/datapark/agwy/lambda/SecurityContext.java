package com.datapark.agwy.lambda;

import com.amazonaws.serverless.proxy.model.ApiGatewayAuthorizerContext;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecurityContext {
    private final ApiGatewayAuthorizerContext claim;
    private final String issuer;
    private final List<String> audiences;

    SecurityContext(ApiGatewayAuthorizerContext apiGatewayAuthorizerContext) {
        this.claim = apiGatewayAuthorizerContext;
        this.issuer = (Optional.ofNullable(apiGatewayAuthorizerContext)
                .map(c -> Optional.ofNullable(c.getContextValue("issuer")).orElse("")).orElse(null));
        this.audiences = Optional.ofNullable(apiGatewayAuthorizerContext)
                .map(m -> Stream.of(Optional.ofNullable(m.getContextValue("audiences")).orElse("")
                        .split(",", -1))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList())).orElse(new ArrayList<>());
    }

    public String getClaim(String key) {
        return Optional.ofNullable(claim).map(m -> m.getContextValue(key)).orElse(null);
    }

    public String getIssuer() {
        return issuer;
    }

    public List<String> getAudiences() {
        return audiences;
    }
}
