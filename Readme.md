## Qboxing - 基于[boxing](https://github.com/bilibili/boxing)的AndroidQ升级适配。

[![](https://jitpack.io/v/iDeMonnnnnn/Qboxing.svg)](https://jitpack.io/#iDeMonnnnnn/Qboxing)

相较于boxing:
1. 兼容AndroidQ
2. 适配AndroidX
3. 100%的Kotlin代码

### 开始使用
#### 添加依赖
```
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
[latest_version](https://github.com/iDeMonnnnnn/Qboxing/releases)
```
dependencies {
	     implementation 'com.github.iDeMonnnnnn:Qboxing:$latest_version'
	}
```

#### 添加权限

**注意申请运行时权限。**

```xml
    <!--如果你使用相机相关功能必须要添加，否则可忽略-->
    <uses-permission android:name="android.permission.CAMERA" />
    <!--存储权限在低于AndroidQ的手机上还是需要的-->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

#### 如何使用

##### 1.初始化
使用前，必须调用此方法初始化。
```js
//初始化，提供一个全局的Context
Boxing.init(this)
```

##### 2.获取图片路径

使用newPath代替path，兼容了AndroidQ无法直接通过文件路径获取非作用域文件的问题。

```js
media.path ---> media.newPath
```

#### 3.更多
1. 基本使用方法与[boxing](https://github.com/bilibili/boxing)完全一致，可参见其文档
2. 参考[app](https://github.com/iDeMonnnnnn/Qboxing/tree/master/app)使用示例

#### 使用效果

[下载Demo.apk体验](https://github.com/iDeMonnnnnn/Qboxing/raw/master/Qboxing.apk)

![xxx](https://github.com/iDeMonnnnnn/Qboxing/blob/master/181212.png?raw=true)

### 其他

如果你有问题或者建议，请[Issues](https://github.com/iDeMonnnnnn/QFsolution/issues).

### 致谢
[boxing](https://github.com/bilibili/boxing)

[QFsolution](https://github.com/iDeMonnnnnn/QFsolution)

### MIT License

```
Copyright (c) 2021 DeMon

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```