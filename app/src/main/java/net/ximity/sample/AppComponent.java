package net.ximity.sample;

import net.ximity.annotation.MvpMainComponent;

import javax.inject.Singleton;

import dagger.Component;

@MvpMainComponent
@Singleton
@Component(modules = {
        MainModule.class
})
public interface AppComponent extends MvpBindings {
}
