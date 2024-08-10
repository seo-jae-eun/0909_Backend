package org.example.fourtreesproject.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fourtreesproject.jwt.JwtUtil;
import org.example.fourtreesproject.jwt.model.entity.RefreshToken;
import org.example.fourtreesproject.jwt.Repository.RefreshTokenRepository;
import org.example.fourtreesproject.user.model.dto.CustomUserDetails;
import org.example.fourtreesproject.user.model.entity.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Cookie authorization = null;
        Cookie rTokenCookie = null;
        Cookie[] cookieArray = request.getCookies();
        if (cookieArray != null) {
            for (Cookie cookie : cookieArray) {
                if (cookie.getName().equals("AToken")) {
                    authorization =  cookie;
                } else if (cookie.getName().equals("RToken")){
                    rTokenCookie = cookie;
                }
            }
        }

        if (authorization == null) {
            log.info("AccessToken 없음");
            filterChain.doFilter(request, response);
            return;
        }
        String accessToken = authorization.split(" ")[1];
        if (jwtUtil.isExpired(accessToken)) {
            log.info("AccessToken 만료");

            if (rTokenCookie == null) { // client가 refresh token을 안가지고 있을 때
                filterChain.doFilter(request, response);
                log.info("RefreshToken 없음");
                return;
            }
            String reissuedAccessToken = reissueToken(cookie);
            if (reissuedAccessToken == null) { // client의 refresh token이 변조되었거나, 만료되었거나, 서버가 가지고있는 refreshtoken과 다르거나
                filterChain.doFilter(request, response);
                log.info("RefreshToken 이상");
                return;
            }
            accessToken = reissuedAccessToken;
            response.addHeader("Authorization", "Bearer " + accessToken);
        }
        String email = jwtUtil.getEmail(accessToken);
        String role = jwtUtil.getRole(accessToken);
        Long idx = jwtUtil.getId(accessToken);
        User user = User.builder()
                .idx(idx)
                .email(email)
                .role(role)
                .emailStatus(true)
                .build();
        CustomUserDetails customUserDetails = new CustomUserDetails(user);
        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);
        filterChain.doFilter(request, response);
    }

    private Cookie findRefreshTokenAtCookies(Cookie[] cookieArray) {
        for (Cookie cookie : cookieArray) {
            if (cookie.getName().equals("refresh_token")) {
                return cookie;
            }
        }
        return null;
    }

    private String reissueToken(Cookie cookie) {
        String cookieRefreshToken = cookie.getValue();
        if (jwtUtil.isExpired(cookieRefreshToken)) {
            log.info("RefreshToken 만료");
            return null;
        }

        String email = jwtUtil.getEmail(cookieRefreshToken);
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByEmail(email).orElse(null);
        if (refreshTokenEntity != null) {
            String refreshToken = refreshTokenEntity.getRefreshToken();
            if (jwtUtil.isValid(refreshToken) && refreshToken.equals(cookieRefreshToken)){
                return jwtUtil.createAccessToken(jwtUtil.getId(cookieRefreshToken), jwtUtil.getEmail(cookieRefreshToken), jwtUtil.getRole(cookieRefreshToken));
            }
        }
        return null;
    }
}
