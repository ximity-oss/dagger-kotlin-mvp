package net.ximity.mvp.template

import android.app.Service

/**
 * Base Service with dependency injection from the Global Object Graph
 *
 * @author by Emarc Magtanong on 2018/04/07.
 */
abstract class MvpService<in M : Any> : Service() {

    override fun onCreate() {
        super.onCreate()
        MvpApplication.getApp<MvpApplication<M>>(this)
                .getComponent()
                ?.let(this::bind)
    }

    /**
     * Injects dependencies with global scope
     *
     * @param component main component to bind globally scoped dependencies
     */
    protected abstract fun bind(component: M)
}