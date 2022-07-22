package org.macula.boot.starter.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.macula.boot.exception.ApiException;
import org.macula.boot.api.ApiResultCode;
import org.macula.boot.api.Result;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

/**
 * 全局异常处理器
 *
 * @author rain
 */
@RestControllerAdvice
@Slf4j
public class ControllerExceptionAdvice implements MessageSourceAware {
    private MessageSource messageSource;

    @ExceptionHandler({BindException.class})
    public Result ValidExceptionHandler(BindException e) {
        // 从异常对象中拿到ObjectError对象
        log.error("BindException:" + ApiResultCode.VALIDATE_ERROR, e);

        StringBuilder validateMsg = new StringBuilder();
        for (ObjectError error : e.getAllErrors()) {
            validateMsg.append("[");
            validateMsg.append(((FieldError) error).getField());
            validateMsg.append(" ");
            validateMsg.append(error.getDefaultMessage());
            validateMsg.append("]");
        }

        return Result.failed(ApiResultCode.VALIDATE_ERROR, validateMsg.toString());
    }

    @ExceptionHandler({ConstraintViolationException.class})
    public Result ValidExceptionHandler(ConstraintViolationException e) {
        // 从异常对象中拿到ObjectError对象
        log.error("ConstraintViolationException:" + ApiResultCode.VALIDATE_ERROR, e);

        StringBuilder validateMsg = new StringBuilder();
        for (ConstraintViolation<?> error : e.getConstraintViolations()) {
            PathImpl pathImpl = (PathImpl) error.getPropertyPath();
            String paramName = pathImpl.getLeafNode().getName();
            validateMsg.append("[");
            validateMsg.append(paramName);
            validateMsg.append(" ");
            validateMsg.append(error.getMessage());
            validateMsg.append("]");
        }

        return Result.failed(ApiResultCode.VALIDATE_ERROR, validateMsg.toString());
    }

    @ExceptionHandler(ApiException.class)
    public Result ApiExceptionHandler(ApiException e) {
        log.error("ApiException:" + e.getResultCode(), e);
        return Result.failed(e.getResultCode(), messageSource.getMessage(e.getMessage(), null, LocaleContextHolder.getLocale()));
    }

    @ExceptionHandler(Exception.class)
    public Result ExceptionHandler(Exception e) {
        return Result.failed(ApiResultCode.SYS_ERROR, e.getMessage());
    }

    @Override
    public void setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource;
    }
}
