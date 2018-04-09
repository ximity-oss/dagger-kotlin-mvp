package net.ximity.sample.login

import android.os.Bundle
import net.ximity.sample.AppComponent
import net.ximity.sample.R
import net.ximity.sample.common.BaseActivity
import net.ximity.sample.login.mvp.LoginMvp

class LoginView :
        BaseActivity(),
        LoginMvp.View {

    override fun bind(component: AppComponent) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_view)
    }

    override fun showHome() {
    }
}
