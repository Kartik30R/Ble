package com.blesense.app.app

import android.app.Activity
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.blesense.app.*
import com.blesense.app.Presentation.AdvertisingDataScreen
 import com.blesense.app.Presentation.MainScreen
import com.blesense.app.core.di.BluetoothModule
import com.blesense.app.core.presentation.IntermediateScreen
import com.blesense.app.features.auth.presentation.screen.AnimatedFirstScreen
import com.blesense.app.features.auth.presentation.viewmodel.AuthState
import com.blesense.app.features.auth.presentation.viewmodel.AuthViewModel
import com.blesense.app.features.auth.presentation.viewmodel.AuthViewModelFactory
import com.blesense.app.features.auth.presentation.screen.RegisterScreen
import com.blesense.app.features.bluetooth.presentation.screens.dataLogger.DataLoggerScreen
import com.blesense.app.features.bluetooth.presentation.screens.graph.ChartScreen
import com.blesense.app.features.remote.RobotControlScreen
import com.blesense.app.features.settings.presentation.screens.ModernSettingsScreen
import presentation.viewmodel.BluetoothScanViewModel

@Composable
fun AppNavigation(navController: NavHostController) {
    // Auth logic remains as is...
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory())
    val authState by authViewModel.authState.collectAsState()

    // Bluetooth ViewModel setup
    val context = LocalContext.current
    val activity = context as ComponentActivity

    // Binding the ViewModel using your BluetoothModule DI
    val bluetoothViewModel: BluetoothScanViewModel = activity.viewModels<BluetoothScanViewModel> {
        BluetoothModule.bluetoothScanViewModelFactory()
    }.value

    // Navigation Sync
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                navController.navigate(Routes.INTERMEDIATE) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                    popUpTo(Routes.FIRST) { inclusive = true }
                }
            }
            is AuthState.Idle -> {
                if (navController.currentDestination?.route !in listOf(Routes.SPLASH, Routes.FIRST)) {
                    navController.navigate(Routes.FIRST) { popUpTo(0) { inclusive = true } }
                }
            }
            else -> Unit
        }
    }


    // -------- NavHost --------
    NavHost(
        navController = navController,
        startDestination =
            if (authViewModel.isUserAuthenticated())
                Routes.INTERMEDIATE
            else
                Routes.SPLASH
    ) {

        // -------- Splash --------
        composable(Routes.SPLASH) {
            SplashScreen(
                viewModel = authViewModel,
                onNavigateToAuth = {
                    navController.navigate(Routes.FIRST) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Routes.INTERMEDIATE) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // -------- First Screen --------
        composable(Routes.FIRST) {
            AnimatedFirstScreen(
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN)
                },
                onNavigateToSignup = {
                    navController.navigate(Routes.REGISTER)
                },
                onGuestSignIn = {
                    authViewModel.loginAsGuest()
                }
            )
        }

        // -------- Login --------
        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                },
                onNavigateToHome = {
                    navController.navigate(Routes.INTERMEDIATE) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // -------- Register --------
        composable(Routes.REGISTER) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Routes.INTERMEDIATE) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // -------- Intermediate --------
        composable(Routes.INTERMEDIATE) {
            IntermediateScreen(
                navController = navController,
             )
        }

        // -------- Home --------
        composable(Routes.HOME) {
            MainScreen(
                navController = navController,
                bluetoothViewModel = bluetoothViewModel
            )
        }

        // -------- Robot --------
        composable(Routes.ROBOT) {
            val act = LocalContext.current as? Activity
            RobotControlScreen {
                act?.finish()
            }
        }

        // -------- Settings --------
        composable(Routes.SETTINGS) {
            ModernSettingsScreen(
                viewModel = authViewModel,
                onSignOut = {
                    authViewModel.logout()
                    navController.navigate(Routes.FIRST) {
                        popUpTo(Routes.INTERMEDIATE) { inclusive = true }
                    }
                },
                navController = navController
            )
        }

        // -------- Advertising --------
        // In AppNavigation.kt
        composable(
            route = Routes.ADVERTISING,
            arguments = listOf(
                navArgument("deviceName") { type = NavType.StringType },
                navArgument("deviceAddress") { type = NavType.StringType },
                navArgument("sensorType") { type = NavType.StringType },
                navArgument("deviceId") { type = NavType.StringType }
            )
        ) { entry ->
            // Extract everything to ensure the backstack is happy
            val deviceName = entry.arguments?.getString("deviceName") ?: "Unknown"
            val deviceAddress = entry.arguments?.getString("deviceAddress") ?: ""
            val sensorType = entry.arguments?.getString("sensorType") ?: ""
            val deviceId = entry.arguments?.getString("deviceId") ?: "0"

            AdvertisingDataScreen(
                deviceName = deviceName,
                deviceAddress = deviceAddress,
                deviceId = deviceId,
                navController = navController,
                viewModel = bluetoothViewModel
            )
        }

        // -------- Data Logger --------
        composable(
            route = Routes.DATA_LOGGER,
            arguments = listOf(
                navArgument("deviceName") { type = NavType.StringType },
                navArgument("deviceAddress") { type = NavType.StringType },
                navArgument("deviceId") { type = NavType.StringType }
            )
        ) { entry ->
            DataLoggerScreen(
                deviceName = entry.arguments?.getString("deviceName") ?: "",
                deviceAddress = entry.arguments?.getString("deviceAddress") ?: "",
                deviceId = entry.arguments?.getString("deviceId") ?: "",
                navController = navController,
                viewModel = bluetoothViewModel as BluetoothScanViewModel
            )
        }

        // -------- Charts --------
//        composable(
//            route = "chart_screen/{deviceAddress}",
//            arguments = listOf(
//                navArgument("deviceAddress") { type = NavType.StringType }
//            )
//        )
//        { backStackEntry ->
//            val encodedAddress = backStackEntry.arguments?.getString("deviceAddress")
//            val deviceAddress = encodedAddress?.let { Uri.decode(it) }
//            ChartScreen(
//                navController = navController,
//                deviceAddress = deviceAddress,
//                viewModel = bluetoothViewModel
//            )
//        }
        composable(
            route = Routes.CHART,
            arguments = listOf(
                navArgument("deviceAddress") { type = NavType.StringType }
            )
        ) { backStackEntry ->

            val encodedAddress =
                backStackEntry.arguments?.getString("deviceAddress")

            val deviceAddress =
                encodedAddress?.let { Uri.decode(it) }

             ChartScreen(
                navController = navController,
                deviceAddress = deviceAddress,
                viewModel = bluetoothViewModel
            )
        }


        composable(
            route = Routes.CHART_2,
            arguments = listOf(
                navArgument("title") { type = NavType.StringType },
                navArgument("value") { type = NavType.StringType }
            )
        ) { entry ->
            ChartScreen2(
                navController = navController,
                title = entry.arguments?.getString("title"),
                value = entry.arguments?.getString("value")
            )
        }
    }
}
