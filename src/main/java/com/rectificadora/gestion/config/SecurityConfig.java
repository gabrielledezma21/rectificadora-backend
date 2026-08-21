package com.rectificadora.gestion.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration c) throws Exception {
    return c.getAuthenticationManager();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(@Value("${app.cors-origins}") List<String> origins) {
    var config = new CorsConfiguration();
    config.setAllowedOrigins(origins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    source.registerCorsConfiguration("/v3/api-docs/**", config);
    return source;
  }

  @Bean
  JwtDecoder jwtDecoder(@Value("${app.jwt-secret}") String secret) {
    return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
        .build();
  }

  @Bean
  JwtEncoder jwtEncoder(@Value("${app.jwt-secret}") String secret) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(secret.getBytes(StandardCharsets.UTF_8)));
  }

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http.cors(cors -> {
    }).csrf(csrf -> csrf
        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        .ignoringRequestMatchers("/api/auth/login"))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a
            .requestMatchers("/api/auth/login", "/api/auth/csrf", "/api/health", "/v3/api-docs/**",
                "/swagger-ui/**", "/swagger-ui.html")
            .permitAll()
            .anyRequest().authenticated())
        .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwt -> {
          var roles = jwt.getClaimAsStringList("roles");
          var permissions = jwt.getClaimAsStringList("permissions");
          var auths = new java.util.ArrayList<org.springframework.security.core.GrantedAuthority>();
          if (roles != null)
            roles.forEach(r -> auths.add(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + r)));
          if (permissions != null)
            permissions.forEach(p -> auths.add(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("PERM_" + p)));
          return new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt,
              auths, jwt.getSubject());
        }))).build();
  }
}
