package com.example.employeemanagement.dto;

import com.example.employeemanagement.model.Employee;

import java.math.BigDecimal;

public record EmployeeResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String department,
        BigDecimal salary
) {
    public static EmployeeResponse from(Employee e) {
        return new EmployeeResponse(
                e.getId(), e.getFirstName(), e.getLastName(),
                e.getEmail(), e.getDepartment(), e.getSalary());
    }
}
