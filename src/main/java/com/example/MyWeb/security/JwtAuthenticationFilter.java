package com.example.MyWeb.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = parseJwt(request);

        if (token != null) {
            boolean isValid = jwtService.validateToken(token);
            System.out.println("JwtAuthenticationFilter: Token found. Valid? " + isValid);

            if (isValid) {
                String email = jwtService.getSubjectFromToken(token);
                System.out.println("JwtAuthenticationFilter: Email from token: " + email);

                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                System.out.println("JwtAuthenticationFilter: User loaded: " + userDetails.getUsername()
                        + ", Authorities: " + userDetails.getAuthorities());

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
                System.out.println("JwtAuthenticationFilter: Authentication set in SecurityContext");
            } else {
                System.out.println("JwtAuthenticationFilter: Token is invalid");
            }
        } else {
            System.out.println("JwtAuthenticationFilter: No token found in request to " + request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
