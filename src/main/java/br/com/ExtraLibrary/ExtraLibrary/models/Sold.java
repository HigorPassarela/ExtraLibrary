package br.com.ExtraLibrary.ExtraLibrary.models;

import br.com.ExtraLibrary.ExtraLibrary.models.enums.FormPayment;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "sold")
public class Sold {

    @Id
    @SequenceGenerator(name = "sold_seq", sequenceName = "sold_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sold_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "date_sale", nullable = false)
    private LocalDateTime dateSale;

    @Column(name = "subtotal", precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "discount", precision = 10, scale = 2)
    private BigDecimal discount;

    @Column(name = "final_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal finalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "form_payment", nullable = false)
    private FormPayment formPayment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ElementCollection
    @CollectionTable(name = "sold_book_ids", joinColumns = @JoinColumn(name = "sold_id"))
    @Column(name = "book_id")
    private List<UUID> bookIds = new ArrayList<>();

    //constructor
    public Sold() {
    }

    //getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDateSale() {
        return dateSale;
    }

    public void setDateSale(LocalDateTime dateSale) {
        this.dateSale = dateSale;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(BigDecimal finalPrice) {
        this.finalPrice = finalPrice;
    }

    public FormPayment getFormPayment() {
        return formPayment;
    }

    public void setFormPayment(FormPayment formPayment) {
        this.formPayment = formPayment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public List<UUID> getBookIds() {
        return bookIds;
    }

    public void setBookIds(List<UUID> bookIds) {
        this.bookIds = bookIds;
    }

    //hash and equal
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Sold sold = (Sold) o;
        return Objects.equals(id, sold.id) && Objects.equals(dateSale, sold.dateSale) && Objects.equals(subtotal, sold.subtotal) && Objects.equals(discount, sold.discount) && Objects.equals(finalPrice, sold.finalPrice) && formPayment == sold.formPayment && Objects.equals(createdAt, sold.createdAt) && Objects.equals(customer, sold.customer) && Objects.equals(bookIds, sold.bookIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, dateSale, subtotal, discount, finalPrice, formPayment, createdAt, customer, bookIds);
    }
}
