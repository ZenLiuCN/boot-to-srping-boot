package cn.zenliu.java.boot.advise.entities;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * ResponseDTO is a global response data structure.
 *
 * @author Zen.Liu
 * @apiNote
 * @since 2020-11-17
 */
@Getter
public class ResponseDTO<T> implements Serializable {
    public static final AtomicBoolean debug = new AtomicBoolean(false);
    private static final long serialVersionUID = -4470232261583204825L;
    /**
     * response timestamp
     */
    final long timestamp = System.currentTimeMillis();
    /**
     * response status code
     */
    final int status;
    /**
     * response error message, mostly the system error information.
     */
    final String error;
    /**
     * response message, mostly use to alert or inform user.
     */
    final String message;

    ResponseDTO(int status, String error, String message, T data) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.data = data;
    }

    final T data;

    public static <R> ResponseDTO<R> success(@NotNull R data) {
        return new ResponseDTO<R>(200, null, null, data);
    }

    public static <R> ResponseDTO<R> success(@NotNull R data, @Nullable String message) {
        return new ResponseDTO<R>(200, null, message, data);
    }

    public static <R> ResponseDTO<R> warp(@NotNull Supplier<R> dataSupplier, @Nullable String messageOnError, @Nullable String messageOnSuccess) {
        try {
            return new ResponseDTO<>(200, null, messageOnSuccess, dataSupplier.get());
        } catch (Throwable throwable) {
            return new ResponseDTO<>(500, debug.get() ? throwable.getMessage() : null, messageOnError, null);
        }
    }

    public static <R> ResponseDTO<R> warp(
        @NotNull Supplier<R> dataSupplier,
        @Nullable Function<Throwable, Map.Entry<Integer, String>> errorProcessor,
        @Nullable String messageOnError,
        @Nullable String messageOnSuccess) {
        if (errorProcessor == null) return warp(dataSupplier, messageOnError, messageOnSuccess);
        try {
            return new ResponseDTO<>(200, null, messageOnSuccess, dataSupplier.get());
        } catch (Throwable throwable) {
            Map.Entry<Integer, String> result = errorProcessor.apply(throwable);
            return new ResponseDTO<>(result.getKey() == null ? 500 : result.getKey(), debug.get() ? throwable.getMessage() : null, messageOnError, null);
        }
    }

    public static <R> ResponseDTO<R> error(int status, @Nullable String message, @Nullable String error) {
        return new ResponseDTO<>(status, error, message, null);
    }

    public static <R> ResponseDTO<R> error(int status, Throwable throwable, @NotNull String message) {
        return new ResponseDTO<>(status, debug.get() ? throwable.getMessage() : null, message, null);
    }

    public static <R> ResponseDTO<R> error(HttpStatus status, Throwable throwable, @NotNull String message) {
        return new ResponseDTO<>(status.value(), debug.get() ? throwable.getMessage() : null, message, null);
    }

}
