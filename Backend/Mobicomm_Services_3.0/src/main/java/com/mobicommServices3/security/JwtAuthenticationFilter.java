package com.mobicommServices3.security;

import jakarta.servlet.FilterChain;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mobicommServices3.repository.SubscriberRepository;
import com.mobicommServices3.model.Subscriber;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private SubscriberRepository subscriberRepository;
    
    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String servletPath = request.getServletPath();
        logger.debug("JwtAuthenticationFilter - shouldNotFilter for: " + servletPath);
        // Bypass only unauthenticated endpoints
        return servletPath.startsWith("/api/auth/") || 
               servletPath.equals("/api/plans") || 
               servletPath.equals("/api/subscriber/validate-number") ||
               servletPath.equals("/api/subscriber/check") ||
               servletPath.startsWith("/api/payment/") ||
               servletPath.startsWith("/api/guest/") ||
               servletPath.startsWith("/api/payment/transaction/");
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;
        String role = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            if (jwtUtils.validateToken(token)) {
                username = jwtUtils.getUsernameFromToken(token); // Should be username for admins, mobileNumber for subscribers
                role = jwtUtils.getRoleFromToken(token); // Now returns "ADMIN" or "SUBSCRIBER"
                logger.debug("Extracted username: {}, role: {}", username, role);
            } else {
                logger.warn("Invalid JWT token for request: " + request.getRequestURI());
            }
        }

        if (username != null && role != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails;
            if ("SUBSCRIBER".equals(role)) {
                Subscriber subscriber = subscriberRepository.findByMobileNumber(username)
                    .orElse(null);
                if (subscriber != null && subscriber.getIsActive()) {
                    userDetails = User.builder()
                        .username(username)
                        .password("")
                        .authorities(new SimpleGrantedAuthority("ROLE_SUBSCRIBER"))
                        .accountExpired(false)
                        .accountLocked(false)
                        .credentialsExpired(false)
                        .disabled(false)
                        .build();
                } else {
                    logger.warn("Subscriber not found or inactive: {}", username);
                    filterChain.doFilter(request, response);
                    return;
                }
            } else {
                // Handle ADMIN role
                try {
                    userDetails = customUserDetailsService.loadUserByUsername(username); // Use username for admins
                } catch (UsernameNotFoundException e) {
                    logger.warn("User not found for username: {}", username);
                    filterChain.doFilter(request, response);
                    return;
                }
            }
            
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
            logger.debug("Set authentication for: {} with role: {}", username, role);
        }

        filterChain.doFilter(request, response);
    }
}