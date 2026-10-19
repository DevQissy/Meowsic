package com.devqissy.meowsic.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.widget.Toast
import androidx.core.net.toUri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.devqissy.meowsic.BuildConfig
import com.devqissy.meowsic.R
import com.devqissy.meowsic.data.preferences.PrefsFiles
import com.devqissy.meowsic.service.ScreenOffCapability
import com.devqissy.meowsic.service.ScreenOffSelfTest
import com.devqissy.meowsic.service.VolumeKeyPrefs
import com.devqissy.meowsic.service.VolumeKeyService
import com.devqissy.meowsic.ui.components.AccentDivider
import com.devqissy.meowsic.ui.components.AmbientGlow
import com.devqissy.meowsic.ui.components.GlassCard
import com.devqissy.meowsic.ui.components.GlassCircleIconButton
import com.devqissy.meowsic.ui.components.SectionTitle
import com.devqissy.meowsic.ui.theme.AppThemeColors
import com.devqissy.meowsic.ui.theme.GlassShapes
import com.devqissy.meowsic.ui.theme.LocalAppThemeColors
import com.devqissy.meowsic.ui.theme.ThemeMode
import com.devqissy.meowsic.ui.theme.glassPanelColor
import com.devqissy.meowsic.ui.theme.glassSheen
import com.devqissy.meowsic.ui.theme.glassSurface
import com.devqissy.meowsic.util.openMyketForRating
import com.devqissy.meowsic.ui.components.glassSpecular

class SettingsManager(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PrefsFiles.SETTINGS, Context.MODE_PRIVATE)

    val prefsRef: SharedPreferences get() = prefs

    var themeMode: ThemeMode
        get() = when (prefs.getString(PrefsFiles.KEY_THEME_MODE, "system")) {
            "dark" -> ThemeMode.DARK
            "light" -> ThemeMode.LIGHT
            else -> ThemeMode.SYSTEM
        }
        set(value) = prefs.edit()
            .putString(PrefsFiles.KEY_THEME_MODE, value.name.lowercase())
            .apply()
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val themeColors = LocalAppThemeColors.current
    val accent = themeColors.primary
    val supportEmail = stringResource(R.string.support_email)
    val supportSubject = stringResource(R.string.support_email_subject)

    var themeMode by remember { mutableStateOf(settingsManager.themeMode) }

    DisposableEffect(settingsManager) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PrefsFiles.KEY_THEME_MODE) {
                themeMode = settingsManager.themeMode
            }
        }
        settingsManager.prefsRef.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            settingsManager.prefsRef.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val setMode: (ThemeMode) -> Unit = { mode ->
        themeMode = mode
        settingsManager.themeMode = mode
    }

    var volumeKeyEnabled by remember { mutableStateOf(VolumeKeyPrefs.isEnabled(context)) }
    var accessibilityGranted by remember {
        mutableStateOf(VolumeKeyService.isAccessibilityEnabled(context))
    }
    var showScreenOffDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityGranted = VolumeKeyService.isAccessibilityEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val accessibilityHint = stringResource(R.string.volume_key_enable_accessibility_hint)

    val onVolumeKeyToggle: (Boolean) -> Unit = { enable ->
        VolumeKeyPrefs.setEnabled(context, enable)
        volumeKeyEnabled = enable
        if (enable) {
            if (!accessibilityGranted) {
                Toast.makeText(context, accessibilityHint, Toast.LENGTH_LONG).show()
                VolumeKeyService.openAccessibilitySettings(context)
            }
            showScreenOffDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        themeColors.gradientStart,
                        themeColors.gradientMiddle,
                        themeColors.gradientEnd
                    )
                )
            )
    ) {
        AmbientGlow(themeColors)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                )
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 10.dp)
                    .glassSurface(
                        shape = GlassShapes.header,
                        fillColor = themeColors.glassPanelColor,
                        borderColor = lerp(themeColors.glassBorder, accent, 0.30f),
                        sheenColor = themeColors.glassSheen
                    )
                    .glassSpecular(
                        shape = GlassShapes.header,
                        sheenColor = themeColors.glassSheen
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassCircleIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        onClick = onBack,
                        themeColors = themeColors
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = stringResource(R.string.settings_title),
                        color = themeColors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            SectionTitle(stringResource(R.string.appearance), themeColors)

            GlassCard(themeColors) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Brightness6,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.display_mode),
                            color = themeColors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeModeButton(
                            text = stringResource(R.string.system),
                            icon = Icons.Default.PhoneAndroid,
                            isSelected = themeMode == ThemeMode.SYSTEM,
                            onClick = { setMode(ThemeMode.SYSTEM) },
                            modifier = Modifier.weight(1f),
                            themeColors = themeColors
                        )
                        ThemeModeButton(
                            text = stringResource(R.string.dark),
                            icon = Icons.Default.DarkMode,
                            isSelected = themeMode == ThemeMode.DARK,
                            onClick = { setMode(ThemeMode.DARK) },
                            modifier = Modifier.weight(1f),
                            themeColors = themeColors
                        )
                        ThemeModeButton(
                            text = stringResource(R.string.light),
                            icon = Icons.Default.LightMode,
                            isSelected = themeMode == ThemeMode.LIGHT,
                            onClick = { setMode(ThemeMode.LIGHT) },
                            modifier = Modifier.weight(1f),
                            themeColors = themeColors
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionTitle(stringResource(R.string.playback_controls_section), themeColors)

            GlassCard(themeColors) {
                SettingsToggleRow(
                    icon = Icons.Default.VolumeUp,
                    title = stringResource(R.string.volume_key_nav_title),
                    subtitle = when {
                        volumeKeyEnabled && accessibilityGranted ->
                            stringResource(R.string.volume_key_status_active)
                        volumeKeyEnabled ->
                            stringResource(R.string.volume_key_status_needs_accessibility)
                        else ->
                            stringResource(R.string.volume_key_status_inactive)
                    },
                    subtitleColor = when {
                        volumeKeyEnabled && accessibilityGranted -> themeColors.primary
                        volumeKeyEnabled -> themeColors.neonPink
                        else -> themeColors.textSecondary
                    },
                    tileColor = themeColors.neonCyan,
                    isChecked = volumeKeyEnabled,
                    onCheckedChange = onVolumeKeyToggle,
                    themeColors = themeColors
                )

                Text(
                    text = stringResource(R.string.volume_key_nav_subtitle),
                    color = themeColors.textSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 64.dp, end = 16.dp, bottom = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionTitle(stringResource(R.string.about), themeColors)

            GlassCard(themeColors) {
                SettingsGradientRow(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.app_version),
                    value = BuildConfig.VERSION_NAME,
                    tileColor = themeColors.neonBlue,
                    themeColors = themeColors
                )

                AccentDivider(themeColors, modifier = Modifier.padding(start = 64.dp, end = 16.dp))

                SettingsGradientRow(
                    icon = Icons.Default.Code,
                    title = stringResource(R.string.developer),
                    value = stringResource(R.string.developer_name),
                    tileColor = themeColors.neonPurple,
                    themeColors = themeColors
                )

                AccentDivider(themeColors, modifier = Modifier.padding(start = 64.dp, end = 16.dp))

                SettingsGradientRow(
                    icon = Icons.Default.Star,
                    title = stringResource(R.string.menu_rate),
                    value = stringResource(R.string.rate_subtitle),
                    tileColor = themeColors.neonPink,
                    themeColors = themeColors,
                    onClick = { openMyketForRating(context) }
                )

                AccentDivider(themeColors, modifier = Modifier.padding(start = 64.dp, end = 16.dp))

                SettingsGradientRow(
                    icon = Icons.Default.Email,
                    title = stringResource(R.string.support),
                    value = stringResource(R.string.support_subtitle),
                    tileColor = themeColors.neonCyan,
                    themeColors = themeColors,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = "mailto:$supportEmail".toUri()
                            putExtra(Intent.EXTRA_SUBJECT, supportSubject)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(
                                context,
                                R.string.no_email_app,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    if (showScreenOffDialog) {
        val status = ScreenOffCapability.getStatus(context)
        val selfTestStatus by ScreenOffSelfTest.status.collectAsState()

        AlertDialog(
            onDismissRequest = {
                showScreenOffDialog = false
                ScreenOffSelfTest.reset()
            },
            title = { Text(stringResource(R.string.screen_off_dialog_title)) },
            text = {
                Column {
                    when (status) {
                        is ScreenOffCapability.Status.Unsupported -> {
                            Text(
                                stringResource(
                                    R.string.screen_off_unsupported,
                                    Build.VERSION.RELEASE
                                )
                            )
                        }
                        is ScreenOffCapability.Status.NeedsAod -> {
                            if (status.aodEnabled) {
                                Text(stringResource(R.string.screen_off_aod_active))
                            } else {
                                Text(stringResource(R.string.screen_off_aod_disabled))
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(
                                    onClick = {
                                        showScreenOffDialog = false
                                        ScreenOffCapability.openAodSettings(context)
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(stringResource(R.string.open_aod_settings))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            TextButton(
                                onClick = { ScreenOffSelfTest.start() },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(stringResource(R.string.screen_off_test_button))
                            }

                            when (selfTestStatus) {
                                ScreenOffSelfTest.Status.Waiting -> Text(
                                    stringResource(R.string.screen_off_test_waiting),
                                    color = accent,
                                    fontSize = 12.sp
                                )
                                ScreenOffSelfTest.Status.Supported -> Text(
                                    stringResource(R.string.screen_off_test_supported),
                                    color = themeColors.primary,
                                    fontSize = 12.sp
                                )
                                ScreenOffSelfTest.Status.NotSupported -> Text(
                                    stringResource(R.string.screen_off_test_failed),
                                    color = themeColors.neonPink,
                                    fontSize = 12.sp
                                )
                                ScreenOffSelfTest.Status.Idle -> {}
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showScreenOffDialog = false
                    ScreenOffSelfTest.reset()
                }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}

@Composable
private fun ThemeModeButton(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    themeColors: AppThemeColors
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "themeBtnScale"
    )

    val g1 by animateColorAsState(
        targetValue = if (isSelected) themeColors.primary else themeColors.glassWhiteStrong,
        animationSpec = tween(220),
        label = "themeBtnG1"
    )
    val g2 by animateColorAsState(
        targetValue = if (isSelected) {
            lerp(themeColors.primary, themeColors.gradientEnd, 0.85f)
        } else {
            themeColors.glassWhiteStrong
        },
        animationSpec = tween(220),
        label = "themeBtnG2"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            lerp(themeColors.primary, themeColors.gradientEnd, 0.5f)
        } else {
            lerp(themeColors.glassBorder, themeColors.primary, 0.30f)
        },
        animationSpec = tween(220),
        label = "themeBtnBorder"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) themeColors.onPrimary else themeColors.textPrimary,
        animationSpec = tween(220),
        label = "themeBtnContent"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (isSelected) 10.dp else 0.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = lerp(themeColors.primary, themeColors.gradientEnd, 0.5f)
                    .copy(alpha = 0.50f),
                ambientColor = Color.Transparent
            )
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(g1, g2)))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = text,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SettingsGradientRow(
    icon: ImageVector,
    title: String,
    value: String,
    tileColor: Color,
    themeColors: AppThemeColors,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "gradientRowScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = tileColor.copy(alpha = 0.45f),
                    ambientColor = Color.Transparent
                )
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            tileColor,
                            lerp(tileColor, Color.White, 0.30f)
                        )
                    )
                )
                .glassSpecular(
                    shape = RoundedCornerShape(12.dp),
                    sheenColor = Color.White.copy(alpha = 0.28f),
                    topLine = false
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = themeColors.onPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            title,
            color = themeColors.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        Text(
            value,
            color = if (onClick != null) {
                themeColors.primary
            } else {
                themeColors.textSecondary
            },
            fontSize = 13.sp,
            fontWeight = if (onClick != null) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    subtitleColor: Color,
    tileColor: Color,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    themeColors: AppThemeColors
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Switch }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onCheckedChange(!isChecked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = tileColor.copy(alpha = 0.45f),
                    ambientColor = Color.Transparent
                )
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(tileColor, lerp(tileColor, Color.White, 0.30f))
                    )
                )
                .glassSpecular(
                    shape = RoundedCornerShape(12.dp),
                    sheenColor = Color.White.copy(alpha = 0.28f),
                    topLine = false
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = themeColors.onPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = themeColors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = subtitleColor, fontSize = 11.sp)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = themeColors.onPrimary,
                checkedTrackColor = lerp(themeColors.primary, themeColors.gradientEnd, 0.5f),
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = themeColors.textSecondary,
                uncheckedTrackColor = themeColors.glassWhiteStrong,
                uncheckedBorderColor = themeColors.glassBorder
            )
        )
    }
}