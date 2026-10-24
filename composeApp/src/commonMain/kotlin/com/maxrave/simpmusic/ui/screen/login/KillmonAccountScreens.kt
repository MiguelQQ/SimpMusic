package com.maxrave.simpmusic.ui.screen.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.login.KillmonAccountDestination
import com.maxrave.simpmusic.ui.navigation.destination.login.KillmonLoginDestination
import com.maxrave.simpmusic.ui.navigation.destination.login.KillmonRegisterDestination
import com.maxrave.simpmusic.ui.theme.killmonBlack
import com.maxrave.simpmusic.ui.theme.killmonCyan
import com.maxrave.simpmusic.ui.theme.killmonGreen
import com.maxrave.simpmusic.ui.theme.killmonSurface
import com.maxrave.simpmusic.viewModel.KillmonAccountViewModel
import org.koin.compose.viewmodel.koinViewModel

private val fieldColors
    @Composable get() =
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = killmonCyan,
            unfocusedBorderColor = Color(0xFF3A3A3A),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = killmonGreen,
            focusedLabelColor = killmonCyan,
            unfocusedLabelColor = Color(0xFFB3B3B3),
            focusedContainerColor = killmonSurface,
            unfocusedContainerColor = killmonSurface,
            errorBorderColor = Color(0xFFFF5A5A),
            errorLabelColor = Color(0xFFFF8A8A),
            errorCursorColor = Color(0xFFFF5A5A),
        )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KillmonLoginScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    hideBottomNavigation: () -> Unit,
    showBottomNavigation: () -> Unit,
    viewModel: KillmonAccountViewModel = koinViewModel(),
) {
    HideBottomBar(hideBottomNavigation, showBottomNavigation)
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var loading by rememberSaveable { mutableStateOf(false) }

    KillmonScaffold(
        title = "Iniciar sesión",
        innerPadding = innerPadding,
        onBack = { navController.navigateUp() },
    ) {
        Text(
            "Entra para sincronizar idioma, tema y región. La cuenta es opcional.",
            color = Color(0xFFB3B3B3),
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(20.dp))
        KillmonField(email, { email = it }, "Correo", KeyboardType.Email)
        Spacer(Modifier.height(12.dp))
        KillmonField(password, { password = it }, "Contraseña", KeyboardType.Password, password = true)
        ErrorText(error)
        Spacer(Modifier.height(20.dp))
        KillmonButton("Iniciar sesión", loading) {
            loading = true
            error = null
            viewModel.login(email, password) { message ->
                loading = false
                if (message == null) openAccount(navController) else error = message
            }
        }
        TextButton(
            onClick = { navController.navigate(KillmonRegisterDestination) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Crear cuenta", color = killmonCyan, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KillmonRegisterScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    hideBottomNavigation: () -> Unit,
    showBottomNavigation: () -> Unit,
    viewModel: KillmonAccountViewModel = koinViewModel(),
) {
    HideBottomBar(hideBottomNavigation, showBottomNavigation)
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var loading by rememberSaveable { mutableStateOf(false) }

    KillmonScaffold(
        title = "Crear cuenta",
        innerPadding = innerPadding,
        onBack = { navController.navigateUp() },
    ) {
        Text(
            "La contraseña debe tener entre 8 y 128 caracteres.",
            color = Color(0xFFB3B3B3),
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(20.dp))
        KillmonField(name, { name = it }, "Nombre", KeyboardType.Text)
        Spacer(Modifier.height(12.dp))
        KillmonField(email, { email = it }, "Correo", KeyboardType.Email)
        Spacer(Modifier.height(12.dp))
        KillmonField(password, { password = it }, "Contraseña", KeyboardType.Password, password = true)
        ErrorText(error)
        Spacer(Modifier.height(20.dp))
        KillmonButton("Crear cuenta", loading) {
            loading = true
            error = null
            viewModel.register(name, email, password) { message ->
                loading = false
                if (message == null) openAccount(navController) else error = message
            }
        }
        TextButton(
            onClick = { navController.navigateUp() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Ya tengo cuenta", color = killmonCyan, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KillmonAccountScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    hideBottomNavigation: () -> Unit,
    showBottomNavigation: () -> Unit,
    viewModel: KillmonAccountViewModel = koinViewModel(),
) {
    HideBottomBar(hideBottomNavigation, showBottomNavigation)
    val session by viewModel.session.collectAsStateWithLifecycle()
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var deletePassword by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var loading by rememberSaveable { mutableStateOf(false) }

    KillmonScaffold(
        title = "Cuenta",
        innerPadding = innerPadding,
        onBack = { navController.navigateUp() },
    ) {
        val user = session?.user
        if (user == null) {
            Text("No has iniciado sesión.", color = Color.White)
            Spacer(Modifier.height(16.dp))
            KillmonButton("Iniciar sesión", false) {
                navController.navigate(KillmonLoginDestination) {
                    popUpTo(KillmonAccountDestination) { inclusive = true }
                }
            }
        } else {
            Text(user.displayName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(user.email, color = Color(0xFFB3B3B3), fontSize = 16.sp)
            ErrorText(error)
            Spacer(Modifier.height(28.dp))
            KillmonButton("Cerrar sesión", false) { confirmLogout = true }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Eliminar cuenta", color = Color(0xFFFF5A5A), fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            containerColor = Color(0xFF1E1E1E),
            title = { Text("Cerrar sesión", color = Color.White) },
            text = { Text("Se cierra la sesión solo en este dispositivo.", color = Color(0xFFB3B3B3)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmLogout = false
                        viewModel.logout { navController.navigateUp() }
                    },
                ) { Text("Cerrar sesión", color = killmonGreen) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Cancelar", color = Color.White) }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { if (!loading) confirmDelete = false },
            containerColor = Color(0xFF1E1E1E),
            title = { Text("Eliminar cuenta", color = Color.White) },
            text = {
                Column {
                    Text(
                        "Esta acción borra la cuenta de Killmon. Escribe tu contraseña para confirmar.",
                        color = Color(0xFFB3B3B3),
                    )
                    Spacer(Modifier.height(12.dp))
                    KillmonField(
                        deletePassword,
                        { deletePassword = it },
                        "Contraseña",
                        KeyboardType.Password,
                        password = true,
                    )
                    ErrorText(error)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !loading,
                    onClick = {
                        loading = true
                        error = null
                        viewModel.deleteAccount(deletePassword) { message ->
                            loading = false
                            if (message == null) {
                                confirmDelete = false
                                navController.navigateUp()
                            } else {
                                error = message
                            }
                        }
                    },
                ) { Text(if (loading) "Eliminando…" else "Eliminar", color = Color(0xFFFF5A5A)) }
            },
            dismissButton = {
                TextButton(enabled = !loading, onClick = { confirmDelete = false }) {
                    Text("Cancelar", color = Color.White)
                }
            },
        )
    }
}

@Composable
private fun HideBottomBar(
    hideBottomNavigation: () -> Unit,
    showBottomNavigation: () -> Unit,
) {
    LaunchedEffect(Unit) { hideBottomNavigation() }
    DisposableEffect(Unit) {
        onDispose { showBottomNavigation() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KillmonScaffold(
    title: String,
    innerPadding: PaddingValues,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(killmonBlack)
            .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding()),
    ) {
        TopAppBar(
            title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(SimpIcons.ArrowBackIosNew, contentDescription = "Atrás", tint = Color.White)
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = killmonBlack,
                    titleContentColor = Color.White,
                ),
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun KillmonField(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    keyboard: KeyboardType,
    password: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        colors = fieldColors,
    )
}

@Composable
private fun KillmonButton(
    text: String,
    loading: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(24.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = killmonGreen,
                contentColor = Color.Black,
                disabledContainerColor = killmonGreen.copy(alpha = 0.4f),
                disabledContentColor = Color.Black.copy(alpha = 0.6f),
            ),
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun ErrorText(error: String?) {
    if (!error.isNullOrBlank()) {
        Spacer(Modifier.height(12.dp))
        Text(error, color = Color(0xFFFF8A8A), fontSize = 14.sp)
    }
}

private fun openAccount(navController: NavController) {
    navController.navigate(KillmonAccountDestination) {
        popUpTo(KillmonLoginDestination) { inclusive = true }
    }
}
