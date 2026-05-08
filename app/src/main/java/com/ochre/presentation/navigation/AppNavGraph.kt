package com.ochre.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ochre.app.di.AppContainer
import com.ochre.domain.usecase.alone.DeleteAloneUseCase
import com.ochre.domain.usecase.walk.DeleteWalkUseCase
import com.ochre.presentation.calendar.CalendarScreen
import com.ochre.presentation.calendar.CalendarViewModel
import com.ochre.presentation.home.HomeScreen
import com.ochre.presentation.home.HomeViewModel
import com.ochre.presentation.medical.MedicalScreen
import com.ochre.presentation.medical.MedicalViewModel
import com.ochre.presentation.settings.SettingsScreen
import com.ochre.presentation.training.TrainingScreen
import com.ochre.presentation.walk.WalkScreen
import com.ochre.presentation.walk.WalkSettingsScreen
import com.ochre.presentation.walk.WalkViewModel

@Composable
fun AppNavGraph(container: AppContainer) {
    val navController = rememberNavController()
    var showWalkSettings by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = { OchreNavBar(navController = navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                val viewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.provideFactory(
                        logEventUseCase = container.logEventUseCase,
                        startWalkUseCase = container.startWalkUseCase,
                        endWalkUseCase = container.endWalkUseCase,
                        getActiveWalkUseCase = container.getActiveWalkUseCase,
                        getWalkHistoryUseCase = container.getWalkHistoryUseCase,
                        startAloneUseCase = container.startAloneUseCase,
                        endAloneUseCase = container.endAloneUseCase,
                        getActiveAloneSessionUseCase = container.getActiveAloneSessionUseCase,
                        logFeedUseCase = container.logFeedUseCase,
                        getFeedLogUseCase = container.getFeedLogUseCase,
                        getMealScheduleUseCase = container.getMealScheduleUseCase,
                        getCurrentStockUseCase = container.getCurrentStockUseCase,
                        saveMealUseCase = container.saveMealUseCase,
                        deleteMealUseCase = container.deleteMealUseCase,
                        addStockUseCase = container.addStockUseCase,
                        deleteEventUseCase = container.deleteEventUseCase
                    )
                )

                // Navigate to Walk tab as soon as a walk is started
                LaunchedEffect(viewModel) {
                    viewModel.navigateToWalk.collect {
                        navController.navigate(Screen.Walk.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                }

                HomeScreen(viewModel = viewModel)
            }

            composable(Screen.Walk.route) {
                val viewModel: WalkViewModel = viewModel(
                    factory = WalkViewModel.provideFactory(
                        startWalkUseCase = container.startWalkUseCase,
                        endWalkUseCase = container.endWalkUseCase,
                        addPooToWalkUseCase = container.addPooToWalkUseCase,
                        addPeeToWalkUseCase = container.addPeeToWalkUseCase,
                        removePooFromWalkUseCase = container.removePooFromWalkUseCase,
                        removePeeFromWalkUseCase = container.removePeeFromWalkUseCase,
                        logEventUseCase = container.logEventUseCase,
                        getActiveWalkUseCase = container.getActiveWalkUseCase,
                        getWalkHistoryUseCase = container.getWalkHistoryUseCase,
                        getWalkScheduleUseCase = container.getWalkScheduleUseCase,
                        saveWalkScheduleUseCase = container.saveWalkScheduleUseCase
                    )
                )
                if (showWalkSettings) {
                    WalkSettingsScreen(
                        viewModel = viewModel,
                        onBack = { showWalkSettings = false }
                    )
                } else {
                    WalkScreen(
                        viewModel = viewModel,
                        onOpenSettings = { showWalkSettings = true }
                    )
                }
            }

            composable(Screen.Calendar.route) {
                val viewModel: CalendarViewModel = viewModel(
                    factory = CalendarViewModel.provideFactory(
                        getAllEventsUseCase = container.getAllEventsUseCase,
                        getWalkHistoryUseCase = container.getWalkHistoryUseCase,
                        getAloneSessionHistoryUseCase = container.getAloneSessionHistoryUseCase,
                        logEventUseCase = container.logEventUseCase,
                        updateEventUseCase = container.updateEventUseCase,
                        deleteEventUseCase = container.deleteEventUseCase,
                        getAllRemindersUseCase = container.getAllRemindersUseCase,
                        saveReminderUseCase = container.saveReminderUseCase,
                        deleteReminderUseCase = container.deleteReminderUseCase,
                        deleteWalkUseCase = container.deleteWalkUseCase,
                        deleteAloneUseCase = container.deleteAloneUseCase
                    )
                )
                CalendarScreen(viewModel = viewModel)
            }

            composable(Screen.Training.route) {
                TrainingScreen()
            }

            composable(Screen.Medical.route) {
                val viewModel: MedicalViewModel = viewModel(
                    factory = MedicalViewModel.provideFactory(
                        logWeightUseCase = container.logWeightUseCase,
                        getWeightHistoryUseCase = container.getWeightHistoryUseCase,
                        deleteWeightUseCase = container.deleteWeightUseCase
                    )
                )
                MedicalScreen(viewModel = viewModel)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(getMealScheduleUseCase = container.getMealScheduleUseCase)
            }
        }
    }
}
