package net.ximity.annotation

import javax.inject.Scope
import kotlin.reflect.KClass

/**
 * Custom view scope for MVP presenters
 */
@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class ViewScope

/**
 * MVP contract annotation
 */
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class Contract(
        /** Required implemented presenter class **/
        val presenter: KClass<*>,
        /** Required implemented view class **/
        val view: KClass<*>,
        /** Optional generated module name **/
        val module: String = "",
        /** Optional generated subcomponent name **/
        val subcomponent: String = "")

/**
 * Main component marker annotation
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
annotation class MainComponent(
        /** Optional generated MVP component bind interface name **/
        val name: String = "MvpBindings")