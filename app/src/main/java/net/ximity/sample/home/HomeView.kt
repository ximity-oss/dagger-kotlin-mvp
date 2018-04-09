package net.ximity.sample.home

import android.os.Bundle
import net.ximity.sample.AppComponent
import net.ximity.sample.R
import net.ximity.sample.common.BaseActivity
import net.ximity.sample.home.mvp.HomeMvp

class HomeView :
        BaseActivity(),
        HomeMvp.View {

    override fun bind(component: AppComponent) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_view)
    }
}