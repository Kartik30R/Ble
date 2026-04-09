package com.blesense.app.features.auth.presentation.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blesense.app.R
import com.blesense.app.coreui.constants.AppStrings
import com.blesense.app.coreui.theme.*
import com.blesense.app.coreui.components.NeonPillButton

@Composable
fun AnimatedFirstScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onGuestSignIn: () -> Unit
) {
    // Everything wrapped in your centralized theme logic
    BleSenseTheme {
        // Animations 
        val backgroundScale = remember { Animatable(0f) }
        val contentAlpha = remember { Animatable(0f) }

        LaunchedEffect(Unit) {
            backgroundScale.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
            contentAlpha.animateTo(1f, tween(1000, easing = LinearEasing))
        }

        var isLoading by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .neumorphicBackground()
        ) {
            // ---- Animated background glow ----
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = backgroundScale.value,
                        scaleY = backgroundScale.value,
                        transformOrigin = TransformOrigin(0f, 1f)
                    )
                    .clip(GenericShape { size, _ ->
                        val path = Path().apply {
                            moveTo(0f, size.height * 0.9f)
                            quadraticBezierTo(
                                size.width * 0.1f, size.height * 0.62f,
                                size.width * 0.55f, size.height * 0.55f
                            )
                            quadraticBezierTo(
                                size.width * 1f, size.height * 0.47f,
                                size.width, size.height * 0.4f
                            )
                            lineTo(size.width, 0f)
                            lineTo(0f, 0f)
                            close()
                        }
                        addPath(path)
                    })
                    .background(MintGreenAccent.copy(alpha = 0.03f))
                    // apply blur if possible to soften the shape
                    .let {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            it.blur(50.dp)
                        } else it
                    }
            )

            // ---- Main content ----
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .alpha(contentAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // ---- Icon ----
                Image(
                    painter = painterResource(id = R.drawable.bg_remove_ble),
                    contentDescription = "App Icon",
                    modifier = Modifier.size(200.dp)
                )

                // ---- App name ----
                Text(
                    text = AppStrings.APP_NAME,
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 120.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                /* -------- Action Buttons -------- */
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ---- Login button ----
                    NeonPillButton(
                        text = AppStrings.LOGIN,
                        onClick = onNavigateToLogin,
                        isActive = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    )

                    // ---- Sign-up button ----
                    NeonPillButton(
                        text = AppStrings.SIGN_UP,
                        onClick = onNavigateToSignup,
                        isActive = false, // renders inactive dark button
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ---- OR text ----
                Text(
                    text = "OR",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                // ---- Guest / Loading ----
                if (isLoading) {
                    LoadingAnimation(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Continue as Guest", // Note: Add this to AppStrings later!
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MintGreenAccent,
                        modifier = Modifier
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                isLoading = true
                                onGuestSignIn()
                            }
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier,
    color: Color
) {
    val infinite = rememberInfiniteTransition(label = "loading")
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rotation"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier
                .graphicsLayer { rotationZ = rotation }
                .size(32.dp),
            color = color,
            strokeWidth = 3.dp
        )
    }
}