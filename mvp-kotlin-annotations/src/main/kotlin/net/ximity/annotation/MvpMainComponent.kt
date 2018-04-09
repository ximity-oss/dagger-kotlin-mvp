package net.ximity.annotation

/**
 * Main component marker annotation
 *
 * @author by Emarc Magtanong on 2018/04/07.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
annotation class MvpMainComponent(
        /** Optional generated MVP component bind interface name **/
        val name: String = "MvpBindings")