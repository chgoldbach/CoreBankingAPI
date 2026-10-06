package com.banking.core.config;

import com.banking.core.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Her HTTP isteğinde bir kez çalışan JWT doğrulama filtresi.
 *
 * İşlem akışı:
 * 1. Authorization header'dan Bearer token çıkartılır.
 * 2. Token'dan username (email) çıkartılır.
 * 3. Kullanıcı veritabanından yüklenir.
 * 4. Token geçerliyse SecurityContext güncellenir → kullanıcı authenticate edilmiş sayılır.
 * 5. FilterChain devam eder → Controller'a ulaşır.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);


    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Authorization header yoksa veya "Bearer " ile başlamıyorsa filtreden geç
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7); // "Bearer " kısmını çıkar

        try {
            final String userEmail = jwtService.extractUsername(jwt);

            // Kullanıcı henüz authenticate edilmemişse kontrol et
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userRepository.findByEmail(userEmail)
                        .orElse(null);

                if (userDetails != null && jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("JWT doğrulandı: {}", userEmail);
                }
            }
        } catch (Exception e) {
            log.debug("JWT işlem hatası: {}", e.getMessage());
            // Token geçersizse SecurityContext temizle ve isteği reddet (401 Spring Security tarafından döner)
        }

        filterChain.doFilter(request, response);
    }
}
