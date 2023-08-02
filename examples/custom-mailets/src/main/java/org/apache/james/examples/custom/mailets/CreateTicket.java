package org.apache.james.examples.custom.mailets;

import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMailet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class CreateTicket extends GenericMailet {
    private static final Logger logger = LoggerFactory.getLogger(CreateTicket.class);
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
            createTicket(mail);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Create Ticket mailet");
        System.out.println(mail);
    }

    private void createTicket(Mail mail) throws MessagingException, IOException {

        String uri = "http://localhost:8082/ticket/createTicket";


        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sender", mail.getMaybeSender().asString());
        jsonObject.put("recipients", new JSONArray(mail.getRecipients().toString()));
        jsonObject.put("subject", mail.getMessage().getSubject());
        jsonObject.put("body", mail.getMessage().getContent());
        jsonObject.put("date", mail.getMessage().getSentDate());

        String args = jsonObject.toString();
        logger.info("Mail args: {}", args);
        String response = makeRemoteCall(uri, args);
        logger.info("API Response: {}", response);
    }

    private String makeRemoteCall(String uri, String args) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(args))
                .build();

        HttpResponse<String> response = null;
        try {
            response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return response.body();
    }

    @Override
    public String getMailetName() {
        return "CreateTicket";
    }
}
