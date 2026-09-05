package com.fitplan.app

import android.app.Application
import com.fitplan.app.data.ActivePlanStore
import com.fitplan.app.data.FitPlanDatabase
import com.fitplan.app.data.repository.FitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class FitPlanApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // 懒加载，避免在 Application.onCreate 阶段做 IO
    val database: FitPlanDatabase by lazy {
        FitPlanDatabase.get(this, applicationScope)
    }

    val repository: FitRepository by lazy {
        FitRepository(database)
    }

    val activePlan: ActivePlanStore by lazy { ActivePlanStore(this) }
}
