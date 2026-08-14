package com.rectificadora.gestion.auth;

import com.rectificadora.gestion.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
import java.time.*;
import java.util.*;

@RestController @RequestMapping("/api/auth")
public class AuthController {
  private final AuthenticationManager authenticationManager; private final JwtEncoder encoder; private final UserRepository users;
  public AuthController(AuthenticationManager a, JwtEncoder e, UserRepository u) { authenticationManager=a; encoder=e; users=u; }
  public record LoginRequest(@Email String email, @NotBlank String password) {}
  public record LoginResponse(String token, Instant expiresAt, UUID id, String name, String email, String role) {}
  @GetMapping("/csrf") public Map<String, String> csrf(CsrfToken token) { return Map.of("token", token.getToken()); }
  @PostMapping("/login") public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
    var user = users.findByEmailIgnoreCase(auth.getName()).orElseThrow(); var now = Instant.now(); var expiry = now.plus(Duration.ofHours(10));
    var claims = JwtClaimsSet.builder().issuer("gestion-ordenes").issuedAt(now).expiresAt(expiry).subject(user.email).claim("roles", List.of(user.role.name())).claim("name", user.name).build();
    String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(), claims)).getTokenValue();
    return new LoginResponse(token, expiry, user.id, user.name, user.email, user.role.name());
  }
}
