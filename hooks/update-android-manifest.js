#!/usr/bin/env node

var fs = require('fs');
var async = require('async');
var exec = require('child_process').exec;
var path = require('path');

var fileExists = function (filePath) {
    try {
        return fs.statSync(filePath).isFile();
    } catch (err) {
        return false;
    }
};

var root = process.cwd();
var androidManifest;
var cordovaAndroid6Path = path.join(root, 'platforms/android/AndroidManifest.xml');
var cordovaAndroid7Path = path.join(root, 'platforms/android/app/src/main/AndroidManifest.xml');
if (fileExists(cordovaAndroid7Path)) {
    androidManifest = cordovaAndroid7Path;
} else if (fileExists(cordovaAndroid6Path)) {
    androidManifest = cordovaAndroid6Path;
} else {
    throw "Can't find AndroidManifest.xml";
}

var txt = fs.readFileSync(androidManifest, 'utf8');
var lines = txt.split('\n');
var searchingFor = '<application';
var searchManifest = '<manifest';
var newManifest = [];
var largeHeap = 'android:networkSecurityConfig="@xml/network_security_config" android:allowBackup="false" tools:replace="android:allowBackup"';
var toolXml = 'xmlns:tools="http://schemas.android.com/tools"'
lines.forEach(function (line) {
    if (line.trim().indexOf(searchingFor) != -1 && line.trim().indexOf(largeHeap) == -1) {
        newManifest.push(line.replace('<application', '<application ' + largeHeap));
    } else if(line.trim().indexOf(searchManifest) != -1 && line.trim().indexOf(toolXml) == -1) {
        newManifest.push(line.replace('<manifest', '<manifest ' + toolXml));
    } else {
        newManifest.push(line);
    }
});

fs.writeFileSync(androidManifest, newManifest.join('\n'));