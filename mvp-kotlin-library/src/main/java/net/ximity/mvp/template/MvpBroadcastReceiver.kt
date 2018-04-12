package net.ximity.mvp.template

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Base broadcast receiver with dependency injection from the Global Object Graph
 * @author by Emarc Magtanong on 2018/04/07.
 */
abstract class MvpBroadcastReceiver<in M : Any> : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        MvpApplication.getApp<MvpApplication<M>>(context)
                .getComponent()
                .let(this::bind)
    }

    /**
     * Injects dependencies with global scope
     *
     * @param component component to bind globally scoped dependencies
     */
    protected abstract fun bind(component: M)
}