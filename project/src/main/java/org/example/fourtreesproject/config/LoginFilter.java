package org.example.fourtreesproject.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.fourtreesproject.common.BaseResponse;
import org.example.fourtreesproject.jwt.JwtUtil;
import org.example.fourtreesproject.jwt.Repository.RefreshTokenRepository;
import org.example.fourtreesproject.jwt.model.entity.RefreshToken;
import org.example.fourtreesproject.user.model.dto.CustomUserDetails;
import org.example.fourtreesproject.user.model.request.UserLoginRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

@RequiredArgsConstructor
public class LoginFilter extends UsernamePasswordAuthenticationFilter {
    private final int COOKIE_MAX_AGE = 60 * 60 * 24 * 30;

    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        UserLoginRequest userLoginRequest;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ServletInputStream inputStream = request.getInputStream();
            String messageBody = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            userLoginRequest = objectMapper.readValue(messageBody, UserLoginRequest.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String email = userLoginRequest.getEmail();
        String password = userLoginRequest.getPassword();


        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, password, null);
        return authenticationManager.authenticate(authToken);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException{
        CustomUserDetails userDetails = (CustomUserDetails) authResult.getPrincipal();
        Collection<? extends GrantedAuthority> authorities = authResult.getAuthorities();
        GrantedAuthority auth = authorities.iterator().next();
        String role = auth.getAuthority();
        String email = userDetails.getUsername();
        Long idx = userDetails.getIdx();

        String accessToken = jwtUtil.createAccessToken(idx,email, role);
        String refreshToken = jwtUtil.createRefreshToken(idx, email, role);
        RefreshToken existingRefreshToken = refreshTokenRepository.findByEmail(email).orElse(null);
        RefreshToken refreshTokenEntity;
        if (existingRefreshToken != null) {
            refreshTokenEntity = RefreshToken.builder().idx(existingRefreshToken.getIdx()).refreshToken(refreshToken).email(email).build();
        } else{
            refreshTokenEntity = RefreshToken.builder().email(email).refreshToken(refreshToken).build();
        }
        refreshTokenRepository.save(refreshTokenEntity);

        response.addHeader("Authorization", "Bearer " + accessToken);

        Cookie aToken = new Cookie("refresh_token", refreshToken);
        aToken.setPath("/");
        aToken.setHttpOnly(true);
        aToken.setMaxAge(COOKIE_MAX_AGE);
        response.addCookie(aToken);

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();
        BaseResponse<String> baseResponse = new BaseResponse<>(userDetails.getUser().getRole());
        out.print(baseResponse);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        super.unsuccessfulAuthentication(request, response, failed);
    }
}
