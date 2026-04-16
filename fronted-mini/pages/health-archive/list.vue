<template>
	<view class="page-root">
		<view :class="['container', rootFontClass]">
		<!-- 顶部导航栏 -->
		<view class="navbar animate-slide-in-down">
			<view class="navbar-left"></view>
			<view class="navbar-center">
				<text class="navbar-title">健康档案</text>
			</view>
			<view class="navbar-right">
				<uni-icons type="plus" size="24" color="#333" @tap="onAddRecord"></uni-icons>
			</view>
		</view>

		<!-- 健康数据统计卡片 -->
		<view class="stats-card animate-slide-in-up">
			<view class="stat-item animate-slide-in-up delay-100">
				<text class="stat-number">{{totalCount}}</text>
				<text class="stat-label">总记录数</text>
			</view>
			<view class="stat-item animate-slide-in-up delay-200">
				<text class="stat-number">{{reportCount}}</text>
				<text class="stat-label">体检报告</text>
			</view>
			<view class="stat-item animate-slide-in-up delay-300">
				<text class="stat-number">{{monitorCount}}</text>
				<text class="stat-label">监测数据</text>
			</view>
		</view>

		<!-- 快捷功能区域 -->
		<view class="section-header animate-slide-in-up delay-200">
			<view class="header-line" style="background: linear-gradient(180deg, #07C160 0%, #00D4AA 100%);"></view>
			<text class="section-title">快捷功能</text>
		</view>

		<view class="quick-actions">
			<view class="action-card animate-slide-in-up delay-300" @tap="onAddRecord">
				<view class="action-icon-wrapper animate-float" style="background-color: #e8f8f0;">
					<uni-icons type="plus" size="24" color="#07C160"></uni-icons>
				</view>
				<text class="action-text">添加记录</text>
			</view>
			<view class="action-card animate-slide-in-up delay-400" @tap="onHealthReport">
				<view class="action-icon-wrapper animate-float delay-100" style="background-color: #f0f9ff;">
					<uni-icons type="bars" size="24" color="#1890ff"></uni-icons>
				</view>
				<text class="action-text">健康报告</text>
			</view>
		</view>

		<!-- 档案记录区域 -->
		<view class="section-header animate-slide-in-up delay-400" style="margin-top: 60rpx;">
			<view class="header-line" style="background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);"></view>
			<text class="section-title">档案记录</text>
		</view>
		
		<!-- 空状态 -->
		<view v-if="archiveList.length === 0" class="empty-state animate-slide-in-up delay-500">
			<uni-icons type="folder" size="120" color="#cccccc"></uni-icons>
			<text class="empty-text">暂无健康档案</text>
			<text class="empty-hint">点击上方按钮添加记录</text>
		</view>

		<!-- 档案列表 -->
		<view v-else class="records-list">
			<view 
				class="record-card animate-slide-in-up" 
				v-for="(item, index) in archiveList" 
				:key="item.archiveId"
				:class="'delay-' + (500 + index * 100)"
				@tap="onArchiveClick(item.archiveId)"
			>
				<view class="record-content">
					<text class="archive-name">{{ item.archiveName || (item.userName || '') + (item.archiveYear || '') + '年度健康档案' || '未命名档案' }}</text>
					<view class="record-info">
						<text class="record-date" v-if="item.archiveDate">{{ item.archiveDate }}</text>
						<text class="record-no" v-if="item.archiveNo">编号：{{ item.archiveNo }}</text>
					</view>
				</view>
				<uni-icons type="right" size="16" color="#cccccc"></uni-icons>
			</view>
		</view>
		</view>
		<!-- 底部导航栏：放在 scale 容器外，保证 fixed 固定 -->
		<custom-tabbar :current="3"></custom-tabbar>
	</view>
</template>

<script>
	import customTabbar from '@/components/custom-tabbar/custom-tabbar.vue'
	import config from '@/utils/config.js'

	export default {
		components: {
			customTabbar
		},
		data() {
			return {
				archiveList: [],
				// 统计数据
				totalCount: 0,
				reportCount: 0,
				monitorCount: 0
			}
		},
		onLoad() {
			this.loadStats()
			this.loadArchiveList()
		},
		onShow() {
			// 每次显示页面时刷新列表和统计
			this.loadStats()
			this.loadArchiveList()
		},
		methods: {
			/**
			 * 加载统计数据
			 */
			loadStats() {
				const loginStatus = uni.getStorageSync('isLogin')
				if (!loginStatus) {
					this.totalCount = 0
					this.reportCount = 0
					this.monitorCount = 0
					return
				}

				const userInfo = uni.getStorageSync('userInfo')
				if (!userInfo || !userInfo.userId) {
					this.totalCount = 0
					this.reportCount = 0
					this.monitorCount = 0
					return
				}

				const token = uni.getStorageSync('token')
				const userId = userInfo.userId

				uni.request({
					url: `${config.baseUrl}/health-archive-process/stats`,
					method: 'GET',
					header: {
						'userId': userId,
						'token': token || '',
						'Content-Type': 'application/json'
					},
					success: (res) => {
						if (res.statusCode === 200 && res.data && res.data.code === 200) {
							const stats = res.data.data || {}
							this.totalCount = stats.totalCount || 0
							this.reportCount = stats.reportCount || 0
							this.monitorCount = stats.monitorCount || 0
						} else {
							this.totalCount = 0
							this.reportCount = 0
							this.monitorCount = 0
						}
					},
					fail: (err) => {
						console.error('加载统计数据失败', err)
						this.totalCount = 0
						this.reportCount = 0
						this.monitorCount = 0
					}
				})
			},
			
			loadArchiveList() {
				const loginStatus = uni.getStorageSync('isLogin')
				if (!loginStatus) {
					// 未登录时显示空状态，不提示
					this.archiveList = []
					return
				}

				const userInfo = uni.getStorageSync('userInfo')
				if (!userInfo || !userInfo.userId) {
					this.archiveList = []
					return
				}

				const token = uni.getStorageSync('token')
				const userId = userInfo.userId

				uni.request({
					url: `${config.baseUrl}/health-archive-process/list`,
					method: 'GET',
					data: {
						userId: userId,
						asCreator: true  // 查询我创建的所有健康档案（包括自己和家人的）
					},
					header: {
						'userId': userId,
						'token': token || ''
					},
					success: (res) => {
						if (res.statusCode === 200 && res.data.code === 200) {
							this.archiveList = res.data.data || []
						} else {
							this.archiveList = []
						}
					},
					fail: (err) => {
						console.error('查询健康档案列表失败', err)
						this.archiveList = []
					}
				})
			},
			onAddRecord() {
				// 跳转到生成健康档案页面
				uni.navigateTo({
					url: '/pages/health-archive/generate'
				})
			},
			onHealthReport() {
				// 跳转到健康报告页面（原始数据列表）
				uni.navigateTo({
					url: '/pages/health-archive/raw-data-list'
				})
			},
			onArchiveClick(archiveId) {
				uni.navigateTo({
					url: `/pages/health-archive/detail?archiveId=${archiveId}`
				})
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page-root {
		min-height: 100vh;
		background: transparent;
	}
	.container {
		min-height: 100vh;
		background: transparent;
		padding-bottom: 120rpx;
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
	.navbar, .content, .stats-card { position: relative; z-index: 1; }

	/* 导航栏 */
	.navbar {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 40rpx 30rpx 20rpx;
	}

	.navbar-left {
		width: 60rpx;
	}

	.navbar-center {
		flex: 1;
		text-align: center;
	}

	.navbar-title {
		font-size: 36rpx;
		font-weight: 600;
		color: #333333;
	}

	.navbar-right {
		width: 60rpx;
		display: flex;
		justify-content: flex-end;
	}

	.content {
		padding: 40rpx 30rpx;
		padding-top: 20rpx;
	}

	/* 统计卡片 */
	.stats-card {
		background: rgba(255, 255, 255, 0.82);
		backdrop-filter: blur(12rpx);
		border: 1rpx solid rgba(255, 255, 255, 0.7);
		border-radius: 16rpx;
		padding: 32rpx;
		margin: 24rpx 30rpx;
		display: flex;
		justify-content: space-around;
		box-shadow: 0 12rpx 30rpx rgba(2, 6, 23, 0.08);
	}

	.stat-item {
		display: flex;
		flex-direction: column;
		align-items: center;
		flex: 1;
	}

	.stat-number {
		font-size: 48rpx;
		font-weight: 700;
		color: #07C160;
		margin-bottom: 8rpx;
	}

	.stat-label {
		font-size: 24rpx;
		color: #666666;
	}

	/* 区域标题 */
	.section-header {
		display: flex;
		align-items: center;
		margin: 40rpx 30rpx 24rpx;
	}

	.header-line {
		width: 6rpx;
		height: 32rpx;
		border-radius: 3rpx;
		margin-right: 16rpx;
	}

	.section-title {
		font-size: 36rpx;
		font-weight: 600;
		color: #333333;
	}

	/* 快捷功能 */
	.quick-actions {
		display: grid;
		grid-template-columns: repeat(2, 1fr);
		gap: 24rpx;
		padding: 0 30rpx;
	}

	.action-card {
		background: rgba(255, 255, 255, 0.82);
		backdrop-filter: blur(12rpx);
		border: 1rpx solid rgba(255, 255, 255, 0.7);
		border-radius: 16rpx;
		padding: 32rpx 24rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
		box-shadow: 0 12rpx 30rpx rgba(2, 6, 23, 0.08);
		transition: all 0.3s;
	}

	.action-card:hover {
		transform: scale(1.03);
		box-shadow: 0 16rpx 36rpx rgba(2, 6, 23, 0.12), 0 0 25rpx rgba(7, 193, 96, 0.15);
	}

	.action-card:active {
		transform: scale(0.98);
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.1);
	}

	.action-icon-wrapper {
		width: 60rpx;
		height: 60rpx;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 20rpx;
		transition: transform 0.2s;
	}

	.action-card:hover .action-icon-wrapper {
		transform: scale(1.1);
	}

	.action-text {
		font-size: 28rpx;
		font-weight: 500;
		color: #333333;
	}

	/* 空状态 */
	.empty-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 120rpx 0;
		margin: 0 30rpx;
	}

	.empty-text {
		font-size: 28rpx;
		color: #999999;
		margin-top: 32rpx;
	}

	.empty-hint {
		font-size: 24rpx;
		color: #cccccc;
		margin-top: 16rpx;
	}

	/* 档案列表 */
	.records-list {
		display: flex;
		flex-direction: column;
		gap: 20rpx;
		padding: 0 30rpx;
	}

	.record-card {
		background: rgba(255, 255, 255, 0.82);
		backdrop-filter: blur(12rpx);
		border: 1rpx solid rgba(255, 255, 255, 0.7);
		border-radius: 16rpx;
		padding: 32rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
		box-shadow: 0 12rpx 30rpx rgba(2, 6, 23, 0.08);
		transition: all 0.3s;
	}

	.record-card:hover {
		transform: scale(1.02);
		box-shadow: 0 16rpx 36rpx rgba(2, 6, 23, 0.12), 0 0 25rpx rgba(102, 126, 234, 0.15);
	}

	.record-card:active {
		transform: scale(0.98);
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.1);
	}

	.record-content {
		flex: 1;
	}

	.archive-name {
		font-size: 32rpx;
		font-weight: 600;
		color: #333333;
		margin-bottom: 12rpx;
		line-height: 1.4;
	}

	.record-info {
		display: flex;
		flex-direction: column;
		gap: 8rpx;
	}

	.record-date {
		font-size: 24rpx;
		color: #999999;
	}

	.record-no {
		font-size: 24rpx;
		color: #999999;
	}
</style>
