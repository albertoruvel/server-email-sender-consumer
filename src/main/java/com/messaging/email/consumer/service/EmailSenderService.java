package com.messaging.email.consumer.service;

import com.amazon.sqs.javamessaging.message.SQSTextMessage;
import com.messaging.email.consumer.exception.EmailSenderException;

public interface EmailSenderService {
    public void sendEmail(SQSTextMessage textMessage) throws EmailSenderException;
}
