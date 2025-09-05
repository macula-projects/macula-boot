/*
 * Copyright (c) 2023 Macula
 *   macula.dev, China
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.macula.boot.starter.web.advice;

import dev.macula.boot.exception.BizCheckException;
import dev.macula.boot.exception.BizException;
import dev.macula.boot.result.ApiResultCode;
import dev.macula.boot.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

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
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> validExceptionHandler(BindException e) {
        // 处理 form data方式调用接口校验失败抛出的异常
        log.error("BindException:" + ApiResultCode.VALIDATE_ERROR, e);

        StringBuilder validateMsg = new StringBuilder();
        for (ObjectError error : e.getAllErrors()) {
            validateMsg.append("[");
            validateMsg.append(((FieldError)error).getField());
            validateMsg.append(" ");
            validateMsg.append(error.getDefaultMessage());
            validateMsg.append("]");
        }

        return Result.failed(ApiResultCode.VALIDATE_ERROR, validateMsg.toString());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> validExceptionHandler(MethodArgumentNotValidException e) {
        // <2> 处理 json 请求体调用接口校验失败抛出的异常
        log.error("MethodArgumentNotValidException:" + ApiResultCode.VALIDATE_ERROR, e);

        StringBuilder validateMsg = new StringBuilder();
        for (ObjectError error : e.getAllErrors()) {
            validateMsg.append("[");
            validateMsg.append(((FieldError)error).getField());
            validateMsg.append(" ");
            validateMsg.append(error.getDefaultMessage());
            validateMsg.append("]");
        }
        return Result.failed(ApiResultCode.VALIDATE_ERROR, validateMsg.toString());
    }

    @ExceptionHandler({IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> validExceptionHandler(IllegalArgumentException e) {
        return Result.failed(ApiResultCode.VALIDATE_ERROR, e.getMessage());
    }

    @ExceptionHandler({ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> validExceptionHandler(ConstraintViolationException e) {
        // 处理单个参数校验失败抛出的异常
        log.error("ConstraintViolationException:" + ApiResultCode.VALIDATE_ERROR, e);

        StringBuilder validateMsg = new StringBuilder();
        for (ConstraintViolation<?> error : e.getConstraintViolations()) {
            PathImpl pathImpl = (PathImpl)error.getPropertyPath();
            String paramName = pathImpl.getLeafNode().getName();
            validateMsg.append("[");
            validateMsg.append(paramName);
            validateMsg.append(" ");
            validateMsg.append(error.getMessage());
            validateMsg.append("]");
        }

        return Result.failed(ApiResultCode.VALIDATE_ERROR, validateMsg.toString());
    }

    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> bizExceptionHandler(BizException e) {
        log.error("BizException:" + e.getCode(), e);
        return Result.failed(e.getCode(), e.getMsg(),
            messageSource.getMessage(e.getMsg(), null, e.getMessage(), LocaleContextHolder.getLocale()));
    }

    @ExceptionHandler(BizCheckException.class)
    @ResponseStatus(HttpStatus.NOT_EXTENDED)
    public Result<?> bizCheckExceptionHandler(BizCheckException e) {
        log.info("BizCheckException:" + e.getCode(), e);
        return Result.failed(e.getCode(), e.getMsg(),
            messageSource.getMessage(e.getMsg(), null, e.getMessage(), LocaleContextHolder.getLocale()));
    }

    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> nullPointExceptionHandler(Exception e) {
        log.error("NullPointException: ", e);
        return Result.failed(ApiResultCode.SYS_ERROR, "空指针异常");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> exceptionHandler(Exception e) {
        log.error("Exception: ", e);
        return Result.failed(ApiResultCode.SYS_ERROR, e.getMessage());
    }

    @Override
    public void setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

}
