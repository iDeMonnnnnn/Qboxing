package com.bilibili.boxing

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * @author DeMon
 * Created on 2021/1/6.
 * E-mail 757454343@qq.com
 * Desc:
 */

/**
 * 将Uri转为File
 */
fun Uri?.uriToFile(context: Context): File? {
    this ?: return null
    Log.i("FileExt", "uriToFile: $this")
    return when (scheme) {
        ContentResolver.SCHEME_FILE -> {
            File(this.path)
        }
        ContentResolver.SCHEME_CONTENT -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getFileFromUriQ(context)
            } else {
                getFileFromUriN(context)
            }
        }
        else -> {
            File(toString())
        }
    }
}

/**
 * 根据Uri获取File，AndroidQ及以上可用
 * AndroidQ中只有沙盒中的文件可以直接根据绝对路径获取File，非沙盒环境是无法根据绝对路径访问的
 * 因此先判断Uri是否是沙盒中的文件，如果是直接拼接绝对路径访问，否则使用[saveFileByUri]复制到沙盒中生成File
 */
fun Uri.getFileFromUriQ(context: Context): File? {
    var file: File? = null
    if (DocumentsContract.isDocumentUri(context, this)) {
        val uriId = DocumentsContract.getDocumentId(this)
        Log.i("FileExt", "getFileFromUriQ: ${DocumentsContract.getDocumentId(this)}")
        val split: List<String> = uriId.split(":")
        //文件存在沙盒中，可直接拼接全路径访问
        //判断依据目前是Android/data/包名，不够严谨
        if (split.size > 1 && split[1].contains("Android/data/${context.packageName}")) {
            //AndroidQ无法通过Environment.getExternalStorageDirectory()获取SD卡根目录，因此直接/storage/emulated/0/拼接
            file = File("/storage/emulated/0/${split[1]}")
        }
    }
    val flag = file?.exists() ?: false
    return if (!flag) {
        this.saveFileByUri(context)
    } else {
        file
    }
}

/**
 * 根据Uri获取File，AndroidN~AndroidQ可用
 */
fun Uri.getFileFromUriN(context: Context): File? {
    var file: File? = null
    var uri = this
    Log.i("FileExt", "getFileFromUriN: $uri ${uri.authority} ${uri.path}")
    val authority = uri.authority
    val path = uri.path
    /**
     * media类型的Uri，形如content://media/external/images/media/11560
     */
    if (file == null && authority != null && authority.startsWith("media")) {
        uri.getDataColumn(context)?.run {
            file = File(this)
        }
    }
    /**
     * fileProvider授权的Uri
     */
    if (file == null && authority != null && authority.startsWith(context.packageName) && path != null) {
        //这里的值来自你的provider_paths.xml，如果不同需要自己进行添加修改
        val externals = mutableListOf(
            "/external",
            "/external_path",
            "/beta_external_files_path",
            "/external_cache_path",
            "/beta_external_path",
            "/external_files",
            "/internal"
        )
        externals.forEach {
            if (path.startsWith(it)) {
                //如果你在provider_paths.xml中修改了path，需要自己进行修改
                val newFile = File("${Environment.getExternalStorageDirectory().absolutePath}/${path.replace(it, "")}")
                if (newFile.exists()) {
                    file = newFile
                }
            }
        }
    }
    /**
     * Intent.ACTION_OPEN_DOCUMENT选择的文件Uri
     */
    if (file == null && DocumentsContract.isDocumentUri(context, this)) {
        val uriId = DocumentsContract.getDocumentId(this)
        Log.i("FileExt", "isDocumentUri: ${DocumentsContract.getDocumentId(this)}")
        val split: List<String> = uriId.split(":")
        when (uri.authority) {
            "com.android.externalstorage.documents" -> { //内部存储设备中选择
                if (split.size > 1) file = File("${Environment.getExternalStorageDirectory().absolutePath}/${split[1]}")
            }
            "com.android.providers.downloads.documents" -> { //下载内容中选择
                if (uriId.startsWith("raw:")) {
                    file = File(split[1])
                }
                //content://com.android.providers.downloads.documents/document/582
            }
            "com.android.providers.media.documents" -> { //多媒体中选择
                var contentUri: Uri? = null
                when (split[0]) {
                    "image" -> {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                    "video" -> {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    }
                    "audio" -> {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }
                }
                contentUri?.run {
                    if (split.size > 1) {
                        uri = ContentUris.withAppendedId(this, split[1].toLong())
                        Log.i("FileExt", "isDocumentUri media: $uri")
                        uri.getDataColumn(context)?.run {
                            file = File(this)
                        }
                    }
                }
            }
        }
    }
    val flag = file?.exists() ?: false
    return if (!flag) {
        //形如content://com.android.providers.downloads.documents/document/582的下载内容中的文件
        //无法根据Uri获取到真实路径的文件，统一使用saveFileByUri(context)方法获取File
        uri.saveFileByUri(context)
    } else {
        file
    }
}


/**
 * 根据Uri查询文件路径
 * Android4.4之前都可用，Android4.4之后只有从多媒体中选择的文件可用
 */
fun Uri?.getDataColumn(context: Context): String? {
    if (this == null) return null
    var str: String? = null
    var cursor: Cursor? = null
    try {
        cursor = context.contentResolver.query(this, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)
        cursor?.run {
            if (this.moveToFirst()) {
                val index = this.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                if (index != -1) str = this.getString(index)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        cursor?.close()
    }
    return str
}

/**
 * 根据Uri将文件保存File到沙盒中
 * 此方法能解决部分Uri无法获取到File的问题
 * 但是会造成文件冗余，可以根据实际情况，决定是否需要删除
 */
fun Uri.saveFileByUri(context: Context): File? {
    try {
        val inputStream = context.contentResolver.openInputStream(this)
        val file = File(context.getExternalFilesDir("Temp"), "${this.authority}_${this.lastPathSegment}.${this.getExtensionByUri(context)}")
        if (file.exists()) {
            return file
        }
        val fos = FileOutputStream(file)
        val bis = BufferedInputStream(inputStream)
        val bos = BufferedOutputStream(fos)
        val byteArray = ByteArray(1024)
        var bytes = bis.read(byteArray)
        while (bytes > 0) {
            bos.write(byteArray, 0, bytes)
            bos.flush()
            bytes = bis.read(byteArray)
        }
        bos.close()
        fos.close()
        return file
    } catch (e: Exception) {

    }
    return null
}

/**
 * 根据Uri获取扩展名
 */
fun Uri.getExtensionByUri(context: Context) = this.getMimeTypeByUri(context)?.getExtensionByMimeType()

/**
 * 根据Uri获取MimeType
 */
fun Uri.getMimeTypeByUri(context: Context) = context.contentResolver.getType(this)


/**
 * 根据MimeType获取拓展名
 */
fun String.getExtensionByMimeType() = MimeTypeMap.getSingleton().getExtensionFromMimeType(this)
