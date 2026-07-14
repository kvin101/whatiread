package com.whatiread.identity.security;

import com.whatiread.identity.domain.User;
import com.whatiread.identity.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final AuthenticatedUserFactory authenticatedUserFactory;

    public UserDetailsServiceImpl(
            UserRepository userRepository,
            AuthenticatedUserFactory authenticatedUserFactory
    ) {
        this.userRepository = userRepository;
        this.authenticatedUserFactory = authenticatedUserFactory;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByEmailIgnoreCase(username)
                .or(() -> userRepository.findByUsernameIgnoreCase(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return authenticatedUserFactory.create(user);
    }
}
