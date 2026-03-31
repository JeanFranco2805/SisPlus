package com.optical.net.sisplus.app.infrastructure.repository;

import com.optical.net.sisplus.app.infrastructure.entity.PayrollReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollReportRepository extends JpaRepository<PayrollReport, Long> {

    boolean existsByMonthAndYear(int month, int year);

    Optional<PayrollReport> findByMonthAndYear(int month, int year);

    @Query("SELECT r.id, r.month, r.year, r.generatedAt, r.generationType, r.fileName, r.totalEmployees, r.totalPayroll FROM PayrollReport r ORDER BY r.year DESC, r.month DESC")
    List<Object[]> findAllWithoutFileData();
}
