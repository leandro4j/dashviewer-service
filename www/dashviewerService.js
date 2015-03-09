/*
 * Service Name
 * This needs to be full qualified name of your service class
 * This will be the combination of the package & class name in your service java file
 */
var serviceName = 'br.com.sankhya.dashviewer.DashViewerService';

/*
 * Get an instance of the background service factory
 * Use it to create a background service wrapper for your service
 */
var factory = require('br.com.sankhya.dashviewer.BackgroundService');
var dashviewerservice = factory.create(serviceName);
module.exports = dashviewerservice;

window.plugins = window.plugins || {};
window.plugins.dashviewerservice = dashviewerservice;