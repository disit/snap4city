;(function(global) {
  'use strict'

  var utils
  var Request

  if (typeof module !== 'undefined' && module.exports) {
    utils = require('./utils')
    Request = require('./node')
  }
  else {
    utils = global.HTTPClient.utils
    Request = global.HTTPClient.Request
  }

  var HTTPClient = function(opts, fn) {
    // new HTTPClient(opts)
    if (!fn) {
      opts = utils.handleOptions(opts)

      for (var i in opts)
        this[i] = opts[i]
    }
    // HTTPClient(opts, fn)
    else {
      var req = new HTTPClient()
      req.request(opts, fn)
    }
  }
  var request = function(opts, fn) {
    if (typeof opts === 'string' || Array.isArray(opts))
      opts = {path: opts}

    opts = utils.handleOptions(opts, this)

    var req = new Request(opts)
    if (!fn)
      return req

    req.onerror = function(err) {
      fn(err)
    }
    req.onresponse = function(response) {
      if (fn.length < 3) {
        return fn(null, response)
      }

      response.onend = function(body) {
        fn(null, response, body)
      }
    }
    return req
  }

  HTTPClient.request = request
  HTTPClient.prototype.request = request

  utils.methods.forEach(function(method) {
    var fn = function(opts, fn) {
      if (typeof opts === 'string')
        opts = {path: opts}

      opts.method = method

      return this.request(opts, fn)
    }

    // instance
    HTTPClient.prototype[method] = fn
    HTTPClient.prototype[method.toLowerCase()] = fn
    // static
    HTTPClient[method] = fn
    HTTPClient[method.toLowerCase()] = fn
  })

  if (typeof module !== 'undefined' && module.exports) {
    module.exports = HTTPClient
  } else {
    global.HTTPClient = HTTPClient
    global.HTTPClient.utils = utils
  }
})(this)
