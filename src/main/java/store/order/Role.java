package store.order;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum Role {
    USER,
    ADMIN;

    public static Role parse(String value) {
        if (value == null || value.isBlank()) {
            return USER;
        }
        try {
            return Role.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid role: " + value);
        }
    }

    public void requireAdmin() {
        if (this != ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }
    }
}
