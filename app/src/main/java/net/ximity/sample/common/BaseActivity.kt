package net.ximity.sample.common

import android.widget.Toast
import net.ximity.mvp.contract.MvpView
import net.ximity.mvp.template.ActivityView

/**
 * Base activity with UI convenience methods
 *
 * @author by Emarc Magtanong on 2018/04/09.
 */
abstract class BaseActivity
    : ActivityView(),
        MvpView {

    override fun showError(message: String) =
            Toast.makeText(this, message, Toast.LENGTH_SHORT)
                    .show()
}