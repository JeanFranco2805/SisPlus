package com.optical.net.sisplus.app.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class PayrollCalculation {
    private double regularHours;
    private double dayOvertimeHours;
    private double nightOvertimeHours;
    private double nightHours;
    private double regularPay;
    private double nightSurchargePay;
    private double dayOvertimePay;
    private double nightOvertimePay;
    private double totalOvertimePay;
    private double totalPay;
}