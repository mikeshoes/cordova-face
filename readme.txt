import { Injectable } from '@angular/core';
import { Cordova, IonicNativePlugin, Plugin } from '@ionic-native/core';

export interface FaceOptions {

  uniqueId: string;
 
  isCollect: boolean;

}

/**
 * @name cordovaNotify
 * @description
 * Cordova / Phonegap plugin for communicating with HTTP servers. Supports iOS and Android.
 *
 * Advantages over Javascript requests:
 * - Background threading - all requests are done in a background thread
 * - SSL Pinning
 *
 * @usage
 * ```typescript
 * import { HTTP } from '@ionic-native/http/ngx';
 *
 * constructor(private http: HTTP) {}
 *
 * ...
 *
 * this.http.get('http://ionic.io', {}, {})
 *   .then(data => {
 *
 *     console.log(data.status);
 *     console.log(data.data); // data received by server
 *     console.log(data.headers);
 *
 *   })
 *   .catch(error => {
 *
 *     console.log(error.status);
 *     console.log(error.error); // error message as string
 *     console.log(error.headers);
 *
 *   });
 *
 * ```
 * @interfaces
 * HTTPResponse
 */
@Plugin({
  pluginName: 'cordovaFace',
  plugin: 'cordova-plugin-face-recognize',
  pluginRef: 'cordova.plugins.cordovaFace',
  repo: 'https://git.dev.tencent.com/a290942762/cordovaplugins.git',
  platforms: ['Android']
})
@Injectable()
export class cordovaFace extends IonicNativePlugin {

  /**
   *
   * @param url {string} The url to send the request to
   * @param body {Object} The body of the request
   * @param headers {Object} The headers to set for this request
   * @param filePath {string} The local path(s) of the file(s) to upload
   * @param name {string} The name(s) of the parameter to pass the file(s) along as
   * @returns {Promise<any>} returns a FileEntry promise that resolve on success, and reject on failure
   */
  @Cordova()
  start(options: FaceOptions): Promise<any> {
    return;
  }
}

