package com.hhplus.concert.interfaces.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.hhplus.concert.business.domain.repository.BalanceRepository
import com.hhplus.concert.business.domain.repository.UserRepository
import com.hhplus.concert.infrastructure.entity.Balance
import com.hhplus.concert.infrastructure.entity.User
import com.hhplus.concert.interfaces.presentation.request.BalanceRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BalanceControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var balanceRepository: BalanceRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `잔액 충전 - 성공`() {
        // given
        val user = userRepository.save(User(name = "Test User"))
        val rechargeRequest = BalanceRequest.Recharge(amount = 10000)

        // when
        val result =
            mockMvc.post("/api/v1/balance/users/${user.id}/recharge") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(rechargeRequest)
            }

        // then
        result.andExpect {
            status { isOk() }
            jsonPath("$.userId").value(user.id)
            jsonPath("$.currentAmount").value(10000)
        }

        val updatedBalance = balanceRepository.findByUserId(user.id)
        assertNotNull(updatedBalance)
        assertEquals(10000L, updatedBalance?.amount)
    }

    @Test
    fun `잔액 충전 - 존재하지 않는 사용자`() {
        // given
        val nonExistentUserId = 9999L
        val rechargeRequest = BalanceRequest.Recharge(amount = 10000)

        // when
        val result =
            mockMvc.post("/api/v1/balance/users/$nonExistentUserId/recharge") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(rechargeRequest)
            }

        // then
        result.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `잔액 충전 - 잘못된 금액`() {
        // given
        val user = userRepository.save(User(name = "Test User"))
        val rechargeRequest = BalanceRequest.Recharge(amount = -1000)

        // when
        val result =
            mockMvc.post("/api/v1/balance/users/${user.id}/recharge") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(rechargeRequest)
            }

        // then
        result.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `잔액 충전 - 기존 잔액이 있는 경우`() {
        // given
        val user = userRepository.save(User(name = "Test User"))
        balanceRepository.save(Balance(user = user, amount = 5000, lastUpdatedAt = LocalDateTime.now()))
        val rechargeRequest = BalanceRequest.Recharge(amount = 3000)

        // when
        val result =
            mockMvc.post("/api/v1/balance/users/${user.id}/recharge") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(rechargeRequest)
            }

        // then
        result.andExpect {
            status { isOk() }
            jsonPath("$.userId").value(user.id)
            jsonPath("$.currentAmount").value(8000)
        }

        val updatedBalance = balanceRepository.findByUserId(user.id)
        assertNotNull(updatedBalance)
        assertEquals(8000L, updatedBalance?.amount)
    }
}
