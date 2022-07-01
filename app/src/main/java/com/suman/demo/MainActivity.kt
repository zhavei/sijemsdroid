package com.suman.demo

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import com.suman.demo.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    //private var webView: WebView? = null
    //region global variables
    private val mCM: String? = null
    private val mUM: ValueCallback<Uri>? = null
    private val mUMA: ValueCallback<Array<Uri>>? = null
    private var mUploadMessage: ValueCallback<Uri?>? = null
    private var mCapturedImageURI: Uri? = null
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var mCameraPhotoPath: String? = null
    private val url = "https://green.tangerangkota.go.id/sijemsdroid/"
    //endregion

    private var progressDialog: ProgressDialog? = null
    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val FCR = 1
        private const val INPUT_FILE_REQUEST_CODE = 1
        private const val FILECHOOSER_RESULTCODE = 1
        private val TAG = MainActivity::class.java.simpleName
    }

    @SuppressLint("SetJavaScriptEnabled", "WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


//        if (savedInstanceState == null) {
//            binding.webviewSample.restoreState(savedInstanceState?.getBundle("webview") ?: Bundle.EMPTY)
//        } else {
//            binding.webviewSample.loadUrl(url)
//        }

        val webView = binding.webviewSample
        val webError = binding.layoutError.nointernet

        val mSwipeRefreshLayout = binding.sweipeRefresh
        //display layout off if offline
        chekInternetConnect()
        //region build version
        if (Build.VERSION.SDK_INT >= 23 && (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA),
                1
            )
        }
        //endregion

        val webSettings = webView.settings
        //region webview setting
        webSettings.javaScriptEnabled = true
        webSettings.allowFileAccess = true
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true

        //otherweb setting
        webSettings.allowContentAccess = true
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        webSettings.domStorageEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webView.scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY
        webView.isScrollbarFadingEnabled = false
        webSettings.databaseEnabled = true
        webSettings.setAppCachePath(applicationContext.filesDir.absolutePath + "/cache")
        webSettings.databaseEnabled = true
        //endregion

        //region build version >= 21
        if (Build.VERSION.SDK_INT >= 21) {
            webSettings.mixedContentMode = 0
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        } else if (Build.VERSION.SDK_INT >= 19) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        } else if (Build.VERSION.SDK_INT < 19) {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
        //endregion


        //webView.webViewClient = Callback()
        //webView.loadUrl("https://green.tangerangkota.go.id/banksampahdroid/")
        webView.webChromeClient = object : WebChromeClient() {
            @Throws(IOException::class)
            private fun createImageFile(): File {
                val timeStamp =
                    SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                val imageFileName = "JPEG_" + timeStamp + "_"
                val storageDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                )
                return File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",  /* suffix */
                    storageDir /* directory */
                )
            }

            override fun onShowFileChooser(
                view: WebView,
                filePath: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                // Double check that we don't have any existing callbacks
                if (mFilePathCallback != null) {
                    mFilePathCallback!!.onReceiveValue(null)
                }
                mFilePathCallback = filePath
                var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (takePictureIntent != null) {
                    if (takePictureIntent.resolveActivity(packageManager) != null) {
                        // Create the File where the photo should go
                        var photoFile: File? = null
                        try {
                            photoFile = createImageFile()
                            takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath)
                        } catch (ex: IOException) {
                            // Error occurred while creating the File
                            Log.e(TAG, "Unable to create Image File gagal", ex)
                        }
                        // Continue only if the File was successfully created
                        if (photoFile != null) {
                            mCameraPhotoPath = "file:" + photoFile.absolutePath
                            takePictureIntent.putExtra(
                                MediaStore.EXTRA_OUTPUT,
                                Uri.fromFile(photoFile)
                            )
                        } else {
                            takePictureIntent = null
                        }
                    }
                }
                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = "image/*"
                val intentArray: Array<Intent?> =
                    takePictureIntent?.let { arrayOf(it) } ?: arrayOfNulls(0)
                val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Pilih Photo")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
                startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE)
                Log.d("this photo", "test logging")
                return true
            }

            fun openFileChooser(uploadMsg: ValueCallback<Uri?>?, acceptType: String?) {
                mUploadMessage = uploadMsg
                // Create AndroidExampleFolder at sdcard
                // Create AndroidExampleFolder at sdcard
                val imageStorageDir = File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES
                    ), "SijemsImages"
                )
                if (!imageStorageDir.exists()) {
                    // Create AndroidExampleFolder at sdcard
                    imageStorageDir.mkdirs()
                }
                // Create camera captured image file path and name
                val file = File(
                    imageStorageDir.toString() + File.separator + "IMG_"
                            + System.currentTimeMillis().toString() + ".jpg"
                )
                mCapturedImageURI = Uri.fromFile(file)
                // Camera capture image intent
                val captureIntent = Intent(
                    MediaStore.ACTION_IMAGE_CAPTURE
                )
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI)
                val i = Intent(Intent.ACTION_GET_CONTENT)
                i.addCategory(Intent.CATEGORY_OPENABLE)
                i.type = "image/*"
                // Create file chooser intent
                val chooserIntent = Intent.createChooser(i, "Pilih Photo")
                // Set camera intent to file chooser
                chooserIntent.putExtra(
                    Intent.EXTRA_INITIAL_INTENTS, arrayOf<Parcelable>(captureIntent)
                )
                // On select image call onActivityResult method of activity
                startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE)
            }

            fun openFileChooser(
                uploadMsg: ValueCallback<Uri?>?,
                acceptType: String?,
                capture: String?
            ) {
                openFileChooser(uploadMsg, acceptType)
            }

        }


        //for swipe refresh
        webView.webViewClient = object : WebViewClient() {


            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {

                mSwipeRefreshLayout.isRefreshing = true

            }

            override fun onPageFinished(view: WebView?, url: String?) {
                mSwipeRefreshLayout.isRefreshing = false
                return
            }

            //onReceivedError (new added jumat)
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                Toast.makeText(applicationContext, "Failed loading app!", Toast.LENGTH_SHORT).show()
                snackBarError()
            }
        }

        mSwipeRefreshLayout.setOnRefreshListener {
            if (isOnline()) {
                webView.reload()
            } else {
                Toast.makeText(
                    applicationContext,
                    "Kamu offline, swipe untuk reload!!",
                    Toast.LENGTH_SHORT
                ).show()
                snackBarError()
                webView.loadUrl(url)
            }
        }

    }

    /*private fun saveStatePreference(url: String) {

        binding.webviewSample.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {

                prefManager?.editor
                if (url != null) {
                    view?.loadUrl(url)
                }

                return true
            }
        }
    }*/

    /* override fun onSaveInstanceState(outState: Bundle) {
         super.onSaveInstanceState(outState)
         binding.webviewSample.saveState(outState)
         outState.putBundle("webview", outState)
     }

     override fun onRestoreInstanceState(savedInstanceState: Bundle) {
         super.onRestoreInstanceState(savedInstanceState)
         binding.webviewSample.restoreState(savedInstanceState)
     }*/

    private fun chekInternetConnect() {
        val webView = binding.webviewSample
        val webError = binding.layoutError.nointernet

        if (isOnline()) {
            webError.visibility = View.GONE
            webView.visibility = View.VISIBLE
            //webView.webViewClient = //Callback()
            webView.loadUrl(url)
        } else {
            Toast.makeText(
                applicationContext,
                "Kamu offline cek internet anda!!",
                Toast.LENGTH_SHORT
            )
                .show()
            //webError.visibility = View.VISIBLE
            //webView.visibility = View.GONE
//            if (progressDialog != null && progressDialog?.isShowing == true)
//                progressDialog?.dismiss()
            snackBarError()
            //webView.loadUrl(url)
        }

    }

    private fun snackBarError() {
        val snackbar: Snackbar =
            Snackbar.make(binding.root, "Koneksi internet terputus", Snackbar.LENGTH_INDEFINITE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            snackbar.view.setBackgroundColor(getColor(R.color.colorPrimary))
        }
        snackbar.setAction("Muat Ulang", View.OnClickListener {

//            if (isOnline()) {
//                binding.webviewSample.loadUrl(url)
//            } else {
//                Toast.makeText(applicationContext, "Kamu offline muat ulang!!", Toast.LENGTH_SHORT).show()
//                snackBarError()
//                binding.webviewSample.loadUrl(url)
//            }

            binding.webviewSample.loadUrl(url)

        })
        snackbar.show()
    }


    private fun isOnline(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = connectivityManager.activeNetworkInfo
        return netInfo != null && netInfo.isConnectedOrConnecting

    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }
        var results: Array<Uri>? = null
        // Check that the response is a good one
        if (resultCode == RESULT_OK) {
            if (data == null) {
                // If there is not data, then we may have taken a photo
                if (mCameraPhotoPath != null) {
                    results = arrayOf(Uri.parse(mCameraPhotoPath))
                }
            } else {
                val dataString = data.dataString
                if (dataString != null) {
                    results = arrayOf(Uri.parse(dataString))
                }
            }
        }
        mFilePathCallback?.onReceiveValue(results)
        mFilePathCallback = null
        return
    }

//    inner class Callback : WebViewClient() {
//        override fun onReceivedError(
//            view: WebView,
//            errorCode: Int,
//            description: String,
//            failingUrl: String
//        ) {
//            Toast.makeText(applicationContext, "Failed loading app!", Toast.LENGTH_SHORT).show()
//        }
//    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val myMenuInflater = menuInflater
        myMenuInflater.inflate(R.menu.super_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            //R.id.myMenuOne -> onBackPressed()
            R.id.myMenuTwo -> goForward()
            R.id.myMenutree -> whatsAppCall(
                "+6285715018087",
                "Halo Admin Sedekah Sampah Kota Tangerang!"
            )
        }
        return true
    }

    private fun whatsAppCall(phoneNumber: String, textInside: String) {
        val url = if (Intent().setPackage("com.whatsapp").resolveActivity(packageManager) != null) {
            "whatsapp://send?text=Hello&phone=$phoneNumber"
        } else {
            "https://api.whatsapp.com/send?phone=$phoneNumber&text=$textInside"
        }
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }

    private fun goForward() {
        if (binding.webviewSample.canGoForward()) {
            binding.webviewSample.goForward()
            Toast.makeText(this, "ke depan", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "sudah paling depan", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * onBackPressed is backpress exit
     */
    private var doubleBackToExitPressedOnce = false
    override fun onBackPressed() {
        if (binding.webviewSample.canGoBack()) {
            binding.webviewSample.goBack()
            Toast.makeText(this, "kembali", Toast.LENGTH_SHORT).show()
        } else if (doubleBackToExitPressedOnce) {
            Toast.makeText(this, "ini halaman utama", Toast.LENGTH_SHORT).show()
            super.onBackPressed()
            return
        } else {
            this.doubleBackToExitPressedOnce = true
            Toast.makeText(this, "klik sekali lagi untuk keluar", Toast.LENGTH_SHORT).show()

            Handler(Looper.getMainLooper()).postDelayed(kotlinx.coroutines.Runnable {
                doubleBackToExitPressedOnce = false
            }, 5000)
        }

    }

}
