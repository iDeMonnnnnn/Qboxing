package com.demon.qboxing

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bilibili.boxing.*
import com.bilibili.boxing.Boxing.Companion.getResult
import com.bilibili.boxing.model.config.BoxingConfig
import com.bilibili.boxing.model.config.BoxingCropOption
import com.bilibili.boxing.model.entity.BaseMedia
import com.bilibili.boxing.model.entity.impl.ImageMedia
import com.bilibili.boxing_impl.ui.BoxingActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
import com.permissionx.guolindev.PermissionX
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    val REQUEST_CODE = 0x001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        BoxingMediaLoader.getInstance().init(GlideLoader())
        BoxingCrop.getInstance().init(BoxingUCrop())

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
        val config = BoxingConfig(BoxingConfig.Mode.SINGLE_IMG) // Mode：Mode.SINGLE_IMG, Mode.MULTI_IMG, Mode.VIDEO
        config.needCamera().needGif()
        btn1.setOnClickListener {
            config.withCropOption(BoxingCropOption().setFreeStyle(true))
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
                val options = RequestOptions().override(SIZE_ORIGINAL)
                val media = medias[0] as ImageMedia
                Log.i(TAG, "onActivityResult: $media")
                Glide.with(img).asBitmap().apply(options).load(media.path).into(img)
            }
        }
    }
}