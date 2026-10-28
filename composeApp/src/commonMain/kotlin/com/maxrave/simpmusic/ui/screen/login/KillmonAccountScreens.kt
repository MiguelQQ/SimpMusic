package com.maxrave.simpmusic.ui.screen.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import com.maxrave.simpmusic.expect.finishApp
import com.maxrave.simpmusic.expect.ui.PlatformBackHandler
import com.maxrave.simpmusic.ui.component.KillmonPrimaryButton
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
import com.maxrave.simpmusic.ui.navigation.destination.login.KillmonRegisterDestination
import com.maxrave.simpmusic.ui.theme.killmonBlack
import com.maxrave.simpmusic.ui.theme.killmonBrand
import com.maxrave.simpmusic.ui.theme.killmonBrandGradient
import com.maxrave.simpmusic.ui.theme.killmonBrandSecondary
import com.maxrave.simpmusic.ui.theme.killmonSurface
import com.maxrave.simpmusic.viewModel.KillmonAccountViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.app_icon
import simpmusic.composeapp.generated.resources.app_name
import simpmusic.composeapp.generated.resources.cd_app_icon

private val fieldColors
    @Composable get() =
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = killmonBrandSecondary(),
            unfocusedBorderColor = Color(0xFF3A3A3A),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = killmonBrand(),
            focusedLabelColor = killmonBrandSecondary(),
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
    LeaveAppWhenRoot(navController)
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var loading by rememberSaveable { mutableStateOf(false) }

    KillmonGate(
        innerPadding = innerPadding,
        onBack = { leaveGate(navController) },
    ) {
        Text(
            "Inicia sesión para continuar.",
            color = Color(0xFFB3B3B3),
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(20.dp))
        KillmonField(email, { email = it }, "Correo", KeyboardType.Email)
        Spacer(Modifier.height(12.dp))
        KillmonField(password, { password = it }, "Contraseña", KeyboardType.Password, password = true)
        ErrorText(error)
        Spacer(Modifier.height(20.dp))
        KillmonPrimaryButton("Iniciar sesión", loading) {
            loading = true
            error = null
            viewModel.login(email, password) { message ->
                loading = false
                if (message == null) navController.resetTo(HomeDestination) else error = message
            }
        }
        TextButton(
            onClick = { navController.navigate(KillmonRegisterDestination) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Crear cuenta", color = killmonBrandSecondary(), fontWeight = FontWeight.SemiBold)
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

    KillmonGate(
        innerPadding = innerPadding,
        onBack = { leaveGate(navController) },
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
        KillmonPrimaryButton("Crear cuenta", loading) {
            loading = true
            error = null
            viewModel.register(name, email, password) { message ->
                loading = false
                if (message == null) navController.resetTo(HomeDestination) else error = message
            }
        }
        TextButton(
            onClick = { leaveGate(navController) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Ya tengo cuenta", color = killmonBrandSecondary(), fontWeight = FontWeight.SemiBold)
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
    var logoutLoading by rememberSaveable { mutableStateOf(false) }
    var deleteLoading by rememberSaveable { mutableStateOf(false) }

    KillmonScaffold(
        title = "Cuenta Killmon",
        innerPadding = innerPadding,
        onBack = { navController.navigateUp() },
    ) {
        val user = session?.user
        if (user == null) {
            Box(Modifier.fillMaxSize().background(killmonBlack))
        } else {
            Text(user.displayName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(user.email, color = Color(0xFFB3B3B3), fontSize = 16.sp)
            ErrorText(error)
            Spacer(Modifier.height(28.dp))
            KillmonPrimaryButton("Cerrar sesión", false) { confirmLogout = true }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Eliminar cuenta", color = Color(0xFFFF5A5A), fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { if (!logoutLoading) confirmLogout = false },
            containerColor = Color(0xFF1E1E1E),
            title = { Text("Cerrar sesión", color = Color.White) },
            text = { Text("Se cierra la sesión solo en este dispositivo.", color = Color(0xFFB3B3B3)) },
            confirmButton = {
                KillmonPrimaryButton(
                    text = "Cerrar sesión",
                    loading = logoutLoading,
                    expanded = false,
                ) {
                    logoutLoading = true
                    viewModel.logout {
                        logoutLoading = false
                        confirmLogout = false
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !logoutLoading,
                    onClick = { confirmLogout = false },
                ) { Text("Cancelar", color = Color.White) }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { if (!deleteLoading) confirmDelete = false },
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
                KillmonPrimaryButton(
                    text = "Eliminar",
                    loading = deleteLoading,
                    expanded = false,
                ) {
                    deleteLoading = true
                    error = null
                    viewModel.deleteAccount(deletePassword) { message ->
                        deleteLoading = false
                        if (message == null) {
                            confirmDelete = false
                        } else {
                            error = message
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(enabled = !deleteLoading, onClick = { confirmDelete = false }) {
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
private fun ErrorText(error: String?) {
    if (!error.isNullOrBlank()) {
        Spacer(Modifier.height(12.dp))
        Text(error, color = Color(0xFFFF8A8A), fontSize = 14.sp)
    }
}

/** Replaces the whole back stack. Sign-in, sign-out and account deletion all land on one screen. */
fun NavController.resetTo(destination: Any) {
    navigate(destination) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun KillmonBootScreen() {
    Box(
        Modifier
            .fillMaxSize()
            .background(killmonBlack),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            KillmonBrand()
            Spacer(Modifier.height(28.dp))
            CircularProgressIndicator(color = killmonBrandSecondary(), strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun LeaveAppWhenRoot(navController: NavController) {
    PlatformBackHandler(enabled = isGateRoot(navController)) {
        finishApp()
    }
}

/** The graph itself sits under the start destination, so "nothing behind login" is that entry too. */
private fun isGateRoot(navController: NavController): Boolean {
    val previous = navController.previousBackStackEntry?.destination
    return previous == null || previous is NavGraph
}

private fun leaveGate(navController: NavController) {
    if (isGateRoot(navController) || !navController.popBackStack()) finishApp()
}

@Composable
private fun KillmonGate(
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
        IconButton(onClick = onBack) {
            Icon(SimpIcons.ArrowBackIosNew, contentDescription = "Atrás", tint = Color.White)
        }
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KillmonBrand()
            Spacer(Modifier.height(28.dp))
            content()
        }
    }
}

@Composable
private fun KillmonBrand() {
    androidx.compose.foundation.Image(
        painter = painterResource(Res.drawable.app_icon),
        contentDescription = stringResource(Res.string.cd_app_icon),
        modifier = Modifier.size(200.dp),
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(Res.string.app_name),
        style =
            TextStyle(
                brush = Brush.horizontalGradient(killmonBrandGradient()),
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
            ),
    )
}
