package net.ximity.sample.home.mvp

import net.ximity.annotation.MvpContract
import net.ximity.mvp.contract.AuthView
import net.ximity.mvp.contract.MvpPresenter
import net.ximity.sample.home.HomeView

/**
 * Home MVP contract
 *
 * @author by Emarc Magtanong on 2018/04/09.
 */
@MvpContract(view = HomeView::class, presenter = HomePresenter::class)
interface HomeMvp {
    interface View : AuthView

    interface Presenter : MvpPresenter {
        /**
         * Fake logout
         */
        fun logout()
    }
}