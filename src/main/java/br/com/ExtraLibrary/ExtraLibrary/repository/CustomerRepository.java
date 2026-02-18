package br.com.ExtraLibrary.ExtraLibrary.repository;

import br.com.ExtraLibrary.ExtraLibrary.models.Customer;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.CustomerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByCpf(String cpf);

    List<Customer> findByStatus(CustomerStatus status);

    List<Customer> findByName(String name);

//    Optional<Customer> findByEmailOrCpf(String email, String cpf);
//
//    Optional<Customer> findByEmailAndCpf(String email, String cpf);
}
