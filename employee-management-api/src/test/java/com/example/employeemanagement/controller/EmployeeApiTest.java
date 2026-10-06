package com.example.employeemanagement.controller;

import com.example.employeemanagement.repository.EmployeeRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * End-to-end API tests: starts the real app on a random port (H2 in-memory DB)
 * and calls it over HTTP with REST Assured.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EmployeeApiTest {

    @LocalServerPort
    private int port;

    @Autowired
    private EmployeeRepository repository;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1/employees";
        repository.deleteAll();
    }

    private String json(String firstName, String email, String department, String salary) {
        return """
                {
                  "firstName": "%s",
                  "lastName": "Doe",
                  "email": "%s",
                  "department": "%s",
                  "salary": %s
                }
                """.formatted(firstName, email, department, salary);
    }

    private int createEmployee(String firstName, String email, String department) {
        return given()
                .contentType(ContentType.JSON)
                .body(json(firstName, email, department, "50000"))
                .when().post()
                .then().statusCode(201)
                .extract().path("id");
    }

    @Test
    @DisplayName("POST /employees -> 201 with Location header and body")
    void createEmployee_returns201() {
        given()
                .contentType(ContentType.JSON)
                .body(json("John", "john@example.com", "Engineering", "50000"))
        .when()
                .post()
        .then()
                .statusCode(201)
                .header("Location", containsString("/api/v1/employees/"))
                .body("id", notNullValue())
                .body("firstName", equalTo("John"))
                .body("email", equalTo("john@example.com"))
                .body("department", equalTo("Engineering"));
    }

    @Test
    @DisplayName("POST /employees with invalid data -> 400 with field errors")
    void createEmployee_invalidBody_returns400() {
        given()
                .contentType(ContentType.JSON)
                .body(json("", "not-an-email", "Engineering", "-5"))
        .when()
                .post()
        .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("message", equalTo("Validation failed"))
                .body("validationErrors.firstName", notNullValue())
                .body("validationErrors.email", notNullValue())
                .body("validationErrors.salary", notNullValue());
    }

    @Test
    @DisplayName("POST /employees with malformed JSON -> 400")
    void createEmployee_malformedJson_returns400() {
        given()
                .contentType(ContentType.JSON)
                .body("{ this is not json")
        .when()
                .post()
        .then()
                .statusCode(400)
                .body("message", containsString("Malformed"));
    }

    @Test
    @DisplayName("POST /employees with an existing email -> 409")
    void createEmployee_duplicateEmail_returns409() {
        createEmployee("John", "john@example.com", "Engineering");

        given()
                .contentType(ContentType.JSON)
                .body(json("Jane", "john@example.com", "HR", "40000"))
        .when()
                .post()
        .then()
                .statusCode(409)
                .body("message", containsString("already exists"));
    }

    @Test
    @DisplayName("GET /employees -> 200 with all employees")
    void getAll_returnsList() {
        createEmployee("John", "john@example.com", "Engineering");
        createEmployee("Jane", "jane@example.com", "HR");

        given()
        .when()
                .get()
        .then()
                .statusCode(200)
                .body("$", hasSize(2));
    }

    @Test
    @DisplayName("GET /employees?department=... -> 200 with filtered employees")
    void getAll_filtersByDepartment() {
        createEmployee("John", "john@example.com", "Engineering");
        createEmployee("Jane", "jane@example.com", "HR");

        given()
                .queryParam("department", "hr")
        .when()
                .get()
        .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].firstName", equalTo("Jane"));
    }

    @Test
    @DisplayName("GET /employees/{id} -> 200 when found")
    void getById_returns200() {
        int id = createEmployee("John", "john@example.com", "Engineering");

        given()
        .when()
                .get("/{id}", id)
        .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("email", equalTo("john@example.com"));
    }

    @Test
    @DisplayName("GET /employees/{id} -> 404 when missing")
    void getById_notFound_returns404() {
        given()
        .when()
                .get("/{id}", 9999)
        .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("message", containsString("not found"));
    }

    @Test
    @DisplayName("GET /employees/abc -> 400 for a non-numeric id")
    void getById_invalidId_returns400() {
        given()
        .when()
                .get("/{id}", "abc")
        .then()
                .statusCode(400)
                .body("message", containsString("Invalid value"));
    }

    @Test
    @DisplayName("PUT /employees/{id} -> 200 with updated data")
    void update_returns200() {
        int id = createEmployee("John", "john@example.com", "Engineering");

        given()
                .contentType(ContentType.JSON)
                .body(json("Johnny", "john@example.com", "Management", "90000"))
        .when()
                .put("/{id}", id)
        .then()
                .statusCode(200)
                .body("firstName", equalTo("Johnny"))
                .body("department", equalTo("Management"));
    }

    @Test
    @DisplayName("PUT /employees/{id} -> 404 when missing")
    void update_notFound_returns404() {
        given()
                .contentType(ContentType.JSON)
                .body(json("John", "john@example.com", "Engineering", "50000"))
        .when()
                .put("/{id}", 9999)
        .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("DELETE /employees/{id} -> 204, then GET -> 404")
    void delete_returns204_thenGone() {
        int id = createEmployee("John", "john@example.com", "Engineering");

        given().when().delete("/{id}", id).then().statusCode(204);
        given().when().get("/{id}", id).then().statusCode(404);
    }

    @Test
    @DisplayName("DELETE /employees/{id} -> 404 when missing")
    void delete_notFound_returns404() {
        given().when().delete("/{id}", 9999).then().statusCode(404);
    }
}
