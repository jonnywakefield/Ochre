package com.ochre.app.di

import android.content.Context
import androidx.room.Room
import com.ochre.data.local.AppDatabase
import com.ochre.data.repository.AloneRepositoryImpl
import com.ochre.data.repository.EventRepositoryImpl
import com.ochre.data.repository.FoodRepositoryImpl
import com.ochre.data.repository.ReminderRepositoryImpl
import com.ochre.data.repository.WalkRepositoryImpl
import com.ochre.domain.repository.AloneRepository
import com.ochre.domain.repository.EventRepository
import com.ochre.domain.repository.FoodRepository
import com.ochre.domain.repository.ReminderRepository
import com.ochre.domain.repository.WalkRepository
import com.ochre.domain.usecase.DeleteEventUseCase
import com.ochre.domain.usecase.GetAllEventsUseCase
import com.ochre.domain.usecase.GetLastEventPerTypeUseCase
import com.ochre.domain.usecase.GetLastEventUseCase
import com.ochre.domain.usecase.LogEventUseCase
import com.ochre.domain.usecase.UpdateEventUseCase
import com.ochre.domain.usecase.alone.DeleteAloneUseCase
import com.ochre.domain.usecase.alone.EndAloneUseCase
import com.ochre.domain.usecase.alone.GetActiveAloneSessionUseCase
import com.ochre.domain.usecase.alone.GetAloneSessionHistoryUseCase
import com.ochre.domain.usecase.alone.StartAloneUseCase
import com.ochre.domain.usecase.food.AddStockUseCase
import com.ochre.domain.usecase.food.DeleteMealUseCase
import com.ochre.domain.usecase.food.GetCurrentStockUseCase
import com.ochre.domain.usecase.food.GetFeedLogUseCase
import com.ochre.domain.usecase.food.GetMealScheduleUseCase
import com.ochre.domain.usecase.food.LogFeedUseCase
import com.ochre.domain.usecase.food.SaveMealUseCase
import com.ochre.domain.usecase.reminder.DeleteReminderUseCase
import com.ochre.domain.usecase.reminder.GetAllRemindersUseCase
import com.ochre.domain.usecase.reminder.SaveReminderUseCase
import com.ochre.domain.usecase.walk.AddPeeToWalkUseCase
import com.ochre.domain.usecase.walk.DeleteWalkUseCase
import com.ochre.domain.usecase.walk.AddPooToWalkUseCase
import com.ochre.domain.usecase.walk.RemovePeeFromWalkUseCase
import com.ochre.domain.usecase.walk.RemovePooFromWalkUseCase
import com.ochre.domain.usecase.walk.EndWalkUseCase
import com.ochre.domain.usecase.walk.GetActiveWalkUseCase
import com.ochre.domain.usecase.walk.GetWalkHistoryUseCase
import com.ochre.domain.usecase.walk.GetWalkScheduleUseCase
import com.ochre.domain.usecase.walk.SaveWalkScheduleUseCase
import com.ochre.domain.usecase.walk.StartWalkUseCase

interface AppContainer {
    // Repositories
    val eventRepository: EventRepository
    val walkRepository: WalkRepository
    val aloneRepository: AloneRepository
    val foodRepository: FoodRepository
    val reminderRepository: ReminderRepository

    // Event use cases
    val logEventUseCase: LogEventUseCase
    val updateEventUseCase: UpdateEventUseCase
    val deleteEventUseCase: DeleteEventUseCase
    val getAllEventsUseCase: GetAllEventsUseCase
    val getLastEventUseCase: GetLastEventUseCase
    val getLastEventPerTypeUseCase: GetLastEventPerTypeUseCase

    // Walk use cases
    val startWalkUseCase: StartWalkUseCase
    val endWalkUseCase: EndWalkUseCase
    val deleteWalkUseCase: DeleteWalkUseCase
    val addPooToWalkUseCase: AddPooToWalkUseCase
    val addPeeToWalkUseCase: AddPeeToWalkUseCase
    val removePooFromWalkUseCase: RemovePooFromWalkUseCase
    val removePeeFromWalkUseCase: RemovePeeFromWalkUseCase
    val getActiveWalkUseCase: GetActiveWalkUseCase
    val getWalkHistoryUseCase: GetWalkHistoryUseCase
    val getWalkScheduleUseCase: GetWalkScheduleUseCase
    val saveWalkScheduleUseCase: SaveWalkScheduleUseCase

    // Alone use cases
    val startAloneUseCase: StartAloneUseCase
    val endAloneUseCase: EndAloneUseCase
    val deleteAloneUseCase: DeleteAloneUseCase
    val getActiveAloneSessionUseCase: GetActiveAloneSessionUseCase
    val getAloneSessionHistoryUseCase: GetAloneSessionHistoryUseCase

    // Food use cases
    val logFeedUseCase: LogFeedUseCase
    val getMealScheduleUseCase: GetMealScheduleUseCase
    val saveMealUseCase: SaveMealUseCase
    val deleteMealUseCase: DeleteMealUseCase
    val getFeedLogUseCase: GetFeedLogUseCase
    val getCurrentStockUseCase: GetCurrentStockUseCase
    val addStockUseCase: AddStockUseCase

    // Reminder use cases
    val saveReminderUseCase: SaveReminderUseCase
    val getAllRemindersUseCase: GetAllRemindersUseCase
    val deleteReminderUseCase: DeleteReminderUseCase
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()
    }

    override val eventRepository: EventRepository by lazy { EventRepositoryImpl(database.eventDao) }
    override val walkRepository: WalkRepository by lazy { WalkRepositoryImpl(database.walkDao) }
    override val aloneRepository: AloneRepository by lazy { AloneRepositoryImpl(database.aloneDao) }
    override val foodRepository: FoodRepository by lazy { FoodRepositoryImpl(database.foodDao, database.eventDao) }
    override val reminderRepository: ReminderRepository by lazy { ReminderRepositoryImpl(database.reminderDao) }

    override val logEventUseCase by lazy { LogEventUseCase(eventRepository) }
    override val updateEventUseCase by lazy { UpdateEventUseCase(eventRepository) }
    override val deleteEventUseCase by lazy { DeleteEventUseCase(eventRepository) }
    override val getAllEventsUseCase by lazy { GetAllEventsUseCase(eventRepository) }
    override val getLastEventUseCase by lazy { GetLastEventUseCase(eventRepository) }
    override val getLastEventPerTypeUseCase by lazy { GetLastEventPerTypeUseCase(eventRepository) }

    override val startWalkUseCase by lazy { StartWalkUseCase(walkRepository) }
    override val endWalkUseCase by lazy { EndWalkUseCase(walkRepository) }
    override val deleteWalkUseCase by lazy { DeleteWalkUseCase(walkRepository) }
    override val addPooToWalkUseCase by lazy { AddPooToWalkUseCase(walkRepository) }
    override val addPeeToWalkUseCase by lazy { AddPeeToWalkUseCase(walkRepository) }
    override val removePooFromWalkUseCase by lazy { RemovePooFromWalkUseCase(walkRepository) }
    override val removePeeFromWalkUseCase by lazy { RemovePeeFromWalkUseCase(walkRepository) }
    override val getActiveWalkUseCase by lazy { GetActiveWalkUseCase(walkRepository) }
    override val getWalkHistoryUseCase by lazy { GetWalkHistoryUseCase(walkRepository) }
    override val getWalkScheduleUseCase by lazy { GetWalkScheduleUseCase(walkRepository) }
    override val saveWalkScheduleUseCase by lazy { SaveWalkScheduleUseCase(walkRepository) }

    override val startAloneUseCase by lazy { StartAloneUseCase(aloneRepository) }
    override val endAloneUseCase by lazy { EndAloneUseCase(aloneRepository) }
    override val deleteAloneUseCase by lazy { DeleteAloneUseCase(aloneRepository) }
    override val getActiveAloneSessionUseCase by lazy { GetActiveAloneSessionUseCase(aloneRepository) }
    override val getAloneSessionHistoryUseCase by lazy { GetAloneSessionHistoryUseCase(aloneRepository) }

    override val logFeedUseCase by lazy { LogFeedUseCase(foodRepository) }
    override val getMealScheduleUseCase by lazy { GetMealScheduleUseCase(foodRepository) }
    override val saveMealUseCase by lazy { SaveMealUseCase(foodRepository) }
    override val deleteMealUseCase by lazy { DeleteMealUseCase(foodRepository) }
    override val getFeedLogUseCase by lazy { GetFeedLogUseCase(foodRepository) }
    override val getCurrentStockUseCase by lazy { GetCurrentStockUseCase(foodRepository) }
    override val addStockUseCase by lazy { AddStockUseCase(foodRepository) }

    override val saveReminderUseCase by lazy { SaveReminderUseCase(reminderRepository) }
    override val getAllRemindersUseCase by lazy { GetAllRemindersUseCase(reminderRepository) }
    override val deleteReminderUseCase by lazy { DeleteReminderUseCase(reminderRepository) }
}
