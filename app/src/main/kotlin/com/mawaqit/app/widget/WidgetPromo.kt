package com.mawaqit.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build

/**
 * PHASE-5.1: one-tap "Add widget" via the system pin dialog
 * (AppWidgetManager.requestPinAppWidget, API 26+). The dialog itself is
 * Android's own UI — consistent, trusted, zero custom layout to maintain.
 */
object WidgetPromo {

    /** True when this launcher can show the pin dialog. */
    fun canPin(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    /**
     * Fires the system "Add widget?" dialog for the Mawaqit widget.
     * Fire-and-forget: the result is verified by the caller (ViewModel
     * re-checks hosted widgets after the user has had time to answer).
     */
    fun pinWidget(context: Context) {
        if (!canPin(context)) return
        AppWidgetManager.getInstance(context).requestPinAppWidget(
            ComponentName(context, MawaqitWidgetReceiver::class.java),
            null, // no extras — the widget renders itself from Room
            null  // no success callback — outcome verified via getGlanceIds
        )
    }
}
