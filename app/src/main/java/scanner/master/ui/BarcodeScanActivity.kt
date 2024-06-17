package scanner.master.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import scanner.master.R
import scanner.master.analyzer.BarcodeAnalyzer
import scanner.master.analyzer.ScanningResultListener
import scanner.master.databinding.ActivityBarcodeScanningBinding
import scanner.master.ui.custom.ConnectivityLiveData
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.time.LocalTime
import java.time.format.DateTimeFormatter

const val ARG_SCANNING_SDK = "scanning_SDK"

class BarcodeScanningActivity : AppCompatActivity() {

    private var lastScanTime: Long = 0
    private val delayMillis: Long = 1000 // 1 second delay

    var lastPlayTimeAudio: Long = 0
    val delayMillisAudio: Long = 2000 // 3 second delay

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var binding: ActivityBarcodeScanningBinding
    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService
    private var flashEnabled = false
    private var scannerSDK = "QRCode" //default is MLKit
    private lateinit var connectivityLiveData:ConnectivityLiveData
    var arr_data = ArrayList<String>()
    private var lensFacing = CameraSelector.LENS_FACING_FRONT


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarcodeScanningBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider, lensFacing)
        }, ContextCompat.getMainExecutor(this))

        binding.overlay.post {
            binding.overlay.setViewFinder()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun bindPreview(cameraProvider: ProcessCameraProvider?, rotate_Camera : Int?) {

        if (isDestroyed || isFinishing) {
            //This check is to avoid an exception when trying to re-bind use cases but user closes the activity.
            //java.lang.IllegalArgumentException: Trying to create use case mediator with destroyed lifecycle.
            return
        }

        cameraProvider?.unbindAll()

        val preview: Preview = Preview.Builder()
            .build()

        var cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(rotate_Camera!!)
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(binding.cameraPreview.width, binding.cameraPreview.height))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        val orientationEventListener = object : OrientationEventListener(this as Context) {
            override fun onOrientationChanged(orientation : Int) {
                // Monitors orientation values to determine the target rotation value
                val rotation : Int = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }

                imageAnalysis.targetRotation = rotation
            }
        }
        orientationEventListener.enable()
//        binding.txtLayout.setHint("Kết quả: ")
        requestWebView()
        fun repeatDelayed(delay: Long, action: () -> Unit): Handler {
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed(object : Runnable {
                override fun run() {
                    action()
                    handler.postDelayed(this, delay)
                }
            }, delay)
            return handler
        }
        //binding.webview.visibility = View.GONE;
        //switch the analyzers here, i.e. MLKitBarcodeAnalyzer, ZXingBarcodeAnalyzer
        class ScanningListener : ScanningResultListener {
            override fun onScanned(result: String) {

                val currentTime1 = System.currentTimeMillis()
                if (currentTime1 - lastScanTime < delayMillis) {
                    return;
                }

                lastScanTime = currentTime1

                //binding.txtLayout.visibility = View.VISIBLE;
                //binding.webview.visibility = View.VISIBLE;
                val delay = 5000L
                var data_rs = binding.edtResult.getText().toString();
                var counter = 0;
                if(counter > 0){
                    repeatDelayed(delay) {
                        val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                        data_rs = "$currentTime - $result\n$data_rs"       ;
                        binding.edtResult.setText(data_rs)
                    }
                }else{
                    if(counter == 0){
                        counter = 1;
                        val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                        data_rs = "$currentTime - $result\n$data_rs";
                        binding.edtResult.setText(data_rs);
                    }
                }


                binding.webview.evaluateJavascript(
                    "(function() { return document.getElementById('inputAllValx').value; })();"
                ) { value ->
                    // value chính là giá trị của phần tử input
                    var value1 = value.replace("\"", "")
                    Log.d("WebView123", "Value of myInput: $value1")

                    ///////////////////////////////////
                    //luôn cho load view ở đây, để sau khi lấy được giá trị js thì load lại
                    //Nếu đặt ngoài thì có thể ko lấy được val1...,
                    // vì đoạn này sẽ bị chạy qua trước khi js lấy được giá trị
                    var newUrl = "https://ncbd.mytree.vn/tool1/_site/event_mng/qr-scaned-post.php?currentTime1=$currentTime1&inputAllValx=$value1&qrs=$result";
//                    newUrl = "https://ncbd.mytree.vn/tool1/_site/event_mng/qr-scaned-post.php?qrs=data=37534259505b560507060677505a565e5b1954585a&inputAllValx=2|2";
//                    binding.webview.loadUrl(newUrl)


                    //Đưa vào đây để đảm bảo webview load xong mới excute js:
                    binding.webview.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            // Now that the page has finished loading, evaluate the JavaScript
                            binding.webview.evaluateJavascript(
                                //Tìm 1 link bất kỳ có myAudio id, src
                                "(function() { return document.getElementById('myAudio').src; })();"
                            ) { mp3Link ->

                                //Nếu thấy link mp3, thì sẽ play
                                var mp3Link1 = mp3Link.replace("\"", "")
                                // mp3Link chính là link mp3 của phần tử audio
                                Log.d("WebView1", "MP3 link: $mp3Link1")

                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastPlayTimeAudio < delayMillisAudio) {
                                    // It has not been 5 seconds since the last play, do not play the mp3
                                    return@evaluateJavascript
                                }
                                // It has been 5 seconds since the last play, update the last play time and play the mp3
                                lastPlayTimeAudio = currentTime
                                try {
                                    var mediaPlayer: MediaPlayer;
                                    if(mp3Link1.contains("up.mp3")){
                                        mediaPlayer = MediaPlayer.create(binding.webview.context, R.raw.up)
                                        mediaPlayer.start() // no need to call prepare(); create() does that for you
                                    }
                                    if(mp3Link1.contains("warning.mp3")){
                                        mediaPlayer = MediaPlayer.create(binding.webview.context, R.raw.warning)
                                        mediaPlayer.start() // no need to call prepare(); create() does that for you
                                    }

//                            val uri = Uri.parse("android.resource://${this.packageName}/" + R.raw.up)
//                            val mediaPlayer: MediaPlayer? = MediaPlayer().apply {
//                                setDataSource(context, uri) //to set media source and send the object to the initialized state
////                                setDataSource(mp3Link1) //to set media source and send the object to the initialized state
//                                prepare() //to send the object to the prepared state, this may take time for fetching and decoding
//                                start() //to start the music and send the object to started state
//                            }
                                } catch (e: IOException) {
                                    // Handle the exception, e.g. show an error message to the user
                                    Log.e("MediaPlayer", "Error setting data source or preparing MediaPlayer", e)
                                }

                            }
                        }
                    }

                    //Load xong, thì chạy js ở trên
                    binding.webview.loadUrl(newUrl)




                }

            }
        }

        var analyzer: ImageAnalysis.Analyzer = BarcodeAnalyzer(ScanningListener())

        imageAnalysis.setAnalyzer(cameraExecutor, analyzer)

        preview.setSurfaceProvider(binding.cameraPreview.surfaceProvider)

        val camera =
            cameraProvider?.bindToLifecycle(this, cameraSelector, imageAnalysis, preview)
        binding.imgRotate.setOnClickListener {
            //camera.cameraControl.
            if(lensFacing == 0){
                lensFacing = CameraSelector.LENS_FACING_BACK
            }else{
                lensFacing = CameraSelector.LENS_FACING_FRONT
            }
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider, lensFacing)
        }
        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            //binding.imgflash.visibility = View.VISIBLE
            binding.imgflash.setOnClickListener {
                camera.cameraControl.enableTorch(!flashEnabled)
            }
            camera.cameraInfo.torchState.observe(this) {
                it?.let { torchState ->
                    if (torchState == TorchState.ON) {
                        flashEnabled = true
                        binding.imgflash.setImageResource(R.drawable.ic_round_flash_on)
                    } else {
                        flashEnabled = false
                        binding.imgflash.setImageResource(R.drawable.ic_round_flash_off)
                    }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun requestWebView() {
        connectivityLiveData= ConnectivityLiveData(application)
        connectivityLiveData.observe(this, Observer {isAvailable->
            if(isAvailable){
                //binding.lnLow.visibility = View.GONE
                //binding.webview.visibility = View.VISIBLE
                binding.webview.loadUrl("https://ncbd.mytree.vn/tool1/_site/event_mng/qr-scaned-post.php")
            }else{
                //binding.webview.visibility = View.GONE
                //binding.lnLow.visibility = View.VISIBLE
                Toast.makeText(this@BarcodeScanningActivity, "Không có kết nối mạng!" , Toast.LENGTH_SHORT).show()
                return@Observer;
            }
        })
        /** Layout of webview screen View  */
        binding.webview.isFocusable = true
        binding.webview.isFocusableInTouchMode = true
        binding.webview.settings.javaScriptEnabled = true
        binding.webview.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        binding.webview.settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        binding.webview.settings.cacheMode = WebSettings.LOAD_DEFAULT
        binding.webview.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        binding.webview.settings.domStorageEnabled = true
        binding.webview.settings.databaseEnabled = true
        // this force use chromeWebClient
        binding.webview.settings.setSupportMultipleWindows(false)
        binding.webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {

                Log.d("URL Log", "URL: " + url!!)
                // If you wnat to open url inside then use
                view.loadUrl(url);
                return true
            }


            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
            }

            override fun onLoadResource(view: WebView, url: String) {
                super.onLoadResource(view, url)
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

            }
        }



    }

    override fun onDestroy() {
        super.onDestroy()
        // Shut down our background executor
        cameraExecutor.shutdown()
    }

}