package cn.zenliu.java.boot.advise.port;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;

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
public interface CustomerFluxAdviser {
    Optional<Object> advise(
        @Nullable Object bodyElement,
        @NotNull ServerWebExchange exchange,
        @NotNull HandlerResult result
    );
}
