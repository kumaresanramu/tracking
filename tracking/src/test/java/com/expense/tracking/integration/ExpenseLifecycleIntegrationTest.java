package com.expense.tracking.integration;

import com.expense.tracking.dto.ExpenseRequest;
import com.expense.tracking.dto.ExpenseResponse;
import com.expense.tracking.entity.Category;
import com.expense.tracking.repository.CategoryRepository;
import com.expense.tracking.repository.ExpenseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
public class ExpenseLifecycleIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    private Category testCategory;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";
        
        // Clean up any existing data
        expenseRepository.deleteAll();
        categoryRepository.deleteAll();

        // Create a test category
        testCategory = Category.builder()
                .name("Test Category")
                .color("#FF0000")
                .description("Test category for integration tests")
                .build();
        testCategory = categoryRepository.save(testCategory);
    }

    @Test
    void testCompleteExpenseLifecycle() throws Exception {
        // Test 1: Create an expense
        ExpenseRequest createRequest = ExpenseRequest.builder()
                .amount(new BigDecimal("50.75"))
                .date(LocalDate.now())
                .categoryId(testCategory.getId())
                .description("Integration test expense")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ExpenseRequest> createEntity = new HttpEntity<>(createRequest, headers);

        ResponseEntity<ExpenseResponse> createResponse = restTemplate.postForEntity(
                baseUrl + "/expenses", createEntity, ExpenseResponse.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ExpenseResponse createdExpense = createResponse.getBody();
        assertThat(createdExpense).isNotNull();
        assertThat(createdExpense.getAmount()).isEqualTo(new BigDecimal("50.75"));
        assertThat(createdExpense.getDescription()).isEqualTo("Integration test expense");
        assertThat(createdExpense.getCategory().getName()).isEqualTo("Test Category");

        Long expenseId = createdExpense.getId();
        assertThat(expenseId).isNotNull();

        // Test 2: Retrieve the expense by month
        LocalDate now = LocalDate.now();
        ResponseEntity<ExpenseResponse[]> getResponse = restTemplate.getForEntity(
                baseUrl + "/expenses/month/" + now.getYear() + "/" + now.getMonthValue(),
                ExpenseResponse[].class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        ExpenseResponse[] expenses = getResponse.getBody();
        assertThat(expenses).isNotNull();
        assertThat(expenses).hasSize(1);
        assertThat(expenses[0].getId()).isEqualTo(expenseId);
        assertThat(expenses[0].getAmount()).isEqualTo(new BigDecimal("50.75"));

        // Test 3: Update the expense
        ExpenseRequest updateRequest = ExpenseRequest.builder()
                .amount(new BigDecimal("75.25"))
                .date(LocalDate.now())
                .categoryId(testCategory.getId())
                .description("Updated integration test expense")
                .build();

        HttpEntity<ExpenseRequest> updateEntity = new HttpEntity<>(updateRequest, headers);
        ResponseEntity<ExpenseResponse> updateResponse = restTemplate.exchange(
                baseUrl + "/expenses/" + expenseId, HttpMethod.PUT, updateEntity, ExpenseResponse.class);

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        ExpenseResponse updatedExpense = updateResponse.getBody();
        assertThat(updatedExpense).isNotNull();
        assertThat(updatedExpense.getAmount()).isEqualTo(new BigDecimal("75.25"));
        assertThat(updatedExpense.getDescription()).isEqualTo("Updated integration test expense");

        // Test 4: Verify the update persisted
        ResponseEntity<ExpenseResponse[]> verifyResponse = restTemplate.getForEntity(
                baseUrl + "/expenses/month/" + now.getYear() + "/" + now.getMonthValue(),
                ExpenseResponse[].class);

        assertThat(verifyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        ExpenseResponse[] updatedExpenses = verifyResponse.getBody();
        assertThat(updatedExpenses).isNotNull();
        assertThat(updatedExpenses).hasSize(1);
        assertThat(updatedExpenses[0].getAmount()).isEqualTo(new BigDecimal("75.25"));

        // Test 5: Delete the expense
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                baseUrl + "/expenses/" + expenseId, HttpMethod.DELETE, null, Void.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Test 6: Verify the expense was deleted
        ResponseEntity<ExpenseResponse[]> finalResponse = restTemplate.getForEntity(
                baseUrl + "/expenses/month/" + now.getYear() + "/" + now.getMonthValue(),
                ExpenseResponse[].class);

        assertThat(finalResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        ExpenseResponse[] finalExpenses = finalResponse.getBody();
        assertThat(finalExpenses).isNotNull();
        assertThat(finalExpenses).isEmpty();

        // Verify in database
        assertThat(expenseRepository.findById(expenseId)).isEmpty();
    }

    @Test
    void testBatchExpenseOperations() throws Exception {
        // Test batch create
        ExpenseRequest expense1 = ExpenseRequest.builder()
                .amount(new BigDecimal("25.00"))
                .date(LocalDate.now())
                .categoryId(testCategory.getId())
                .description("Batch expense 1")
                .build();

        ExpenseRequest expense2 = ExpenseRequest.builder()
                .amount(new BigDecimal("35.50"))
                .date(LocalDate.now())
                .categoryId(testCategory.getId())
                .description("Batch expense 2")
                .build();

        mockMvc.perform(post("/api/expenses/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Arrays.asList(expense1, expense2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].amount").value(25.00))
                .andExpect(jsonPath("$[1].amount").value(35.50));

        // Verify both expenses were created
        LocalDate now = LocalDate.now();
        mockMvc.perform(get("/api/expenses/month/{year}/{month}", now.getYear(), now.getMonthValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testCategoryHierarchyIntegration() throws Exception {
        // Create parent category
        Category parentCategory = Category.builder()
                .name("Parent Category")
                .color("#00FF00")
                .description("Parent category for testing")
                .build();
        parentCategory = categoryRepository.save(parentCategory);

        // Create child category
        Category childCategory = Category.builder()
                .name("Child Category")
                .color("#0000FF")
                .description("Child category for testing")
                .parent(parentCategory)
                .build();
        childCategory = categoryRepository.save(childCategory);

        // Create expense with child category
        ExpenseRequest request = ExpenseRequest.builder()
                .amount(new BigDecimal("100.00"))
                .date(LocalDate.now())
                .categoryId(childCategory.getId())
                .description("Hierarchical category test")
                .build();

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category.name").value("Child Category"))
                .andExpect(jsonPath("$.category.fullPath").value("Parent Category > Child Category"));
    }

    @Test
    void testErrorHandling() throws Exception {
        // Test creating expense with invalid data
        ExpenseRequest invalidRequest = ExpenseRequest.builder()
                .amount(new BigDecimal("-10.00")) // Negative amount
                .date(LocalDate.now().plusDays(1)) // Future date
                .categoryId(999L) // Non-existent category
                .description("")
                .build();

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        // Test updating non-existent expense
        ExpenseRequest updateRequest = ExpenseRequest.builder()
                .amount(new BigDecimal("50.00"))
                .date(LocalDate.now())
                .categoryId(testCategory.getId())
                .description("Update test")
                .build();

        mockMvc.perform(put("/api/expenses/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());

        // Test deleting non-existent expense
        mockMvc.perform(delete("/api/expenses/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("expense-tracking"));
    }

    @Test
    void testCategoriesEndpoint() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Test Category"));
    }
}