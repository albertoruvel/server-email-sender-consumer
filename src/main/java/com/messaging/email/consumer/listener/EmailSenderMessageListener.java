package com.messaging.email.consumer.listener;

import com.amazon.sqs.javamessaging.message.SQSTextMessage;
import com.messaging.email.consumer.exception.EmailSenderException;
import com.messaging.email.consumer.service.EmailSenderService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EmailSenderMessageListener {

    @Autowired
    private EmailSenderService emailSenderService;

    private final Logger logger = Logger.getLogger(getClass());

    public void onMessage(SQSTextMessage message){
        try{
            emailSenderService.sendEmail(message);
        }catch(EmailSenderException ex){
            logger.error(ex);
        }catch(Exception ex){
            logger.fatal(ex);
        }
    }
}
