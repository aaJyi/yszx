<template>
	<view class="page-root">
		<view :class="['container', rootFontClass]">
		<!-- 顶部导航栏 -->
		<view class="navbar animate-slide-in-down">
			<view class="navbar-left"></view>
			<view class="navbar-center">
				<text class="navbar-title">我的</text>
			</view>
			<view class="navbar-right">
				<uni-icons type="settings" size="24" color="#333" @tap="onSettingsClick"></uni-icons>
			</view>
		</view>

		<!-- 未登录状态 -->
		<view v-if="!isLogin" class="not-login-section">
			<view class="login-card animate-slide-in-up">
				<view class="avatar-placeholder animate-float">
					<uni-icons type="person" size="60" color="#07C160"></uni-icons>
				</view>
				<text class="login-text animate-slide-in-up delay-100">点击登录，享受更多服务</text>
				<button class="login-btn animate-slide-in-up delay-200" @tap="goToLogin">立即登录</button>
			</view>
		</view>

		<!-- 已登录状态 -->
		<view v-else class="logged-in-section">
			<!-- 用户信息区域 -->
			<view class="user-info-card animate-slide-in-up" @tap="onUserInfoClick">
				<view class="user-avatar animate-float">
					<image v-if="userInfo.avatarUrl" :src="userInfo.avatarUrl" class="avatar-image" mode="aspectFill"></image>
					<uni-icons v-else type="person" size="50" color="#ffffff"></uni-icons>
				</view>
				<view class="user-details animate-slide-in-up delay-100">
					<text class="user-name">{{ userInfo.nickname || '用户未设置联系方式' }}</text>
					<text class="user-phone" v-if="userInfo.phone">{{ userInfo.phone }}</text>
				</view>
				<uni-icons type="right" size="16" color="#ffffff"></uni-icons>
			</view>

			<!-- 统计数据卡片 -->
			<view class="stats-card animate-slide-in-up delay-200">
				<view class="stat-item animate-slide-in-up delay-300">
					<text class="stat-number">{{ statText(stats.healthRecordCount) }}</text>
					<text class="stat-label">健康报告</text>
				</view>
				<view class="stat-item animate-slide-in-up delay-400">
					<text class="stat-number">{{ statText(stats.familyMemberCount) }}</text>
					<text class="stat-label">家人管理</text>
				</view>
				<view class="stat-item animate-slide-in-up delay-500">
					<text class="stat-number">{{ statText(stats.healthReportCount) }}</text>
					<text class="stat-label">健康档案</text>
				</view>
			</view>

			<!-- 功能菜单列表 -->
			<view class="section-header animate-slide-in-up delay-300">
				<view class="header-line" style="background: linear-gradient(180deg, #07C160 0%, #00D4AA 100%);"></view>
				<text class="section-title">健康管理</text>
			</view>

			<view class="menu-grid">
				<view class="menu-card animate-slide-in-up delay-400" @tap="onMenuClick" data-index="0">
					<view class="menu-icon-wrapper animate-float" style="background-color: #e8f8f0;">
						<uni-icons type="folder-add" size="24" color="#07C160"></uni-icons>
					</view>
					<text class="menu-text">健康档案</text>
				</view>
				<view class="menu-card animate-slide-in-up delay-500" @tap="onMenuClick" data-index="1">
					<view class="menu-icon-wrapper animate-float delay-100" style="background-color: #f0f9ff;">
						<uni-icons type="paperplane" size="24" color="#1890ff"></uni-icons>
					</view>
					<text class="menu-text">健康报告</text>
				</view>
				<view class="menu-card animate-slide-in-up delay-600" @tap="onMenuClick" data-index="2">
					<view class="menu-icon-wrapper animate-float delay-200" style="background-color: #fff8e1;">
						<uni-icons type="chatbubble" size="24" color="#ffc107"></uni-icons>
					</view>
					<text class="menu-text">情绪记录</text>
				</view>
				<view class="menu-card animate-slide-in-up delay-700" @tap="onMenuClick" data-index="3">
					<view class="menu-icon-wrapper animate-float delay-300" style="background-color: #f3e5f5;">
						<uni-icons type="person" size="24" color="#9c27b0"></uni-icons>
					</view>
					<text class="menu-text">家人管理</text>
				</view>
			</view>

			<!-- 退出登录按钮 -->
			<button class="logout-btn animate-slide-in-up delay-800" @tap="onLogout">退出登录</button>
		</view>
		</view>
		<!-- 底部导航栏：放在 scale 容器外，保证 fixed 固定 -->
		<custom-tabbar :current="5"></custom-tabbar>
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
				isLogin: false,
				userInfo: {},
				stats: {
					healthRecordCount: 0,
					familyMemberCount: 0,
					healthReportCount: 0
				}
			}
		},
		onLoad() {
			this.checkLoginStatus()
		},
		onShow() {
			// 每次页面显示时检查登录状态
			this.checkLoginStatus()
			if (!this.isLogin || !this.userInfo || this.userInfo.userId == null) {
				this.stats = { healthRecordCount: 0, familyMemberCount: 0, healthReportCount: 0 }
				return
			}
			// 与数据库同步昵称、头像、手机号（本地仅存登录当次数据，库中修改后需拉取）
			this.refreshUserProfileFromServer()
			this.loadProfileStats()
		},
		methods: {
			/** 无数据、非数字、接口失败时统一显示 0 */
			statText(n) {
				if (n === null || n === undefined || n === '') return '0'
				const num = Number(n)
				return Number.isFinite(num) ? String(Math.max(0, Math.floor(num))) : '0'
			},
			resetStatsZero() {
				this.stats = { healthRecordCount: 0, familyMemberCount: 0, healthReportCount: 0 }
			},
			refreshUserProfileFromServer() {
				const uid = this.userInfo && this.userInfo.userId
				if (uid === null || uid === undefined || uid === '') return
				uni.request({
					url: config.baseUrl + '/t-user/profile',
					method: 'GET',
					header: { userId: String(uid) },
					success: (res) => {
						const body = res.data || {}
						if (res.statusCode !== 200 || body.code !== 200 || !body.data) return
						const d = body.data
						const prev = uni.getStorageSync('userInfo') || {}
						const merged = {
							...prev,
							userId: d.userId != null ? d.userId : prev.userId,
							nickname: d.nickname != null ? d.nickname : prev.nickname,
							avatarUrl: d.avatarUrl != null ? d.avatarUrl : prev.avatarUrl,
							phone: d.phone != null ? d.phone : prev.phone,
							token: prev.token
						}
						this.userInfo = merged
						uni.setStorageSync('userInfo', merged)
					}
				})
			},
			loadProfileStats() {
				const uid = this.userInfo && this.userInfo.userId
				if (uid === null || uid === undefined || uid === '') return
				uni.request({
					url: config.baseUrl + '/t-user/profile-stats',
					method: 'GET',
					header: {
						userId: String(uid)
					},
					success: (res) => {
						let ok = false
						if (res.statusCode === 200 && res.data) {
							const body = res.data
							if (body.code && body.code !== 200) {
								this.resetStatsZero()
								return
							}
							const d = body.data != null ? body.data : body
							if (d && typeof d === 'object') {
								const a = Number(d.healthRecordCount)
								const b = Number(d.familyMemberCount)
								const c = Number(d.healthReportCount)
								this.stats = {
									healthRecordCount: Number.isFinite(a) ? Math.max(0, Math.floor(a)) : 0,
									familyMemberCount: Number.isFinite(b) ? Math.max(0, Math.floor(b)) : 0,
									healthReportCount: Number.isFinite(c) ? Math.max(0, Math.floor(c)) : 0
								}
								ok = true
							}
						}
						if (!ok) this.resetStatsZero()
					},
					fail: () => {
						this.resetStatsZero()
					}
				})
			},
			checkLoginStatus() {
				// 从本地存储检查登录状态
				const loginStatus = uni.getStorageSync('isLogin')
				const userInfo = uni.getStorageSync('userInfo')
				
				this.isLogin = loginStatus === true
				this.userInfo = userInfo || {}
				
			},
			goToLogin() {
				uni.navigateTo({
					url: '/pages/login/login'
				})
			},
			onSettingsClick() {
				uni.navigateTo({
					url: '/pages/settings/index'
				})
			},
			onUserInfoClick() {
				uni.navigateTo({
					url: '/pages/profile/user-info'
				})
			},
			onMenuClick(e) {
				const index = parseInt(e.currentTarget.dataset.index)
				const menuItems = [
					'健康档案',
					'健康报告',
					'情绪记录',
					'家人管理',
					'设置'
				]
				
				// 健康档案跳转到列表页面
				if (index === 0) {
					uni.navigateTo({
						url: '/pages/health-archive/list'
					})
					return
				}

				// 健康报告：跳转到原始数据列表（数据库里用户提交的拍报告等原始资料）
				if (index === 1) {
					uni.navigateTo({
						url: '/pages/health-archive/raw-data-list'
					})
					return
				}
				
				// 情绪记录跳转到情绪管理页面
				if (index === 2) {
					uni.navigateTo({
						url: '/pages/emotion-detection/index'
					})
					return
				}
				
				// 家人管理跳转到家人管理页面
				if (index === 3) {
					uni.navigateTo({
						url: '/pages/family/list'
					})
					return
				}
				
				// 设置跳转到设置页面
				if (index === 4) {
					uni.navigateTo({
						url: '/pages/settings/index'
					})
					return
				}
				
				uni.showToast({
					title: menuItems[index] + '功能开发中',
					icon: 'none'
				})
			},
			onLogout() {
				uni.showModal({
					title: '提示',
					content: '确定要退出登录吗？',
					success: (res) => {
						if (res.confirm) {
							// 清除登录信息
							uni.removeStorageSync('isLogin')
							uni.removeStorageSync('userInfo')
							uni.removeStorageSync('token')
							
							this.isLogin = false
							this.userInfo = {}
							
							uni.showToast({
								title: '已退出登录',
								icon: 'success'
							})
						}
					}
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
	/* 与首页一致的漂浮光斑背景 */
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
	.navbar, .not-login-section, .logged-in-section { position: relative; z-index: 1; }

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

	/* 未登录状态 */
	.not-login-section {
		padding: 40rpx 30rpx;
	}

	.login-card {
		background: rgba(255, 255, 255, 0.82);
		backdrop-filter: blur(12rpx);
		border: 1rpx solid rgba(255, 255, 255, 0.7);
		border-radius: 16rpx;
		padding: 60rpx 32rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
		box-shadow: 0 12rpx 30rpx rgba(2, 6, 23, 0.08);
	}

	.avatar-placeholder {
		width: 120rpx;
		height: 120rpx;
		background-color: #e8f8f0;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 32rpx;
	}

	.login-text {
		font-size: 32rpx;
		color: #333333;
		margin-bottom: 48rpx;
	}

	.login-btn {
		background: linear-gradient(135deg, #07C160 0%, #1890ff 100%);
		color: #ffffff;
		border-radius: 50rpx;
		padding: 20rpx 80rpx;
		font-size: 28rpx;
		font-weight: 500;
		border: none;
		transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
	}

	.login-btn:hover {
		transform: scale(1.05);
		box-shadow: 0 10rpx 24rpx rgba(7, 193, 96, 0.3), 0 0 20rpx rgba(24, 144, 255, 0.15);
		background: linear-gradient(135deg, #1890ff 0%, #07C160 100%);
	}

	.login-btn::after {
		border: none;
	}

	/* 已登录状态 */
	.logged-in-section {
		padding: 0 30rpx;
	}

	/* 用户信息区域 */
	.user-info-card {
		background: linear-gradient(135deg, #07C160 0%, #06A050 100%);
		border-radius: 16rpx;
		padding: 40rpx 32rpx;
		margin: 24rpx 0;
		display: flex;
		align-items: center;
		box-shadow: 0 12rpx 30rpx rgba(7, 193, 96, 0.2);
	}

	.user-avatar {
		width: 100rpx;
		height: 100rpx;
		background-color: rgba(255, 255, 255, 0.2);
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-right: 24rpx;
		overflow: hidden;
	}

	.avatar-image {
		width: 100%;
		height: 100%;
		border-radius: 50%;
	}

	.user-details {
		flex: 1;
		display: flex;
		flex-direction: column;
	}

	.user-name {
		font-size: 32rpx;
		font-weight: 600;
		color: #ffffff;
		margin-bottom: 8rpx;
	}

	.user-phone {
		font-size: 24rpx;
		color: rgba(255, 255, 255, 0.8);
	}

	/* 统计数据卡片 */
	.stats-card {
		background: rgba(255, 255, 255, 0.82);
		backdrop-filter: blur(12rpx);
		border: 1rpx solid rgba(255, 255, 255, 0.7);
		border-radius: 16rpx;
		padding: 32rpx;
		margin: 24rpx 0;
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
		margin: 40rpx 0 24rpx;
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

	/* 功能菜单网格 */
	.menu-grid {
		display: grid;
		grid-template-columns: repeat(2, 1fr);
		gap: 24rpx;
		margin-bottom: 40rpx;
	}

	.menu-card {
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

	.menu-card:hover {
		transform: scale(1.03);
		box-shadow: 0 16rpx 36rpx rgba(2, 6, 23, 0.12), 0 0 25rpx rgba(7, 193, 96, 0.15);
	}

	.menu-card:active {
		transform: scale(0.98);
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.1);
	}

	.menu-icon-wrapper {
		width: 60rpx;
		height: 60rpx;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 20rpx;
		transition: transform 0.2s;
	}

	.menu-card:hover .menu-icon-wrapper {
		transform: scale(1.1);
	}

	.menu-text {
		font-size: 28rpx;
		font-weight: 500;
		color: #333333;
	}

	/* 退出登录按钮 */
	.logout-btn {
		width: 100%;
		background: linear-gradient(135deg, #f44336 0%, #ff6b9d 100%);
		color: #ffffff;
		border-radius: 16rpx;
		padding: 24rpx;
		font-size: 28rpx;
		border: none;
		margin-bottom: 40rpx;
		transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
	}

	.logout-btn:hover {
		transform: scale(1.02);
		box-shadow: 0 10rpx 24rpx rgba(244, 67, 54, 0.3), 0 0 20rpx rgba(255, 107, 157, 0.15);
		background: linear-gradient(135deg, #ff6b9d 0%, #f44336 100%);
	}

	.logout-btn::after {
		border: none;
	}
</style>
