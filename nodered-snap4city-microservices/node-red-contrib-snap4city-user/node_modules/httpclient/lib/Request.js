;(function(global) {
  'use strict'

  var utils
  var Response

  if (typeof module !== 'undefined' && module.exports) {
    utils = require('./utils')
    Response = require('./Response')
  } else {
    utils = global.HTTPClient.utils
    Response = global.HTTPClient.Response
  }

  var Request = function(opts) {
    opts = utils.handleOptions(opts)

    for (var j in opts) {
      this[j] = opts[j]
    }

    this.response = new Response()
    this.impl = {}
  }
  Request.prototype.abort = function() {}
  Request.prototype.onresponse = function() {}
  Request.prototype.onprogress = function() {}
  Request.prototype.onerror = function() {}

  if (typeof module !== 'undefined' && module.exports) {
    module.exports = Request
  } else {
    global.HTTPClient.Request = Request
  }
})(this)
