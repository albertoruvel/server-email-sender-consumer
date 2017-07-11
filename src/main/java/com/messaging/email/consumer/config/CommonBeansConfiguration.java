package com.messaging.email.consumer.config;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.ui.velocity.VelocityEngineFactoryBean;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Properties;

@Configuration
@ComponentScan("com.messaging.email.consumer")
@Import(JmsConfiguration.class)
public class CommonBeansConfiguration {

    @Value("${mail.config.host}")
    private String mailHost;

    @Value("${mail.config.port}")
    private int mailPort;

    @Value("${mail.config.username}")
    private String mailUsername;

    @Value("${mail.config.password}")
    private String mailPassword;

    @Value("${mail.config.debug}")
    private String mailDebug;

    @Bean
    public static PropertySourcesPlaceholderConfigurer pspc() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean(name = "dateFormat")
    public DateFormat dateFormat(){
        return new SimpleDateFormat("MM-dd-YYYY HH:ss");
    }

    @Bean(name = "gson")
    public Gson gson(){
        return new Gson();
    }

    @Bean(name = "velocityEngine")
    public VelocityEngineFactoryBean velocityEngineFactoryBean(){
        VelocityEngineFactoryBean velocityEngineFactoryBean = new VelocityEngineFactoryBean();
        Properties velocityProperties = new Properties();
        velocityProperties.put("resource.loader", "class");
        velocityProperties.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        velocityEngineFactoryBean.setVelocityProperties(velocityProperties);
        return velocityEngineFactoryBean;
    }

    @Bean
    public JavaMailSender javaMailSender(){
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(mailHost);
        sender.setPort(mailPort);
        sender.setUsername(mailUsername);
        sender.setPassword(mailPassword);

        Properties javaMailProperties = new Properties();

        javaMailProperties.put("mail.smtp.starttls.enable", "true");
        javaMailProperties.put("mail.smtp.auth", "true");
        javaMailProperties.put("mail.transport.protocol", "smtp");
        javaMailProperties.put("mail.debug", mailDebug);

        sender.setJavaMailProperties(javaMailProperties);
        return sender;
    }

}
