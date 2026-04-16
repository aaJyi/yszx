<template>
	<view :class="['container', rootFontClass]">
		<view class="content">
			<!-- 生成动画 -->
			<view class="generating-section">
				<view class="animation-wrapper">
					<view class="spinner" v-if="generating"></view>
					<uni-icons v-else-if="success" type="checkmarkempty" size="80" color="#07C160"></uni-icons>
					<uni-icons v-else-if="error" type="closeempty" size="80" color="#ff3b30"></uni-icons>
				</view>
				<text class="status-text">{{statusText}}</text>
				<text class="status-desc" v-if="statusDesc">{{statusDesc}}</text>
			</view>

			<!-- 生成结果 -->
			<view class="result-section" v-if="archiveData && !generating">
				<view class="result-card">
					<text class="result-title">健康档案生成成功</text>
					<view class="result-info">
						<text class="info-label">档案名称：</text>
						<text class="info-value">{{archiveData.archiveName}}</text>
					</view>
					<view class="result-info">
						<text class="info-label">档案编号：</text>
						<text class="info-value">{{archiveData.archiveNo}}</text>
					</view>
					<view class="result-info">
						<text class="info-label">创建日期：</text>
						<text class="info-value">{{archiveData.archiveDate}}</text>
					</view>
				</view>
			</view>

			<!-- 操作按钮 -->
			<view class="action-section" v-if="!generating">
				<button class="action-btn primary" @tap="viewArchive" v-if="success">查看档案</button>
				<button class="action-btn" @tap="goBack" v-if="success">返回列表</button>
				<button class="action-btn" @tap="retry" v-if="error">重试</button>
				<button class="action-btn" @tap="goBack" v-if="error">返回</button>
			</view>
		</view>
	</view>
</template>

<script>
	import config from '@/utils/config.js'

	export default {
		data() {
			return {
				generating: true,
				success: false,
				error: false,
				statusText: '正在生成健康档案...',
				statusDesc: '',
				archiveData: null
			}
		},
		onLoad() {
			this.generateArchive()
		},
		methods: {
			/**
			 * 生成健康档案
			 */
			async generateArchive() {
				this.generating = true
				this.success = false
				this.error = false
				this.statusText = '正在生成健康档案...'
				this.statusDesc = '请稍候，正在为您创建最新的健康档案'

				const userInfo = uni.getStorageSync('userInfo')
				if (!userInfo || !userInfo.userId) {
					this.showError('请先登录')
					return
				}

				const token = uni.getStorageSync('token')
				const userId = userInfo.userId

				try {
					// 1. 调用后端接口生成健康档案（复制基本信息）
					this.statusText = '正在创建健康档案...'
					this.statusDesc = '正在复制基本信息'

					const createRes = await this.createHealthArchive(userId, token)
					if (!createRes.success) {
						this.showError(createRes.message || '创建健康档案失败')
						return
					}

					this.archiveData = createRes.data
					this.statusText = '正在分析健康数据...'
					this.statusDesc = '正在调用智能体生成其他信息'

					// 2. 调用智能体接口生成其他信息（异步，不阻塞）
					// 注意：这里可以异步调用，不等待结果
					this.callAiAgent(userId, this.archiveData.archiveId).catch(err => {
						console.error('调用智能体接口失败', err)
						// 即使智能体调用失败，也不影响档案创建成功
					})

					// 3. 显示成功
					this.generating = false
					this.success = true
					this.statusText = '健康档案生成成功'
					this.statusDesc = '基本信息已复制，其他信息正在后台生成中'

				} catch (err) {
					console.error('生成健康档案失败', err)
					this.showError(err.message || '生成健康档案失败，请重试')
				}
			},

			/**
			 * 创建健康档案
			 */
			createHealthArchive(userId, token) {
				return new Promise((resolve, reject) => {
					uni.request({
						url: `${config.baseUrl}/health-archive-process/generate-latest`,
						method: 'POST',
						header: {
							'userId': userId,
							'token': token || '',
							'Content-Type': 'application/json'
						},
						success: (res) => {
							if (res.statusCode === 200 && res.data && res.data.code === 200) {
								resolve({
									success: true,
									data: res.data.data
								})
							} else {
								resolve({
									success: false,
									message: res.data?.message || '创建健康档案失败'
								})
							}
						},
						fail: (err) => {
							reject(err)
						}
					})
				})
			},

			/**
			 * 调用智能体接口生成其他信息
			 */
			async callAiAgent(userId, archiveId) {
				// 这里调用智能体的生成健康档案接口
				// 根据实际接口文档调整
				// 示例：调用智能体的生成接口
				try {
					// TODO: 根据实际智能体接口文档调用
					// 例如：POST /health-ai-analysis/generate-archive/{userId}?archiveId={archiveId}
					console.log('调用智能体接口生成其他信息，用户ID：', userId, '档案ID：', archiveId)
					
					// 这里可以调用智能体的接口
					// 智能体会分析原始数据和最新健康档案，生成其他信息
					
				} catch (err) {
					console.error('调用智能体接口失败', err)
				}
			},

			/**
			 * 显示错误
			 */
			showError(message) {
				this.generating = false
				this.error = true
				this.statusText = '生成失败'
				this.statusDesc = message
			},

			/**
			 * 查看档案
			 */
			viewArchive() {
				if (this.archiveData && this.archiveData.archiveId) {
					uni.navigateTo({
						url: `/pages/health-archive/detail?archiveId=${this.archiveData.archiveId}`
					})
				}
			},

			/**
			 * 返回列表
			 */
			goBack() {
				// 返回上一页并刷新
				uni.navigateBack({
					delta: 1
				})
				// 延迟刷新，确保页面已返回
				setTimeout(() => {
					const pages = getCurrentPages()
					const prevPage = pages[pages.length - 1]
					if (prevPage) {
						// 调用列表页面的刷新方法
						if (typeof prevPage.loadArchiveList === 'function') {
							prevPage.loadArchiveList()
						}
						if (typeof prevPage.loadStats === 'function') {
							prevPage.loadStats()
						}
					}
				}, 300)
			},

			/**
			 * 重试
			 */
			retry() {
				this.generateArchive()
			}
		}
	}
</script>

<style lang="scss" scoped>
	.container {
		min-height: 100vh;
		background-color: #f5f5f5;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.content {
		width: 100%;
		padding: 60rpx 40rpx;
	}

	.generating-section {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 80rpx 0;
	}

	.animation-wrapper {
		width: 160rpx;
		height: 160rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 40rpx;
	}

	.spinner {
		width: 80rpx;
		height: 80rpx;
		border: 6rpx solid #e5e5e5;
		border-top-color: #07C160;
		border-radius: 50%;
		animation: spin 1s linear infinite;
	}

	@keyframes spin {
		to {
			transform: rotate(360deg);
		}
	}

	.status-text {
		font-size: 36rpx;
		font-weight: 600;
		color: #333333;
		margin-bottom: 16rpx;
	}

	.status-desc {
		font-size: 28rpx;
		color: #999999;
		text-align: center;
	}

	.result-section {
		margin-top: 40rpx;
	}

	.result-card {
		background-color: #ffffff;
		border-radius: 16rpx;
		padding: 40rpx;
		box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.06);
	}

	.result-title {
		font-size: 32rpx;
		font-weight: 600;
		color: #07C160;
		display: block;
		margin-bottom: 32rpx;
		text-align: center;
	}

	.result-info {
		display: flex;
		margin-bottom: 24rpx;
		align-items: center;
	}

	.info-label {
		font-size: 28rpx;
		color: #666666;
		width: 160rpx;
	}

	.info-value {
		font-size: 28rpx;
		color: #333333;
		flex: 1;
	}

	.action-section {
		margin-top: 60rpx;
		display: flex;
		flex-direction: column;
		gap: 24rpx;
	}

	.action-btn {
		width: 100%;
		height: 88rpx;
		border-radius: 44rpx;
		font-size: 32rpx;
		font-weight: 500;
		border: none;
		background-color: #ffffff;
		color: #333333;
		border: 2rpx solid #e5e5e5;
	}

	.action-btn.primary {
		background-color: #07C160;
		color: #ffffff;
		border-color: #07C160;
	}

	.action-btn:active {
		opacity: 0.8;
	}
</style>
