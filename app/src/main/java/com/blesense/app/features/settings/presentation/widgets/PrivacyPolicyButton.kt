package com.blesense.app.features.settings.presentation.widgets

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyPolicyButton() {
    val context = LocalContext.current

    Button(
        onClick = {
            val urlIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://sumankumar891.github.io/privacy_policy_blesense/")
            )
            context.startActivity(urlIntent)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text("Privacy Policy")
    }
}
