package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.CreateUserDTO;
import com.nakamahub.backend.dtos.LoginResponseDTO;
import com.nakamahub.backend.dtos.SignupResponseDTO;
import com.nakamahub.backend.models.User;
import com.nakamahub.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Override
    public SignupResponseDTO registerUser(CreateUserDTO createUserDTO) {
        if (userRepository.existsByEmail(createUserDTO.getEmail())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El email ya esta registrado");
        }
        if (userRepository.existsByUsername(createUserDTO.getUsername())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de usuario ya esta registrado");
        }

        User newUser = new User();

        newUser.setEmail(createUserDTO.getEmail());
        newUser.setUsername(createUserDTO.getUsername());
        newUser.setPassword(passwordEncoder.encode(createUserDTO.getPassword()));

        return SignupResponseDTO.builder()
                .id(newUser.getId())
                .email(newUser.getEmail())
                .username(newUser.getUsername())
                .build();
    }

    @Override
    public LoginResponseDTO authenticateUser(LoginResponseDTO loginResponseDTO) {
        return null;
    }

}
