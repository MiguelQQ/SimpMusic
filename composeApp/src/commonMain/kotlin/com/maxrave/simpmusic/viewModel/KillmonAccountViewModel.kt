package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.model.killmon.KillmonAuthResult
import com.maxrave.domain.repository.KillmonAccountRepository
import kotlinx.coroutines.launch

class KillmonAccountViewModel(
    private val repository: KillmonAccountRepository,
) : ViewModel() {
    val session = repository.session

    fun login(
        email: String,
        password: String,
        onResult: (String?) -> Unit,
    ) {
        val validation = validateEmail(email) ?: validatePassword(password)
        if (validation != null) {
            onResult(validation)
            return
        }
        viewModelScope.launch {
            onResult(resultMessage(repository.login(email, password)))
        }
    }

    fun register(
        displayName: String,
        email: String,
        password: String,
        onResult: (String?) -> Unit,
    ) {
        val name = displayName.trim()
        val validation =
            when {
                name.isEmpty() -> "Escribe un nombre."
                name.length > 64 -> "El nombre es demasiado largo."
                else -> validateEmail(email) ?: validatePassword(password)
            }
        if (validation != null) {
            onResult(validation)
            return
        }
        viewModelScope.launch {
            onResult(resultMessage(repository.register(email, password, name)))
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            onDone()
        }
    }

    fun deleteAccount(
        password: String,
        onResult: (String?) -> Unit,
    ) {
        val validation = validatePassword(password)
        if (validation != null) {
            onResult(validation)
            return
        }
        viewModelScope.launch {
            onResult(resultMessage(repository.deleteAccount(password)))
        }
    }

    private fun resultMessage(result: KillmonAuthResult): String? =
        when (result) {
            KillmonAuthResult.Success -> null
            is KillmonAuthResult.Error -> result.message
        }

    private fun validateEmail(email: String): String? {
        val value = email.trim()
        if (value.isEmpty()) return "Escribe tu correo."
        if (!value.contains('@') || !value.substringAfter('@').contains('.')) return "El correo no es válido."
        return null
    }

    private fun validatePassword(password: String): String? =
        when {
            password.isEmpty() -> "Escribe tu contraseña."
            password.length < 8 || password.length > 128 -> "La contraseña debe tener entre 8 y 128 caracteres."
            else -> null
        }
}
