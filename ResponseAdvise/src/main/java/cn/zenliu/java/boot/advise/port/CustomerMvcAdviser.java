package cn.zenliu.java.boot.advise.port;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import java.util.Optional;

/**
 * this bean will use to process response advice before any internal process.<br>
 * if any of them returns a none empty result , advice process will be terminated.
 *
 * @author Zen.Liu
 * @apiNote
 * @since 2020-11-17
 */
@FunctionalInterface
public interface CustomerMvcAdviser {
    Optional<Object> advise(
        Object body,
        @NotNull MethodParameter returnType,
        MediaType selectedContentType,
        @NotNull Class<? extends HttpMessageConverter<?>> converterType,
        @NotNull ServerHttpRequest serverHttpRequest,
        @NotNull ServerHttpResponse serverHttpResponse
    );
}
