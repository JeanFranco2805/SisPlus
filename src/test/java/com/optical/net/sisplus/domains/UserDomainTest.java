package com.optical.net.sisplus.domains;

import com.optical.net.sisplus.app.domain.AttendanceDomain;
import com.optical.net.sisplus.app.domain.PayrollCalculation;
import com.optical.net.sisplus.app.domain.UserDomain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("UserDomain - Cálculos de Nómina")
class UserDomainTest {

    private UserDomain user;

    @BeforeEach
    void setUp() {
        // Configurar tarifas estáticas (en producción, esto vendría de configuración)
        UserDomain.REGULAR_HOUR_RATE = 7959.0;
        UserDomain.DAY_OVERTIME_RATE = 9948.0;
        UserDomain.NIGHT_SURCHARGE_RATE = 2786.0;
        UserDomain.NIGHT_OVERTIME_RATE = 13928.25;
        UserDomain.NIGHT_START_HOUR = 19;
        UserDomain.NIGHT_END_HOUR = 6;

        user = UserDomain.builder()
                .id(1L)
                .name("Juan")
                .lastName("Pérez")
                .cc("1234567890")
                .attendance(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("Cálculo de Nómina Diaria")
    class DailyPayrollTests {

        @Test
        @DisplayName("Debe calcular pago regular para 8 horas normales")
        void shouldCalculateRegularPayFor8Hours() {
            // Given: Asistencia de 8 horas (8:00 AM - 4:00 PM)
            LocalDate today = LocalDate.of(2026, 1, 25);
            AttendanceDomain attendance = createAttendance(
                    today.atTime(8, 0),
                    today.atTime(16, 0)
            );
            user.getAttendance().add(attendance);

            // When
            PayrollCalculation payroll = user.calculateDailyPayroll(today);

            // Then
            assertThat(payroll.getRegularHours()).isEqualTo(8.0);
            assertThat(payroll.getRegularPay()).isEqualTo(8.0 * 7959.0);
            assertThat(payroll.getDayOvertimeHours()).isZero();
            assertThat(payroll.getNightOvertimeHours()).isZero();
            assertThat(payroll.getNightHours()).isZero();
            assertThat(payroll.getTotalPay()).isEqualTo(payroll.getRegularPay());
        }

        @Test
        @DisplayName("Debe calcular horas extras diurnas para jornada de 10 horas")
        void shouldCalculateDayOvertimeFor10Hours() {
            // Given: 10 horas (8:00 AM - 6:00 PM)
            LocalDate today = LocalDate.of(2026, 1, 25);
            AttendanceDomain attendance = createAttendance(
                    today.atTime(8, 0),
                    today.atTime(18, 0)
            );
            user.getAttendance().add(attendance);

            // When
            PayrollCalculation payroll = user.calculateDailyPayroll(today);

            // Then
            assertThat(payroll.getRegularHours()).isEqualTo(8.0);
            assertThat(payroll.getDayOvertimeHours()).isEqualTo(2.0);
            assertThat(payroll.getDayOvertimePay()).isEqualTo(2.0 * 9948.0);
            assertThat(payroll.getNightOvertimeHours()).isZero();
        }

        @Test
        @DisplayName("Debe calcular recargo nocturno para trabajo de noche")
        void shouldCalculateNightSurcharge() {
            // Given: 8 horas nocturnas (10:00 PM - 6:00 AM)
            LocalDate today = LocalDate.of(2026, 1, 25);
            AttendanceDomain attendance = createAttendance(
                    today.atTime(22, 0),
                    today.plusDays(1).atTime(6, 0)
            );
            user.getAttendance().add(attendance);

            // When
            PayrollCalculation payroll = user.calculateDailyPayroll(today);

            // Then
            assertThat(payroll.getRegularHours()).isEqualTo(8.0);
            assertThat(payroll.getNightHours()).isEqualTo(8.0);
            assertThat(payroll.getNightSurchargePay()).isEqualTo(8.0 * 2786.0);
            assertThat(payroll.getTotalPay()).isGreaterThan(payroll.getRegularPay());
        }

        @Test
        @DisplayName("Debe calcular horas extras nocturnas")
        void shouldCalculateNightOvertime() {
            // Given: 10 horas nocturnas (8:00 PM - 6:00 AM)
            LocalDate today = LocalDate.of(2026, 1, 25);
            AttendanceDomain attendance = createAttendance(
                    today.atTime(20, 0),
                    today.plusDays(1).atTime(6, 0)
            );
            user.getAttendance().add(attendance);

            // When
            PayrollCalculation payroll = user.calculateDailyPayroll(today);

            // Then
            assertThat(payroll.getRegularHours()).isEqualTo(8.0);
            assertThat(payroll.getNightOvertimeHours()).isGreaterThan(0);
            assertThat(payroll.getNightOvertimePay()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Debe retornar cálculo vacío si no hay asistencia")
        void shouldReturnEmptyCalculationWithoutAttendance() {
            // Given: Usuario sin asistencias
            LocalDate today = LocalDate.of(2026, 1, 25);

            // When
            PayrollCalculation payroll = user.calculateDailyPayroll(today);

            // Then
            assertThat(payroll.getRegularHours()).isZero();
            assertThat(payroll.getTotalPay()).isZero();
        }

        @Test
        @DisplayName("Debe retornar cálculo vacío si asistencia está incompleta")
        void shouldReturnEmptyCalculationWithIncompleteAttendance() {
            // Given: Asistencia solo con entrada (sin salida)
            LocalDate today = LocalDate.of(2026, 1, 25);
            AttendanceDomain attendance = AttendanceDomain.builder()
                    .id(1L)
                    .user(user)
                    .entryTime(today.atTime(8, 0))
                    .departureTime(null)
                    .build();
            user.getAttendance().add(attendance);

            // When
            PayrollCalculation payroll = user.calculateDailyPayroll(today);

            // Then
            assertThat(payroll.getTotalPay()).isZero();
        }
    }

    @Nested
    @DisplayName("Cálculo de Nómina Semanal")
    class WeeklyPayrollTests {

        @Test
        @DisplayName("Debe calcular nómina semanal para 5 días trabajados")
        void shouldCalculateWeeklyPayrollFor5Days() {
            // Given: 5 días de trabajo (Lunes a Viernes, 8 horas cada día)
            LocalDate monday = LocalDate.of(2026, 1, 19);

            for (int i = 0; i < 5; i++) {
                LocalDate day = monday.plusDays(i);
                AttendanceDomain attendance = createAttendance(
                        day.atTime(8, 0),
                        day.atTime(16, 0)
                );
                user.getAttendance().add(attendance);
            }

            // When
            PayrollCalculation payroll = user.calculateWeeklyPayroll(monday.plusDays(6));

            // Then
            assertThat(payroll.getRegularHours()).isEqualTo(40.0); // 5 días * 8 horas
            assertThat(payroll.getRegularPay()).isEqualTo(40.0 * 7959.0);
        }

        @Test
        @DisplayName("Debe acumular horas extras de toda la semana")
        void shouldAccumulateOvertimeForWeek() {
            // Given: 5 días con 2 horas extras cada día
            LocalDate monday = LocalDate.of(2026, 1, 19);

            for (int i = 0; i < 5; i++) {
                LocalDate day = monday.plusDays(i);
                AttendanceDomain attendance = createAttendance(
                        day.atTime(8, 0),
                        day.atTime(18, 0) // 10 horas
                );
                user.getAttendance().add(attendance);
            }

            // When
            PayrollCalculation payroll = user.calculateWeeklyPayroll(monday.plusDays(6));

            // Then
            assertThat(payroll.getDayOvertimeHours()).isEqualTo(10.0); // 5 días * 2 horas extras
        }
    }

    @Nested
    @DisplayName("Cálculo de Nómina Mensual")
    class MonthlyPayrollTests {

        @Test
        @DisplayName("Debe calcular nómina mensual completa")
        void shouldCalculateMonthlyPayroll() {
            // Given: 22 días laborables en enero 2026
            int workDays = 22;
            for (int i = 1; i <= workDays; i++) {
                LocalDate day = LocalDate.of(2026, 1, i);
                if (day.getDayOfWeek().getValue() <= 5) { // Lunes a Viernes
                    AttendanceDomain attendance = createAttendance(
                            day.atTime(8, 0),
                            day.atTime(16, 0)
                    );
                    user.getAttendance().add(attendance);
                }
            }

            // When
            PayrollCalculation payroll = user.calculateMonthlyPayroll(1, 2026);

            // Then
            assertThat(payroll.getRegularHours()).isGreaterThan(0);
            assertThat(payroll.getTotalPay()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Debe filtrar solo asistencias del mes especificado")
        void shouldFilterBySpecificMonth() {
            // Given: Asistencias en enero y febrero
            AttendanceDomain januaryAttendance = createAttendance(
                    LocalDate.of(2026, 1, 15).atTime(8, 0),
                    LocalDate.of(2026, 1, 15).atTime(16, 0)
            );
            AttendanceDomain februaryAttendance = createAttendance(
                    LocalDate.of(2026, 2, 15).atTime(8, 0),
                    LocalDate.of(2026, 2, 15).atTime(16, 0)
            );
            user.getAttendance().add(januaryAttendance);
            user.getAttendance().add(februaryAttendance);

            // When: Calcular solo enero
            PayrollCalculation payroll = user.calculateMonthlyPayroll(1, 2026);

            // Then
            assertThat(payroll.getRegularHours()).isEqualTo(8.0); // Solo el día de enero
        }

        @Test
        @DisplayName("Debe retornar cálculo vacío para mes sin asistencias")
        void shouldReturnEmptyForMonthWithoutAttendances() {
            // Given: Asistencia en enero
            AttendanceDomain attendance = createAttendance(
                    LocalDate.of(2026, 1, 15).atTime(8, 0),
                    LocalDate.of(2026, 1, 15).atTime(16, 0)
            );
            user.getAttendance().add(attendance);

            // When: Calcular febrero
            PayrollCalculation payroll = user.calculateMonthlyPayroll(2, 2026);

            // Then
            assertThat(payroll.getTotalPay()).isZero();
        }
    }

    @Nested
    @DisplayName("Validaciones de AttendanceDomain")
    class AttendanceValidationTests {

        @Test
        @DisplayName("Debe identificar asistencia completa")
        void shouldIdentifyCompleteAttendance() {
            // Given
            AttendanceDomain attendance = createAttendance(
                    LocalDateTime.of(2026, 1, 25, 8, 0),
                    LocalDateTime.of(2026, 1, 25, 16, 0)
            );

            // Then
            assertThat(attendance.isComplete()).isTrue();
        }

        @Test
        @DisplayName("Debe identificar asistencia incompleta sin salida")
        void shouldIdentifyIncompleteAttendance() {
            // Given
            AttendanceDomain attendance = AttendanceDomain.builder()
                    .entryTime(LocalDateTime.of(2026, 1, 25, 8, 0))
                    .departureTime(null)
                    .build();

            // Then
            assertThat(attendance.isComplete()).isFalse();
        }

        @Test
        @DisplayName("Debe calcular horas trabajadas correctamente")
        void shouldCalculateWorkedHours() {
            // Given: 8 horas de trabajo
            AttendanceDomain attendance = createAttendance(
                    LocalDateTime.of(2026, 1, 25, 8, 0),
                    LocalDateTime.of(2026, 1, 25, 16, 0)
            );

            // Then
            assertThat(attendance.getWorkedHours()).isEqualTo(8.0);
        }
    }

    // Helper method
    private AttendanceDomain createAttendance(LocalDateTime entry, LocalDateTime departure) {
        return AttendanceDomain.builder()
                .id(System.currentTimeMillis())
                .user(user)
                .entryTime(entry)
                .departureTime(departure)
                .build();
    }
}