
package com.web.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<Void> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        return ApiResponse.fail(1003, "file too large");
    }


    @ExceptionHandler(MultipartException.class)
    public ApiResponse<Void> handleMultipartException(MultipartException e) {
        Throwable c = e.getCause();
        if (c instanceof MaxUploadSizeExceededException) {
            return ApiResponse.fail(1003, "file too large");
        }
        return ApiResponse.fail(5000, "multipart error: " + e.getMessage());
    }
}