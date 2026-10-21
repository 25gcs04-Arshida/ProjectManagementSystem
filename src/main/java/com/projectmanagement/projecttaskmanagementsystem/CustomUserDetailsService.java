package com.projectmanagement.projecttaskmanagementsystem;

import com.projectmanagement.projecttaskmanagementsystem.repository.AppUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;

    public CustomUserDetailsService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        System.out.println("LOGIN ATTEMPT: username = " + username);

        AppUser user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> {
                    System.out.println("LOGIN FAILED: username not found.");
                    return new UsernameNotFoundException(
                            "User not found: " + username);
                });

        System.out.println("LOGIN USER FOUND: " + user.getUsername());
        System.out.println("LOGIN ROLE: " + user.getRole());

        return user;
    }
}