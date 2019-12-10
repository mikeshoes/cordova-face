
# 基于*百度云推送*的android端cordova人脸识别插件说明

-----

1. 编译环境
  * android8.0 
  * sdk 
  * ndk 项目内部使用了基于C语言的动态so库 需要ndk编译
  * 手机支持 armeabi armeabi-v7a 

2. 使用说明
    
 1. 安装插件
    `ionic cordova plugin add https://github.com/mikeshoes/cordova-face.git --variable FACE_APP_ID=xxx --variable FD_SDK_KEY=xxx`
 2. 使用插件   
    将readme.txt 重命名为 ts
    
    token , bizId, queryUrl 请从服务端调用接口拿到，
    
    ```
    cordova.plugins.cordovaFace.start({
        token: '', // 验证时传入
        bizId: '', // 验证时传入
        queryUrl: '', // 验证时传入
        isCollect: true // 当数据为采集数据时，传递true , 验证时传递false
    }, function(data){
        // 采集成功会返回 [status: true|false, image: 'base64编码数据'] true 标识本次采集完成。 false标识识别采集失败
        // 验证时 会返回  [status: true | false ],不返回image image服务端自己处理
    }, function(){
        // 失败调用
    });

    ```
3. plugin.xml 配置

    ```
      <preference name="face_app_id" value="" /> // 虹软的appId
      <preference name="fd_sdk_key" value="" /> // 虹软的人脸检测SDKKey

    ```           
