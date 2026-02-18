package br.com.ExtraLibrary.ExtraLibrary.models;

import br.com.ExtraLibrary.ExtraLibrary.models.enums.CustomerStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Past;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "customer")
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "email", length = 100, unique = true, nullable = false)
    private String email;

    @Column(name = "cpf", unique = true)
    private String cpf;

    @Column(name = "phone", length = 20, nullable = false)
    private String phone;

    @Past
    @Column(name = "date_birth", nullable = false)
    private LocalDate dateBirth;

    @Column(name = "address", length = 100, nullable = false)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(20)")
    private CustomerStatus status;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Sold> sales = new ArrayList<>();

    // constructor
    public Customer() {
    }

    // constructor complete
    public Customer(UUID id, String name, String email, String cpf, String phone, LocalDate dateBirth, String address, CustomerStatus status, LocalDateTime createdAt, LocalDateTime updatedAt, List<Sold> sales) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.cpf = cpf;
        this.phone = phone;
        this.dateBirth = dateBirth;
        this.address = address;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.sales = sales;
    }

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = CustomerStatus.ACTIVE;
        }
    }

    //getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getDateBirth() {
        return dateBirth;
    }

    public void setDateBirth(LocalDate dateBirth) {
        this.dateBirth = dateBirth;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public void setStatus(CustomerStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<Sold> getSales() {
        return sales;
    }

    public void setSales(List<Sold> sales) {
        this.sales = sales;
    }

    //hash and equal
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return Objects.equals(id, customer.id) && Objects.equals(name, customer.name) && Objects.equals(email, customer.email) && Objects.equals(cpf, customer.cpf) && Objects.equals(phone, customer.phone) && Objects.equals(dateBirth, customer.dateBirth) && Objects.equals(address, customer.address) && status == customer.status && Objects.equals(createdAt, customer.createdAt) && Objects.equals(updatedAt, customer.updatedAt) && Objects.equals(sales, customer.sales);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, email, cpf, phone, dateBirth, address, status, createdAt, updatedAt, sales);
    }

    //toString
    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", cpf='" + cpf + '\'' +
                ", phone='" + phone + '\'' +
                ", dateBirth=" + dateBirth +
                ", address='" + address + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", sales=" + sales +
                '}';
    }
}
