package com.datapark.agwy.lambda;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestModel {
    @JsonProperty("id")
    private final String id;
    @NotNull
    @Pattern(regexp = "[A-Z]{3}")
    @JsonProperty("country")
    private final String country;

    @JsonCreator
    public TestModel(
            @JsonProperty("id") String id,
            @JsonProperty("country") String country) {
        this.id = id;
        this.country = country;
    }

    public String getId() {
        return id;
    }

    public String getCountry() {
        return country;
    }
}