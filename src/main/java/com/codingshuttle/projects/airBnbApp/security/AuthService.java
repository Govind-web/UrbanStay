package com.codingshuttle.projects.airBnbApp.security;

import com.codingshuttle.projects.airBnbApp.dto.LoginDto;
import com.codingshuttle.projects.airBnbApp.dto.SignUpRequestDto;
import com.codingshuttle.projects.airBnbApp.dto.UserDto;
import com.codingshuttle.projects.airBnbApp.entity.User;
import com.codingshuttle.projects.airBnbApp.entity.enums.Role;
import com.codingshuttle.projects.airBnbApp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;

    @Transactional
    public UserDto signUp(SignUpRequestDto signUpRequestDto) {

        // 1. Check whether user already exists
        if (userRepository.findByEmail(signUpRequestDto.getEmail()).isPresent()) {
            throw new RuntimeException(
                    "User is already present with same email id"
            );
        }

        // 2. Create a new User entity
        User newUser = modelMapper.map(
                signUpRequestDto,
                User.class
        );

        // 3. Assign default role
        newUser.setRoles(Set.of(Role.GUEST));

        // 4. Encode password before saving
        newUser.setPassword(
                passwordEncoder.encode(
                        signUpRequestDto.getPassword()
                )
        );

        // 5. Save user
        User savedUser = userRepository.save(newUser);

        // 6. Convert entity -> DTO
        return modelMapper.map(savedUser, UserDto.class);
    }

    public String[] login(LoginDto loginDto) {

        // 1. Authenticate username/email + password
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                loginDto.getEmail(),
                                loginDto.getPassword()
                        )
                );

        // 2. Get authenticated User
        User user = (User) authentication.getPrincipal();

        // 3. Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // 4. Return both tokens
        return new String[]{
                accessToken,
                refreshToken
        };
    }
}