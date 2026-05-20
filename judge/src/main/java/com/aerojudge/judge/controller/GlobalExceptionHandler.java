package com.aerojudge.judge.controller;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ControllerAdvice(assignableTypes = { RootController.class })
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            GlobalExceptionHandler.class);

    // this method handles all ioe exceptions for the application
    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String scoreUnavailable(HttpServletRequest request,
            HttpServletResponse response, Exception ex) {
        LOGGER.error("Exception occurred: " + ex.getMessage(), ex);
        LOGGER.info("PATH>>>>: " + request.getContextPath());
        System.out.println();
        // do something with request or response

        return "/needscore";
    }
}