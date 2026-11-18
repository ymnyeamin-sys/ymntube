package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AmberSoft
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.SurfaceLevel1
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ApiSetupBanner(
    missingKeyName: String,
    description: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, AmberWarn.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
        color = SurfaceLevel1,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = AmberSoft,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Key,
                        contentDescription = null,
                        tint = AmberWarn,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "${stringResource(R.string.api_setup_title)}: $missingKeyName",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Configure via AI Studio Secrets Panel",
                        style = MaterialTheme.typography.labelMedium,
                        color = AmberWarn
                    )
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Surface(
                color = AmberSoft.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, OutlineSubtle, RoundedCornerShape(10.dp))
            ) {
                Text(
                    text = "Secret name: $missingKeyName",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }

            if (onRetry != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonAccent,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Retry Connection")
                }
            }
        }
    }
}
