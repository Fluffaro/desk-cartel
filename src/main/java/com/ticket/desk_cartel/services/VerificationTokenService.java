package com.ticket.desk_cartel.services;

import com.ticket.desk_cartel.entities.User;
import com.ticket.desk_cartel.entities.VerificationToken;
import com.ticket.desk_cartel.repositories.VerificationTokenRepository;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class VerificationTokenService {

    private final VerificationTokenRepository tokenRepository;
    private final JavaMailSender mailSender;

    public VerificationTokenService(VerificationTokenRepository tokenRepository, JavaMailSender mailSender) {
        this.tokenRepository = tokenRepository;
        this.mailSender = mailSender;
    }

    /**
     * Generates a verification token, stores it, and sends an email.
     */
    public void sendVerificationEmail(User user) {
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiryDate(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)); // 24-hour expiry

        tokenRepository.save(verificationToken);

        String verificationLink = "http://localhost:8080/auth/verify?token=" + token;
        sendEmail(user.getEmail(), "Verify your email", "Click here to verify your account: " + verificationLink);
    }

    /**
     * Validates the verification token and marks the user as verified.
     */
    @Transactional
    public Optional<User> verifyToken(String token) {
        Optional<VerificationToken> verificationToken = tokenRepository.findByToken(token);
        if (verificationToken.isPresent() && verificationToken.get().getExpiryDate().after(new Date())) {
            User user = verificationToken.get().getUser();
            tokenRepository.deleteByToken(token); // Remove used token
            return Optional.of(user);
        }
        return Optional.empty();
    }

    /**
     * Sends an email with a given subject and message.
     */
    public void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);

            System.out.println("✅ Email sent successfully to: " + to);
        } catch (MailException e) {
            System.err.println("❌ Email sending failed: " + e.getMessage());
            e.printStackTrace(); // Print full error stack trace
        }
    }
}