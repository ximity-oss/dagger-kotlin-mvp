package net.ximity.mvp.template

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

/**
 * Base activity with dependency injection from the Global Object Graph
 *
 * @author by Emarc Magtanong on 2018/04/07.
 */
abstract class MvpActivity<in M : Any> : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MvpApplication.getApp<MvpApplication<M>>(this)
                .getComponent()
                ?.let(this::bind)
    }

    /**
     * Injects dependencies with global scope
     *
     * @param component component to bind globally scoped dependencies
     */
    protected abstract fun bind(component: M)
}
