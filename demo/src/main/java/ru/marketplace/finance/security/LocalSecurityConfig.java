package ru.marketplace.finance.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("local")
public class LocalSecurityConfig {

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	SecurityFilterChain localSecurityFilterChain(HttpSecurity http) throws Exception {
		return http
				.securityMatcher("/api/**", "/actuator/health")
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/api/**", "/actuator/health").permitAll()
						.anyRequest().permitAll())
				.httpBasic(Customizer.withDefaults())
				.formLogin(Customizer.withDefaults())
				.build();
	}
}
