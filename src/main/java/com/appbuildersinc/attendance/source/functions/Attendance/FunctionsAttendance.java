package com.appbuildersinc.attendance.source.functions.Attendance;

import com.appbuildersinc.attendance.source.Utilities.Email.emailUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.FacultyJwtUtil;
import com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.KeyPairUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.SuperAdminjwtUtil;
import com.appbuildersinc.attendance.source.database.MongoDB.FacultyDB;
import com.appbuildersinc.attendance.source.database.MongoDB.SuperAdminDB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.text.SimpleDateFormat;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;


@Service
public class FunctionsAttendance {
    private final FacultyDB userdb;
    private emailUtil emailclass;
    private final KeyPairUtil keyclass;
    private final FacultyJwtUtil jwtclass;
    private final SuperAdminDB admindb;
    private final SuperAdminjwtUtil adminjwtclass;
    @Autowired
    public FunctionsAttendance(FacultyDB userdb, FacultyJwtUtil jwtutil, emailUtil emailutil, KeyPairUtil keyutil, SuperAdminDB admindb, SuperAdminjwtUtil adminjwtclass) {
        this.userdb = userdb;
        this.emailclass =emailutil;
        this.keyclass =keyutil;
        this.jwtclass = jwtutil;
        this.admindb=admindb;
        this.adminjwtclass = adminjwtclass;
    }

    public String generateSubstitutionCode(String classCode, Date date) throws NoSuchAlgorithmException {

            // Convert Date to a string like "2025-06-16"
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String dateStr = sdf.format(date);

            // Combine classCode + date string
            String input = classCode + dateStr;

            // Hash using SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Take first few bytes and convert to long
            long hashLong = 0;
            for (int i = 0; i < 5; i++) {
                hashLong = (hashLong << 8) | (hashBytes[i] & 0xff);
            }

            // Convert to base36 (lowercase letters + digits)
            String code = Long.toString(Math.abs(hashLong), 36);

            // Ensure exactly 6 characters
            if (code.length() < 6) {
                code = String.format("%6s", code).replace(' ', 'a'); // pad with 'a'
            } else if (code.length() > 6) {
                code = code.substring(0, 6);
            }

            return code;
    }

    public Date createCleanDate (Date dateofuse){
        Calendar cal = Calendar.getInstance();
        cal.setTime(dateofuse);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date cleanDate = cal.getTime();
        return cleanDate;
    }


}
