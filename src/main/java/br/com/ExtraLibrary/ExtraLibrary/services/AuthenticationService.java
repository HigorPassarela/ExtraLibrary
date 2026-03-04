package br.com.ExtraLibrary.ExtraLibrary.services;

import br.com.ExtraLibrary.ExtraLibrary.dto.request.LoginRequest;
import br.com.ExtraLibrary.ExtraLibrary.dto.request.RegisterRequest;
import br.com.ExtraLibrary.ExtraLibrary.dto.response.AuthResponse;
import br.com.ExtraLibrary.ExtraLibrary.exception.DuplicatedRegisterException;
import br.com.ExtraLibrary.ExtraLibrary.models.Customer;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.CustomerRole;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.CustomerStatus;
import br.com.ExtraLibrary.ExtraLibrary.repository.CustomerRepository;
import br.com.ExtraLibrary.ExtraLibrary.validators.CustomerValidator;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomerValidator customerValidator;

    public AuthenticationService(CustomerRepository customerRepository,
                                 PasswordEncoder passwordEncoder,
                                 JwtService jwtService,
                                 AuthenticationManager authenticationManager,
                                 CustomerValidator customerValidator) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.customerValidator = customerValidator;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Criar customer temporário para validação de email e CPF
        Customer tempCustomer = Customer.builder()
                .email(request.email())
                .cpf(request.cpf())
                .build();

        // Validar se email e CPF são únicos
        customerValidator.valid(tempCustomer);

        // Criar customer com senha criptografada
        var customer = Customer.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password())) // Senha criptografada aqui
                .cpf(request.cpf())
                .phone(request.phone())
                .dateBirth(request.dateBirth())
                .address(request.address())
                .status(CustomerStatus.ACTIVE)
                .role(CustomerRole.CUSTOMER)
                .build();

        Customer savedCustomer = customerRepository.save(customer);

        var jwtToken = jwtService.generateToken(savedCustomer);

        return new AuthResponse(
                jwtToken,
                "Bearer",
                savedCustomer.getId(),
                savedCustomer.getEmail(),
                savedCustomer.getName(),
                savedCustomer.getRole().name()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse authenticate(LoginRequest request) {
        try {
            // Autenticar usando email e senha
            // O Spring Security vai automaticamente verificar a senha criptografada
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );

            // Buscar o customer autenticado
            var customer = customerRepository.findByEmail(request.email())
                    .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

            // Gerar JWT token
            var jwtToken = jwtService.generateToken(customer);

            return new AuthResponse(
                    jwtToken,
                    "Bearer",
                    customer.getId(),
                    customer.getEmail(),
                    customer.getName(),
                    customer.getRole().name()
            );
        } catch (Exception e) {
            throw new BadCredentialsException("Credenciais inválidas");
        }
    }
}
