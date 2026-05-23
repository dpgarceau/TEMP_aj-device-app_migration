package com.aerojudge.judge.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.aerojudge.judge.dto.APIErrorMessage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ControllerAdvice(assignableTypes = { APIController.class })

public class GlobalRestExceptionHandler {
    // ConnectException all connection exeption api resonses
    private static final Logger LOGGER = LoggerFactory.getLogger(
            GlobalExceptionHandler.class);

    @ResponseBody
    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public APIErrorMessage restScoreUnavailable(HttpServletRequest request,
            HttpServletResponse response, Exception ex) {
        LOGGER.error("Exception occurred: " + ex.getMessage(), ex);
        LOGGER.info("PATH>>>>: " + request.getContextPath());
        // do something with request or response

        return new APIErrorMessage("fail", ("Couldnt Contact Score :" + ex.getMessage()));
    }
}
