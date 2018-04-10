package net.ximity.sample.login

import android.os.Bundle
import net.ximity.mvp.template.ActivityView
import net.ximity.sample.AppComponent
import net.ximity.sample.R
import net.ximity.sample.login.mvp.LoginMvp
import net.ximity.sample.login.mvp.LoginMvpModule
import net.ximity.sample.login.mvp.LoginPresenter
import javax.inject.Inject

class LoginView :
        ActivityView(),
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
    }

    override fun showHome() {
    }
}
