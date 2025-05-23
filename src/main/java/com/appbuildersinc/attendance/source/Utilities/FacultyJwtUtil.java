package com.appbuildersinc.attendance.source.Utilities;

import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.*;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

//FacultyJwtUtil is a utility class for creating, signing, parsing, and validating JSON Web Tokens (JWTs) using HMAC SHA-256 algorithm
@Service
public class FacultyJwtUtil {
    private final Dotenv dotenv = Dotenv.configure().filename("apiee.env").load();
    private final String HMAC_SECRET = dotenv.get("JWT_HMAC_SECRET");
    private final int EXPIRATION_MINUTES =  Integer.parseInt(dotenv.get("JWT_EXPIRATION_MINUTES"));

    // Create initial claims map
    public Map<String, Object> createClaims(String email,
                                                   boolean authorised,  String enc_otp,
                                                   boolean otp_auth, String addnl_role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("authorised", authorised);
        claims.put("enc_otp", enc_otp);
        claims.put("otp_auth", otp_auth);
        claims.put("role", "FACULTY");
        claims.put("addnl_role", addnl_role);
        return claims;
    }

    // Update methods (modify the claims map)

    public void updateAddnlRole(Map<String, Object> claims, String addnl_role) {
        claims.put("addnl_role", addnl_role);
    }
    public void updateAuthorised(Map<String, Object> claims, boolean authorised) {
        claims.put("authorised", authorised);
    }
    public void updateEmail(Map<String, Object> claims, String email) {
        claims.put("email", email);
    }
    public void updateEncOtp(Map<String, Object> claims, String enc_otp) {
        claims.put("enc_otp", enc_otp);
    }
    public void updateOtpAuthStatus(Map<String, Object> claims, boolean otpauth) {
        claims.put("otp_auth", otpauth);
    }

    // Sign the JWT after all updates
    public String signJwt(Map<String, Object> claims) {
        long now = System.currentTimeMillis();
        Date expiration = new Date(now + (long) EXPIRATION_MINUTES * 60 * 1000);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(now))
                .setExpiration(expiration)
                .signWith(SignatureAlgorithm.HS256, HMAC_SECRET)
                .compact();
    }

    // Parse and validate JWT, return claims
    public Map<String, Object> parseJwt(String jwt) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(HMAC_SECRET)
                    .parseClaimsJws(jwt)
                    .getBody();
            return new HashMap<>(claims);
        } catch (ExpiredJwtException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Token expired");
            return error;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Invalid token");
            return error;
        }
    }
}