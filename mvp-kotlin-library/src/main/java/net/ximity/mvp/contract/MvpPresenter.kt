package net.ximity.mvp.contract

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.v4.app.Fragment
import android.util.Log

/**
 * Base view presenter contract with hooks to view lifecycle
 *
 * @author by Emarc Magtanong on 2018/04/07.
 */
interface MvpPresenter<V : MvpView> {
    var view: V?

    /**
     * Lifecycle callback bound to [Activity.onCreate] or [Fragment.onViewCreated]
     *
     * @param saved saved state bundle, non-null if previously recreated view
     */
    @SuppressLint("LogNotTimber")
    fun create(saved: Bundle?) {
        Log.d("Presenter", "Created: ${this}")
    }

    /**
     * Lifecycle callback bound to [Activity.onResume] or [Fragment.onResume]
     */
    @SuppressLint("LogNotTimber")
    fun resume() {
        Log.d("Presenter", "Resumed: ${this}")
    }

    /**
     * Lifecycle callback for the presenter to start. For a view presenter, this will bind to
     * [Activity.onStart] or [Fragment.onStart]
     */
    @SuppressLint("LogNotTimber")
    fun start() {
        Log.d("Presenter", "Started: ${this}")
    }

    /**
     * Lifecycle callback for the presenter to pause. For a view presenter, this will bind to
     * [Activity.onPause] or [Fragment.onPause]
     */
    @SuppressLint("LogNotTimber")
    fun pause() {
        Log.d("Presenter", "Paused: ${this}")
    }

    /**
     * Lifecycle callback for the presenter to save state. For a view presenter, this will bind to
     * [Activity.onSaveInstanceState] or [Fragment.onSaveInstanceState]
     *
     * @param out out state bundle
     */
    @SuppressLint("LogNotTimber")
    fun saveState(out: Bundle) {
        Log.d("Presenter", "Saved: ${this}")
    }

    /**
     * Lifecycle callback for the presenter to stop. For a view presenter, this will bind to
     * [Activity.onStop] or [Fragment.onStop]
     */
    @SuppressLint("LogNotTimber")
    fun stop() {
        Log.d("Presenter", "Stopped: ${this}")
    }

    /**
     * Lifecycle callback bound to [Activity.onDestroy] or [Fragment.onDestroy]
     */
    @SuppressLint("LogNotTimber")
    @CallSuper
    fun destroy() {
        Log.d("Presenter", "Destroyed: ${this}")
        view = null
    }
}