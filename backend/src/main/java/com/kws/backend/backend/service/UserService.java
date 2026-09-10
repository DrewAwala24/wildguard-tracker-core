package com.kws.backend.backend.service;

import com.kws.backend.backend.model.UserAccount;
import com.kws.backend.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserAccount registerUser(UserAccount user) {
        return userRepository.save(user);
    }
}