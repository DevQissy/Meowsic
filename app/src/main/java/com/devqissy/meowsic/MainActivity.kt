package com.devqissy.meowsic

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.devqissy.meowsic.data.viewmodel.MusicViewModel
import com.devqissy.meowsic.ui.navigation.NavGraph
import com.devqissy.meowsic.ui.theme.LocalAppThemeColors
import com.devqissy.meowsic.ui.theme.NavaTheme
import com.devqissy.meowsic.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val pendingExternalUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingExternalUri.value = extractAudioUri(intent)

        setContent {
            NavaTheme {
                val navController = rememberNavController()
                val viewModel: MusicViewModel = hiltViewModel()

                LaunchedEffect(pendingExternalUri.value) {
                    pendingExternalUri.value?.let { uri ->
                        viewModel.playExternalUri(uri)
                        pendingExternalUri.value = null
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val deleteLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartIntentSenderForResult()
                    ) { result ->
                        if (result.resultCode == RESULT_OK) {
                            viewModel.onDeleteConfirmed()
                        } else {
                            viewModel.onDeleteCancelled()
                        }
                    }

                    LaunchedEffect(Unit) {
                        viewModel.deletePermissionRequest.collect { pendingIntent ->
                            deleteLauncher.launch(
                                IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                            )
                        }
                    }
                }

                RequestPermissions(viewModel = viewModel) {
                    NavGraph(navController = navController, viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingExternalUri.value = extractAudioUri(intent)
    }

    private fun extractAudioUri(intent: Intent?): Uri? {
        if (intent == null) return null
        if (intent.action != Intent.ACTION_VIEW) return null
        return intent.data
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestPermissions(
    viewModel: MusicViewModel,
    content: @Composable () -> Unit
) {
    val themeColors = LocalAppThemeColors.current
    val context = LocalContext.current

    val readAudioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_AUDIO
    else
        Manifest.permission.READ_EXTERNAL_STORAGE

    val notificationPermission = Manifest.permission.POST_NOTIFICATIONS

    val readAudioPermissionState = rememberPermissionState(readAudioPermission)
    val notificationPermissionState = rememberPermissionState(notificationPermission)

    var step by remember { mutableIntStateOf(0) }
    var isNotificationGranted by remember { mutableStateOf(false) }
    var isReadAudioGranted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (step == 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (!notificationPermissionState.status.isGranted) {
                    notificationPermissionState.launchPermissionRequest()
                } else {
                    isNotificationGranted = true
                    step = 1
                }
            } else {
                isNotificationGranted = true
                step = 1
            }
        }
    }

    LaunchedEffect(notificationPermissionState.status.isGranted) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (notificationPermissionState.status.isGranted && !isNotificationGranted) {
                isNotificationGranted = true
                step = 1
            }
        }
    }

    LaunchedEffect(step) {
        if (step == 1 && !readAudioPermissionState.status.isGranted) {
            readAudioPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(readAudioPermissionState.status.isGranted) {
        if (readAudioPermissionState.status.isGranted && !isReadAudioGranted) {
            isReadAudioGranted = true
            step = 2
            viewModel.reloadAll()
        }
    }

    val canShowContent = readAudioPermissionState.status.isGranted

    if (canShowContent) {
        content()
        return
    }

    val isPermanentlyDenied = !readAudioPermissionState.status.shouldShowRationale &&
            step >= 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        themeColors.gradientMiddle,
                        themeColors.background,
                        themeColors.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .wrapContentSize(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(28.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = themeColors.neonPurple.copy(alpha = 0.12f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = stringResource(R.string.music_note_icon),
                        tint = themeColors.neonPurple,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = when {
                    isPermanentlyDenied -> stringResource(R.string.permission_denied_permanently)
                    step == 0 -> stringResource(R.string.notification_permission_required)
                    else -> stringResource(R.string.permission_required)
                },
                color = themeColors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isPermanentlyDenied) {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } else {
                        when (step) {
                            0 -> notificationPermissionState.launchPermissionRequest()
                            else -> readAudioPermissionState.launchPermissionRequest()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeColors.neonPurple
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(52.dp)
            ) {
                Text(
                    text = when {
                        isPermanentlyDenied -> stringResource(R.string.open_settings)
                        step == 0 -> stringResource(R.string.permission_grant_notification)
                        else -> stringResource(R.string.permission_grant)
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}