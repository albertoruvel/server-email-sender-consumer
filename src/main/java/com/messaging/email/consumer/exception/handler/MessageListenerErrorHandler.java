package com.messaging.email.consumer.exception.handler;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;

@Component
public class MessageListenerErrorHandler implements ErrorHandler {

    private final Logger logger = Logger.getLogger(getClass());

    @Override
    public void handleError(Throwable throwable) {
        logger.fatal(throwable);
    }
}
