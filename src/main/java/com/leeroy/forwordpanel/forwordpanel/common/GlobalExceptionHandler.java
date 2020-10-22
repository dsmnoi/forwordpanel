package com.leeroy.forwordpanel.forwordpanel.common;

import com.leeroy.forwordpanel.forwordpanel.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public ApiResponse defaultErrorHandler(HttpServletRequest req, Exception e) {
        if (e instanceof WebException) {
            return ApiResponse.error(((WebException) e).getErrorCode(), ((WebException) e).getErrorMsg());
        }
        log.error(">>>>>>>>>>>>>>GlobalExceptionHandler catch exception {}", e);
        return ApiResponse.error("500", "操作失败");
    }
}
