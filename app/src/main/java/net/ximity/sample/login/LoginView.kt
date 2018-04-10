package net.ximity.sample.login

import android.os.Bundle
import kotlinx.android.synthetic.main.login_view.*
import net.ximity.sample.AppComponent
import net.ximity.sample.R
import net.ximity.sample.common.BaseActivity
import net.ximity.sample.home.HomeView
import net.ximity.sample.login.mvp.LoginMvp
import net.ximity.sample.login.mvp.LoginMvpModule
import net.ximity.sample.login.mvp.LoginPresenter
import javax.inject.Inject

class LoginView :
        BaseActivity(),
        LoginMvp.View {

    /** [LoginPresenter] **/
    @Inject
    internal lateinit var presenter: LoginMvp.Presenter

    override fun bind(component: AppComponent) {
        component.add(LoginMvpModule(this))
                .bind(this)
                .bindPresenter(presenter)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_view)
        login.setOnClickListener { presenter.login() }
    }

    override fun showHome() =
            startActivity(HomeView.newIntent(this))
}
