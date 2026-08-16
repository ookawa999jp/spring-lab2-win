package com.example.springlab.auth;

import com.example.springlab.security.JwtProperties;

import java.time.Instant;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public AuthService(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
    }

    public String login(String userId, String password) {
        if (!DemoUser.USER_ID.equals(userId) || !DemoUser.PASSWORD.equals(password)) {
            throw new IllegalArgumentException("ユーザーIDまたはパスワードが不正です");
        }

        Instant now = Instant.now();

        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(jwtProperties.issuer())
                        .issuedAt(now)
                        .expiresAt(now.plusSeconds(jwtProperties.expiresInSeconds()))
                        .subject(userId)
                        .claim("role", "USER")
                        .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
