package net.ximity.mvp.contract

import android.annotation.SuppressLint
import android.app.Activity
import android.support.v4.app.Fragment
import android.util.Log

/**
 * Base view presenter contract with hooks to view lifecycle
 * @author by Emarc Magtanong on 2018/04/07.
 */
interface Presenter {
    /**
     * Lifecycle callback bound to [Activity.onCreate] or [Fragment.onCreate]
     */
    @SuppressLint("LogNotTimber")
    fun create() = Log.d("Presenter", "Created: ${this}")

    /**
     * Lifecycle callback for the presenter to start. For a view presenter, this will bind to
     * [Activity.onStart] or [Fragment.onStart]
     */
    @SuppressLint("LogNotTimber")
    fun start() = Log.d("Presenter", "Started: ${this}")

    /**
     * Lifecycle callback for the presenter to stop. For a view presenter, this will bind to
     * [Activity.onStop] or [Fragment.onStop]
     */
    @SuppressLint("LogNotTimber")
    fun stop() = Log.d("Presenter", "Stopped: ${this}")

    /**
     * Lifecycle callback bound to [Activity.onDestroy] or [Fragment.onDestroy]
     */
    @SuppressLint("LogNotTimber")
    fun destroy() = Log.d("Presenter", "Destroyed: ${this}")
}