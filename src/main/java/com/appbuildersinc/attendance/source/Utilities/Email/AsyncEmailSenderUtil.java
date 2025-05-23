
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
            org.slf4j.LoggerFactory.getLogger(AsyncEmailSenderUtil.class).error("Failed to connect to email server", e);
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
            //String sender = username;


            try {
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(username, "Attendez App"));
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
                message.setSubject("Your OTP for AttendEz App Login");

                String htmlContent = String.format(
                        """
                        <div style='background:linear-gradient(135deg,#e0e7ff 0%%,#f7f9fa 100%%);padding:40px 0;font-family:Segoe UI,Arial,sans-serif; background-image: url("data:image/svg+xml,%%3Csvg xmlns='http://www.w3.org/2000/svg' width='100' height='100' viewBox='0 0 100 100'%%3E%%3Ccircle cx='50' cy='50' r='40' fill='%%23E6F4EA' /%%3E%%3C/svg%%3E"); background-repeat: repeat;'>
                            <div style='max-width:480px;margin:auto;background:#FFFFFF;border-radius:18px;box-shadow:0 4px 24px rgba(0,0,0,0.1);padding:40px 32px;'>
                                <div style='text-align:center;margin-bottom:24px;'>
                                    <img src='https://img.icons8.com/color/96/000000/lock--v2.png' alt='OTP' style='width:64px;height:64px;margin-bottom:12px;'/>
                                    <h2 style='color:#0055A4;font-size:2rem;margin:0 0 8px 0;'>AttendEz App</h2>
                                </div>
                                <p style='font-size:17px;color:#222;text-align:center;margin-bottom:24px;'>
                                    Use the following <strong style='color:#28A745;'>One-Time Password (OTP)</strong> to continue:
                                </p>
                                <div style='margin:32px 0;text-align:center;'>
                                    <span style='display:inline-block;background-color:#E6F4EA;padding:22px 44px;font-size:36px;font-weight:800;letter-spacing:8px;color:#28A745;border-radius:12px;box-shadow:0 2px 12px #28A745AA;'>
                                        %s
                                    </span>
                                </div>
                                <p style='color:#666666;font-size:15px;text-align:center;margin-bottom:24px;'>
                                    This OTP is valid for one login and should not be shared with anyone.
                                </p>
                                <hr style='border:none;border-top:1px solid #DDDDDD;margin:28px 0;'>
                                <p style='font-size:13px;color:#999999;text-align:center;'>
                                    If you did not request this, please ignore this email.<br>
                                    AttendEz App &copy; 2025
                                </p>
                            </div>
                        </div>
                        """,
                        otp
                );



                message.setContent(htmlContent, "text/html; charset=utf-8");
                Transport.send(message);
                break;
            } catch (MessagingException mex) {
                org.slf4j.LoggerFactory.getLogger(AsyncEmailSenderUtil.class).error("Failed to send OTP email to {}: {}", recipient, mex.getMessage(), mex);

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