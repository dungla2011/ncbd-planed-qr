import android.app.Activity
import android.content.Context
import android.view.View
import android.webkit.JavascriptInterface
import android.widget.Toast
import scanner.master.ui.BarcodeScanningActivity
import scanner.master.ui.CameraVisibilityController


class WebAppInterface internal constructor(var mContext: Context,
private val cameraController: CameraVisibilityController
) {
    @JavascriptInterface
    fun showToast(toast: String?) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show()
//        BarcodeScanningActivity.cameraPreviewStatic?.visibility = View.GONE
    }


    @JavascriptInterface
    fun moveToNextScreen() {
    }

    @JavascriptInterface
    fun hideCameraJs(toast: String?) {
        if(toast !== null && toast.length > 0)
            Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show()
        cameraController.hideCamera()

//        BarcodeScanningActivity.cameraPreviewStatic?.visibility = View.GONE
    }

    @JavascriptInterface
    fun showCameraJs(toast: String?) {
        if(toast !== null && toast.length > 0)
            Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show()
        cameraController.showCamera()
//        BarcodeScanningActivity.cameraPreviewStatic?.visibility = View.GONE
    }
}