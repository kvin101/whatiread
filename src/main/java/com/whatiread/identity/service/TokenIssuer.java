package com.whatiread.identity.service;

import com.whatiread.identity.api.AuthResponse;
import com.whatiread.identity.domain.RefreshToken;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.repository.RefreshTokenRepository;
import com.whatiread.identity.security.JwtService;
import com.whatiread.identity.security.TokenHasher;
import org.springframework.stereotype.Component;

@Component
public class TokenIssuer {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public TokenIssuer(
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            UserMapper userMapper
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    public AuthResponse issueTokens(User user) {
        String accessToken = jwtService.createAccessToken(user.getId(), user.getEmail(), user.getTokenVersion());
        String refreshToken = jwtService.createRefreshTokenValue();
        refreshTokenRepository.save(new RefreshToken(
                user,
                TokenHasher.hash(refreshToken),
                jwtService.refreshTokenExpiry()
        ));
        return new AuthResponse(accessToken, refreshToken, userMapper.toUserResponse(user));
    }
}
