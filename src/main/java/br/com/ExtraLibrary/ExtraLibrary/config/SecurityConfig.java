package br.com.ExtraLibrary.ExtraLibrary.config;

import br.com.ExtraLibrary.ExtraLibrary.config.JwtAuthenticationFilter;
import br.com.ExtraLibrary.ExtraLibrary.services.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          CustomUserDetailsService userDetailsService,
                          PasswordEncoder passwordEncoder,
                          CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(auth -> auth
                        // 🟢 ENDPOINTS PÚBLICOS - SISTEMA
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/error").permitAll()

                        // 🟢 ENDPOINTS PÚBLICOS - AUTENTICAÇÃO
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()

                        // 🟢 ENDPOINTS PÚBLICOS - DOCUMENTAÇÃO
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/swagger-resources/**", "/webjars/**").permitAll()
                        .requestMatchers("/favicon.ico").permitAll()

                        // 🟢 ENDPOINTS PÚBLICOS - CONSULTA DE LIVROS
                        .requestMatchers(HttpMethod.GET, "/api/v1/book").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/book/search/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/book/{id}").permitAll()

                        // 🟢 ENDPOINTS PÚBLICOS - CONSULTA DE AUTORES  
                        .requestMatchers(HttpMethod.GET, "/api/v1/author").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/author/name/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/author/nationality/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/author/{id}").permitAll()

                        // 🔴 ENDPOINTS CRÍTICOS - APENAS ADMIN
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/reports/**").hasRole("ADMIN")

                        // 🟡 ENDPOINTS DE GESTÃO - APENAS ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/v1/customer/*/activate").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/customer/*/block").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/customer/*/disable").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/customer/*/status").hasRole("ADMIN")

                        // 🔵 VENDAS - USUÁRIOS AUTENTICADOS
                        .requestMatchers(HttpMethod.POST, "/api/v1/sold").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/sold").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/sold/{id}").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/sold/{id}").authenticated()
                        .requestMatchers("/api/v1/sold/**").authenticated()

                        // 🟠 GESTÃO DE LIVROS E AUTORES - ADMIN OU LIBRARIAN
                        .requestMatchers(HttpMethod.POST, "/api/v1/book").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/book/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/author").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/author/**").hasAnyRole("ADMIN", "LIBRARIAN")

                        // 🟠 GESTÃO DE CLIENTES - ADMIN OU LIBRARIAN
                        .requestMatchers(HttpMethod.GET, "/api/v1/customer").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/customer/{id}").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/customer").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/customer/{id}").hasAnyRole("ADMIN", "LIBRARIAN")

                        // 🔵 LOGOUT E PERFIL - USUÁRIOS AUTENTICADOS
                        .requestMatchers("/api/v1/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/profile").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/auth/profile").authenticated()

                        // 🔒 DEMAIS ENDPOINTS - AUTENTICAÇÃO OBRIGATÓRIA
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}