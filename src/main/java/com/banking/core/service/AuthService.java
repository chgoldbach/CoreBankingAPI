package com.banking.core.service;

import com.banking.core.config.JwtService;
import com.banking.core.dto.request.LoginRequest;
import com.banking.core.dto.request.RefreshTokenRequest;
import com.banking.core.dto.request.RegisterRequest;
import com.banking.core.dto.response.AuthResponse;
import com.banking.core.dto.response.UserResponse;
import com.banking.core.entity.Role;
import com.banking.core.entity.User;
import com.banking.core.exception.EmailAlreadyExistsException;
import com.banking.core.exception.ResourceNotFoundException;
import com.banking.core.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kullanıcı kimlik doğrulama ve kayıt işlemleri.
 */
@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);


    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }


    @Value("${jwt.access-token-expiry}")
    private long accessTokenExpiry;

    /**
     * Yeni kullanıcı kaydeder ve token döner.
     *
     * @param request Kayıt bilgileri (ad, soyad, email, şifre)
     * @return Access + Refresh token çifti
     * @throws EmailAlreadyExistsException E-posta zaten kayıtlıysa
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // E-posta unique kontrolü
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        // Kullanıcı oluştur
        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .active(true)
                .build();

        user = userRepository.save(user);
        log.info("Yeni kullanıcı kayıt edildi: {}", user.getEmail());

        // Token oluştur ve döndür
        return buildAuthResponse(user);
    }

    /**
     * Kullanıcı girişi yapar ve token döner.
     *
     * @param request Email + şifre
     * @return Access + Refresh token çifti
     * @throws org.springframework.security.authentication.BadCredentialsException Hatalı kimlik bilgisi
     */
    public AuthResponse login(LoginRequest request) {
        // Spring Security AuthenticationManager kimlik doğrulama yapar
        // Başarısızsa BadCredentialsException fırlatır
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException(request.email()));

        log.info("Kullanıcı giriş yaptı: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    /**
     * Refresh token kullanarak yeni access token döner.
     *
     * @param request Mevcut refresh token
     * @return Yeni access token (refresh token aynı kalır)
     */
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        // Token'ın refresh token olduğunu doğrula
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Geçersiz refresh token");
        }

        String email = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new IllegalArgumentException("Refresh token geçersiz veya süresi dolmuş");
        }

        // Yeni access token, aynı refresh token
        String newAccessToken = jwtService.generateAccessToken(user);

        log.info("Access token yenilendi: {}", email);

        return new AuthResponse(
                newAccessToken,
                refreshToken,
                "Bearer",
                accessTokenExpiry / 1000,
                toUserResponse(user)
        );
    }

    // ─── Yardımcı metodlar ────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return AuthResponse.of(accessToken, refreshToken, accessTokenExpiry / 1000, toUserResponse(user));
    }

    public static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}
