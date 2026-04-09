package com.blesense.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
 import com.blesense.app.coreui.constants.AppStrings
import com.blesense.app.features.auth.presentation.viewmodel.AuthViewModel
import com.blesense.app.coreui.theme.*
import kotlinx.coroutines.delay


@Composable
fun SplashScreen(
    viewModel: AuthViewModel,
    onNavigateToAuth: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    // Theme logic handled by modifiers now
    val textColor = TextPrimary

     LaunchedEffect(Unit) {
        delay(2000L)
        if (viewModel.isUserAuthenticated()) {
            onNavigateToHome()
        } else {
            onNavigateToAuth()
        }
    }

    // Animations
    val infiniteTransition = rememberInfiniteTransition()

    val imageScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val titleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .neumorphicBackground(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.bg_remove_2),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .graphicsLayer(scaleX = imageScale, scaleY = imageScale),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(40.dp))

            BasicText(
                text = AppStrings.APP_NAME,
                style = TextStyle(
                    fontSize = 40.sp,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                 ),
                modifier = Modifier.graphicsLayer(
                    scaleX = titleScale,
                    scaleY = titleScale
                )
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {

}
