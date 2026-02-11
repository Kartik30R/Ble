@file:OptIn(ExperimentalMaterial3Api::class)

package com.blesense.app

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.blesense.app.core.common.google.GoogleSignInHelper
 import com.blesense.app.features.auth.presentation.viewmodel.AuthState
import com.blesense.app.features.auth.presentation.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException


@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    // 🔑 Theme (single source of truth)
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val shapes = MaterialTheme.shapes

    // UI State
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }

    val isFormValid by remember(email, password) {
        derivedStateOf { isValidEmail(email) && isValidPassword(password) }
    }

    // ---------------- Google Sign-In ----------------

    val googleSignInClient = remember {
        GoogleSignInHelper.getGoogleSignInClient(context)
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { viewModel.loginWithGoogle(it) }
            } catch (e: ApiException) {
                errorMessage = "Google Sign-In failed"
                showErrorDialog = true
            }
        } else {
            errorMessage = "Google Sign-In cancelled"
            showErrorDialog = true
        }
    }

    // ---------------- Auth State ----------------

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> onNavigateToHome()
            is AuthState.Error -> {
                errorMessage = (authState as AuthState.Error).message
                showErrorDialog = true
            }
            else -> Unit
        }
    }

    // ---------------- UI ----------------

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(Modifier.height(60.dp))

        Text(
            text = "Welcome Back",
            style = typography.headlineLarge,
            color = colors.onBackground
        )

        Text(
            text = "Sign in to continue",
            style = typography.titleMedium,
            color = colors.onBackground.copy(alpha = 0.7f)
        )

        Spacer(Modifier.height(80.dp))

        EmailTextField(
            email = email,
            onEmailChange = { email = it },
            isError = !isValidEmail(email) && email.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Email",
            invalidMessage = "Invalid email",

        )

        Spacer(Modifier.height(12.dp))

        PasswordTextField(
            password = password,
            onPasswordChange = { password = it },
            passwordVisible = passwordVisible,
            onPasswordVisibilityChange = { passwordVisible = it },
            isError = !isValidPassword(password) && password.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Password",
            invalidMessage = "Min 8 characters",

        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { viewModel.login(email.trim(), password) },
            enabled = isFormValid && authState !is AuthState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = shapes.medium
        ) {
            Text("Sign In")
        }

        Spacer(Modifier.height(40.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f))
            Text(
                "Or continue with",
                Modifier.padding(12.dp),
                style = typography.labelLarge,
                color = colors.onBackground.copy(alpha = 0.6f)
            )
            HorizontalDivider(Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = { launcher.launch(googleSignInClient.signInIntent) },
            modifier = Modifier.size(64.dp),
            shape = shapes.medium
        ) {
            Icon(
                painter = painterResource(id = R.drawable.google_g),
                contentDescription = null,
                tint = Color.Unspecified
            )
        }

        Spacer(Modifier.weight(1f))

        TextButton(onClick = onNavigateToRegister) {
            Text("Create account")
        }
    }

    // ---------------- Error Dialog ----------------

    if (showErrorDialog && errorMessage != null) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Error") },
            text = { Text(errorMessage!!) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}


// ---------------- Helpers ----------------
@Composable
fun SocialLoginButton(
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    backgroundColor: Color,
    borderColor: Color
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = backgroundColor
        ),
        border = BorderStroke(1.dp, borderColor),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = "Google Login",
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified // Important: Keeps the Google "G" original colors
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Google",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (backgroundColor == Color.White) Color.Black else Color.White
                )
            )
        }
    }
}


@Composable
private fun PasswordTextField(
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: (Boolean) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
    placeholder: String,
    invalidMessage: String
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val shapes = MaterialTheme.shapes

    TextField(
        value = password,
        onValueChange = onPasswordChange,
        modifier = modifier,
        placeholder = {
            Text(
                text = placeholder,
                color = colors.onSurface.copy(alpha = 0.6f)
            )
        },
        visualTransformation =
            if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        isError = isError,
        supportingText = {
            if (isError) {
                Text(
                    text = invalidMessage,
                    color = colors.error,
                    style = typography.labelLarge
                )
            }
        },
        trailingIcon = {
            IconButton(
                onClick = { onPasswordVisibilityChange(!passwordVisible) }
            ) {
                Icon(
                    painter = painterResource(
                        id = if (passwordVisible)
                            R.drawable.invisible
                        else
                            R.drawable.show
                    ),
                    contentDescription =
                        if (passwordVisible) "Hide password" else "Show password",
                    tint = colors.onSurface
                )
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
            disabledContainerColor = colors.surface,
            focusedIndicatorColor = colors.primary,
            unfocusedIndicatorColor = colors.outline,
            errorIndicatorColor = colors.error,
            focusedTextColor = colors.onSurface,
            unfocusedTextColor = colors.onSurface
        ),
        shape = shapes.medium,
        textStyle = typography.bodyMedium
    )
}

@Composable
private fun EmailTextField(
    email: String,
    onEmailChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
    placeholder: String,
    invalidMessage: String
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val shapes = MaterialTheme.shapes

    TextField(
        value = email,
        onValueChange = onEmailChange,
        modifier = modifier,
        placeholder = {
            Text(
                text = placeholder,
                color = colors.onSurface.copy(alpha = 0.6f)
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        ),
        isError = isError,
        supportingText = {
            if (isError) {
                Text(
                    text = invalidMessage,
                    color = colors.error,
                    style = typography.labelLarge
                )
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
            disabledContainerColor = colors.surface,
            focusedIndicatorColor = colors.primary,
            unfocusedIndicatorColor = colors.outline,
            errorIndicatorColor = colors.error,
            focusedTextColor = colors.onSurface,
            unfocusedTextColor = colors.onSurface
        ),
        shape = shapes.medium,
        textStyle = typography.bodyMedium
    )
}

private fun isValidEmail(email: String): Boolean =
    email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

private fun isValidPassword(password: String): Boolean =
    password.length >= 8

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
 }
