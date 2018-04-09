package net.ximity.sample.login.mvp

import net.ximity.annotation.MvpContract
import net.ximity.mvp.contract.MvpPresenter
import net.ximity.mvp.contract.MvpView
import net.ximity.sample.home.HomeView
import net.ximity.sample.login.LoginView

/**
 * Login MVP contract
 *
 * @author by Emarc Magtanong on 2018/04/09.
 */
@MvpContract(view = LoginView::class, presenter = LoginPresenter::class)
interface LoginMvp {
    interface View : MvpView {
        /**
         * Shows the [HomeView]
         */
        fun showHome()
    }

    interface Presenter : MvpPresenter {
        /**
         * Fake login to navigate to [HomeView]
         */
        fun login()
    }
}