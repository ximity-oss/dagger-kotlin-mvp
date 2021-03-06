package net.ximity.sample.login.mvp

import android.content.res.Resources
import net.ximity.annotation.MvpScope
import net.ximity.sample.R
import net.ximity.sample.login.LoginView
import javax.inject.Inject

/**
 * [LoginMvp.Presenter] implementation
 *
 * @author by Emarc Magtanong on 2018/04/09.
 */
@MvpScope
class LoginPresenter @Inject internal constructor(
        /** [LoginView] **/
        override var view: LoginMvp.View?,
        /** Application resources **/
        private val resources: Resources
) : LoginMvp.Presenter {

    override fun login() {
        view?.showError(resources.getString(R.string.logging_in))
        view?.showHome()
    }
}