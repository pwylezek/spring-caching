package de.nubisoft.backend.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    public String getLoggedDoctorId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
