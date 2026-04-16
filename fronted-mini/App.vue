<script>
	export default {
		globalData: {
			fontSizeScale: 1
		},
		onLaunch: function() {
			console.log('医视智行 App Launch')
			const scale = uni.getStorageSync('fontSizeScale')
			this.globalData.fontSizeScale = scale ? Number(scale) : 1
		},
		onShow: function() {
			console.log('App Show')
			const scale = uni.getStorageSync('fontSizeScale')
			this.globalData.fontSizeScale = scale ? Number(scale) : 1
		},
		onHide: function() {
			console.log('App Hide')
		}
	}
</script>

<style lang="scss">
	:root {
		--hl-mint: #07c160;
		--hl-sky: #1890ff;
		--hl-sun: #fa8c16;
		--hl-pink: #ff6b9d;
		--hl-purple: #9c27b0;
		--hl-yellow: #ffc107;
		--hl-text: #0f172a;
		--hl-card: rgba(255, 255, 255, 0.82);
		--hl-card-strong: rgba(255, 255, 255, 0.9);
		--hl-stroke: rgba(255, 255, 255, 0.7);
	}

	/* 每个页面公共 css；全局字体缩放：页面根 view 加 :class="rootFontClass" 后整页按比例缩放 */
	page {
		background:
			radial-gradient(1200rpx 800rpx at 10% 10%, rgba(7, 193, 96, 0.2), transparent 55%),
			radial-gradient(1000rpx 700rpx at 90% 20%, rgba(24, 144, 255, 0.2), transparent 55%),
			radial-gradient(900rpx 700rpx at 30% 90%, rgba(250, 140, 22, 0.2), transparent 55%),
			radial-gradient(1100rpx 800rpx at 70% 70%, rgba(255, 107, 157, 0.15), transparent 55%),
			radial-gradient(800rpx 600rpx at 50% 30%, rgba(156, 39, 176, 0.12), transparent 55%),
			linear-gradient(180deg, #f7fbff 0%, #f5f7fb 100%);
		font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
		color: var(--hl-text);
	}
	.global-font-small {
		transform: scale(0.9);
		transform-origin: 0 0;
		width: 111.11%;
		min-height: 111.11%;
		box-sizing: border-box;
		overflow-x: hidden;
	}
	.global-font-normal {
		transform: scale(1);
		transform-origin: 0 0;
		width: 100%;
		min-height: 100%;
	}
	.global-font-large {
		transform: scale(1.1);
		transform-origin: 0 0;
		width: 90.91%;
		min-height: 90.91%;
		box-sizing: border-box;
		overflow-x: hidden;
	}
	.global-font-xlarge {
		transform: scale(1.2);
		transform-origin: 0 0;
		width: 83.33%;
		min-height: 83.33%;
		box-sizing: border-box;
		overflow-x: hidden;
	}
	
	/* 通用按钮样式 */
	.btn-primary {
		background: linear-gradient(135deg, #07C160 0%, #1890ff 35%, #ff6b9d 70%, #fa8c16 100%);
		color: #ffffff;
		border-radius: 50rpx;
		padding: 24rpx 48rpx;
		font-size: 32rpx;
		font-weight: 500;
		border: none;
		box-shadow: 0 10rpx 24rpx rgba(7, 193, 96, 0.2), 0 0 20rpx rgba(24, 144, 255, 0.15);
		transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
		position: relative;
		overflow: hidden;
	}

	.btn-primary::before {
		content: '';
		position: absolute;
		top: -50%;
		left: -50%;
		width: 200%;
		height: 200%;
		background: linear-gradient(45deg, transparent, rgba(255,255,255,0.3), transparent);
		transform: rotate(45deg);
		animation: shine 2s infinite;
		pointer-events: none;
	}

	@keyframes shine {
		0% { transform: translateX(-100%) rotate(45deg); }
		100% { transform: translateX(100%) rotate(45deg); }
	}

	.btn-primary:hover {
		transform: scale(1.05);
		box-shadow: 0 15rpx 30rpx rgba(7, 193, 96, 0.3), 0 0 25rpx rgba(24, 144, 255, 0.2);
		background: linear-gradient(135deg, #1890ff 0%, #ff6b9d 35%, #fa8c16 70%, #07C160 100%);
	}

	.btn-primary:active {
		transform: scale(0.95);
		box-shadow: 0 5rpx 12rpx rgba(7, 193, 96, 0.3), 0 0 15rpx rgba(24, 144, 255, 0.2);
	}
	
	/* 通用卡片样式 */
	.card {
		background: var(--hl-card);
		backdrop-filter: blur(12rpx);
		border-radius: 24rpx;
		padding: 32rpx;
		margin: 24rpx;
		box-shadow: 0 10rpx 28rpx rgba(2, 6, 23, 0.06), 0 0 20rpx rgba(7, 193, 96, 0.1);
		border: 1rpx solid var(--hl-stroke);
		transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
		position: relative;
	}

	.card::before {
		content: '';
		position: absolute;
		top: 0;
		left: 0;
		right: 0;
		bottom: 0;
		border-radius: 24rpx;
		padding: 1rpx;
		background: linear-gradient(135deg, #07C160, #1890ff, #ff6b9d, #fa8c16);
		-webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
		-webkit-mask-composite: xor;
		mask-composite: exclude;
		pointer-events: none;
	}

	.card:hover {
		transform: translateY(-8rpx);
		box-shadow: 0 15rpx 35rpx rgba(2, 6, 23, 0.12), 0 0 30rpx rgba(24, 144, 255, 0.15);
	}

	/* 轻量动效：用于卡片/按钮的呼吸感 */
	@keyframes floaty {
		0% { transform: translateY(0); }
		50% { transform: translateY(-6rpx); }
		100% { transform: translateY(0); }
	}

	@keyframes shimmer {
		0% { transform: translateX(-60%) skewX(-12deg); opacity: 0; }
		20% { opacity: 1; }
		60% { opacity: 0.6; }
		100% { transform: translateX(160%) skewX(-12deg); opacity: 0; }
	}

	@keyframes pulse {
		0% { transform: scale(1); }
		50% { transform: scale(1.05); }
		100% { transform: scale(1); }
	}

	@keyframes slideInUp {
		from {
			transform: translateY(30rpx);
			opacity: 0;
		}
		to {
			transform: translateY(0);
			opacity: 1;
		}
	}

	@keyframes slideInLeft {
		from {
			transform: translateX(-30rpx);
			opacity: 0;
		}
		to {
			transform: translateX(0);
			opacity: 1;
		}
	}

	@keyframes slideInRight {
		from {
			transform: translateX(30rpx);
			opacity: 0;
		}
		to {
			transform: translateX(0);
			opacity: 1;
		}
	}

	@keyframes slideInDown {
		from {
			transform: translateY(-30rpx);
			opacity: 0;
		}
		to {
			transform: translateY(0);
			opacity: 1;
		}
	}

	/* 通用：玻璃拟态容器（可叠加在任意 view 上） */
	.glass {
		background: var(--hl-card);
		backdrop-filter: blur(14rpx);
		border: 1rpx solid var(--hl-stroke);
		box-shadow: 0 12rpx 30rpx rgba(2, 6, 23, 0.08), 0 0 20rpx rgba(24, 144, 255, 0.1);
		transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
		position: relative;
	}

	.glass::before {
		content: '';
		position: absolute;
		top: 0;
		left: 0;
		right: 0;
		bottom: 0;
		border-radius: inherit;
		padding: 1rpx;
		background: linear-gradient(135deg, #07C160, #1890ff, #ff6b9d, #fa8c16);
		-webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
		-webkit-mask-composite: xor;
		mask-composite: exclude;
		pointer-events: none;
	}

	.glass:hover {
		transform: translateY(-4rpx);
		box-shadow: 0 16rpx 36rpx rgba(2, 6, 23, 0.12), 0 0 30rpx rgba(255, 107, 157, 0.15);
	}

	/* 通用：流光描边（用于卡片边缘的“iOS26”质感） */
	.shine-border {
		position: relative;
		overflow: hidden;
	}
	.shine-border::after {
		content: '';
		position: absolute;
		top: -20%;
		left: -40%;
		width: 60%;
		height: 140%;
		background: linear-gradient(90deg, transparent, rgba(255,255,255,0.65), transparent);
		filter: blur(2rpx);
		animation: shimmer 4.2s ease-in-out infinite;
		pointer-events: none;
	}

	/* 动态效果类 */
	.animate-float {
		animation: floaty 3s ease-in-out infinite;
	}

	.animate-pulse {
		animation: pulse 2s ease-in-out infinite;
	}

	.animate-slide-in-up {
		animation: slideInUp 0.5s ease-out forwards;
	}

	.animate-slide-in-left {
		animation: slideInLeft 0.5s ease-out forwards;
	}

	.animate-slide-in-right {
		animation: slideInRight 0.5s ease-out forwards;
	}

	.animate-slide-in-down {
		animation: slideInDown 0.5s ease-out forwards;
	}

	/* 为不同元素添加延迟动画 */
	.delay-100 {
		animation-delay: 0.1s;
	}

	.delay-200 {
		animation-delay: 0.2s;
	}

	.delay-300 {
		animation-delay: 0.3s;
	}

	.delay-400 {
		animation-delay: 0.4s;
	}

	.delay-500 {
		animation-delay: 0.5s;
	}
</style>
