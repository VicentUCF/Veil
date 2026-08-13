package dev.vicent.veil.launcher.repository

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.provider.Settings
import dev.vicent.veil.launcher.model.CalendarEventSummary
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch

class CalendarRepository(private val context: Context) {
    private val mutableEvents = MutableStateFlow<List<CalendarEventSummary>>(emptyList())
    val events: StateFlow<List<CalendarEventSummary>> = mutableEvents.asStateFlow()
    private var observer: ContentObserver? = null
    private var observerScope: CoroutineScope? = null
    private var accessCheck: (() -> Boolean)? = null

    fun startObserving(scope: CoroutineScope, hasAccess: () -> Boolean) {
        observerScope = scope
        accessCheck = hasAccess
        if (hasAccess()) ensureObserver()
        scope.launch {
            try {
                awaitCancellation()
            } finally {
                removeObserver()
            }
        }
    }

    suspend fun refresh(accessGranted: Boolean) {
        if (accessGranted) ensureObserver() else removeObserver()
        mutableEvents.value = if (accessGranted) queryEvents() else emptyList()
    }

    fun open(eventId: Long): Boolean {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    fun createEvent(): Boolean = start(
        Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI),
    )

    fun openCalendar(): Boolean {
        val uri = ContentUris.withAppendedId(
            CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build(),
            System.currentTimeMillis(),
        )
        return start(Intent(Intent.ACTION_VIEW, uri))
    }

    fun configureGoogleCalendar(): Boolean {
        val launchGoogleCalendar = context.packageManager
            .getLaunchIntentForPackage(GoogleCalendarPackage)
        return if (launchGoogleCalendar != null) {
            start(launchGoogleCalendar)
        } else {
            start(
                Intent(Settings.ACTION_ADD_ACCOUNT).putExtra(
                    Settings.EXTRA_ACCOUNT_TYPES,
                    arrayOf("com.google"),
                ),
            )
        }
    }

    private fun start(intent: Intent): Boolean = runCatching {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess

    private fun ensureObserver() {
        if (observer != null) return
        val scope = observerScope ?: return
        val nextObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                scope.launch { refresh(accessCheck?.invoke() == true) }
            }
        }
        val registered = runCatching {
            context.contentResolver.registerContentObserver(
                CalendarContract.Events.CONTENT_URI,
                true,
                nextObserver,
            )
        }.isSuccess
        if (registered) observer = nextObserver
    }

    private fun removeObserver() {
        observer?.let { current ->
            runCatching { context.contentResolver.unregisterContentObserver(current) }
        }
        observer = null
    }

    private suspend fun queryEvents(): List<CalendarEventSummary> = withContext(Dispatchers.IO) {
        val begin = System.currentTimeMillis()
        val end = Calendar.getInstance().apply { timeInMillis = begin; add(Calendar.DAY_OF_YEAR, 7) }.timeInMillis
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, begin)
        ContentUris.appendId(builder, end)
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
        )
        runCatching {
            context.contentResolver.query(
                builder.build(),
                projection,
                "${CalendarContract.Instances.VISIBLE}=1 AND ${CalendarContract.Instances.END}>=?",
                arrayOf(begin.toString()),
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            CalendarEventSummary(
                                id = cursor.getLong(0),
                                title = cursor.getString(1)?.trim().orEmpty().ifBlank { "Evento" },
                                startMillis = cursor.getLong(2),
                                endMillis = cursor.getLong(3),
                            ),
                        )
                    }
                }.distinctBy { it.id to it.startMillis }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val GoogleCalendarPackage = "com.google.android.calendar"
    }
}
