package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.EmployeeRequest;
import com.example.employeemanagement.dto.EmployeeResponse;
import com.example.employeemanagement.exception.DuplicateEmailException;
import com.example.employeemanagement.exception.ResourceNotFoundException;
import com.example.employeemanagement.model.Employee;
import com.example.employeemanagement.repository.EmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository repository;

    @InjectMocks
    private EmployeeService service;

    private EmployeeRequest request(String email) {
        return new EmployeeRequest("John", "Doe", email, "Engineering", new BigDecimal("50000"));
    }

    private Employee employee(Long id, String email) {
        Employee e = new Employee();
        e.setId(id);
        e.setFirstName("John");
        e.setLastName("Doe");
        e.setEmail(email);
        e.setDepartment("Engineering");
        e.setSalary(new BigDecimal("50000"));
        return e;
    }

    @Test
    @DisplayName("create: saves and returns the new employee")
    void create_savesEmployee() {
        when(repository.existsByEmail("john@example.com")).thenReturn(false);
        when(repository.save(any(Employee.class))).thenAnswer(inv -> {
            Employee e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        EmployeeResponse response = service.create(request("john@example.com"));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.department()).isEqualTo("Engineering");
        verify(repository).save(any(Employee.class));
    }

    @Test
    @DisplayName("create: duplicate email throws and nothing is saved")
    void create_duplicateEmail_throws() {
        when(repository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("john@example.com")))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("john@example.com");

        verify(repository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("getAll: returns all employees when no department filter is given")
    void getAll_noFilter_returnsAll() {
        when(repository.findAll()).thenReturn(List.of(employee(1L, "a@example.com"), employee(2L, "b@example.com")));

        List<EmployeeResponse> result = service.getAll(null);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getAll: filters by department when provided")
    void getAll_withDepartment_filters() {
        when(repository.findByDepartmentIgnoreCase("engineering"))
                .thenReturn(List.of(employee(1L, "a@example.com")));

        List<EmployeeResponse> result = service.getAll("engineering");

        assertThat(result).hasSize(1);
        verify(repository, never()).findAll();
    }

    @Test
    @DisplayName("getById: returns the employee when found")
    void getById_found() {
        when(repository.findById(1L)).thenReturn(Optional.of(employee(1L, "john@example.com")));

        EmployeeResponse response = service.getById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.firstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("getById: throws when the employee does not exist")
    void getById_notFound_throws() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("update: changes the fields and saves")
    void update_success() {
        Employee existing = employee(1L, "john@example.com");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByEmailAndIdNot("new@example.com", 1L)).thenReturn(false);
        when(repository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeResponse response = service.update(1L, request("new@example.com"));

        assertThat(response.email()).isEqualTo("new@example.com");
        verify(repository).save(existing);
    }

    @Test
    @DisplayName("update: throws when the employee does not exist")
    void update_notFound_throws() {
        when(repository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(5L, request("x@example.com")))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("update: throws when the email belongs to another employee")
    void update_emailTakenByAnother_throws() {
        when(repository.findById(1L)).thenReturn(Optional.of(employee(1L, "john@example.com")));
        when(repository.existsByEmailAndIdNot("taken@example.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, request("taken@example.com")))
                .isInstanceOf(DuplicateEmailException.class);

        verify(repository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("delete: removes an existing employee")
    void delete_success() {
        when(repository.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("delete: throws when the employee does not exist")
    void delete_notFound_throws() {
        when(repository.existsById(42L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(42L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).deleteById(any());
    }
}
