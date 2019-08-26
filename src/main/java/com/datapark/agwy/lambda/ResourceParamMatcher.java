package com.datapark.agwy.lambda;

import io.vavr.Tuple2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

final class ResourceParamMatcher {
    public static Function<String, Tuple2<Boolean, Map<String, String>>> getPathMatch(String matchingResource) {
        List<String> pathParamNames = new ArrayList<>();
        final String matchingRoot;
        int startPos = matchingResource.replace(" ", "").indexOf("/{");
        if (startPos >= 0) {
            matchingRoot = matchingResource.substring(0, startPos);
            pathParamNames.addAll(Optional.ofNullable(matchingResource).map(s -> s.split("/"))
                    .map(a -> Stream.of(a))
                    .map(s -> s.map(String::trim)
                            .filter(ss -> ss.startsWith("{"))
                            .filter(se -> se.endsWith("}"))
                            .map(v -> v.substring(1, v.length() - 1))
                            .map(String::trim)
                            .collect(toList())).orElse(new ArrayList<>()));
        } else {
            matchingRoot = matchingResource;
        }

        return (resource) -> {
            Map<String, String> captured = new HashMap<>();

            if (!pathParamNames.isEmpty() && resource.startsWith(matchingRoot) && !resource.equals(matchingRoot)) {
                List<String> pathValues = Optional.ofNullable(resource).map(r -> r.substring(startPos + 1, r.length()))
                        .map(s -> s.split("/"))
                        .map(a -> Stream.of(a))
                        .map(s -> s.map(String::trim)
                                .collect(toList())).orElse(new ArrayList<>());
                if (pathParamNames.size() == pathValues.size()) {
                    IntStream.range(0, pathParamNames.size()).forEach(i -> captured.put(pathParamNames.get(i), pathValues.get(i)));
                    return new Tuple2<>(true, captured);
                }
            }
            return new Tuple2<>(false, captured);
        };
    }
}
