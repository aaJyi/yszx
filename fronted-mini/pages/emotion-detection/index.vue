<template>
	<view class="page-root">
	<view :class="['container', rootFontClass]">
		<!-- 顶部标题卡片 -->
		<view class="header-card">
			<text class="header-title">情绪管理</text>
			<text class="header-subtitle">记录每日情绪,关注心理健康</text>
		</view>

		<!-- 今日情绪区域 -->
		<view class="emotion-section">
			<view class="section-header">
				<text class="section-title">今日情绪</text>
				<text class="section-hint">今天感觉如何?点击表情记录你的心情</text>
			</view>
			<view class="emotion-list">
				<view 
					class="emotion-item" 
					v-for="emotion in emotions" 
					:key="emotion.id"
					:class="{ active: selectedEmotion === emotion.id }"
					@tap="selectEmotion(emotion.id)"
				>
					<text class="emotion-emoji">{{emotion.emoji}}</text>
					<text class="emotion-label">{{emotion.label}}</text>
				</view>
			</view>
		</view>

		<!-- 情绪数据统计卡片 -->
		<view class="stats-section">
			<view class="stat-card">
				<view class="stat-icon blue-icon">
					<uni-icons type="bars" size="32" color="#1890ff"></uni-icons>
				</view>
				<text class="stat-value blue-value">{{ statInt(averageEmotion) }}</text>
				<text class="stat-label">平均情绪</text>
			</view>
			<view class="stat-card">
				<view class="stat-icon green-icon">
					<uni-icons type="calendar" size="32" color="#52c41a"></uni-icons>
				</view>
				<text class="stat-value green-value">{{ statInt(recordDays) }}</text>
				<text class="stat-label">记录天数</text>
			</view>
			<view class="stat-card">
				<view class="stat-icon orange-icon">
					<uni-icons type="arrowup" size="32" color="#ff9800"></uni-icons>
				</view>
				<text class="stat-value orange-value">{{ emotionTrendText }}</text>
				<text class="stat-label">趋势</text>
			</view>
		</view>



	</view>
	</view>
</template>

<script>
	import config from '@/utils/config.js'
	
	export default {
		data() {
			return {
				// 选中的情绪
				selectedEmotion: null,
				
				// 情绪选项（对应分数：100-非常开心 80-开心 60-平静 40-低落 20-很难过）
				emotions: [
					{
						id: 1,
						emoji: '😄',
						label: '非常开心',
						score: 100
					},
					{
						id: 2,
						emoji: '😊',
						label: '开心',
						score: 80
					},
					{
						id: 3,
						emoji: '😐',
						label: '平静',
						score: 60
					},
					{
						id: 4,
						emoji: '😔',
						label: '低落',
						score: 40
					},
					{
						id: 5,
						emoji: '😢',
						label: '很难过',
						score: 20
					}
				],
				
				// 统计数据
				averageEmotion: 0,
				recordDays: 0,
				emotionTrend: '平稳情绪',
				/** 无任何情绪记录时为 true，趋势区不展示误导性文案 */
				hasEmotionHistory: false,
				todayEmotion: null,
				hasRecordedToday: false, // 今天是否已记录情绪
				
				// 心理评估列表
				assessments: [
					{
						id: 1,
						name: 'PHQ-9 抑郁量表评估抑郁程度',
						icon: 'clipboard',
						type: 'phq9'
					},
					{
						id: 2,
						name: 'GAD-7 焦虑量表评估焦虑程度',
						icon: 'checkbox',
						type: 'gad7'
					},
					{
						id: 3,
						name: 'PSS 压力量表评估压力水平',
						icon: 'bars',
						type: 'pss'
					}
				],
				
				// 当前用户ID
				currentUserId: null
			}
		},
		onLoad(options) {
			this.syncUserId()
			this.loadEmotionData()
		},
		onShow() {
			this.syncUserId()
			this.loadEmotionData()
		},
		computed: {
			emotionTrendText() {
				if (!this.hasEmotionHistory) return '—'
				return this.emotionTrend || '—'
			}
		},
		methods: {
			syncUserId() {
				const userInfo = uni.getStorageSync('userInfo')
				this.currentUserId = userInfo && userInfo.userId != null ? userInfo.userId : null
			},
			/** 无记录或非数字时显示 0 */
			statInt(v) {
				if (v === null || v === undefined || v === '') return 0
				const n = Number(v)
				return Number.isFinite(n) ? Math.max(0, Math.round(n)) : 0
			},
			resetEmotionStatsZero() {
				this.averageEmotion = 0
				this.recordDays = 0
				this.emotionTrend = '平稳情绪'
				this.hasEmotionHistory = false
				this.todayEmotion = null
				this.hasRecordedToday = false
				this.selectedEmotion = null
			},
			/**
			 * 加载情绪监测数据
			 */
			loadEmotionData() {
				const self = this
				
				if (!self.currentUserId) {
					self.resetEmotionStatsZero()
					return
				}
				
				const uid = encodeURIComponent(String(self.currentUserId))
				uni.request({
					url: config.baseUrl + '/emotion-monitoring/data?userId=' + uid,
					method: 'GET',
					header: {
						'Content-Type': 'application/json'
					},
					success: function(res) {
						let ok = false
						if (res.statusCode === 200 && res.data) {
							const body = res.data
							const code = body.code
							if (code !== 200 && code !== '200') {
								self.resetEmotionStatsZero()
								return
							}
							const data = body.data != null ? body.data : {}
							if (typeof data !== 'object') {
								self.resetEmotionStatsZero()
								return
							}
							const rd = Number(data.recordDays)
							const avg = data.averageEmotion != null ? Number(data.averageEmotion) : NaN
							self.recordDays = Number.isFinite(rd) ? Math.max(0, Math.floor(rd)) : 0
							self.averageEmotion = Number.isFinite(avg) ? avg : 0
							self.hasEmotionHistory = self.recordDays > 0
							self.todayEmotion = data.todayEmotion != null ? data.todayEmotion : null
							self.hasRecordedToday = self.todayEmotion !== null && self.todayEmotion !== undefined
							const se = data.stableEmotion
							if (self.hasEmotionHistory) {
								if (se === 1) {
									self.emotionTrend = '上升情绪'
								} else if (se === 0) {
									self.emotionTrend = '下降情绪'
								} else {
									self.emotionTrend = typeof data.emotionTrend === 'string' ? data.emotionTrend : '平稳情绪'
								}
							} else {
								self.emotionTrend = '平稳情绪'
							}
							if (self.todayEmotion) {
								const emotion = self.emotions.find(function(e) {
									return e.score === self.todayEmotion
								})
								self.selectedEmotion = emotion ? emotion.id : null
							} else {
								self.selectedEmotion = null
							}
							ok = true
						}
						if (!ok) self.resetEmotionStatsZero()
					},
					fail: function(err) {
						console.error('查询情绪监测数据失败:', err)
						self.resetEmotionStatsZero()
					}
				})
			},
			
			/**
			 * 选择情绪
			 */
			selectEmotion(emotionId) {
				const self = this
				self.selectedEmotion = emotionId
				const emotion = self.emotions.find(function(e) {
					return e.id === emotionId
				})
				
				if (!emotion) {
					uni.showToast({
						title: '选择情绪失败',
						icon: 'none'
					})
					return
				}
				
				if (!self.currentUserId) {
					uni.showToast({
						title: '请先登录',
						icon: 'none'
					})
					return
				}
				
				// 如果今天已记录且选择的是同一个情绪，不需要重复提交
				if (self.hasRecordedToday && self.todayEmotion === emotion.score) {
					uni.showToast({
						title: '今天已记录此情绪',
						icon: 'none'
					})
					return
				}
				
				// 调用接口记录情绪（如果今天已记录，后端会自动更新）
				uni.request({
					url: config.baseUrl + '/emotion-monitoring/record',
					method: 'POST',
					data: {
						userId: self.currentUserId,
						todayEmotion: emotion.score
					},
					header: {
						'Content-Type': 'application/json'
					},
					success: function(res) {
						if (res.statusCode === 200 && res.data && res.data.code === 200) {
							// 根据是否已记录显示不同的提示
							if (self.hasRecordedToday) {
								uni.showToast({
									title: '已修改为：' + emotion.label,
									icon: 'success'
								})
							} else {
								uni.showToast({
									title: '已记录：' + emotion.label,
									icon: 'success'
								})
							}
							
							// 重新加载数据以更新统计
							self.loadEmotionData()
						} else {
							console.error('记录情绪失败:', res.data)
							uni.showToast({
								title: res.data && res.data.message ? res.data.message : '记录情绪失败',
								icon: 'none'
							})
						}
					},
					fail: function(err) {
						console.error('记录情绪失败:', err)
						uni.showToast({
							title: '网络请求失败',
							icon: 'none'
						})
					}
				})
			},
			
		}
	}
</script>

<style lang="scss" scoped>
	.page-root { min-height: 100vh; background: transparent; }
	.container {
		min-height: 100vh;
		background: transparent;
		padding-bottom: 40rpx;
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
	.header-card, .emotion-section, .assessments-section { position: relative; z-index: 1; }

	/* 顶部标题卡片 */
	.header-card {
		padding: 60rpx 32rpx 80rpx 32rpx;
		background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
		border-radius: 0 0 40rpx 40rpx;
		display: flex;
		flex-direction: column;
	}

	.header-title {
		font-size: 48rpx;
		font-weight: 700;
		color: #ffffff;
		margin-bottom: 16rpx;
	}

	.header-subtitle {
		font-size: 24rpx;
		color: #ffffff;
		opacity: 0.9;
	}

	/* 今日情绪区域 */
	.emotion-section {
		margin: -40rpx 32rpx 24rpx 32rpx;
		background-color: #ffffff;
		border-radius: 24rpx;
		padding: 32rpx;
		box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.08);
	}

	.section-header {
		margin-bottom: 32rpx;
	}

	.section-title {
		font-size: 32rpx;
		font-weight: 600;
		color: #333333;
		display: block;
		margin-bottom: 8rpx;
	}

	.section-hint {
		font-size: 24rpx;
		color: #999999;
		display: block;
	}

	.emotion-list {
		display: flex;
		justify-content: space-around;
		gap: 16rpx;
	}

	.emotion-item {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 24rpx 16rpx;
		border-radius: 16rpx;
		border: 2rpx solid #f0f0f0;
		transition: all 0.3s;
	}

	.emotion-item.active {
		border-color: #1890ff;
		background-color: rgba(24, 144, 255, 0.05);
	}

	.emotion-emoji {
		font-size: 64rpx;
		margin-bottom: 12rpx;
	}

	.emotion-label {
		font-size: 24rpx;
		color: #333333;
	}

	/* 情绪数据统计卡片 */
	.stats-section {
		display: flex;
		gap: 16rpx;
		margin: 24rpx 32rpx;
	}

	.stat-card {
		flex: 1;
		background-color: #ffffff;
		border-radius: 16rpx;
		padding: 32rpx 24rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
	}

	.stat-icon {
		width: 64rpx;
		height: 64rpx;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 16rpx;
	}

	.blue-icon {
		background-color: rgba(24, 144, 255, 0.1);
	}

	.green-icon {
		background-color: rgba(82, 196, 26, 0.1);
	}

	.orange-icon {
		background-color: rgba(255, 152, 0, 0.1);
	}

	.stat-value {
		font-size: 48rpx;
		font-weight: 700;
		margin-bottom: 8rpx;
	}

	.blue-value {
		color: #1890ff;
	}

	.green-value {
		color: #52c41a;
	}

	.orange-value {
		color: #ff9800;
		font-size: 28rpx;
	}

	.stat-label {
		font-size: 24rpx;
		color: #666666;
	}

	/* 心理评估区域 */
	.assessment-section {
		margin: 24rpx 32rpx;
	}

	.assessment-list {
		background-color: #ffffff;
		border-radius: 16rpx;
		overflow: hidden;
		margin-top: 24rpx;
	}

	.assessment-item {
		display: flex;
		align-items: center;
		padding: 32rpx;
		border-bottom: 1rpx solid #f0f0f0;
	}

	.assessment-item:last-child {
		border-bottom: none;
	}

	.assessment-icon {
		width: 64rpx;
		height: 64rpx;
		border-radius: 50%;
		background-color: rgba(24, 144, 255, 0.1);
		display: flex;
		align-items: center;
		justify-content: center;
		margin-right: 24rpx;
		flex-shrink: 0;
	}

	.assessment-text {
		flex: 1;
		font-size: 28rpx;
		color: #333333;
	}

</style>
