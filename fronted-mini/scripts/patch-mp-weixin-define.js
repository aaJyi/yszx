/**
 * 为微信小程序构建产物的 app.js 注入 define 兼容代码，解决 "define is not defined" 报错。
 * 须在 app.js 的 require(runtime/vendor/main) 之前执行，故只改 app.js 入口即可。
 *
 * 每次用 HBuilderX「运行」或「发行」到微信小程序后，若仍出现该报错：
 * 1) 确认项目根目录 project.config.json 中 useMultiFrameRuntime 为 false 并已重新编译
 * 2) 仍报错时再执行：node scripts/patch-mp-weixin-define.js
 */

const fs = require('fs')
const path = require('path')

const MARK = '__UNI_MP_DEFINE_SHIM__'

const PATCH =
  ';(function(){var g=typeof globalThis!=="undefined"?globalThis:typeof wx!=="undefined"?wx:Function("return this")();if(g["' +
  MARK +
  '"])return;g["' +
  MARK +
  '"]=1;if(typeof g.define==="function"&&g.define.amd)return;g.define=function(name,deps,factory){if(typeof name!=="string"){factory=deps;deps=name;name=void 0;}if(!Array.isArray(deps)){factory=deps;deps=[];}var module={exports:{}};var exports=module.exports;var require=function(id){if(id==="exports")return exports;if(id==="module")return module;throw new Error("define shim: "+id)};if(typeof factory==="function"){var ret=factory(require,exports,module);if(ret!==void 0)module.exports=ret}return module.exports};g.define.amd={}})();\n'

const appJsPaths = [
  path.join(__dirname, '../unpackage/dist/dev/mp-weixin/app.js'),
  path.join(__dirname, '../unpackage/dist/build/mp-weixin/app.js'),
]

appJsPaths.forEach((filePath) => {
  if (!fs.existsSync(filePath)) return
  let content = fs.readFileSync(filePath, 'utf8')
  if (content.includes(MARK)) {
    console.log('Already patched:', filePath)
    return
  }
  fs.writeFileSync(filePath, PATCH + content, 'utf8')
  console.log('Patched:', filePath)
})
