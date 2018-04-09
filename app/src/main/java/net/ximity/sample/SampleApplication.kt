package net.ximity.sample

import net.ximity.mvp.template.BaseMvpApplication

class SampleApplication : BaseMvpApplication() {

    override fun initializeMainComponent(): AppComponent =
            DaggerAppComponent.builder()
                    .build()
}