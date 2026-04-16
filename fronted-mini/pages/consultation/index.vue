<template>
	<view :class="['container', rootFontClass]">
		<view class="section">
			<view class="section-title">选择问诊方式</view>
			<view class="consultation-methods">
				<view
					class="method-card"
					:class="{ active: selectedMethod === 'text' }"
					@tap="selectMethod('text')"
				>
					<view class="method-icon">
						<uni-icons type="chatbubble" size="32" :color="selectedMethod === 'text' ? '#1890ff' : '#666666'"></uni-icons>
					</view>
					<text class="method-name">图文问诊</text>
					<text class="method-desc">AI 辅助健康咨询</text>
				</view>
			</view>
		</view>

		<view class="section">
			<view class="section-title">描述您的症状</view>
			<view class="symptom-input-wrapper">
				<textarea
					class="symptom-input"
					v-model="symptomDescription"
					placeholder="请详细描述您的症状、持续时间等信息..."
					:maxlength="500"
					:auto-height="true"
				></textarea>
				<view class="char-count">{{ symptomDescription.length }}/500</view>
			</view>
		</view>

		<view class="section">
			<button class="start-btn" type="primary" @tap="startChat">进入 AI 问诊对话</button>
			<text class="hint">原「医生列表」依赖的数据表已下线，请直接使用对话页咨询。</text>
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				selectedMethod: 'text',
				symptomDescription: '',
			}
		},
		methods: {
			selectMethod(method) {
				this.selectedMethod = method
			},
			startChat() {
				const q = this.symptomDescription.trim()
					? `symptom=${encodeURIComponent(this.symptomDescription)}`
					: ''
				uni.navigateTo({
					url: `/pages/consultation/chat${q ? '?' + q : ''}`,
				})
			},
		},
	}
</script>

<style lang="scss" scoped>
	.container {
		min-height: 100vh;
		background-color: #f5f5f5;
		padding: 24rpx;
	}
	.section {
		margin-bottom: 32rpx;
	}
	.section-title {
		font-size: 30rpx;
		font-weight: 600;
		margin-bottom: 16rpx;
		color: #333;
	}
	.consultation-methods {
		display: flex;
		gap: 16rpx;
	}
	.method-card {
		flex: 1;
		padding: 24rpx;
		background: #fff;
		border-radius: 12rpx;
		border: 2rpx solid #e5e5e5;
	}
	.method-card.active {
		border-color: #1890ff;
	}
	.method-icon {
		margin-bottom: 12rpx;
	}
	.method-name {
		display: block;
		font-size: 28rpx;
		font-weight: 500;
	}
	.method-desc {
		display: block;
		font-size: 24rpx;
		color: #888;
		margin-top: 8rpx;
	}
	.symptom-input-wrapper {
		background: #fff;
		border-radius: 12rpx;
		padding: 16rpx;
	}
	.symptom-input {
		width: 100%;
		min-height: 200rpx;
		font-size: 28rpx;
	}
	.char-count {
		text-align: right;
		font-size: 24rpx;
		color: #999;
	}
	.start-btn {
		width: 100%;
		background: #1890ff;
		color: #fff;
		border-radius: 12rpx;
		margin-bottom: 16rpx;
	}
	.hint {
		display: block;
		font-size: 24rpx;
		color: #999;
		line-height: 1.5;
	}
</style>
