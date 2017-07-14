package com.messaging.email.consumer.service.impl;

import com.amazon.sqs.javamessaging.message.SQSTextMessage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.messaging.dto.email.*;
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
    private static final String CONTACT_MESSAGE_TEMPLATE_PATH = "/templates/contact-message.vm";
    private static final String WELCOME_USER_TEMPLATE_PATH = "/templates/welcome-user-template.vm";
    private static final String REQUESTED_FRIENDSHIP_TEMPLATE_PATH = "/templates/friendship-requested.vm";
    private static final String CONTACT_MESSAGE_REPLY_TEMPLATE_PATH = "/templates/contact-message-reply.vm";

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
                sendEmail(emailRequest, helper, template, adminRecipients.split(","), String.format("%s error", emailRequest.getApplicationId()));

                break;
            case USER_REGISTRATION:
                emailRequest = gson.fromJson(jsonText, new TypeToken<EmailRequest<WelcomeEmailPayload>>(){}.getType());
                template = velocityEngine.getTemplate(WELCOME_USER_TEMPLATE_PATH);
                sendEmail(emailRequest, helper, template, (String[])emailRequest.getRecipients().toArray(), "Welcome to DareU!");
                break;
            case CONTACT_MESSAGE_REPLY:
                emailRequest = gson.fromJson(jsonText, new TypeToken<EmailRequest<ContactReplyEmailPayload>>(){}.getType());
                template = velocityEngine.getTemplate(CONTACT_MESSAGE_REPLY_TEMPLATE_PATH);
                sendEmail(emailRequest, helper, template, (String[])emailRequest.getRecipients().toArray(), "Your message has been replied!");
                break;
            case REQUESTED_FRIENDSHIP:
                emailRequest = gson.fromJson(jsonText, new TypeToken<EmailRequest<RequestedFriendshipEmailRequest>>(){}.getType());
                template = velocityEngine.getTemplate(REQUESTED_FRIENDSHIP_TEMPLATE_PATH);
                sendEmail(emailRequest, helper, template, (String[])emailRequest.getRecipients().toArray(), "You have pending connection requests!");
                break;
            case CONTACT_MESSAGE:
                emailRequest = gson.fromJson(jsonText, new TypeToken<EmailRequest<ContactEmailRequest>>(){}.getType());
                template = velocityEngine.getTemplate(CONTACT_MESSAGE_TEMPLATE_PATH);
                sendEmail(emailRequest, helper, template, (String[])emailRequest.getRecipients().toArray(), "New  contact message!");
                break;
            default:
                log.info(String.format("Received email type %s as default, will skip it"));
                break;
        }
    }

    private void sendEmail(EmailRequest request, MimeMessageHelper helper, Template template, String[] recipients, String emailTitle)throws MessagingException{
        Object payload = request.getBody();
        VelocityContext context = new VelocityContext();
        context.put("request", payload);
        helper.setTo(recipients);
        final StringWriter writer = new StringWriter();
        template.merge(context, writer);
        helper.setText(writer.toString(), true);
        helper.setFrom(mailUsername);
        helper.setSubject(emailTitle);
        javaMailSender.send(helper.getMimeMessage());
    }


}
