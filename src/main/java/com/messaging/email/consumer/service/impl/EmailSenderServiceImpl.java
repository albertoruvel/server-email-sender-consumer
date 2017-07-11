package com.messaging.email.consumer.service.impl;

import com.amazon.sqs.javamessaging.message.SQSTextMessage;
import com.dareu.web.dto.request.email.EmailRequest;
import com.google.gson.Gson;
import com.messaging.email.consumer.exception.EmailSenderException;
import com.messaging.email.consumer.service.EmailSenderService;
import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.StringWriter;

@Component
public class EmailSenderServiceImpl implements EmailSenderService {

    private static final String ERROR_EMAIL_TEMPLATE_PATH = "/templates/error-message-template.vm";

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    @Qualifier("velocityEngine")
    private VelocityEngine velocityEngine;

    @Value("${mail.config.recipients}")
    private String adminRecipients;

    @Autowired
    @Qualifier("gson")
    private Gson gson;

    private final Logger log = Logger.getLogger(getClass());


    @Override
    public void sendEmail(SQSTextMessage textMessage)  throws EmailSenderException {
        try{
            final String jsonMessage = textMessage.getText();
            final EmailRequest emailRequest = gson.fromJson(jsonMessage, EmailRequest.class);
            final MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            Template template;
            switch(emailRequest.getEmailType()){
                case ERROR:
                    template = velocityEngine.getTemplate(ERROR_EMAIL_TEMPLATE_PATH);
                    sendErrorEmail(emailRequest, helper, template);
                    break;
                case ANOTHER_USER_ACTION:
                    //todo: add more actions here
                    break;
            }
        }catch(JMSException ex){
            throw new EmailSenderException("Error reading message content: " + ex.getMessage());
        }catch(MessagingException ex){
            throw new EmailSenderException("Error sending email: " + ex.getMessage());
        }catch(ResourceNotFoundException ex){
            throw new EmailSenderException("Error reading template file: " + ex.getMessage());
        }catch(Exception ex){
            throw new EmailSenderException("Unknown Error: " + ex.getMessage());
        }
    }

    private final void sendErrorEmail(EmailRequest request, final MimeMessageHelper helper, Template template)throws MessagingException{
        String[] recipients = adminRecipients.split(",");
        if (recipients.length == 0) {
            throw new IllegalArgumentException("No email recipients to send error email");
        }
        VelocityContext context = new VelocityContext();
        context.put("request", request);
        helper.setTo(recipients);
        final StringWriter writer = new StringWriter();
        template.merge(context, writer);
        helper.setText(writer.toString(), true);
        helper.setFrom("Custom Error Reporter");
        helper.setSubject(String.format("%s error", request.getApplicationId()));
        javaMailSender.send(helper.getMimeMessage());
    }
}
