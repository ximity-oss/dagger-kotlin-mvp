package net.ximity.annotation

import kotlin.reflect.KClass

/**
 * MVP contract annotation
 *
 * @author by Emarc Magtanong on 2018/04/07.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class MvpContract(
        /** Required implemented presenter class **/
        val presenter: KClass<*>,
        /** Required implemented view class **/
        val view: KClass<*>,
        /** Optional generated module name **/
        val module: String = "",
        /** Optional generated subcomponent name **/
        val subcomponent: String = "")
