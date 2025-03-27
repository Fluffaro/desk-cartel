package com.ticket.desk_cartel.services;


import com.ticket.desk_cartel.entities.User;
import com.ticket.desk_cartel.entities.VerificationToken;
import com.ticket.desk_cartel.repositories.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;

public class VerificationTokenServiceTest {

    @Mock
    private VerificationTokenRepository tokenRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private VerificationTokenService verificationTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
    }
    @Test
    void testSendVerificationEmail() {
        // Arrange
        when(tokenRepository.save(any(VerificationToken.class))).thenReturn(null);

        // Act
        verificationTokenService.sendVerificationEmail(user);

        // Assert
        // Verify that the save method was called once
        verify(tokenRepository, times(1)).save(any(VerificationToken.class));

        // Verify that the email is sent with the expected parameters
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testVerifyToken_Success() {
        // Arrange
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiryDate(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)); // 24 hours expiry

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(verificationToken));

        // Act
        Optional<User> result = verificationTokenService.verifyToken(token);

        // Assert
        verify(tokenRepository, times(1)).findByToken(token);
        verify(tokenRepository, times(1)).deleteByToken(token);
        assert result.isPresent();
        assert result.get().equals(user);
    }

    @Test
    void testVerifyToken_Expired() {
        // Arrange
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiryDate(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)); // Expired token

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(verificationToken));

        // Act
        Optional<User> result = verificationTokenService.verifyToken(token);

        // Assert
        verify(tokenRepository, times(1)).findByToken(token);
        assert result.isEmpty();
    }

    @Test
    void testVerifyToken_TokenNotFound() {
        // Arrange
        String token = UUID.randomUUID().toString();

        when(tokenRepository.findByToken(token)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = verificationTokenService.verifyToken(token);

        // Assert
        verify(tokenRepository, times(1)).findByToken(token);
        assert result.isEmpty();
    }

    @Test
    void testSendEmail() {
        // Arrange
        String to = "test@example.com";
        String subject = "Test Subject";
        String text = "Test Message";

        // Act
        verificationTokenService.sendEmail(to, subject, text);

        // Assert
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}