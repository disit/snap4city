;(function(global) {
  'use strict'

  var utils

  if (typeof module !== 'undefined' && module.exports) {
    utils = require('./utils')
  } else {
    utils = global.HTTPClient.utils
  }

  var Response = function() {
    this.headers = {}
    this.status = ''
    this.statusCode = NaN
    this.type = ''
    this.size = 0
  }
  Response.prototype.onend = function() {}
  Response.prototype.onprogress = function() {}
  Response.prototype.setHeaders = function(headers) {
    for (var i in headers)
      this.headers[i] = headers[i]

    this.type = utils.getTypeFromHeaders(this.headers)
    this.size = utils.getSizeFromHeaders(this.headers)
  }
  Response.prototype.setStatus = function(status) {
    this.statusCode = status
    this.status = status.toString()
  }

  if (typeof module !== 'undefined' && module.exports) {
    module.exports = Response
  } else {
    global.HTTPClient.Response = Response
  }
})(this)
