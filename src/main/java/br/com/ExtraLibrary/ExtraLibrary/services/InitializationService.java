package br.com.ExtraLibrary.ExtraLibrary.services;

import br.com.ExtraLibrary.ExtraLibrary.models.Customer;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.CustomerRole;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.CustomerStatus;
import br.com.ExtraLibrary.ExtraLibrary.repository.CustomerRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class InitializationService {

    private static final Logger logger = LoggerFactory.getLogger(InitializationService.class);

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.name:Administrador}")
    private String adminName;

    @Value("${app.admin.email:admin@extralibrary.com}")
    private String adminEmail;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Value("${app.admin.cpf:00000000000}")
    private String adminCpf;

    @Value("${app.admin.phone:11999999999}")
    private String adminPhone;

    @Value("${app.admin.dateBirth:1990-01-01}")
    private String adminDateBirth;

    @Value("${app.admin.address:Endereço Admin}")
    private String adminAddress;

    public InitializationService(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void init() {
        createDefaultAdmin();
    }

    private void createDefaultAdmin() {
        try {
            if (customerRepository.findByEmail(adminEmail).isEmpty()) {
                Customer admin = Customer.builder()
                        .name(adminName)
                        .email(adminEmail)
                        .password(passwordEncoder.encode(adminPassword))
                        .cpf(adminCpf)
                        .phone(adminPhone)
                        .dateBirth(LocalDate.parse(adminDateBirth))
                        .address(adminAddress)
                        .status(CustomerStatus.ACTIVE)
                        .role(CustomerRole.ADMIN)
                        .build();

                customerRepository.save(admin);
                logger.info("✅ Admin criado com sucesso!");
                logger.info("📧 Email: {}", adminEmail);
                logger.info("🔑 Senha: {}", adminPassword);
                logger.warn("⚠️  ALTERE A SENHA PADRÃO APÓS O PRIMEIRO LOGIN!");
            } else {
                logger.info("ℹ️  Admin já existe no sistema");
            }
        } catch (Exception e) {
            logger.error("❌ Erro ao criar admin: {}", e.getMessage());
        }
    }
}
