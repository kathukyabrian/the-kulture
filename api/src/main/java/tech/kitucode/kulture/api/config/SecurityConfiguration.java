package tech.kitucode.kulture.api.config;

import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.csrf(csrf -> csrf.disable())
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register", "/api/auth/password/setup", "/api/auth/password/forgot", "/api/auth/password/reset").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/vehicles/arrival-estimates", "/api/vehicles/{vehicleId}/arrival-estimate").permitAll()
				.requestMatchers("/api/routes/**", "/api/vehicles/**", "/api/media/**").permitAll()
				.requestMatchers("/api/admin/**").hasRole("ADMIN")
				.requestMatchers("/api/crew/**").hasRole("CREW")
				.requestMatchers("/api/traveller/**").hasRole("TRAVELLER")
				.anyRequest().authenticated())
			.exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) -> response.sendError(401)))
			.build();
	}

	@Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(List.of(
			"https://thekulture.co.ke",
			"https://www.thekulture.co.ke",
			"http://localhost:4200",
			"http://127.0.0.1:4200",
			"http://localhost",
			"https://localhost",
			"capacitor://localhost"
		));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		return source;
	}
}
