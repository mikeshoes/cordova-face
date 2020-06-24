
# 基于*百度云推送*的android端cordova人脸识别插件说明

-----

1. 编译环境
  * android8.0 
  * sdk 
  * ndk 项目内部使用了基于C语言的动态so库 需要ndk编译
  * 手机支持 armeabi armeabi-v7a 

2. 使用说明
    
 1. 安装插件
    `ionic cordova plugin add https://github.com/mikeshoes/cordova-face.git#{branch} --variable face_app_id=xxxx --variable fd_sdk_key=xxx `
 2. 使用插件   
    插件基于 虹软的人脸识别android sdk 需要配置plugin.xml中配置sdk的授权
    ionic 使用外部cordova插件有两种方式 *ionic-native* *直接使用*
    这里介绍直接使用方式 需在app deviceready 回调中使用。

    ```
    declare var cordova: any;
    declare var window: any;

    cordova.plugins.cordovaFace.start({
        bizId: '', // 校验时传递
        queryUrl: '', // 校验时传递 比较接口地址 bizId 也可以封在queryUrl中自己取用于登录
        isCollect: true // 当数据为采集数据时，传递
    }, function(data){
        // 初始化成功会返回 [status: true|false, image: 'base64编码数据'] true 标识本次采集或者身份识别通过。 false标识识别或身份识别失败。失败可重新调用方法再次识别

        可以使用 “data:image/jpg;base64,” + image 显示在图片中，请上传这个图片作为采集到的数据
    }, function(){
        // 失败调用
    });

    ```
3. plugin.xml 配置

    ```
            <preference name="face_app_id" value="" /> // 虹软的appId
            <preference name="fd_sdk_key" value="" /> // 虹软的人脸检测SDKKey

    ```           
