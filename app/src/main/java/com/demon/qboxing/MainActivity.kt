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
import com.bilibili.boxing.utils.ImageCompressor
import com.bilibili.boxing_impl.ui.BoxingActivity
import com.demon.qboxing.databinding.ActivityMainBinding
import com.permissionx.guolindev.PermissionX


class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    val REQUEST_IMG_CODE = 0x001
    private lateinit var binding: ActivityMainBinding

    val adapter = ImgsAdapter()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //初始化，提供一个全局的Context
        Boxing.init(this)
        //考虑到不同项目中FileProvider的authorities可能不一样
        //因此这里改成可以根据自己项目FileProvider的authorities自由设置
        //如:android:authorities="${applicationId}.fileProvider",你只需要传入“fileProvider”即可
        Boxing.setFileProvider("fileProvider")
        /**
         * 初始化图片加载器
         * @see GlideLoader
         */
        BoxingMediaLoader.getInstance().init(GlideLoader())
        //申请权限
        PermissionX.init(this)
            .permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
            .request { allGranted, _, deniedList ->
                if (allGranted) {
                    Toast.makeText(this, "All permissions are granted", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "These permissions are denied: $deniedList", Toast.LENGTH_LONG).show()
                }
            }
        // Mode：Mode.SINGLE_IMG, Mode.MULTI_IMG, Mode.VIDEO
        binding.btn1.setOnClickListener {
            //单选
            val config = BoxingConfig(BoxingConfig.Mode.SINGLE_IMG).needCamera().needGif()
            Boxing.of(config).withIntent(this@MainActivity, BoxingActivity::class.java).start(this, REQUEST_IMG_CODE)
        }
        binding.btn2.setOnClickListener {
            //多选，默认9张
            val config = BoxingConfig(BoxingConfig.Mode.MULTI_IMG).withMaxCount(9)
            Boxing.of(config).withIntent(this@MainActivity, BoxingActivity::class.java).start(this, REQUEST_IMG_CODE)
        }
        binding.btn3.setOnClickListener {
            //压缩图片
            Toast.makeText(this, "开始压缩！", Toast.LENGTH_SHORT).show()
            adapter.datas.forEach {
                it.compress(ImageCompressor(this))
            }
            Toast.makeText(this, "压缩完成!", Toast.LENGTH_SHORT).show()
            adapter.notifyDataSetChanged()
        }

        /**
         * 推荐使用UCrop裁剪
         * @see BoxingUCrop
         */
        binding.btn4.setOnClickListener {
            //初始化裁剪，UCrop
            BoxingCrop.getInstance().init(BoxingUCrop())
            //单选比例裁剪，需要设置比例
            val config = BoxingConfig(BoxingConfig.Mode.SINGLE_IMG).needCamera()
            config.withCropOption(BoxingCropOption().aspectRatio(1.0f, 1.0f))
            Boxing.of(config).withIntent(this@MainActivity, BoxingActivity::class.java).start(this, REQUEST_IMG_CODE)
        }
        binding.btn5.setOnClickListener {
            //初始化裁剪，UCrop
            BoxingCrop.getInstance().init(BoxingUCrop())
            //单选自由裁剪,无须设置比例，需要setFreeStyle(true)
            val config = BoxingConfig(BoxingConfig.Mode.SINGLE_IMG).needCamera()
            config.withCropOption(BoxingCropOption().setFreeStyle(true))
            Boxing.of(config).withIntent(this@MainActivity, BoxingActivity::class.java).start(this, REQUEST_IMG_CODE)
        }

        /**
         * 系统裁剪在不同手机的表现都有差异，建议使用UCrop
         * @see BoxingSystemCrop
         */
        binding.btn6.setOnClickListener {
            //初始化裁剪，系统裁剪
            BoxingCrop.getInstance().init(BoxingSystemCrop())
            //单选比例裁剪，需要设置比例
            val config = BoxingConfig(BoxingConfig.Mode.SINGLE_IMG).needCamera()
            config.withCropOption(BoxingCropOption().withMaxResultSize(200, 300))
            Boxing.of(config).withIntent(this@MainActivity, BoxingActivity::class.java).start(this, REQUEST_IMG_CODE)
        }
        binding.btn7.setOnClickListener {
            //初始化裁剪，系统裁剪
            BoxingCrop.getInstance().init(BoxingSystemCrop())
            //单选自由裁剪,无须设置比例，需要setFreeStyle(true)
            val config = BoxingConfig(BoxingConfig.Mode.SINGLE_IMG).needCamera()
            config.withCropOption(BoxingCropOption().setFreeStyle(true))
            Boxing.of(config).withIntent(this@MainActivity, BoxingActivity::class.java).start(this, REQUEST_IMG_CODE)
        }
        binding.rv.adapter = adapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == REQUEST_IMG_CODE) {
            val medias: List<BaseMedia>? = getResult(data)
            val imgMedias = mutableListOf<ImageMedia>()
            medias?.forEach {
                Log.i(TAG, "onActivityResult: $it")
                if (it is ImageMedia) {
                    imgMedias.add(it)
                }
            }
            adapter.datas = imgMedias
            adapter.notifyDataSetChanged()
        }
    }
}