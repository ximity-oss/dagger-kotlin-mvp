package net.ximity.mvp.template

import android.app.Application
import android.content.Context

/**
 * Base application with dependency injection methods
 *
 * @author by Emarc Magtanong on 2018/04/07.
 */
abstract class MvpApplication<out M : Any> : Application() {

    /** Application main component  */
    private lateinit var component: M

    fun getComponent(): M? = component

    override fun onCreate() {
        super.onCreate()
        component = initializeMainComponent()
    }

    /**
     * Initializes dagger component for dependency injection.
     */
    protected abstract fun initializeMainComponent(): M

    companion object {
        inline fun <reified APP : MvpApplication<*>> getApp(context: Context): APP {
            return context.applicationContext as APP
        }
    }
}
