<template>
	<view class="page">
		<!-- 微信小程序需在后台配置业务域名（如 toutiao.com、so.toutiao.com）后 web-view 才可加载 -->
		<web-view v-if="src" :src="src"></web-view>
		<view v-else class="fallback">
			<text class="t">无法打开链接</text>
			<button class="btn" @tap="copyLink">复制链接到浏览器打开</button>
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				src: '',
				rawUrl: ''
			}
		},
		onLoad(query) {
			if (query.url) {
				this.rawUrl = decodeURIComponent(query.url)
				this.src = this.rawUrl
			}
		},
		methods: {
			copyLink() {
				if (!this.rawUrl) return
				uni.setClipboardData({
					data: this.rawUrl,
					success: () => {
						uni.showToast({ title: '已复制', icon: 'success' })
					}
				})
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; }
	.fallback { padding: 60rpx; text-align: center; }
	.t { font-size: 28rpx; color: #666; display: block; margin-bottom: 40rpx; }
	.btn { background: #07C160; color: #fff; font-size: 28rpx; }
</style>
