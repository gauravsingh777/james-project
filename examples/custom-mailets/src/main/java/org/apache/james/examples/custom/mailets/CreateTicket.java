package org.apache.james.examples.custom.mailets;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMailet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import javax.mail.MessagingException;
import java.io.IOException;

public class CreateTicket extends GenericMailet {
    private static final Logger logger = LoggerFactory.getLogger(CreateTicket.class);

    private String ticketServiceUrl;
    @Override
    public void service(Mail mail) throws MessagingException {
        logger.info("Custom mailet called: {}", mail);
        logger.info("mail name: {}", mail.getName());
        logger.info("Sender ID: {}", mail.getMaybeSender().asString());
        logger.info("Recipients: {}", mail.getRecipients());
        logger.info("Recipients array: {}", mail.getRecipients().toArray());
        logger.info("mail from: {}", mail.getMessage().getFrom());
        logger.info("mail ReplyTo: {}", mail.getMessage().getReplyTo());
        logger.info("mail Subject: {}", mail.getMessage().getSubject());
        logger.info("mail description: {}", mail.getMessage().getDescription());
        logger.info("mail sent date: {}", mail.getMessage().getSentDate());

        try {
            logger.info("mail content: {}", mail.getMessage().getContent());
            createTicket(mail);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Create Ticket mailet");
        System.out.println(mail);
    }

    private void createTicket(Mail mail) throws MessagingException, IOException {

        String uri = ticketServiceUrl;


        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sender", mail.getMaybeSender().asString());
        jsonObject.put("recipients", new JSONArray(mail.getRecipients().toString()));
        jsonObject.put("subject", mail.getMessage().getSubject());
        try {
            String mailContent = new MimeMessageParser(mail.getMessage()).parse().getHtmlContent();
            logger.info("initial content: {}", mailContent);
            mailContent = mailContent.split("<div style=\"color:#a0a8a8;\" id=\"mo-hld-reply\">")[0];
            logger.info("Processed content: {}", mailContent);
            jsonObject.put("body", mailContent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        jsonObject.put("date", mail.getMessage().getSentDate());

        String args = jsonObject.toString();
        logger.info("Mail args: {}", args);
        String response = makeRestRemoteCall(uri, args);
        logger.info("API Response: {}", response);
    }

    private String makeRestRemoteCall(String uri, String args) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(args, headers);
            return restTemplate.postForObject(uri, request, String.class);
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            return StringUtils.EMPTY;
        }
    }

    @Override
    public void init() throws MessagingException {
        ticketServiceUrl = getInitParameter("ticketServiceUrl");

        if (Strings.isNullOrEmpty(ticketServiceUrl)) {
            throw new MessagingException("'ticketServiceUrl' is compulsory");
        }
    }

    @Override
    public String getMailetName() {
        return "CreateTicket";
    }
}
