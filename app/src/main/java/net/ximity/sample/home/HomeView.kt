package net.ximity.sample.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.home_view.*
import net.ximity.sample.AppComponent
import net.ximity.sample.R
import net.ximity.sample.common.BaseActivity
import net.ximity.sample.home.mvp.HomeMvp
import net.ximity.sample.home.mvp.HomeMvpModule
import net.ximity.sample.home.mvp.HomePresenter
import javax.inject.Inject

class HomeView :
        BaseActivity<HomeMvp.Presenter>(),
        HomeMvp.View {

    /** [HomePresenter] **/
    @Inject
    internal lateinit var presenter: HomeMvp.Presenter

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, HomeView::class.java)
    }

    override fun bind(component: AppComponent) =
            component.add(HomeMvpModule(this))
                    .bind(this)
                    .bindPresenter(presenter)

    override fun onBackPressed() = Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_view)
        logout.setOnClickListener { presenter.logout() }
    }

    override fun onLogout() = finish()
}