;(function(global) {
  'use strict'

  var base64
  if (typeof Buffer !== 'undefined') {
    base64 = function(str) {
      return (new Buffer(str)).toString('base64')
    }
  }
  else {
    base64 = global.btoa
  }

  var methods = [
    //http://tools.ietf.org/html/rfc2616
    'OPTIONS',
    'GET',
    'HEAD',
    'POST',
    'PUT',
    'DELETE',
    'TRACE',
    'CONNECT',
    //http://tools.ietf.org/html/rfc5789
    'PATCH'
  ];

  var joinPaths = function(paths) {
    if (typeof paths === 'string')
      return paths

    var joined = ''
    var length = 0
    var pathsLength = paths.length

    for (var i = 0; i < paths.length; i++) {
      var path = paths[i]
      if (typeof path !== 'string')
        throw 'TypeError: argument to joinPaths must be an array with strings'

      for (var y = 0; y < path.length; y++) {
        var l = path[y]
        if (l === '/' && joined[length - 1] === '/')
          continue

        joined += l
        length++
      }

      if (i !== pathsLength - 1 && joined[length - 1] !== '/') {
        joined += '/'
        length++
      }
    }

    return joined
  }

  var getPrototypeOf = function(obj) {
    if (Object.getPrototypeOf)
      return Object.getPrototypeOf(obj)
    else
      /* eslint-disable */
      return obj.__proto__
      /* eslint-enable */
  }

  var prototypeOfObject = getPrototypeOf({})

  var isObject = function(obj) {
    if (typeof obj !== 'object')
      return false

    return getPrototypeOf(obj) === prototypeOfObject || getPrototypeOf(obj) === null
  }

  var defaultOptions = {
    query: {},
    secure: false,
    host: 'localhost',
    path: '/',
    headers: {},
    method: 'GET',
    port: 80,
    jsonp: false,
    username: '',
    password: ''
  }

  var handleOptions = function(opts, overrides) {
    opts = opts || {}

    var options = {}

    for (var i in defaultOptions) {
      if (i === 'path') {
        if (typeof opts.path === 'string') {
          if (overrides && typeof overrides.path === 'string')
            options.path = joinPaths([overrides.path, opts.path])
          else
            options.path = joinPaths(opts.path)
        }
        else if (overrides && overrides.path === 'string') {
          options.path = joinPaths(overrides.path)
        }
        else
          options.path = joinPaths(defaultOptions.path)
      }
      else if (typeof opts[i] === typeof defaultOptions[i])
        options[i] = opts[i]
      else if (overrides && typeof overrides[i] === typeof defaultOptions[i])
        options[i] = overrides[i]
      else
        options[i] = defaultOptions[i]
    }

    options.method = options.method.toUpperCase()

    //jsonp
    if (opts.jsonp === true)
      opts.jsonp = 'callback'
    if (typeof opts.jsonp === 'string') {
      options.jsonp = opts.jsonp
      options.query[opts.jsonp] = 'HTTPClient' + Date.now()
    }

    //lower cases headers
    for (var k in options.headers) {
      var v = options.headers[k]
      delete options.headers[k]
      options.headers[k.toLowerCase()] = v
    }

    //basic auth
    if (opts.username && typeof opts.username === 'string' && opts.password && typeof opts.password === 'string') {
      var creds = opts.username + ':' + opts.password
      options.headers.authorization = 'Basic ' + base64(creds)
    }

    //body json
    if (Array.isArray(opts.body) || isObject(opts.body)) {
      options.body = JSON.stringify(opts.body)
      if (!options.headers['content-type'])
        options.headers['content-type'] = 'application/json; charset=utf-8'
    }
    //body string
    else if (typeof opts.body === 'string') {
      options.body = opts.body
      if (!options.headers['content-type'])
        options.headers['content-type'] = 'text/plain; charset=utf-8'
    }
    else if (opts.body !== undefined || opts.body !== null) {
      options.body = opts.body
    }

    return options
  }

  var getTypeFromHeaders = function(headers) {
    var type = ''
    if (typeof headers === 'object') {
      var contentType = headers['content-type']
      if (contentType)
        type = contentType.split(';')[0]
    }
    return type
  }

  var getSizeFromHeaders = function(headers) {
    var size = 0
    if (typeof headers === 'object') {
      var contentLength = headers['content-length']
      if (contentLength)
        size = parseInt(contentLength, 10)
    }
    return size
  }

  var Promise
  if (typeof module !== 'undefined' && module.exports) {
    if (!global.Promise) {
      try {
        Promise = require('es6-promise').Promise
      }
      catch (ex) {}
    }
  }
  else {
    Promise = global.Promise
  }

  //https://github.com/isaacs/inherits
  var inherits = (function() {
    if (typeof module !== 'undefined' && module.exports && typeof require !== 'undefined')
      return require('util').inherits
    else if (typeof Object.create === 'function') {
      return function(ctor, superCtor) {
        ctor.super_ = superCtor
        ctor.prototype = Object.create(superCtor.prototype, {
          constructor: {
            value: ctor,
            enumerable: false,
            writable: true,
            configurable: true
          }
        })
      }
    }
    else {
      return function(ctor, superCtor) {
        ctor.super_ = superCtor
        var TempCtor = function () {}
        TempCtor.prototype = superCtor.prototype
        ctor.prototype = new TempCtor()
        ctor.prototype.constructor = ctor
      }
    }
  })()

  var warn = function(message) {
    console.warn('HTTPClient.js warns: ' + message)
  }

  var utils = {
    handleOptions: handleOptions,
    joinPaths: joinPaths,
    getTypeFromHeaders: getTypeFromHeaders,
    getSizeFromHeaders: getSizeFromHeaders,
    getPrototypeOf: getPrototypeOf,
    Promise: Promise,
    methods: methods,
    defaultOptions: defaultOptions,
    inherits: inherits,
    warn: warn
  }

  if (typeof module !== 'undefined' && module.exports) {
    module.exports = utils
  } else {
    global.HTTPClient = {utils: utils}
  }
})(this)
