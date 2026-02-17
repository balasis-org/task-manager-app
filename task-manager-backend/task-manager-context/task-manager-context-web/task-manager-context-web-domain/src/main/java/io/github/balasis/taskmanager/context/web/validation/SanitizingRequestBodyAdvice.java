package io.github.balasis.taskmanager.context.web.validation;

import io.github.balasis.taskmanager.context.web.resource.BaseInboundResource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * Automatically sanitises every {@link String} field on incoming
 * {@link BaseInboundResource} request bodies <b>before</b> validation
 * and controller logic run.
 * <p>
 * project-wide XSS protection
 */
@RestControllerAdvice
public class SanitizingRequestBodyAdvice extends RequestBodyAdviceAdapter {

    @Override
    public boolean supports(MethodParameter methodParameter,
                            Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        if (targetType instanceof Class<?> clazz) {
            return BaseInboundResource.class.isAssignableFrom(clazz);
        }
        return false;
    }

    @Override
    public Object afterBodyRead(Object body,
                                HttpInputMessage inputMessage,
                                MethodParameter parameter,
                                Type targetType,
                                Class<? extends HttpMessageConverter<?>> converterType) {
        sanitizeFields(body);
        return body;
    }

    private static void sanitizeFields(Object obj) {
        if (obj == null) return;

        Class<?> clazz = obj.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getType() == String.class) {
                    field.setAccessible(true);
                    try {
                        String value = (String) field.get(obj);
                        if (value != null) {
                            field.set(obj, InputSanitizer.sanitize(value));
                        }
                    } catch (IllegalAccessException e) {
                        // should not happen after setAccessible(true)
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
    }
}
