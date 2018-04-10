package net.ximity.sample.home.mvp

import android.content.res.Resources
import net.ximity.annotation.MvpScope
import net.ximity.sample.R
import net.ximity.sample.home.HomeView
import javax.inject.Inject

/**
 * [HomeMvp.Presenter] implementation
 *
 * @author by Emarc Magtanong on 2018/04/09.
 */
@MvpScope
class HomePresenter @Inject internal constructor(
        /** [HomeView] **/
        private val view: HomeMvp.View,
        /** Application resources **/
        private val resources: Resources
) : HomeMvp.Presenter {

    override fun logout() {
        view.showError(resources.getString(R.string.logging_out))
        view.onLogout()
    }
}