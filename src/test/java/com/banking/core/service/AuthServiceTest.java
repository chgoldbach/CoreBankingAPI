package com.banking.core.service;

import com.banking.core.dto.request.RegisterRequest;
import com.banking.core.dto.response.AuthResponse;
import com.banking.core.entity.Role;
import com.banking.core.entity.User;
import com.banking.core.exception.EmailAlreadyExistsException;
import com.banking.core.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.banking.core.config.JwtService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService için unit testler.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Kayıt — yeni kullanıcı başarıyla kaydedilir")
    void register_newUser_returnsAuthResponse() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "Ali", "Yılmaz", "ali@bank.com", "Password1!"
        );

        when(userRepository.existsByEmail("ali@bank.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("hashedPassword");

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .email("ali@bank.com")
                .firstName("Ali")
                .lastName("Yılmaz")
                .passwordHash("hashedPassword")
                .role(Role.USER)
                .active(true)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateAccessToken(any())).thenReturn("accessToken123");
        when(jwtService.generateRefreshToken(any())).thenReturn("refreshToken456");

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("accessToken123");
        assertThat(response.refreshToken()).isEqualTo("refreshToken456");
        assertThat(response.user().email()).isEqualTo("ali@bank.com");

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Kayıt — duplicate e-posta ile EmailAlreadyExistsException fırlatılır")
    void register_duplicateEmail_throwsException() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "Ali", "Yılmaz", "ali@bank.com", "Password1!"
        );
        when(userRepository.existsByEmail("ali@bank.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("ali@bank.com");

        verify(userRepository, never()).save(any());
    }
}
