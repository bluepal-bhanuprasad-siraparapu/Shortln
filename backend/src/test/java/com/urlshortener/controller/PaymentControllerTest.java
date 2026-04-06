package com.urlshortener.controller;

import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import com.urlshortener.dto.UserDto;
import com.urlshortener.entity.Plan;
import com.urlshortener.security.JwtAuthenticationFilter;
import com.urlshortener.security.TestSecurityUtils;
import com.urlshortener.service.UserService;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void tearDown() {
        TestSecurityUtils.clear();
    }

    @Test
    void createOrder_Success() throws Exception {
        try (MockedConstruction<RazorpayClient> mocked = mockConstruction(RazorpayClient.class, (mock, context) -> {
            OrderClient orderClient = mock(OrderClient.class);
            Order order = mock(Order.class);
            when(order.get("id")).thenReturn("order_123");
            when(order.get("amount")).thenReturn(99900);
            when(order.get("currency")).thenReturn("INR");
            
            mock.orders = orderClient;
            when(orderClient.create(any(JSONObject.class))).thenReturn(order);
        })) {
            mockMvc.perform(post("/api/payments/create-order")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("order_123"))
                    .andExpect(jsonPath("$.amount").value(99900));
        }
    }

    @Test
    void verifyPayment_Success() throws Exception {
        TestSecurityUtils.setupMockUser();
        UserDto userDto = UserDto.builder().id(2L).name("Regular User").build();
        when(userService.updateUserPlan(anyLong(), any(Plan.class))).thenReturn(userDto);
        
        try (MockedStatic<Utils> mockedStatic = mockStatic(Utils.class)) {
            mockedStatic.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), any()))
                    .thenReturn(true);

            mockMvc.perform(post("/api/payments/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"razorpay_order_id\":\"order_123\", \"razorpay_payment_id\":\"pay_123\", \"razorpay_signature\":\"sig_123\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.user.name").value("Regular User"));

            verify(userService).updateUserPlan(eq(2L), eq(Plan.PRO));
        }
    }

    @Test
    void verifyPayment_InvalidSignature() throws Exception {
        TestSecurityUtils.setupMockUser();
        
        try (MockedStatic<Utils> mockedStatic = mockStatic(Utils.class)) {
            mockedStatic.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), any()))
                    .thenReturn(false);

            mockMvc.perform(post("/api/payments/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"razorpay_order_id\":\"order_123\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid payment signature"));
        }
    }
}
