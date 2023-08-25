package com.hh.easypanspringboot.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.hh.easypanspringboot.entity.constants.Constants;

import java.util.Calendar;
import java.util.HashMap;

public class JWTUtils {

    public static String createToken(String userId, String sign) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 1);
        return JWT.create().
                withHeader(new HashMap<>()).
                withClaim("userId", userId).
                withExpiresAt(calendar.getTime()).
                sign(Algorithm.HMAC256(sign));
    }

    public static String resolveToken(String token) {
        JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(Constants.JWT_SIGN)).build();
        DecodedJWT decodedJWT = jwtVerifier.verify(token);
        Claim userId = decodedJWT.getClaim("userId");
        return userId.asString();
    }
}
