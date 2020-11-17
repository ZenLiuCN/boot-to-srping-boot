package cn.zenliu.java.boot.advise.mvc;

import cn.zenliu.java.boot.advise.annotations.IgnoreBodyAdvice;
import cn.zenliu.java.boot.advise.entities.ResponseDTO;
import cn.zenliu.java.boot.advise.port.CustomerMvcAdviser;
import cn.zenliu.java.boot.advise.properties.ResponseAdviserProperty;
import cn.zenliu.java.boot.advise.util.Util;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.*;
import java.util.function.Predicate;

/**
 * @author Zen.Liu
 * @apiNote
 * @since 2020-11-17
 */
@RestControllerAdvice
@Slf4j
public class ResponseAdviser implements ResponseBodyAdvice<Object> {

    private final boolean debug;
    private final boolean enable;
    private final Predicate<String> predicate;
    private final CustomerMvcAdviser customerAdviser;

    private static CustomerMvcAdviser adviserBuilder(List<CustomerMvcAdviser> customerAdvisers) {
        if (customerAdvisers.isEmpty()) {
            return (a, b, c, d, e, f) -> Optional.empty();
        } else
            return (a, b, c, d, e, f) -> {
                for (CustomerMvcAdviser i : customerAdvisers) {
                    Optional<Object> advise = i.advise(a, b, c, d, e, f);
                    if (advise.isPresent()) {
                        return advise;
                    }
                }
                return Optional.empty();
            };
    }

    public ResponseAdviser(ResponseAdviserProperty property, List<CustomerMvcAdviser> customerAdvisers) {
        this.debug = property.isDebug();
        this.enable = property.isEnable();
        this.predicate = Util.predicateBuilder(property);
        this.customerAdviser = adviserBuilder(customerAdvisers);

    }

    static String className(MethodParameter methodParameter) {
        try {
            return methodParameter.getDeclaringClass().getCanonicalName() + "#" + methodParameter.getMethod().getName();
        } catch (Exception e) {
            return methodParameter.getDeclaringClass().getCanonicalName();
        }
    }

    @Override
    public boolean supports(
        @NotNull MethodParameter returnType,
        @NotNull Class<? extends HttpMessageConverter<?>> converterType) {
        if (!enable) return false;
        String methodPredicate = className(returnType);
        final boolean res =
            !returnType.getDeclaringClass().isAnnotationPresent(IgnoreBodyAdvice.class) ||
                //!returnType.getParameterType().isAssignableFrom(ResponseDTO.class) ||
                !predicate.test(methodPredicate);
        if (debug) {
            log.info("advice on {} is {};", methodPredicate, res ? "ON" : "OFF");
        }
        return res;
    }

    final HashMap<String, Object> emptyMap = new HashMap<>();
    final ArrayList<Object> emptyList = new ArrayList<>();

    @Override
    public Object beforeBodyWrite(
        Object body,
        @NotNull MethodParameter returnType,
        MediaType selectedContentType,
        @NotNull Class<? extends HttpMessageConverter<?>> converterType,
        @NotNull ServerHttpRequest serverHttpRequest,
        @NotNull ServerHttpResponse serverHttpResponse) {
        if (selectedContentType.toString().contains("actuator")) return body;
        serverHttpResponse.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        final Optional<Object> advise = customerAdviser.advise(body, returnType, selectedContentType, converterType, serverHttpRequest, serverHttpResponse);
        if (advise.isPresent()) {
            return advise;
        }
        int status;
        if (returnType.getParameterType().isAssignableFrom(ResponseEntity.class)) {
            if (serverHttpResponse instanceof ServletServerHttpResponse) {
                status = ((ServletServerHttpResponse) serverHttpResponse).getServletResponse().getStatus();
            } else {
                status = 200;
            }
            if (debug) log.info("found ResponseEntity body {}({}) with status {}", body, body.getClass(), status);
            if (body instanceof String) {
                return status == 200 ? ResponseDTO.success(body) : ResponseDTO.error(status, (String) null, (String) body);
            } else {
                return status == 200 ? ResponseDTO.success(body) : ResponseDTO.error(status, "special response", null);
            }
        }


        if (body == null) {
            if (returnType.getParameterType() == String.class) {
                return ResponseDTO.success("");
            } else if (
                returnType.getParameterType().isArray() ||
                    returnType.getParameterType().isAssignableFrom(Collection.class)
            ) {
                return ResponseDTO.success(emptyList);
            } else {
                return ResponseDTO.success(emptyMap);
            }
        } else if (body instanceof ResponseDTO) {
            status = ((ResponseDTO<?>) body).getStatus();
            HttpStatus httpStatus = HttpStatus.resolve(status);
            if (httpStatus == null) {
                if (serverHttpResponse instanceof ServletServerHttpResponse) {
                    ((ServletServerHttpResponse) serverHttpResponse).getServletResponse().setStatus(status);
                } else {
                    serverHttpResponse.setStatusCode(((ResponseDTO<?>) body).getStatus() != 200 ?
                        HttpStatus.INTERNAL_SERVER_ERROR
                        : HttpStatus.OK);
                }
            } else {
                serverHttpResponse.setStatusCode(httpStatus);
            }
            return body;
        } else {
            return ResponseDTO.success(body);
        }

    }
}
