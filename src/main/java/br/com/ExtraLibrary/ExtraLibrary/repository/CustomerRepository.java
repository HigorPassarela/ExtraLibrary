package br.com.ExtraLibrary.ExtraLibrary.repository;

import br.com.ExtraLibrary.ExtraLibrary.models.Customer;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.CustomerRole;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.CustomerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByCpf(String cpf);

    List<Customer> findByStatus(CustomerStatus status);

    List<Customer> findByName(String name);

    // ✅ Busca case-insensitive por nome
    @Query("SELECT c FROM Customer c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Customer> findByNameContainingIgnoreCase(@Param("name") String name);

    // ✅ Contar por role (para relatórios)
    long countByRole(CustomerRole role);

    // ✅ Contar por status (para relatórios)
    long countByStatus(CustomerStatus status);

    // ✅ Buscar clientes ativos por role
    @Query("SELECT c FROM Customer c WHERE c.status = 'ACTIVE' AND c.role = :role")
    List<Customer> findActiveCustomersByRole(@Param("role") CustomerRole role);

    // ✅ Buscar apenas dados básicos (para listagens)
    @Query("SELECT new Customer(c.id, c.name, c.email, null, c.cpf, c.phone, c.dateBirth, c.address, c.status, c.role, c.createdAt, c.updatedAt, null) FROM Customer c")
    List<Customer> findAllBasicInfo();
}