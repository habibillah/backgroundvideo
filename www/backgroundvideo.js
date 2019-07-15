var cordova = require('cordova');

var backgroundvideo = {
    startCamera: function(successFunction, errorFunction, camera) {
        successFunction = successFunction || function (filePath) {
            console.log('video saved:' + filePath);
        };
        errorFunction = errorFunction || function (error) {
            console.log('video recording error:' + error);
        };
        camera = camera || 'back';
        cordova.exec(successFunction, errorFunction, "backgroundvideo", "startCamera", [camera]);
    },
    start: function(fileStorage, filename, camera, quality, successFunction, errorFunction) {
        camera = camera || 'back';
        cordova.exec(successFunction, errorFunction, 'backgroundvideo', 'start', [fileStorage, filename, camera, quality]);
        window.document.body.style.opacity = .99;
        setTimeout(function () {
          window.document.body.style.opacity = 1;
        }, 23)
    },
    stop: function(successFunction, errorFunction) {
        cordova.exec(successFunction, errorFunction, 'backgroundvideo','stop', []);
    }
};

module.exports = backgroundvideo;
