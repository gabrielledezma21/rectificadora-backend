package com.rectificadora.gestion.auth;

import com.rectificadora.gestion.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
  private final UserRepository users;

  public UserDetailsServiceImpl(UserRepository users) {
    this.users = users;
  }

  @Override
  public UserDetails loadUserByUsername(String email) {
    var user = users.findByEmailIgnoreCase(email)
        .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    return org.springframework.security.core.userdetails.User.withUsername(user.email).password(user.passwordHash)
        .roles(user.role.name()).disabled(!user.active).build();
  }
}
