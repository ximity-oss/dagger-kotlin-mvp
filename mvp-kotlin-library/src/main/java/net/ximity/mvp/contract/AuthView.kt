package net.ximity.mvp.contract

import android.util.Log

/**
 * Authenticated view contract for all view the requires an authenticated user for handling authentication errors
 *
 * @author by Emarc Magtanong on 2018/04/07.
 */
interface AuthView {
    /**
     * Callback for authentication error
     */
    @SuppressWarnings("LogNotTimber")
    fun showLogin() = Log.d("AuthView", "User is not authenticated in an Authenticated View.")
}