'use strict'

var http = require('http')
var https = require('https')
var utils = require('./utils')
var Request = require('./Request')
var querystring = require('querystring')

var NodeRequest = function(opts) {
  Request.call(this, opts)

  var response = this.response
  var request = this

  //http://nodejs.org/api/http.html#http_http_request_options_callback
  var path = opts.path
  var qs = querystring.stringify(opts.query)
  if (qs)
    path += '?' + qs

  var options = {
    hostname: opts.host,
    port: opts.port,
    method: opts.method,
    path: path,
    headers: opts.headers
  }

  var req = (this.secure ? https : http).request(options)
  this.impl.request = req

  req.once('error', function(err) {
    request.onerror(err)
  })

  req.once('response', function(res) {
    // fixme
    if (opts.body)
      request.onprogress(opts.body.length, opts.body.length)

    request.impl.response = res

    response.setHeaders(res.headers)
    response.setStatus(res.statusCode)
    var size = response.size
    request.onresponse(response)

    // buffering :/
    var buffer
    var chunks
    if (size)
      buffer = new Buffer(size)
    else
      chunks = []

    var loaded = 0
    res.on('data', function(chunk) {
      if (size)
        buffer.write(chunk.toString(), loaded)
      else
        chunks.push(chunk)

      response.onprogress(loaded += chunk.length, response.size)
    })

    res.once('end', function() {
      var body

      if (loaded > 0) {
        if (!size)
          body = Buffer.concat(chunks)
        else
          body = buffer
      }
      response.onend(body)
    })
  })

  if (opts.body) {
    req.once('socket', function() {
      request.onprogress(0, opts.body.length)
    })
  }

  if (typeof opts.body === 'string')
    req.end(opts.body, 'utf8')
  else
    req.end(opts.body)

  this.req = req
}

NodeRequest.prototype.abort = function() {
  this.req.abort()
}

utils.inherits(NodeRequest, Request)

module.exports = NodeRequest
