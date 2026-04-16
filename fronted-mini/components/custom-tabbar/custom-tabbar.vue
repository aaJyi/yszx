<template>
	<view class="custom-tabbar">
		<view 
			class="tabbar-item" 
			v-for="(item, index) in tabList" 
			:key="index"
			:class="{ 'tabbar-item-active': currentIndex === index }"
			@tap="switchTab(item, index)"
		>
			<view class="tabbar-icon-wrapper">
				<uni-icons 
					:type="item.icon" 
					:size="22" 
					:color="currentIndex === index ? activeColor : inactiveColor"
				></uni-icons>
			</view>
			<text class="tabbar-text" :style="{ color: currentIndex === index ? activeColor : inactiveColor }">
				{{ item.text }}
			</text>
		</view>
	</view>
</template>

<script>
	export default {
		name: 'CustomTabbar',
		props: {
			current: {
				type: Number,
				default: 0
			}
		},
		data() {
			return {
				currentIndex: 0,
				activeColor: '#07C160',
				inactiveColor: '#7A7E83',
				tabList: [
					{ pagePath: '/pages/index/index', text: '首页', icon: 'home' },
					{ pagePath: '/pages/ai-chat/index', text: 'AI', icon: 'chatbubble-filled' },
					{ pagePath: '/pages/exercise/index', text: '运动', icon: 'flag' },
					{ pagePath: '/pages/health-archive/list', text: '档案', icon: 'folder-add' },
					{ pagePath: '/pages/social/index', text: '社交', icon: 'contact' },
					{ pagePath: '/pages/profile/profile', text: '我的', icon: 'person' }
				]
			}
		},
		watch: {
			current(newVal) {
				this.currentIndex = newVal
			}
		},
		onShow() {
			// 每次页面显示时更新当前索引
			this.updateCurrentIndex()
		},
		mounted() {
			// 组件挂载时更新当前索引
			this.updateCurrentIndex()
		},
		methods: {
			updateCurrentIndex() {
				// 如果传入了 current prop，优先使用
				if (this.current !== undefined && this.current !== null) {
					this.currentIndex = this.current
					return
				}
				
				// 否则根据当前页面路径自动判断
				const pages = getCurrentPages()
				if (pages.length === 0) return
				
				const currentPage = pages[pages.length - 1]
				const currentPath = '/' + currentPage.route
				
				const index = this.tabList.findIndex(item => item.pagePath === currentPath)
				if (index !== -1) {
					this.currentIndex = index
				}
			},
			switchTab(item, index) {
				if (this.currentIndex === index) {
					return // 如果点击的是当前页面，不进行跳转
				}
				
				// 先更新索引（视觉反馈）
				this.currentIndex = index
				
				// 使用 reLaunch 进行页面跳转（重新加载应用并跳转到指定页面）
				// 这样可以确保页面完全刷新，底部导航栏状态正确
				uni.reLaunch({
					url: item.pagePath,
					success: () => {
						// 跳转成功
					},
					fail: (err) => {
						console.error('页面跳转失败:', err)
						uni.showToast({
							title: '页面跳转失败',
							icon: 'none'
						})
						// 恢复之前的索引
						this.updateCurrentIndex()
					}
				})
			}
		}
	}
</script>

<style lang="scss" scoped>
	/* 底部导航栏始终固定在屏幕下方，不随页面滚动 */
	.custom-tabbar {
		position: fixed !important;
		bottom: 0 !important;
		left: 0;
		right: 0;
		display: flex;
		align-items: center;
		justify-content: space-around;
		height: 98rpx;
		padding-bottom: constant(safe-area-inset-bottom);
		padding-bottom: env(safe-area-inset-bottom);
		background-color: #ffffff;
		border-top: 1rpx solid #e5e5e5;
		z-index: 9999;
		box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.08);
	}

	.tabbar-item {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 8rpx 0;
		transition: all 0.2s;
	}

	.tabbar-item-active {
		.tabbar-icon-wrapper {
			transform: scale(1.1);
		}
	}

	.tabbar-icon-wrapper {
		width: 44rpx;
		height: 44rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 4rpx;
		transition: transform 0.2s;
	}

	.tabbar-text {
		font-size: 18rpx;
		line-height: 1;
		transition: color 0.2s;
	}
</style>
