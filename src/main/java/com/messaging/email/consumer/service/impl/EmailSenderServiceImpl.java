package com.messaging.email.consumer.service.impl;

import com.amazon.sqs.javamessaging.message.SQSTextMessage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.messaging.dto.EmailRequest;
import com.messaging.dto.EmailType;
import com.messaging.dto.WelcomeEmailPayload;
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
    private static final String WELCOME_USER_TEMPLATE_PATH = "/templates/welcome-user-template.vm";

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    @Qualifier("velocityEngine")
    private VelocityEngine velocityEngine;

    @Value("${mail.config.recipients}")
    private String adminRecipients;

    @Value("${mail.config.username}")
    private String mailUsername;

    @Autowired
    @Qualifier("gson")
    private Gson gson;

    private final Logger log = Logger.getLogger(getClass());


    @Override
    public void sendEmail(SQSTextMessage textMessage)  throws EmailSenderException {
        try{
            final String emailTypeValue = textMessage.getStringProperty("emailType");
            if(emailTypeValue == null || emailTypeValue.isEmpty())
                throw new EmailSenderException("No emailType was found on message headers");
            EmailType emailType = EmailType.fromString(emailTypeValue);
            if(emailType == null)
                throw new EmailSenderException("No email type was provided");

            dispatchEmailRequest(textMessage, emailType);
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

    private void dispatchEmailRequest(SQSTextMessage textMessage, EmailType emailType)throws JMSException, MessagingException, ResourceNotFoundException{
        Template template;
        String jsonText = textMessage.getText();
        //try to parse message depending on email type
        final MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        final MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        EmailRequest emailRequest;
        switch(emailType){
            case ERROR:
                emailRequest = gson.fromJson(jsonText, new TypeToken<EmailRequest<String>>(){}.getType());
                template = velocityEngine.getTemplate(ERROR_EMAIL_TEMPLATE_PATH);
                sendErrorEmail(emailRequest, helper, template);
                break;
            case USER_REGISTRATION:
                emailRequest = gson.fromJson(jsonText, new TypeToken<EmailRequest<WelcomeEmailPayload>>(){}.getType());
                template = velocityEngine.getTemplate(WELCOME_USER_TEMPLATE_PATH);
                sendWelcomeUserEmail(emailRequest, helper, template);
                break;
        }
    }

    private void sendWelcomeUserEmail(EmailRequest<WelcomeEmailPayload> emailRequest, MimeMessageHelper helper, Template template)throws MessagingException {
        //get user id
        final WelcomeEmailPayload welcomePayload = emailRequest.getBody();
        VelocityContext context = new VelocityContext();
        context.put("request", welcomePayload);
        helper.setTo(welcomePayload.getEmail());
        final StringWriter writer = new StringWriter();
        template.merge(context, writer);
        helper.setText(writer.toString(), true);
        helper.setFrom(mailUsername);
        helper.setSubject("Welcome to DareU!");
        javaMailSender.send(helper.getMimeMessage());
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
        helper.setFrom(mailUsername);
        helper.setSubject(String.format("%s error", request.getApplicationId()));
        javaMailSender.send(helper.getMimeMessage());
    }
}
