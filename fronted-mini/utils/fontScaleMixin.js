/**
 * 全局字体缩放 mixin：从设置页读取 fontSizeScale，页面根 view 绑定 :class="rootFontClass" 后整页缩放生效
 */
function getStoredScale() {
  try {
    const s = uni.getStorageSync('fontSizeScale')
    if (s != null && s !== '') return Number(s) || 1
  } catch (e) {}
  return 1
}

export default {
  data() {
    return {
      fontSizeScale: 1
    }
  },
  created() {
    if (typeof uni !== 'undefined') {
      this.fontSizeScale = getStoredScale()
    }
  },
  onShow() {
    if (typeof uni !== 'undefined') {
      this.fontSizeScale = getStoredScale()
    }
  },
  computed: {
    rootFontClass() {
      const map = { 0.8: 'small', 1: 'normal', 1.2: 'large', 1.4: 'xlarge' }
      return 'global-font-' + (map[this.fontSizeScale] || 'normal')
    }
  }
}
