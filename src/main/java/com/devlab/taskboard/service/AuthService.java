package com.devlab.taskboard.service;

import com.devlab.taskboard.dto.request.LoginRequest;
import com.devlab.taskboard.dto.request.RegisterRequest;
import com.devlab.taskboard.dto.response.AuthResponse;
import com.devlab.taskboard.dto.response.UserResponse;
import com.devlab.taskboard.entity.User;
import com.devlab.taskboard.exception.BadRequestException;
import com.devlab.taskboard.exception.EmailAlreadyExistsException;
import com.devlab.taskboard.mapper.UserMapper;
import com.devlab.taskboard.repository.UserRepository;
import com.devlab.taskboard.util.EmailUtils;
import com.devlab.taskboard.security.JwtService;
import com.devlab.taskboard.security.UserPrincipal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
public class AuthService {

    private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = EmailUtils.normalize(request.email());

        if (request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES) {
            throw new BadRequestException("Password is too long");
        }

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = userMapper.toEntity(request, email, passwordHash); // role=USER, status=ACTIVE

        User saved;
        try {
            // flush: unique constraint xətası burada çıxsın (eyni anda iki eyni email ilə qeydiyyat)
            saved = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyExistsException();
        }

        return userMapper.toResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        String email = EmailUtils.normalize(request.email());

        // Səhv parol, mövcud olmayan email və ya BLOCKED user → AuthenticationException.
        // GlobalExceptionHandler bunu 401 "Invalid email or password" kimi qaytarır.
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, request.password())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String accessToken = jwtService.generateToken(principal);

        return AuthResponse.bearer(accessToken, jwtService.getExpirationSeconds());
    }
}