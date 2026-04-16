<template>
	<view class="user-header" v-if="isLogin && userInfo">
		<view class="user-info-wrapper" @tap="onUserInfoClick">
			<view class="avatar-wrapper">
				<image v-if="userInfo.avatarUrl" :src="userInfo.avatarUrl" class="avatar-image" mode="aspectFill"></image>
				<uni-icons v-else type="person" size="20" color="#333"></uni-icons>
			</view>
			<text class="nickname-text">{{ userInfo.nickname || '用户' }}</text>
		</view>
	</view>
</template>

<script>
	export default {
		name: 'UserHeader',
		data() {
			return {
				isLogin: false,
				userInfo: {}
			}
		},
		mounted() {
			// 组件挂载时检查登录状态
			this.checkLoginStatus()
		},
		onShow() {
			// 每次页面显示时检查登录状态
			this.checkLoginStatus()
		},
		methods: {
			checkLoginStatus() {
				// 从本地存储检查登录状态
				const loginStatus = uni.getStorageSync('isLogin')
				const userInfo = uni.getStorageSync('userInfo')
				
				this.isLogin = loginStatus === true
				this.userInfo = userInfo || {}
			},
			onUserInfoClick() {
				// 跳转到我的页面
				uni.switchTab({
					url: '/pages/profile/profile',
					fail: () => {
						uni.navigateTo({
							url: '/pages/profile/profile'
						})
					}
				})
			}
		}
	}
</script>

<style lang="scss" scoped>
	.user-header {
		position: fixed;
		top: 0;
		left: 0;
		right: 0;
		z-index: 1000;
		background-color: rgba(255, 255, 255, 0.95);
		backdrop-filter: blur(10rpx);
		padding: 20rpx 32rpx;
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
	}

	.user-info-wrapper {
		display: flex;
		align-items: center;
		width: fit-content;
	}

	.avatar-wrapper {
		width: 56rpx;
		height: 56rpx;
		border-radius: 50%;
		background-color: #f5f5f5;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-right: 16rpx;
		overflow: hidden;
	}

	.avatar-image {
		width: 100%;
		height: 100%;
		border-radius: 50%;
	}

	.nickname-text {
		font-size: 28rpx;
		color: #333333;
		font-weight: 500;
		max-width: 200rpx;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}
</style>
