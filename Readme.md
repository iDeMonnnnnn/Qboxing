# Qboxing

[![](https://jitpack.io/v/iDeMonnnnnn/Qboxing.svg)](https://jitpack.io/#iDeMonnnnnn/Qboxing)

基于[boxing](https://github.com/bilibili/boxing)的AndroidQ升级适配。

相较于boxing:
1. 完全兼容AndroidQ
2. 升级适配到AndroidX
3. 代码改为kotlin

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
	        implementation 'com.github.iDeMonnnnnn.Qboxing:boxing:1.0'
	}
```

#### 如何使用

1. 使用方法与[boxing](https://github.com/bilibili/boxing)完全一致，可参见其文档
2. 参考[app](https://github.com/iDeMonnnnnn/Qboxing/tree/master/app)使用实例

#### 使用体验
[下载Demo.apk体验](https://github.com/iDeMonnnnnn/QFsolution/raw/master/QFDemo.apk)


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