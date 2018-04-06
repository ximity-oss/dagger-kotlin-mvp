package net.ximity.mvp.contract

import android.annotation.SuppressLint
import android.util.Log

/**
 * Marker interface for all views using the MVP pattern
 *
 * @author by Emarc Magtanong on 2018/04/07.
 */
interface MvpView {
    /**
     * Callback for showing an error message
     *
     * @param message error message
     */
    @SuppressLint("LogNotTimber")
    fun showError(message: String) = Log.d("MvpView", message)

    /**
     * Checks if the view is currently visible to the user
     *
     * @return true if the view is visible to the user, false otherwise
     */
    fun isViewVisible(): Boolean = true
}