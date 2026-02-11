package com.blesense.app.features.settings.presentation.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.blesense.app.R
import com.blesense.app.coreui.theme.Helvetica
 import kotlin.random.Random

@Composable
fun UserProfileCard(
    cardBackground: Color,
    textColor: Color,
    secondaryTextColor: Color,
    iconTint: Color,
    userName: String,
    userEmail: String,
    profilePictureUrl: String? = null,
    onLogout: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = cardBackground
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            /* -------- Avatar -------- */

            if (!profilePictureUrl.isNullOrBlank()) {
                AsyncImage(
                    model = profilePictureUrl,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape),
                    error = painterResource(R.drawable.error),
                    contentScale = ContentScale.Crop
                )
            } else {
                val avatarColor = remember { generateRandomColor() }
                val initials = userName.take(2).uppercase()

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = Helvetica
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            /* -------- Name + Email -------- */

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = Helvetica
                    ),
                    color = textColor
                )

                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Helvetica
                    ),
                    color = secondaryTextColor
                )
            }

            /* -------- Logout -------- */

            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = "Logout",
                    tint = iconTint
                )
            }
        }
    }
}

fun generateRandomColor(): Color {
    val random = Random
    return Color(
        red = random.nextInt(256),
        green = random.nextInt(256),
        blue = random.nextInt(256)
    ).copy(alpha = 0.8f)
}
