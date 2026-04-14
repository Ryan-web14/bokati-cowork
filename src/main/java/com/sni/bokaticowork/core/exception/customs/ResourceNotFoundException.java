package com.sni.bokaticowork.core.exception.customs;

import com.sni.bokaticowork.core.utils.error.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends BaseException {

    private static final String ERROR_CODE = ErrorCode.RESOURCE_NOT_FOUND;

    public ResourceNotFoundException(String message) {
        super(message, ERROR_CODE);
    }

    public ResourceNotFoundException(String message, Throwable cause){
        super(message, ERROR_CODE, cause);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue){
        super(String.format("The ressource %s with the field %s with the value %s not found",
                resourceName, fieldName, fieldValue), ERROR_CODE);
    }

}
