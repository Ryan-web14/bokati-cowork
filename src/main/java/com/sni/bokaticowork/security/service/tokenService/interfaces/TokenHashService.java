package com.sni.bokaticowork.security.service.tokenService.interfaces;

public interface TokenHashService {

    String hash(String rawToken);
    boolean matches(String rawToken, String storedHash);
}
