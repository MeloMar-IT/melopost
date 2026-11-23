package com.melomarit.melopost.service;

import com.melomarit.melopost.model.User;
import com.melomarit.melopost.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    /**
     * Saves a user. The password in the entity is ignored: a non-blank {@code newPassword} is always
     * bcrypt-encoded (even if it looks like a hash); a blank one keeps the stored hash (null for a new user).
     */
    public User save(User user, String newPassword) {
        if (newPassword != null && !newPassword.isEmpty()) {
            user.setPassword(passwordEncoder.encode(newPassword));
        } else {
            String stored = null;
            if (user.getUuid() != null) {
                stored = userRepository.findById(user.getUuid()).map(User::getPassword).orElse(null);
            }
            user.setPassword(stored);
        }
        return userRepository.save(user);
    }

    public void deleteById(UUID id) {
        userRepository.deleteById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
