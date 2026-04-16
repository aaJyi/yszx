<template>
	<view :class="['container', rootFontClass]">
		<view class="navbar">
			<view class="back-btn" @click="goBack">
				<uni-icons type="back" size="24" color="#ffffff"></uni-icons>
			</view>
			<text class="navbar-title">快乐活动</text>
			<view class="navbar-right"></view>
		</view>

		<view class="content">
			<view class="page-header">
				<text class="page-title">活动入口</text>
				<text class="page-subtitle">原社区活动、旅居疗愈相关数据表已下线，本入口保留占位。</text>
			</view>
		</view>
	</view>
</template>

<script>
	export default {
		methods: {
			goBack() {
				uni.navigateBack()
			},
		},
	}
</script>

<style lang="scss" scoped>
	.container {
		min-height: 100vh;
		background-color: #f5f5f5;
	}
	.navbar {
		height: 88rpx;
		background: linear-gradient(135deg, #4caf50 0%, #45a049 100%);
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 0 32rpx;
	}
	.back-btn {
		width: 48rpx;
		height: 48rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}
	.navbar-title {
		font-size: 32rpx;
		font-weight: 600;
		color: #ffffff;
		flex: 1;
		text-align: center;
	}
	.content {
		padding: 48rpx 32rpx;
	}
	.page-title {
		font-size: 36rpx;
		font-weight: 600;
		color: #333;
		display: block;
		margin-bottom: 16rpx;
	}
	.page-subtitle {
		font-size: 28rpx;
		color: #666;
		line-height: 1.6;
	}
</style>
