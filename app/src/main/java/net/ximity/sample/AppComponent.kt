package net.ximity.sample

import dagger.Component
import net.ximity.annotation.MvpMainComponent
import net.ximity.sample.home.mvp.HomeMvpComponent
import net.ximity.sample.home.mvp.HomeMvpModule
import net.ximity.sample.login.mvp.LoginMvpComponent
import net.ximity.sample.login.mvp.LoginMvpModule
import javax.inject.Singleton

@MvpMainComponent
@Singleton
@Component(modules = [(MainModule::class)])
interface AppComponent : MvpBindings {
    override fun add(module: HomeMvpModule): HomeMvpComponent

    override fun add(module: LoginMvpModule): LoginMvpComponent

}