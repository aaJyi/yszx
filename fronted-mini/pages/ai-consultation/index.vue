<template>
	<view :class="['container', rootFontClass]">
		<!-- 自定义导航栏 -->
		<view class="custom-navbar">
			<view class="navbar-content">
				<view class="navbar-left" @click="goBack">
					<uni-icons type="left" size="20" color="#333333"></uni-icons>
				</view>
				<view class="navbar-title">AI问诊</view>
				<view class="navbar-right"></view>
			</view>
		</view>

		<!-- 选择问诊类型 -->
		<view class="section">
			<view class="section-title">选择问诊类型</view>
			<view class="consultation-types">
				<view class="type-card" @tap="selectType('ai-doctor')">
					<view class="type-icon">
						<uni-icons type="chatbubble-filled" size="48" color="#07C160"></uni-icons>
					</view>
					<view class="type-content">
						<view class="type-name">AI医生</view>
						<view class="type-desc">智能健康助手，实时回答健康问题</view>
					</view>
				</view>
			</view>
		</view>

		<!-- 问诊说明 -->
		<view class="section">
			<view class="section-title">问诊说明</view>
			<view class="instructions">
				<view class="instruction-item">
					<uni-icons type="info-filled" size="16" color="#1890ff"></uni-icons>
					<text class="instruction-text">AI问诊仅供参考，不能替代专业医生的诊断和治疗</text>
				</view>
				<view class="instruction-item">
					<uni-icons type="info-filled" size="16" color="#1890ff"></uni-icons>
					<text class="instruction-text">请详细描述您的症状，以便AI能够提供更准确的建议</text>
				</view>
				<view class="instruction-item">
					<uni-icons type="info-filled" size="16" color="#1890ff"></uni-icons>
					<text class="instruction-text">对于紧急医疗情况，请立即就医或拨打急救电话</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
			}
		},
		methods: {
			// 选择问诊类型
			selectType(type) {
				if (type === 'ai-doctor') {
					uni.navigateTo({
						url: '/pages/ai-consultation/chat'
					})
				}
			},
			// 返回上一页
			goBack() {
				uni.navigateBack()
			}
		}
	}
</script>

<style lang="scss" scoped>
	.container {
		min-height: 100vh;
		background-color: #f5f5f5;
	}

	/* 自定义导航栏 */
	.custom-navbar {
		position: fixed;
		top: 0;
		left: 0;
		right: 0;
		z-index: 999;
		background-color: #ffffff;
		border-bottom: 1px solid #e5e5e5;
	}

	.navbar-content {
		display: flex;
		align-items: center;
		justify-content: space-between;
		height: 88rpx;
		padding: 0 32rpx;
		padding-top: var(--status-bar-height, 0);
	}

	.navbar-left {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 80rpx;
		height: 88rpx;
	}

	.navbar-title {
		flex: 1;
		text-align: center;
		font-size: 32rpx;
		font-weight: 500;
		color: #333333;
	}

	.navbar-right {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 80rpx;
		height: 88rpx;
	}

	/* 内容区域 */
	.section {
		margin-top: calc(88rpx + var(--status-bar-height, 0) + 20rpx);
		margin-bottom: 20rpx;
		padding: 0 32rpx;
	}

	.section-title {
		font-size: 32rpx;
		font-weight: bold;
		color: #333333;
		margin-bottom: 24rpx;
	}

	/* 问诊类型选择 */
	.consultation-types {
		display: flex;
		flex-direction: column;
		gap: 24rpx;
	}

	.type-card {
		display: flex;
		align-items: center;
		padding: 32rpx;
		background-color: #ffffff;
		border-radius: 16rpx;
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.1);
		transition: all 0.3s;
	}

	.type-card:hover {
		transform: translateY(-4rpx);
		box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.15);
	}

	.type-icon {
		width: 120rpx;
		height: 120rpx;
		border-radius: 50%;
		background-color: #f0f9ff;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-right: 32rpx;
	}

	.type-content {
		flex: 1;
	}

	.type-name {
		font-size: 36rpx;
		font-weight: bold;
		color: #333333;
		margin-bottom: 12rpx;
	}

	.type-desc {
		font-size: 28rpx;
		color: #666666;
		line-height: 1.5;
	}

	/* 问诊说明 */
	.instructions {
		background-color: #ffffff;
		border-radius: 16rpx;
		padding: 24rpx;
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.1);
	}

	.instruction-item {
		display: flex;
		align-items: flex-start;
		margin-bottom: 20rpx;
	}

	.instruction-item:last-child {
		margin-bottom: 0;
	}

	.instruction-item uni-icons {
		margin-right: 16rpx;
		margin-top: 4rpx;
	}

	.instruction-text {
		flex: 1;
		font-size: 26rpx;
		color: #666666;
		line-height: 1.6;
	}
</style>