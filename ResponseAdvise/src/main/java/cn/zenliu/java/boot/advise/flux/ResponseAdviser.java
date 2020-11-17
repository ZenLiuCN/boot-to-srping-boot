package cn.zenliu.java.boot.advise.flux;

import cn.zenliu.java.boot.advise.annotations.IgnoreBodyAdvice;
import cn.zenliu.java.boot.advise.entities.ResponseDTO;
import cn.zenliu.java.boot.advise.port.CustomerFluxAdviser;
import cn.zenliu.java.boot.advise.properties.ResponseAdviserProperty;
import cn.zenliu.java.boot.advise.util.Util;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.*;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * @author Zen.Liu
 * @apiNote
 * @since 2020-11-17
 */
@Slf4j
public class ResponseAdviser extends ResponseBodyResultHandler {
    private final boolean debug;
    private final boolean enable;
    private final Predicate<String> predicate;
    private final CustomerFluxAdviser customerAdviser;

    private static CustomerFluxAdviser adviserBuilder(List<CustomerFluxAdviser> customerAdvisers) {
        if (customerAdvisers.isEmpty()) {
            return (a, b, c) -> Optional.empty();
        } else
            return (a, b, c) -> {
                for (CustomerFluxAdviser i : customerAdvisers) {
                    Optional<Object> advise = i.advise(a, b, c);
                    if (advise.isPresent()) {
                        return advise;
                    }
                }
                return Optional.empty();
            };
    }


    public ResponseAdviser(
        ResponseAdviserProperty property, List<CustomerFluxAdviser> customerAdvisers,
        List<HttpMessageWriter<?>> writers, RequestedContentTypeResolver resolver) {
        super(writers, resolver);
        this.debug = property.isDebug();
        this.enable = property.isEnable();
        this.predicate = Util.predicateBuilder(property);
        this.customerAdviser = adviserBuilder(customerAdvisers);
    }

    public ResponseAdviser(
        ResponseAdviserProperty property, List<CustomerFluxAdviser> customerAdvisers,
        List<HttpMessageWriter<?>> writers, RequestedContentTypeResolver resolver, ReactiveAdapterRegistry registry) {
        super(writers, resolver, registry);
        this.debug = property.isDebug();
        this.enable = property.isEnable();
        this.predicate = Util.predicateBuilder(property);
        this.customerAdviser = adviserBuilder(customerAdvisers);
    }

    static String className(HandlerResult result) {
        try {
            return result.getReturnTypeSource().getDeclaringClass().getCanonicalName() + "#" + result.getReturnTypeSource().getMethod().getName();
        } catch (Exception e) {
            return result.getReturnTypeSource().getDeclaringClass().getCanonicalName();
        }
    }

    @Override
    public boolean supports(@NotNull HandlerResult result) {
        if (!enable) return false;
        String methodPredicate = className(result);
        final boolean res =
            !result.getReturnTypeSource().getDeclaringClass().isAnnotationPresent(IgnoreBodyAdvice.class) ||
                //!returnType.getParameterType().isAssignableFrom(ResponseDTO.class) ||
                !predicate.test(methodPredicate);
        if (debug) {
            log.info("advice on {} is {};", methodPredicate, res ? "ON" : "OFF");
        }
        return res;
    }

    @Override
    public @NotNull Mono<Void> handleResult(@NotNull ServerWebExchange exchange, @NotNull HandlerResult result) {
        ReactiveAdapter adapter = this.getAdapter(result);
        MethodParameter actualParameter = result.getReturnTypeSource();
        if (adapter == null || !adapter.isMultiValue()) {
            Mono<?> returnValueMono;
            MethodParameter bodyParameter;
            if (adapter != null && !adapter.isMultiValue()) {
                returnValueMono = Mono.from(adapter.toPublisher(result.getReturnValue()));
                bodyParameter = actualParameter.nested().nested();
            } else {
                returnValueMono = Mono.justOrEmpty(result.getReturnValue());
                bodyParameter = actualParameter.nested();
            }
            final AtomicBoolean hasAdvise = new AtomicBoolean(true);
            returnValueMono = returnValueMono.map(o -> processInnerData(o, hasAdvise, exchange, result));
            return writeBody(returnValueMono, bodyParameter, exchange);
        } else {
            final AtomicBoolean hasAdvise = new AtomicBoolean(true);
            Flux<?> returnValueFlux = Flux.from(adapter.toPublisher(result.getReturnValue()))
                .map(o -> processInnerData(o, hasAdvise, exchange, result));
            MethodParameter bodyParameter = actualParameter.nested().nested();
            return writeBody(returnValueFlux, bodyParameter, exchange);
        }

    }

    private Object processInnerData(Object o, AtomicBoolean hasAdvise, @NotNull ServerWebExchange exchange, @NotNull HandlerResult result) {
        HttpEntity<?> httpEntity;
        if (o instanceof HttpEntity) {
            httpEntity = (HttpEntity<?>) o;
            Object body = httpEntity.getBody();
            Object newBody = processBody(body, hasAdvise, exchange, result);
            httpEntity = new HttpEntity<>(newBody, httpEntity.getHeaders());
        } else if (o instanceof HttpHeaders) {
            httpEntity = new ResponseEntity<>((HttpHeaders) o, HttpStatus.OK);
        } else {
            return processBody(o, hasAdvise, exchange, result);
        }
        HttpHeaders entityHeaders = httpEntity.getHeaders();
        HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
        if (!entityHeaders.isEmpty()) {
            entityHeaders.forEach(responseHeaders::put);
        }
        entityHeaders.setContentType(MediaType.APPLICATION_JSON);
        return httpEntity;
    }

    private Object processBody(Object o, AtomicBoolean hasAdvise, @NotNull ServerWebExchange exchange, @NotNull HandlerResult result) {
        if (hasAdvise.get()) {
            Optional<Object> advise = customerAdviser.advise(o, exchange, result);
            if (!advise.isPresent()) {
                hasAdvise.set(false);
            } else {
                return advise.get();
            }
        }
        if (o instanceof ResponseDTO) {
            int status = ((ResponseDTO<?>) o).getStatus();
            if (HttpStatus.resolve(status) != null) {
                exchange.getResponse().setRawStatusCode(status);
            }
            return o;
        } else return ResponseDTO.success(o);
    }
}
