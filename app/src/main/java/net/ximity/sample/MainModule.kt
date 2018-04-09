package net.ximity.sample

import android.app.Application
import android.content.res.Resources
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class MainModule internal constructor(
        private val application: Application) {

    @Provides
    @Singleton
    internal fun providesApplication(): Application {
        return application
    }

    @Provides
    @Singleton
    internal fun providesResources(): Resources {
        return application.resources
    }
}