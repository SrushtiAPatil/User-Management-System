
package com.example.usermanagementsystem.service;

import com.example.usermanagementsystem.entity.User;
import com.example.usermanagementsystem.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> findAll() {
        return this.userRepository.findAll(Sort.by(new String[]{"id"}));
    }

    public Optional<User> findById(Long id) {
        return this.userRepository.findById(id);
    }

    public User create(User user) {
        user.setId((Long)null);
        this.clean(user);
        return (User)this.userRepository.save(user);
    }

    public User update(Long id, User form) {
        User existing = (User)this.userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        this.clean(form);
        existing.setName(form.getName());
        existing.setEmail(form.getEmail());
        existing.setPhone(form.getPhone());
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            existing.setPassword(form.getPassword());
        }

        return (User)this.userRepository.save(existing);
    }

    public void delete(Long id) {
        this.userRepository.deleteById(id);
    }

    public boolean emailTaken(String email, Long excludeId) {
        if (email == null) {
            return false;
        } else {
            String cleanedEmail = email.trim();
            return excludeId == null ? this.userRepository.existsByEmailIgnoreCase(cleanedEmail) : this.userRepository.existsByEmailIgnoreCaseAndIdNot(cleanedEmail, excludeId);
        }
    }

    public Optional<User> authenticate(String email, String password) {
        return email != null && password != null ? this.userRepository.findByEmailIgnoreCase(email.trim()).filter((user) -> user.getPassword().equals(password)) : Optional.empty();
    }

    private void clean(User user) {
        if (user.getName() != null) {
            user.setName(user.getName().trim());
        }

        if (user.getEmail() != null) {
            user.setEmail(user.getEmail().trim().toLowerCase());
        }

        if (user.getPhone() != null) {
            user.setPhone(user.getPhone().trim());
        }

    }
}
