package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.CreateUserDTO;
import com.nakamahub.backend.dtos.LoginResponseDTO;
import com.nakamahub.backend.dtos.SingupResponseDTO;
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
    public SingupResponseDTO registerUser(CreateUserDTO createUserDTO) {
        return null;
    }

    @Override
    public LoginResponseDTO authenticateUser(LoginResponseDTO loginResponseDTO) {
        return null;
    }

    @Override
    public boolean isEmail(String identifier) {
        return identifier.contains("@");
    }

}
