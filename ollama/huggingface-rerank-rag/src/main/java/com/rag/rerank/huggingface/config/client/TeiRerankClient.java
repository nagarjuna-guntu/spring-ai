package com.rag.rerank.huggingface.config.client;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;
import java.util.Objects;

@NullMarked
@HttpExchange(
        url = "/rerank",
        contentType = MediaType.APPLICATION_JSON_VALUE,
        accept = MediaType.APPLICATION_JSON_VALUE)
public interface TeiRerankClient {

    @PostExchange
    @Nullable List<@Nullable RerankResult> rerankRaw(@RequestBody RerankRequest request);

    default List<RerankResult> rerank(RerankRequest request) {
        List<@Nullable RerankResult> rawResult = rerankRaw(request);

        return rawResult == null ? List.of() : rawResult.stream().filter(Objects::nonNull).toList();
    }
}
