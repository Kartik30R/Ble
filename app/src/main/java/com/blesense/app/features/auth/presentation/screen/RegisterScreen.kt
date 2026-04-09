@file:OptIn(ExperimentalMaterial3Api::class)

package com.blesense.app.features.auth.presentation.screen

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blesense.app.* 
import com.blesense.app.core.common.google.GoogleSignInHelper
import com.blesense.app.coreui.theme.*
import com.blesense.app.coreui.components.*
import com.blesense.app.features.auth.presentation.viewmodel.AuthViewModel
import com.blesense.app.features.auth.presentation.viewmodel.AuthState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit
) {

    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()

    // Glass theme colors
    val backgroundColor = DarkGradientStart
    val textColor = TextPrimary
    val secondaryTextColor = TextSecondary
    val textFieldBackgroundColor = GlassSurfaceColor
    val buttonBackgroundColor = MintGreenAccent
    val buttonTextColor = DarkGradientStart
    val dividerColor = GlassBorderColor
    val borderColor = GlassBorderColor

    // Form State
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Validation State
    val isUsernameValid = remember(username) { username.length >= 4 }
    val isEmailValid = remember(email) { email.contains("@") && email.contains(".") }
    val isPasswordValid = remember(password) { password.length >= 8 }
    val isConfirmPasswordValid = remember(password, confirmPassword) { password == confirmPassword && confirmPassword.isNotEmpty() }

    // Google Sign-In Setup
    val googleSignInClient = remember { GoogleSignInHelper.getGoogleSignInClient(context) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { viewModel.loginWithGoogle(it) }
            } catch (e: ApiException) {
                Toast.makeText(context, "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Auth Navigation Logic
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) onNavigateToHome()
        if (authState is AuthState.Error) {
            Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_LONG).show()
        }
    }

    if (authState is AuthState.Loading) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = { },
            title = { Text("Creating Account", color = textColor) },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = buttonBackgroundColor)
                }
            },
            containerColor = DarkGradientStart
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        Text("Create Account", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = textColor)
        Text("Sign up to get started", fontSize = 17.sp, color = secondaryTextColor, modifier = Modifier.padding(top = 8.dp))
        Spacer(modifier = Modifier.height(60.dp))

        // Username
        AuthTextField(
            value = username,
            onValueChange = { username = it },
            placeholder = "Username",
            isError = username.isNotEmpty() && !isUsernameValid,
            backgroundColor = textFieldBackgroundColor,
            textColor = textColor,
            borderColor = borderColor,
            activeColor = buttonBackgroundColor
        )

        // Email
        AuthTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = "Email",
            isError = email.isNotEmpty() && !isEmailValid,
            keyboardType = KeyboardType.Email,
            backgroundColor = textFieldBackgroundColor,
            textColor = textColor,
            borderColor = borderColor,
            activeColor = buttonBackgroundColor
        )

        // Password
        AuthTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Password",
            isError = password.isNotEmpty() && !isPasswordValid,
            isPassword = true,
            passwordVisible = passwordVisible,
            onToggleVisibility = { passwordVisible = !passwordVisible },
            backgroundColor = textFieldBackgroundColor,
            textColor = textColor,
            borderColor = borderColor,
            activeColor = buttonBackgroundColor
        )

        // Confirm Password
        AuthTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            placeholder = "Confirm Password",
            isError = confirmPassword.isNotEmpty() && !isConfirmPasswordValid,
            isPassword = true,
            passwordVisible = confirmPasswordVisible,
            onToggleVisibility = { confirmPasswordVisible = !confirmPasswordVisible },
            backgroundColor = textFieldBackgroundColor,
            textColor = textColor,
            borderColor = borderColor,
            activeColor = buttonBackgroundColor
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.register(email, password) },
            enabled = isUsernameValid && isEmailValid && isPasswordValid && isConfirmPasswordValid,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = buttonBackgroundColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Create Account", color = buttonTextColor, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Social Login
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f), color = dividerColor)
            Text("Or continue with", Modifier.padding(horizontal = 16.dp), color = secondaryTextColor, fontSize = 15.sp)
            HorizontalDivider(Modifier.weight(1f), color = dividerColor)
        }

        Spacer(modifier = Modifier.height(24.dp))

         SocialLoginButton(
             icon = android.R.drawable.ic_menu_info_details,
            onClick = { launcher.launch(googleSignInClient.signInIntent) },
            backgroundColor = textFieldBackgroundColor,
            borderColor = borderColor
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(Modifier.padding(bottom = 32.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Already have an account?", color = textColor, fontSize = 15.sp)
            TextButton(onClick = onNavigateToLogin) {
                Text("Login Now", color = buttonBackgroundColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isError: Boolean,
    backgroundColor: Color,
    textColor: Color,
    borderColor: Color,
    activeColor: Color,
     keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onToggleVisibility: () -> Unit = {}
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        placeholder = { Text(placeholder, color = Color.Gray) },
        isError = isError,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = textColor.copy(alpha = 0.7f) // Matches your theme
                    )
                }
            }
        } else null,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = backgroundColor,
            unfocusedContainerColor = backgroundColor,
            errorContainerColor = backgroundColor,
            focusedIndicatorColor = activeColor,
            unfocusedIndicatorColor = borderColor,
            focusedTextColor = textColor,
            unfocusedTextColor = textColor
        ),
        shape = RoundedCornerShape(12.dp)
    )
}