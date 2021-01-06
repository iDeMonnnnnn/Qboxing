package com.demon.qxing

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bilibili.boxing.Boxing
import com.bilibili.boxing.Boxing.Companion.getResult
import com.bilibili.boxing.BoxingCrop
import com.bilibili.boxing.BoxingMediaLoader
import com.bilibili.boxing.model.config.BoxingConfig
import com.bilibili.boxing.model.entity.BaseMedia
import com.bilibili.boxing.model.entity.impl.ImageMedia
import com.bilibili.boxing.uriToFile
import com.bilibili.boxing.utils.ImageCompressor
import com.bilibili.boxing_impl.ui.BoxingActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.permissionx.guolindev.PermissionX
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    val REQUEST_CODE = 0x001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        BoxingMediaLoader.getInstance().init(GlideLoader())
        //BoxingCrop.getInstance().init()
        PermissionX.init(this)
            .permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
            .request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    Toast.makeText(this, "All permissions are granted", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "These permissions are denied: $deniedList", Toast.LENGTH_LONG).show()
                }
            }
        Boxing.init(this)
        val config = BoxingConfig(BoxingConfig.Mode.MULTI_IMG) // Mode：Mode.SINGLE_IMG, Mode.MULTI_IMG, Mode.VIDEO
        config.needCamera().needGif().withMaxCount(9)
        btn1.setOnClickListener {
            Boxing.of(config).withIntent(this@MainActivity, BoxingActivity::class.java).start(this, REQUEST_CODE)
        }
        btn2.setOnClickListener {

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE) {
            val medias: List<BaseMedia>? = getResult(data)
            medias?.forEach {
                Log.i(TAG, "onActivityResult: $it")
            }
            if (medias != null) {
                val options = RequestOptions().error(R.drawable.ic_qf_image).placeholder(R.drawable.ic_qf_image).centerCrop()
                val media = medias[0] as ImageMedia
                media.compress(ImageCompressor(this))
                Log.i(TAG, "onActivityResult: $media")
                Glide.with(img).asBitmap().apply(options).load(media.compressPath).into(img)
            }
        }
    }
}