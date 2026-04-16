<template>
	<view class="page-root">
	<view class="container">
		<!-- 顶部导航栏 -->
		<view class="navbar">
			<view class="navbar-left" @tap="goBack">
				<uni-icons type="back" size="24" color="#333"></uni-icons>
			</view>
			<view class="navbar-center">
				<text class="navbar-title">设置</text>
			</view>
			<view class="navbar-right"></view>
		</view>

		<!-- 内容区域 -->
		<view class="content">
			<!-- 字体大小设置 -->
			<view class="setting-section">
				<view class="font-size-block">
					<text class="font-size-block-label">字体大小</text>
					<view class="font-size-controls-row">
						<view class="font-size-icon-wrap" @tap="decreaseFontSize">
							<uni-icons type="minus" size="20" color="#07C160"></uni-icons>
						</view>
						<text class="font-size-value">{{ currentFontSize }}</text>
						<view class="font-size-icon-wrap" @tap="increaseFontSize">
							<uni-icons type="plus" size="20" color="#07C160"></uni-icons>
						</view>
					</view>
				</view>
				<view class="font-size-preview">
					<view class="preview-text" :style="previewStyleObj">医视智行</view>
				</view>
			</view>

			<!-- 其他设置项 -->
			<view class="setting-section">
				<view class="setting-item">
					<text class="setting-label">通知设置</text>
					<uni-icons type="right" size="16" color="#cccccc"></uni-icons>
				</view>
				<view class="setting-item">
					<text class="setting-label">隐私设置</text>
					<uni-icons type="right" size="16" color="#cccccc"></uni-icons>
				</view>
				<view class="setting-item">
					<text class="setting-label">关于我们</text>
					<uni-icons type="right" size="16" color="#cccccc"></uni-icons>
				</view>
			</view>
		</view>
	</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				fontSizeScale: 1,
				fontSizeOptions: [0.8, 1, 1.2, 1.4],
				currentFontSize: '标准',
				previewStyleObj: {}
			}
		},
		onLoad() {
			// 从本地存储加载字体大小设置
			const savedScale = uni.getStorageSync('fontSizeScale')
			if (savedScale != null && this.fontSizeOptions.indexOf(Number(savedScale)) >= 0) {
				this.fontSizeScale = Number(savedScale)
			} else if (savedScale != null) {
				this.fontSizeScale = 1
				uni.setStorageSync('fontSizeScale', 1)
			}
			this.updateFontSizeText()
			this.refreshPreviewStyle()
		},
		onShow() {
			this.refreshPreviewStyle()
		},
		methods: {
			refreshPreviewStyle() {
				const s = Number(this.fontSizeScale)
				const scale = Number.isFinite(s) ? s : 1
				const rpx = 40 * scale
				let px = rpx
				if (typeof uni !== 'undefined' && typeof uni.upx2px === 'function') {
					try {
						px = uni.upx2px(rpx)
					} catch (e) {}
				} else {
					px = rpx * 0.52
				}
				const line = Math.round(px * 1.4)
				this.previewStyleObj = {
					fontSize: px + 'px',
					lineHeight: line + 'px',
					color: '#333333',
					fontWeight: '600'
				}
			},
			currentScaleIndex() {
				let i = this.fontSizeOptions.indexOf(this.fontSizeScale)
				if (i >= 0) return i
				const s = Number(this.fontSizeScale)
				i = this.fontSizeOptions.findIndex((x) => Math.abs(x - s) < 1e-6)
				return i >= 0 ? i : 1
			},
			goBack() {
				uni.navigateBack()
			},
			increaseFontSize() {
				const currentIndex = this.currentScaleIndex()
				if (currentIndex < this.fontSizeOptions.length - 1) {
					this.fontSizeScale = this.fontSizeOptions[currentIndex + 1]
					this.updateFontSizeText()
					this.refreshPreviewStyle()
					this.saveFontSizeSetting()
					this.updateGlobalFontSize()
				}
			},
			decreaseFontSize() {
				const currentIndex = this.currentScaleIndex()
				if (currentIndex > 0) {
					this.fontSizeScale = this.fontSizeOptions[currentIndex - 1]
					this.updateFontSizeText()
					this.refreshPreviewStyle()
					this.saveFontSizeSetting()
					this.updateGlobalFontSize()
				}
			},
			updateFontSizeText() {
				const labels = ['小', '标准', '大', '超大']
				const i = this.currentScaleIndex()
				this.currentFontSize = labels[i] || '标准'
			},
			saveFontSizeSetting() {
				uni.setStorageSync('fontSizeScale', this.fontSizeScale)
				try {
					const app = getApp()
					if (app && app.globalData) app.globalData.fontSizeScale = this.fontSizeScale
				} catch (e) {}
			},
			updateGlobalFontSize() {
				uni.$emit('updateFontSize', this.fontSizeScale)
			}
		}
	}
</script>

<style lang="scss">
	.page-root { min-height: 100vh; background: transparent; }
	.container {
		width: 100%;
		min-height: 100vh;
		background: transparent;
		position: relative;
	}
	.container::before,
	.container::after {
		content: '';
		position: absolute;
		border-radius: 9999rpx;
		filter: blur(24rpx);
		opacity: 0.9;
		z-index: 0;
		pointer-events: none;
	}
	.container::before {
		width: 520rpx; height: 520rpx;
		left: -220rpx; top: 160rpx;
		background: radial-gradient(circle at 30% 30%, rgba(7,193,96,0.35), transparent 60%);
		animation: floaty 6.8s ease-in-out infinite;
	}
	.container::after {
		width: 600rpx; height: 600rpx;
		right: -260rpx; top: 420rpx;
		background: radial-gradient(circle at 30% 30%, rgba(24,144,255,0.28), transparent 60%);
		animation: floaty 8.2s ease-in-out infinite;
	}
	@keyframes floaty {
		0%, 100% { transform: translate(0, 0); }
		50% { transform: translate(20rpx, -20rpx); }
	}
	.navbar, .content { position: relative; z-index: 1; }

	.navbar {
		display: flex;
		align-items: center;
		justify-content: space-between;
		height: 100rpx;
		background-color: #ffffff;
		padding: 0 20rpx;
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.08);

		.navbar-left {
			width: 60rpx;
			display: flex;
			align-items: center;
			justify-content: flex-start;
		}

		.navbar-center {
			flex: 1;
			display: flex;
			align-items: center;
			justify-content: center;

			.navbar-title {
				font-size: 36rpx;
				font-weight: 600;
				color: #333333;
			}
		}

		.navbar-right {
			width: 60rpx;
		}
	}

	.content {
		padding: 20rpx;

		.setting-section {
			background-color: #ffffff;
			border-radius: 12rpx;
			margin-bottom: 20rpx;
			overflow: hidden;

			.font-size-block {
				padding: 24rpx 32rpx;
				border-bottom: 1rpx solid #f0f0f0;
				box-sizing: border-box;
				width: 100%;
			}

			.font-size-block-label {
				display: block;
				font-size: 32rpx;
				color: #333333;
				margin-bottom: 20rpx;
			}

			.font-size-controls-row {
				display: flex;
				align-items: center;
				justify-content: space-between;
				width: 100%;
				box-sizing: border-box;
				min-height: 56rpx;
			}

			.font-size-icon-wrap {
				flex-shrink: 0;
				width: 56rpx;
				height: 56rpx;
				display: flex;
				align-items: center;
				justify-content: center;
			}

			.font-size-value {
				flex: 1;
				min-width: 0;
				padding: 0 16rpx;
				font-size: 28rpx;
				color: #07C160;
				text-align: center;
				white-space: nowrap;
				overflow: hidden;
				text-overflow: ellipsis;
			}

			.setting-item {
				display: flex;
				align-items: center;
				justify-content: space-between;
				padding: 24rpx 32rpx;
				border-bottom: 1rpx solid #f0f0f0;
				min-width: 0;

				&:last-child {
					border-bottom: none;
				}

				.setting-label {
					font-size: 32rpx;
					color: #333333;
					flex-shrink: 0;
				}
			}

			.font-size-preview {
				padding: 32rpx 32rpx;
				background-color: #f9f9f9;
				display: flex;
				justify-content: center;
				align-items: center;

				.preview-text {
					font-weight: 600;
				}
			}
		}
	}
</style>