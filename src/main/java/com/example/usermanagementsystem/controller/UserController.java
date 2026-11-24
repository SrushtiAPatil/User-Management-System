package com.example.usermanagementsystem.controller;

import com.example.usermanagementsystem.entity.User;
import com.example.usermanagementsystem.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {

    private static final String SESSION_USER_ID = "userId";

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody @Valid User user) {
        if (user.getPassword() == null || user.getPassword().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Password must be at least 6 characters"));
        }

        if (userService.emailTaken(user.getEmail(), null)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Email already exists"));
        }

        User created = userService.create(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Registration successful", "user", toResponse(created)));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        Optional<User> authenticated =
                userService.authenticate(loginRequest.email(), loginRequest.password());

        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password"));
        }

        HttpSession session = request.getSession(true);
        request.changeSessionId();
        User user = authenticated.get();
        session.setAttribute(SESSION_USER_ID, user.getId());

        return ResponseEntity.ok(Map.of("message", "Login successful", "user", toResponse(user)));
    }

    // FIX: replaced map/orElseGet lambda with if/else to avoid the generics error
    @GetMapping("/auth/me")
    public ResponseEntity<?> currentUser(HttpSession session) {
        Optional<User> user = getLoggedInUser(session);

        if (user.isEmpty()) {
            return unauthorized();
        }

        return ResponseEntity.ok(toResponse(user.get()));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(HttpSession session) {
        if (getLoggedInUser(session).isEmpty()) {
            return unauthorized();
        }

        List<UserResponse> users = userService.findAll().stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(users);
    }

    // FIX: replaced map/orElseGet lambda with if/else to avoid the generics error
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id, HttpSession session) {
        if (getLoggedInUser(session).isEmpty()) {
            return unauthorized();
        }

        Optional<User> user = userService.findById(id);

        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        }

        return ResponseEntity.ok(toResponse(user.get()));
    }

    @PostMapping("/users")
    public ResponseEntity<?> addUser(@RequestBody @Valid User user, HttpSession session) {
        if (getLoggedInUser(session).isEmpty()) {
            return unauthorized();
        }

        if (user.getPassword() == null || user.getPassword().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Password must be at least 6 characters"));
        }

        if (userService.emailTaken(user.getEmail(), null)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Email already exists"));
        }

        User created = userService.create(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "User added successfully", "user", toResponse(created)));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestBody @Valid User form,
            HttpSession session) {

        if (getLoggedInUser(session).isEmpty()) {
            return unauthorized();
        }

        if (userService.findById(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        }

        if (userService.emailTaken(form.getEmail(), id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Email already exists"));
        }

        if (form.getPassword() != null && !form.getPassword().isBlank()
                && form.getPassword().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Password must be at least 6 characters"));
        }

        User updated = userService.update(id, form);
        return ResponseEntity.ok(Map.of("message", "User updated successfully", "user", toResponse(updated)));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpSession session) {
        Optional<User> loggedInUser = getLoggedInUser(session);

        if (loggedInUser.isEmpty()) {
            return unauthorized();
        }

        if (id.equals(loggedInUser.get().getId())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "You cannot delete your own account while logged in"));
        }

        if (userService.findById(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        }

        userService.delete(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    private Optional<User> getLoggedInUser(HttpSession session) {
        Object id = session.getAttribute(SESSION_USER_ID);

        if (id instanceof Long userId) {
            Optional<User> user = userService.findById(userId);
            if (user.isEmpty()) {
                session.removeAttribute(SESSION_USER_ID);
            }
            return user;
        }

        return Optional.empty();
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Please log in to continue"));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity.badRequest()
                .body(Map.of("message", "Please check the entered details", "errors", errors));
    }

    public record LoginRequest(String email, String password) {
    }

    public record UserResponse(Long id, String name, String email, String phone) {
    }
}
