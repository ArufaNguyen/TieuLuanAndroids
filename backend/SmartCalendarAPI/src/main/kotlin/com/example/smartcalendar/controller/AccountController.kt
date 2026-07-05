package com.example.smartcalendar.controller

import com.example.smartcalendar.common.ApiResponse
import com.example.smartcalendar.dto.account.request.CreateAccountRequest
import com.example.smartcalendar.dto.account.request.UpdateAccountRequest
import com.example.smartcalendar.dto.account.response.AccountResponse
import com.example.smartcalendar.dto.account.response.AccountResponseDetail
import com.example.smartcalendar.service.AccountService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/accounts")
class AccountController(private val accountService: AccountService) {

    @GetMapping
    fun getAccounts(@RequestParam(required = false) keyword: String?): ApiResponse<List<AccountResponse>> =
        accountService.getAccounts(keyword)

    @GetMapping("/{id}")
    fun getAccountById(@PathVariable id: Int): ApiResponse<AccountResponseDetail> =
        accountService.getAccountById(id)

    @PostMapping
    fun createAccount(@Valid @RequestBody request: CreateAccountRequest): ApiResponse<AccountResponseDetail> =
        accountService.createAccount(request)

    @PutMapping("/{id}")
    fun updateAccount(
        @PathVariable id: Int,
        @Valid @RequestBody request: UpdateAccountRequest
    ): ApiResponse<AccountResponseDetail> = accountService.updateAccount(id, request)

    @DeleteMapping("/{id}")
    fun deleteAccount(@PathVariable id: Int): ApiResponse<String> = accountService.deleteAccount(id)
}
