package org.apache.james.examples.custom.mailets;

import com.google.common.base.Strings;
import org.apache.commons.mail.util.MimeMessageParser;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMailet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;

public class CreateTicket extends GenericMailet {
    private static final Logger logger = LoggerFactory.getLogger(CreateTicket.class);

    private String ticketServiceUrl;
    @Override
    public void service(Mail mail) throws MessagingException {

        try {
            createTicketWithAttachments(mail);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.info("Create Ticket mailet");
    }

    private void createTicketWithAttachments(Mail mail) throws MessagingException, IOException {
        MimeMessage message = mail.getMessage();
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        if (message.isMimeType("multipart/*")) {
            logger.info("multipart mail");
            Multipart multipart = (Multipart) message.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                MimeBodyPart part = (MimeBodyPart) multipart.getBodyPart(i);
                logger.info("multipart body: {}", part);
                if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                    InputStream attachmentStream = part.getInputStream();
                    builder.addBinaryBody("files", attachmentStream, ContentType.DEFAULT_BINARY, part.getFileName());
                }
            }
        }
        builder.addTextBody("json", getMailJsonString(mail), ContentType.APPLICATION_JSON);
        sendHttpRequest(builder);
    }

    private void sendHttpRequest(MultipartEntityBuilder builder) {
        try(CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost postRequest = new HttpPost(ticketServiceUrl);
            HttpEntity multipart = builder.build();
            logger.info("Built multipart: {}", multipart);
            logger.info("Built multipart content: {}", multipart.getContent());
            postRequest.setEntity(multipart);

            try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
                HttpEntity responseEntity = response.getEntity();
                String responseString = EntityUtils.toString(responseEntity);
                logger.info("Response: {}", responseString);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getMailJsonString(Mail mail) throws MessagingException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sender", mail.getMaybeSender().asString());
        jsonObject.put("recipients", new JSONArray(mail.getRecipients().toString()));
        jsonObject.put("subject", mail.getMessage().getSubject());
        try {
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
            logger.info("mail content: {}", mail.getMessage().getContent());

            logger.info("mail : {}", mail);
            logger.info("mail message : {}", mail.getMessage());
            logger.info("parsed mail : {}", new MimeMessageParser(mail.getMessage()).parse());
//            String mailContent = new MimeMessageParser(mail.getMessage()).parse().getHtmlContent();
//            logger.info("initial content: {}", mailContent);
//            mailContent = mailContent.split("<div style=\"color:#a0a8a8;\" id=\"mo-hld-reply\">")[0];
//            logger.info("Processed content: {}", mailContent);
            String body = "";
            Object content = mail.getMessage().getContent();
            if (content instanceof String) {
                logger.info("string content");
                body = new MimeMessageParser(mail.getMessage()).parse().getHtmlContent();
            } else if (content instanceof Multipart) {
                logger.info("multipart content");
                body = getBodyFromMultipart((Multipart) content, mail);
            }
            logger.info("Body original : {}", body);
            body = body.split("<div style=\"color:#a0a8a8\">----Please reply above this line----</div>")[0];
            logger.info("Parsed body : {}", body);
            jsonObject.put("body", body);
        } catch (Exception e) {
            logger.error("An error occurred while fetching mail contents");
            e.printStackTrace();
        }
        jsonObject.put("date", mail.getMessage().getSentDate());

        return jsonObject.toString();
    }

    private String getBodyFromMultipart(Multipart multipart, Mail mail) throws Exception {
        StringBuilder textContent = new StringBuilder();
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain") || bodyPart.isMimeType("text/html")) {
                logger.info("plain text");
                textContent.append(new MimeMessageParser(mail.getMessage()).parse().getHtmlContent());
                break;
            } else if (bodyPart.getContent() instanceof Multipart) {
                // Recursively handle nested multipart
                logger.info("multipart");
                textContent.append(getBodyFromMultipart((Multipart) bodyPart.getContent(), mail));
            }
        }
        return textContent.toString();
    }

//    private void createTicket(Mail mail) throws MessagingException, IOException {
//
//        String uri = ticketServiceUrl;
//
//
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("sender", mail.getMaybeSender().asString());
//        jsonObject.put("recipients", new JSONArray(mail.getRecipients().toString()));
//        jsonObject.put("subject", mail.getMessage().getSubject());
//        try {
//            String mailContent = new MimeMessageParser(mail.getMessage()).parse().getHtmlContent();
//            logger.info("initial content: {}", mailContent);
//            mailContent = mailContent.split("<div style=\"color:#a0a8a8;\" id=\"mo-hld-reply\">")[0];
//            logger.info("Processed content: {}", mailContent);
//            jsonObject.put("body", mailContent);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//        jsonObject.put("date", mail.getMessage().getSentDate());
//
//        String args = jsonObject.toString();
//        logger.info("Mail args: {}", args);
//        String response = makeRestRemoteCall(uri, args);
//        logger.info("API Response: {}", response);
//    }
//
//    private String makeRestRemoteCall(String uri, String args) {
//        try {
//            RestTemplate restTemplate = new RestTemplate();
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            HttpEntity<String> request = new HttpEntity<>(args, headers);
//            return restTemplate.postForObject(uri, request, String.class);
//        } catch (Exception e) {
//            logger.error(e.getMessage());
//            e.printStackTrace();
//            return StringUtils.EMPTY;
//        }
//    }

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
