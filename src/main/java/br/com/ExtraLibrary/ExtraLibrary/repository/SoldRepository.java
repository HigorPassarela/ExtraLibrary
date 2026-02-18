package br.com.ExtraLibrary.ExtraLibrary.repository;

import br.com.ExtraLibrary.ExtraLibrary.models.Sold;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.FormPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SoldRepository extends JpaRepository<Sold, Long> {

    List<Sold> findByCustomerId(UUID customerId);

    List<Sold> findByFormPayment(FormPayment formPayment);

    List<Sold> findByDateSaleBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<Sold> findByCustomerIdAndDateSaleBetween(UUID customerId, LocalDateTime startDate, LocalDateTime endDate);

    List<Sold> findByFinalPriceGreaterThan(BigDecimal minValue);

    List<Sold> findByFinalPriceLessThan(BigDecimal maxValue);

    List<Sold> findByFinalPriceBetween(BigDecimal minValue, BigDecimal maxValue);

    @Query("SELECT SUM(s.finalPrice) FROM Sold s")
    BigDecimal sumTotalSales();

    @Query("SELECT SUM(s.finalPrice) FROM Sold s WHERE s.customer.id = :customerId")
    BigDecimal sumTotalSalesByCustomer(@Param("customerId") UUID customerId);

    @Query("SELECT SUM(s.finalPrice) FROM Sold s WHERE s.dateSale BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalSalesByPeriod(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    List<Sold> findTop10ByOrderByDateSaleDesc();

    List<Sold> findTop10ByOrderByFinalPriceDesc();
}
