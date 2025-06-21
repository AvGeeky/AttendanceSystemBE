package com.appbuildersinc.attendance.source.Utilities.JWTUtils;

import io.jsonwebtoken.*;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
@Service
public class SuperAdminjwtUtil {
// SuperAdminjwtUtil is a utility class for creating, signing, parsing, and validating JSON Web Tokens (JWTs) using HMAC SHA-256 algorithm
    private final String HMAC_SECRET = System.getenv("JWT_HMAC_SECRET");
    private final int EXPIRATION_MINUTES =  Integer.parseInt(System.getenv("JWT_EXPIRATION_MINUTES"));
    public Map<String,Object> createClaims(String email,String dept,Boolean authorised){
        Map<String,Object> claims =new HashMap<>();
        claims.put("email",email);
        claims.put("dept",dept);
        claims.put("authorised",authorised);
        claims.put("role","ADMIN");
        return claims;
    }
    public void updateAuthorised(Map<String,Object> claims,Boolean authorised){
        claims.put("authorised",authorised);
    }
    public void updateEmail(Map<String,Object> claims, String email){
        claims.put("email",email);
    }
    public String signJwt(Map<String,Object> claims){
        long now = System.currentTimeMillis();
        Date expiration = new Date(now + (long) EXPIRATION_MINUTES * 60 * 1000);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(now))
                .setExpiration(expiration)
                .signWith(SignatureAlgorithm.HS256, HMAC_SECRET)
                .compact();
    }
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
