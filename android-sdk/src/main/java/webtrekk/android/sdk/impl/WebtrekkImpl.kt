/*
 *  MIT License
 *
 *  Copyright (c) 2019 Webtrekk GmbH
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 *
 */

package webtrekk.android.sdk.impl

import android.app.Activity
import android.content.Context
import androidx.annotation.RestrictTo
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.dsl.module.module
import org.koin.log.EmptyLogger
import org.koin.standalone.KoinComponent
import org.koin.standalone.StandAloneContext.startKoin
import org.koin.standalone.inject
import webtrekk.android.sdk.WebtrekkLogger
import webtrekk.android.sdk.Webtrekk
import webtrekk.android.sdk.Config
import webtrekk.android.sdk.core.AppState
import webtrekk.android.sdk.core.Logger
import webtrekk.android.sdk.core.Scheduler
import webtrekk.android.sdk.core.Sessions
import webtrekk.android.sdk.core.extension.initOrException
import webtrekk.android.sdk.core.extension.resolution
import webtrekk.android.sdk.core.util.CoroutineDispatchers
import webtrekk.android.sdk.core.util.coroutineExceptionHandler
import webtrekk.android.sdk.data.WebtrekkSharedPrefs
import webtrekk.android.sdk.data.entity.TrackRequest
import webtrekk.android.sdk.data.getWebtrekkDatabase
import webtrekk.android.sdk.domain.external.AutoTrack
import webtrekk.android.sdk.domain.external.ManualTrack
import webtrekk.android.sdk.domain.external.TrackCustomPage
import webtrekk.android.sdk.domain.external.TrackCustomEvent
import webtrekk.android.sdk.domain.external.Optout
import webtrekk.android.sdk.module.dataModule
import webtrekk.android.sdk.module.internalInteractorsModule
import webtrekk.android.sdk.util.appFirstStart
import webtrekk.android.sdk.util.currentSession
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class WebtrekkImpl private constructor() : Webtrekk(), KoinComponent, CoroutineScope {

    private val _job = SupervisorJob()

    private val coroutineDispatchers by inject<CoroutineDispatchers>()
    private val appState by inject<AppState<TrackRequest>>()

    private val scheduler by inject<Scheduler>()

    private val autoTrack by inject<AutoTrack>()
    private val manualTrack by inject<ManualTrack>()
    private val trackCustomPage by inject<TrackCustomPage>()
    private val trackCustomEvent by inject<TrackCustomEvent>()
    private val optOutUser by inject<Optout>()

    internal val sessions by inject<Sessions>()
    internal val logger by inject<Logger>()

    internal var context: Context by Delegates.initOrException(errorMessage = "Context must be initialized first")
    internal var config: Config by Delegates.initOrException(
        errorMessage = "Webtrekk configurations must be set before invoking any method." +
            " Use Webtrekk.getInstance().init(context, configuration)"
    )

    override val coroutineContext: CoroutineContext
        get() = _job + coroutineDispatchers.defaultDispatcher

    override fun init(context: Context, config: Config) {
        this.context = context.applicationContext
        this.config = config

        loadModules()
        internalInit()
    }

    override fun trackPage(
        context: Context,
        customPageName: String?,
        trackingParams: Map<String, String>
    ) = config.run {
        val contextName = customPageName ?: (context as Activity).localClassName

        manualTrack(
            ManualTrack.Params(
                trackRequest = TrackRequest(
                    name = contextName,
                    screenResolution = context.resolution(),
                    fns = currentSession,
                    one = appFirstStart
                ),
                trackingParams = trackingParams,
                autoTrack = autoTracking,
                isOptOut = hasOptOut()
            ), coroutineDispatchers
        )
    }

    override fun trackCustomPage(pageName: String, trackingParams: Map<String, String>) =
        config.run {
            trackCustomPage(
                TrackCustomPage.Params(
                    trackRequest = TrackRequest(
                        name = pageName,
                        screenResolution = context.resolution(),
                        fns = currentSession,
                        one = appFirstStart
                    ),
                    trackingParams = trackingParams,
                    isOptOut = hasOptOut()
                ), coroutineDispatchers
            )
        }

    override fun trackCustomEvent(eventName: String, trackingParams: Map<String, String>) =
        config.run {
            trackCustomEvent(
                TrackCustomEvent.Params(
                    trackRequest = TrackRequest(
                        name = eventName,
                        screenResolution = context.resolution(),
                        fns = currentSession,
                        one = appFirstStart
                    ),
                    trackingParams = trackingParams,
                    isOptOut = hasOptOut()
                ), coroutineDispatchers
            )
        }

    override fun optOut(value: Boolean, sendCurrentData: Boolean) = context.run {
        optOutUser(
            Optout.Params(
                context = this,
                optOutValue = value,
                sendCurrentData = sendCurrentData
            ), coroutineDispatchers
        )
    }

    override fun hasOptOut(): Boolean = context.run {
        return optOutUser.isActive()
    }

    override fun getEverId(): String = context.run {
        sessions.getEverId()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    private fun loadModules() {
        val mainModule = module {
            single { WebtrekkSharedPrefs(context) }
            single { config.okHttpClient }
            single { WorkManager.getInstance(context) }
            single { getWebtrekkDatabase(context).trackRequestDao() }
            single { getWebtrekkDatabase(context).customParamDataDao() }
            single { WebtrekkLogger(config.logLevel) as Logger }
            single { CoroutineDispatchers(Dispatchers.Main, Dispatchers.Default, Dispatchers.IO) }
            if (config.fragmentsAutoTracking) single { AppStateImpl() as AppState<TrackRequest> }
            else single { ActivityAppStateImpl() as AppState<TrackRequest> }
        }

        val externalInteractorsModule = module {
            single { AutoTrack(coroutineContext, get(), get()) }
            single { ManualTrack(coroutineContext, get(), get()) }
            single { TrackCustomPage(coroutineContext, get()) }
            single { TrackCustomEvent(coroutineContext, get()) }
            single { Optout(coroutineContext, get(), get(), get(), get()) }
        }

        try {
            startKoin(
                listOf(
                    mainModule,
                    dataModule,
                    internalInteractorsModule,
                    externalInteractorsModule
                ), logger = EmptyLogger()
            )
        } catch (e: Exception) {
            logger.error("Webtrekk is already in use: $e")
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    private fun internalInit() = launch(coroutineExceptionHandler(logger)) {
        sessions.setEverId()
        sessions.startNewSession().also { logger.info("A new session has started") }

        scheduler.scheduleCleanUp()
        scheduler.scheduleSendRequests(
            repeatInterval = config.requestsInterval,
            constraints = config.workManagerConstraints
        )

        if (config.autoTracking) {
            autoTrack(
                AutoTrack.Params(
                    context = context,
                    isOptOut = hasOptOut()
                ), coroutineDispatchers
            ).also { logger.info("Webtrekk has started auto tracking") }
        } else {
            appState.disable(context)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    internal fun cancelParentJob() {
        _job.cancel()
    }

    companion object {

        @Volatile
        private lateinit var INSTANCE: WebtrekkImpl

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun getInstance(): WebtrekkImpl {
            synchronized(this) {
                if (!Companion::INSTANCE.isInitialized) {
                    INSTANCE =
                        WebtrekkImpl()
                }
            }

            return INSTANCE
        }
    }
}