// 微信小程序部分运行模式/依赖会产出 AMD define，环境未提供时报 define is not defined
;(function (g) {
  if (typeof g === 'undefined') g = typeof globalThis !== 'undefined' ? globalThis : (typeof wx !== 'undefined' ? wx : this)
  if (typeof g.define === 'function' && g.define.amd) return
  g.define = function (name, deps, factory) {
    if (typeof name !== 'string') { factory = deps; deps = name; name = undefined }
    if (!Array.isArray(deps)) { factory = deps; deps = [] }
    var module = { exports: {} }
    var exports = module.exports
    var require = function (id) {
      if (id === 'exports') return exports
      if (id === 'module') return module
      throw new Error('define shim: ' + id)
    }
    if (typeof factory === 'function') {
      var ret = factory(require, exports, module)
      if (ret !== undefined) module.exports = ret
    }
    return module.exports
  }
  g.define.amd = {}
})(typeof globalThis !== 'undefined' ? globalThis : typeof wx !== 'undefined' ? wx : undefined)

import App from './App'
import fontScaleMixin from './utils/fontScaleMixin.js'

// #ifndef VUE3
import Vue from 'vue'
import './uni.promisify.adaptor'
Vue.config.productionTip = false
Vue.mixin(fontScaleMixin)
App.mpType = 'app'
const app = new Vue({
  ...App
})
app.$mount()
// #endif

// #ifdef VUE3
import { createSSRApp } from 'vue'
export function createApp() {
  const app = createSSRApp(App)
  return {
    app
  }
}
// #endif