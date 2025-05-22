
package com.appbuildersinc.attendance.source.Utilities.Email;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

// AsyncEmailSenderUtil is a functions class for sending emails asynchronously
@Service
public class AsyncEmailSenderUtil {

    private static Transport mailTransport = null;
    private static Session session = null;
    private static final int MAX_ATTEMPTS = 1;

    public static void initEmailProperties() {
        // config setup
        Dotenv dotenv = Dotenv.configure()
                .filename("apiee.env")
                .load();

        String host = "smtp.gmail.com";
        String username = dotenv.get("MAIL_ID");
        String password = dotenv.get("MAIL_PASSKEY");

        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.starttls.required", "true");
        properties.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        properties.put("mail.smtp.connectiontimeout", "7000");
        properties.put("mail.smtp.timeout", "7000");
        properties.put("mail.smtp.writetimeout", "7000");

        // Creating a new session with authentication
        session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            mailTransport = session.getTransport("smtp");
            mailTransport.connect();
            System.out.println("Successfully connected to email server.");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
    @Async
    public void sendOtpEmail(String recipient, String otp) {


        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            if (mailTransport == null || !mailTransport.isConnected()) {
                initEmailProperties();
            }

            Dotenv dotenv = Dotenv.configure()
                    .filename("apiee.env")
                    .load();
            String username = dotenv.get("MAIL_ID");
            String sender = username;


            try {
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(username, "Attendez App"));
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
                message.setSubject("Your OTP for Attendez App Login");

                String htmlContent = String.format(
                        "<div style='background:#f7f9fa;padding:32px 0;font-family:Segoe UI,Arial,sans-serif;'>"
                                + "<div style='max-width:420px;margin:auto;background:#fff;border-radius:12px;box-shadow:0 2px 12px #0001;padding:32px;'>"
                                + "<h2 style='color:#2d7ff9;text-align:center;margin-bottom:16px;'>Attendex App</h2>"
                                + "<p style='font-size:16px;color:#333;text-align:center;'>Use the following <b>One-Time Password (OTP)</b> to continue:</p>"
                                + "<div style='margin:32px 0;text-align:center;'>"
                                + "  <span style='display:inline-block;background:#f0f4f8;padding:18px 36px;font-size:28px;font-weight:700;letter-spacing:6px;color:#2d7ff9;border-radius:8px;'>%s</span>"
                                + "</div>"
                                + "<p style='color:#666;font-size:14px;text-align:center;'>This OTP is valid for one login and should not be shared with anyone.</p>"
                                + "<hr style='border:none;border-top:1px solid #eee;margin:24px 0;'>"
                                + "<p style='font-size:12px;color:#aaa;text-align:center;'>"
                                + "If you did not request this, please ignore this email.<br>"
                                + "Attendez App &copy; 2025"
                                + "</p></div></div>",
                        otp
                );

                message.setContent(htmlContent, "text/html; charset=utf-8");
                Transport.send(message);
                break;
            } catch (MessagingException mex) {
                mex.printStackTrace();
                try {
                    Thread.sleep((long) (Math.pow(2, i) * 1000));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }


}