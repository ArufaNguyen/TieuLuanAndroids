package com.example.smartcalendar.service

import com.example.smartcalendar.common.ApiResponse
import com.example.smartcalendar.dto.account.request.CreateAccountRequest
import com.example.smartcalendar.dto.account.request.UpdateAccountRequest
import com.example.smartcalendar.dto.account.response.AccountResponse
import com.example.smartcalendar.dto.account.response.AccountResponseDetail
import com.example.smartcalendar.dto.user.response.UserResponse
import com.example.smartcalendar.model.Account
import com.example.smartcalendar.repository.AccountRepository
import com.example.smartcalendar.repository.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val userRepository: UserRepository
) {

    fun getAccounts(keyword: String?): ApiResponse<List<AccountResponse>> {
        val accounts = accountRepository.searchAccounts(keyword?.takeIf { it.isNotBlank() })
        return ApiResponse.success(accounts.map(::toResponse))
    }

    fun getAccountById(id: Int): ApiResponse<AccountResponseDetail> {
        val account = accountRepository.findById(id).orElse(null) ?: return ApiResponse.notFound("account not found")
        return ApiResponse.success(toDetail(account))
    }

    fun createAccount(request: CreateAccountRequest): ApiResponse<AccountResponseDetail> {
        val userId = request.userId ?: return ApiResponse.badRequest("userId is required")
        val loginName = request.loginName?.takeIf { it.isNotBlank() } ?: request.username

        if (accountRepository.existsByUsername(request.username)) return ApiResponse.conflict("account username already exists")
        if (accountRepository.existsByLoginName(loginName)) return ApiResponse.conflict("login name already exists")
        if (accountRepository.existsByUserId(userId)) return ApiResponse.conflict("user already has an account")

        val user = userRepository.findById(userId).orElse(null) ?: return ApiResponse.notFound("user not found")
        val account = accountRepository.save(
            Account(username = request.username, loginName = loginName, password = request.password, user = user)
        )
        return ApiResponse.created(toDetail(account))
    }

    fun updateAccount(id: Int, request: UpdateAccountRequest): ApiResponse<AccountResponseDetail> {
        val account = accountRepository.findById(id).orElse(null) ?: return ApiResponse.notFound("account not found")
        val userId = request.userId ?: return ApiResponse.badRequest("userId is required")
        val loginName = request.loginName?.takeIf { it.isNotBlank() } ?: request.username

        if (accountRepository.existsByUsernameAndIdNot(request.username, id)) return ApiResponse.conflict("account username already exists")
        if (accountRepository.existsByLoginNameAndIdNot(loginName, id)) return ApiResponse.conflict("login name already exists")
        if (accountRepository.existsByUserIdAndIdNot(userId, id)) return ApiResponse.conflict("user already has an account")

        val user = userRepository.findById(userId).orElse(null) ?: return ApiResponse.notFound("user not found")
        account.username = request.username
        account.loginName = loginName
        account.user = user
        request.password?.takeIf { it.isNotBlank() }?.let { account.password = it }
        return ApiResponse.success(toDetail(accountRepository.save(account)))
    }

    fun deleteAccount(id: Int): ApiResponse<String> {
        if (!accountRepository.existsById(id)) return ApiResponse.notFound("account not found")
        return try {
            accountRepository.deleteById(id)
            ApiResponse.success("account deleted successfully")
        } catch (_: DataIntegrityViolationException) {
            ApiResponse.conflict("account cannot be deleted because it is used by another record")
        }
    }

    private fun toResponse(account: Account) = AccountResponse(
        account.id, account.username, account.loginName, account.user?.id ?: 0, account.createdAt
    )

    private fun toDetail(account: Account) = AccountResponseDetail(
        id = account.id,
        username = account.username,
        loginName = account.loginName,
        createdAt = account.createdAt,
        user = account.user?.let { UserResponse(it.id, it.username, it.email, it.fullName) }
    )

}
