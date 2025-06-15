// src/main/java/com/appbuildersinc/attendance/source/Utilities/emailUtil.java
package com.appbuildersinc.attendance.source.Utilities.Email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.Map;
import java.util.Random;

// emailUtil is a utility class for sending emails asynchronously
//CALL THIS CLASS TO SEND EMAILS
@Service
public class emailUtil {

    @Autowired
    private AsyncEmailSenderUtil asyncEmailSender;

    public int sendMail(String mail) {
        int otp = 100000 + new Random().nextInt(900000);
        asyncEmailSender.sendOtpEmail(mail, String.valueOf(otp));
        return otp;
    }

    public void sendClassTransferMail(String mail, Map<String, Object> details) {

        asyncEmailSender.ClassTransferEmail(mail, details);

    }

}