package br.com.ExtraLibrary.ExtraLibrary.config;

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
                        // 🟢 Endpoints públicos - Autenticação
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()

                        // 🟢 Endpoints públicos - Documentação
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/swagger-resources/**", "/webjars/**").permitAll()

                        // 🟢 Endpoints públicos - Consulta de livros (sem compra)
                        .requestMatchers(HttpMethod.GET, "/api/v1/book").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/book/search/author/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/book/{id}").permitAll()

                        // 🟢 Endpoints públicos - Consulta de autores
                        .requestMatchers(HttpMethod.GET, "/api/v1/author").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/author/name").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/author/nacionality").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/author/{id}").permitAll()

                        // 🔴 Endpoints críticos - apenas ADMIN
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/reports/**").hasRole("ADMIN")

                        // 🟡 Endpoints de gestão de status - apenas ADMIN
                        .requestMatchers("/api/v1/customer/*/activate").hasRole("ADMIN")
                        .requestMatchers("/api/v1/customer/*/block").hasRole("ADMIN")
                        .requestMatchers("/api/v1/customer/*/disable").hasRole("ADMIN")

                        // 🔵 VENDAS - APENAS USUÁRIOS AUTENTICADOS (PRINCIPAL MUDANÇA)
                        .requestMatchers(HttpMethod.POST, "/api/v1/sold").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/sold/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/sold/**").authenticated()
                        .requestMatchers("/api/v1/sold/**").authenticated()

                        // 🔵 Logout requer autenticação
                        .requestMatchers("/api/v1/auth/logout").authenticated()

                        // 🟠 Demais endpoints - verificação detalhada nos Services
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
        // ✅ Usando o método não-deprecated
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
