package net.ximity.mvp.template

import android.content.Context
import android.support.v4.app.Fragment

/**
 * Base fragment with dependency injection from the Global Object Graph
 *
 * @author by Emarc Magtanong on 2018/04/07.
 */
abstract class MvpFragment<in M : Any> : Fragment() {

    override fun onAttach(context: Context) {
        super.onAttach(context)
        MvpApplication.getApp<MvpApplication<M>>(context)
                .getComponent()
                .let(this::bind)
    }

    /**
     * Injects dependencies with global scope
     *
     * @param component main component to bind globally scoped dependencies
     */
    protected abstract fun bind(component: M)
}