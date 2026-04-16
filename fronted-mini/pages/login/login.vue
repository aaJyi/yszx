<template>
	<view :class="['login-container', rootFontClass]">
		<view class="bg-blob bg-blob-1"></view>
		<view class="bg-blob bg-blob-2"></view>

		<!-- 左上角返回 -->
		<view class="back-button" @tap="onBack">
			<uni-icons type="left" size="24" color="#ffffff"></uni-icons>
		</view>

		<!-- 顶部品牌 -->
		<view class="logo-section">
			<view class="logo-ring">
				<view class="logo-inner">
					<image class="logo-img" :src="appLogoUrl" mode="aspectFit" />
				</view>
			</view>
			<text class="app-name">医视智行</text>
			<text class="app-slogan">您的健康管理专家</text>
		</view>

		<!-- 登录卡片 -->
		<view class="login-form">
			<view class="form-head">
				<text class="form-title">欢迎回来</text>
				<text class="form-sub">使用手机号登录，数据更安全</text>
			</view>

			<view class="form-item">
				<text class="field-label">手机号</text>
				<view class="input-wrapper">
					<uni-icons type="phone" size="20" color="#94a3b8"></uni-icons>
					<input
						class="input-field"
						type="number"
						v-model="phone"
						placeholder="请输入11位手机号"
						placeholder-style="color: #94a3b8"
						maxlength="11"
					/>
				</view>
			</view>

			<view class="form-item">
				<text class="field-label">密码</text>
				<view class="input-wrapper">
					<uni-icons type="locked" size="20" color="#94a3b8"></uni-icons>
					<input
						class="input-field"
						:type="showPassword ? 'text' : 'password'"
						v-model="password"
						placeholder="请输入密码"
						placeholder-style="color: #94a3b8"
					/>
					<view class="eye-icon" @tap="togglePassword">
						<uni-icons :type="showPassword ? 'eye' : 'eye-slash'" size="20" color="#94a3b8"></uni-icons>
					</view>
				</view>
			</view>

			<view class="form-options">
				<view class="remember-me">
					<checkbox-group @change="onRememberChange">
						<label>
							<checkbox value="remember" :checked="rememberMe" color="#07C160" />
							<text class="option-text">记住密码</text>
						</label>
					</checkbox-group>
				</view>
				<text class="forgot-password" @tap="onForgotPassword">忘记密码？</text>
			</view>

			<button class="login-btn" :class="{ 'login-btn-active': canLogin }" @tap="onLogin" :disabled="!canLogin">登 录</button>

			<view class="register-section">
				<text class="register-text">还没有账号？</text>
				<text class="register-link" @tap="onRegister">立即注册</text>
			</view>
		</view>
	</view>
</template>

<script>
	import config from '@/utils/config.js'
	
	export default {
		data() {
			return {
				appLogoUrl: config.appLogoUrl,
				phone: '',
				password: '',
				showPassword: false,
				rememberMe: false
			}
		},
		computed: {
			canLogin() {
				// 简单的验证：手机号11位，密码至少6位
				return this.phone.length === 11 && this.password.length >= 6
			}
		},
		onLoad(options) {
			// 检查是否有记住的登录信息
			this.loadRememberedInfo()
			
			// 如果从注册页面跳转过来，填充手机号
			if (options && options.phone) {
				this.phone = options.phone
			}
		},
		methods: {
			onBack() {
				// 返回上一页或退出登录页面
				uni.navigateBack({
					delta: 1,
					fail: () => {
						// 如果没有上一页，则跳转到首页
						uni.reLaunch({
							url: '/pages/index/index'
						})
					}
				})
			},
			loadRememberedInfo() {
				// 从本地存储加载记住的登录信息
				const rememberedPhone = uni.getStorageSync('rememberedPhone')
				const rememberedPassword = uni.getStorageSync('rememberedPassword')
				const rememberMe = uni.getStorageSync('rememberMe')
				
				if (rememberMe && rememberedPhone) {
					this.phone = rememberedPhone
					this.password = rememberedPassword || ''
					this.rememberMe = true
				}
			},
			togglePassword() {
				this.showPassword = !this.showPassword
			},
			onRememberChange(e) {
				this.rememberMe = e.detail.value.includes('remember')
			},
			onForgotPassword() {
				uni.showToast({
					title: '忘记密码功能开发中',
					icon: 'none'
				})
			},
			onRegister() {
				uni.navigateTo({
					url: '/pages/register/register'
				})
			},
			onLogin() {
				if (!this.canLogin) {
					uni.showToast({
						title: '请填写完整的登录信息',
						icon: 'none'
					})
					return
				}

				// 验证手机号格式
				const phoneReg = /^1[3-9]\d{9}$/
				if (!phoneReg.test(this.phone)) {
					uni.showToast({
						title: '请输入正确的手机号',
						icon: 'none'
					})
					return
				}

				// 显示加载提示
				uni.showLoading({
					title: '登录中...',
					mask: true
				})

				// 调用后端登录接口
				uni.request({
					url: config.baseUrl + '/t-user/login',
					method: 'POST',
					header: {
						'Content-Type': 'application/json'
					},
					data: {
						phone: this.phone,
						password: this.password
					},
					success: (res) => {
						uni.hideLoading()
						
						if (res.statusCode === 200 && res.data) {
							// 检查是否有错误信息
							if (res.data.code && res.data.code !== 200) {
								uni.showToast({
									title: res.data.message || '账号或密码错误，请重新输入',
									icon: 'none',
									duration: 2000
								})
								return
							}
							
							// 登录成功
							const loginData = res.data.data || res.data
							const userInfo = {
								userId: loginData.userId,
								phone: loginData.phone,
								nickname: loginData.nickname || '用户' + this.phone.substring(7),
								avatarUrl: loginData.avatarUrl || '',
								token: loginData.token
							}
							
							// 保存到本地存储
							uni.setStorageSync('userInfo', userInfo)
							uni.setStorageSync('isLogin', true)
							
							// 如果记住密码，保存登录信息
							if (this.rememberMe) {
								uni.setStorageSync('rememberedPhone', this.phone)
								uni.setStorageSync('rememberedPassword', this.password)
								uni.setStorageSync('rememberMe', true)
							} else {
								uni.removeStorageSync('rememberedPhone')
								uni.removeStorageSync('rememberedPassword')
								uni.removeStorageSync('rememberMe')
							}
							
							uni.showToast({
								title: '登录成功',
								icon: 'success'
							})
							
							// 跳转到我的页面
							setTimeout(() => {
								uni.reLaunch({
									url: '/pages/profile/profile'
								})
							}, 1500)
						} else {
							// 登录失败
							const errorMsg = res.data?.message || res.data || '账号或密码错误，请重新输入'
							uni.showToast({
								title: errorMsg,
								icon: 'none',
								duration: 2000
							})
						}
					},
					fail: (err) => {
						uni.hideLoading()
						console.error('登录请求失败：', err)
						uni.showToast({
							title: '网络错误，请稍后重试',
							icon: 'none',
							duration: 2000
						})
					}
				})
			},
		}
	}
</script>

<style lang="scss" scoped>
	.login-container {
		min-height: 100vh;
		background: linear-gradient(165deg, #059669 0%, #07C160 42%, #0d9488 100%);
		padding: calc(88rpx + env(safe-area-inset-top, 0)) 40rpx 48rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
		position: relative;
		overflow: hidden;
		box-sizing: border-box;
	}

	.bg-blob {
		position: absolute;
		border-radius: 9999rpx;
		filter: blur(40rpx);
		opacity: 0.45;
		pointer-events: none;
		z-index: 0;
	}
	.bg-blob-1 {
		width: 420rpx;
		height: 420rpx;
		left: -120rpx;
		top: 180rpx;
		background: radial-gradient(circle, rgba(255, 255, 255, 0.35), transparent 70%);
	}
	.bg-blob-2 {
		width: 480rpx;
		height: 480rpx;
		right: -160rpx;
		bottom: 120rpx;
		background: radial-gradient(circle, rgba(20, 184, 166, 0.5), transparent 65%);
	}

	.back-button {
		position: absolute;
		top: calc(24rpx + env(safe-area-inset-top, 0));
		left: 28rpx;
		width: 72rpx;
		height: 72rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		background: rgba(255, 255, 255, 0.18);
		border-radius: 50%;
		z-index: 10;
		backdrop-filter: blur(8rpx);
	}

	.logo-section {
		display: flex;
		flex-direction: column;
		align-items: center;
		margin-bottom: 48rpx;
		margin-top: 24rpx;
		position: relative;
		z-index: 1;
		width: 100%;
		max-width: 680rpx;
		margin-left: auto;
		margin-right: auto;
	}

	.logo-ring {
		padding: 6rpx;
		border-radius: 50%;
		background: linear-gradient(135deg, rgba(255, 255, 255, 0.95), rgba(255, 255, 255, 0.35));
		margin-bottom: 28rpx;
		box-shadow: 0 16rpx 48rpx rgba(0, 0, 0, 0.12);
	}

	.logo-inner {
		width: 152rpx;
		height: 152rpx;
		background: #ffffff;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		overflow: hidden;
	}

	.logo-img {
		width: 132rpx;
		height: 132rpx;
	}

	.app-name {
		font-size: 52rpx;
		font-weight: 800;
		color: #ffffff;
		letter-spacing: 2rpx;
		margin-bottom: 12rpx;
		text-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.12);
	}

	.app-slogan {
		font-size: 26rpx;
		color: rgba(255, 255, 255, 0.88);
	}

	.login-form {
		position: relative;
		z-index: 1;
		background: rgba(255, 255, 255, 0.97);
		border-radius: 36rpx;
		padding: 44rpx 36rpx 40rpx;
		box-shadow: 0 24rpx 64rpx rgba(0, 0, 0, 0.14);
		border: 1rpx solid rgba(255, 255, 255, 0.6);
		width: 100%;
		max-width: 680rpx;
		margin-left: auto;
		margin-right: auto;
		box-sizing: border-box;
	}

	.form-head {
		margin-bottom: 36rpx;
	}
	.form-title {
		display: block;
		font-size: 38rpx;
		font-weight: 700;
		color: #0f172a;
		margin-bottom: 10rpx;
	}
	.form-sub {
		display: block;
		font-size: 24rpx;
		color: #64748b;
	}

	.form-item {
		margin-bottom: 28rpx;
	}
	.field-label {
		display: block;
		font-size: 24rpx;
		color: #475569;
		margin-bottom: 12rpx;
		font-weight: 500;
	}

	.input-wrapper {
		display: flex;
		align-items: center;
		background: #f8fafc;
		border-radius: 20rpx;
		padding: 26rpx 28rpx;
		border: 2rpx solid #e2e8f0;
		transition: border-color 0.2s;
	}

	.input-wrapper uni-icons {
		margin-right: 16rpx;
		flex-shrink: 0;
	}

	.input-field {
		flex: 1;
		font-size: 30rpx;
		color: #0f172a;
	}

	.eye-icon {
		margin-left: 12rpx;
		padding: 8rpx;
		flex-shrink: 0;
	}

	.form-options {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 36rpx;
		margin-top: 8rpx;
	}

	.remember-me {
		display: flex;
		align-items: center;
	}

	.option-text {
		font-size: 24rpx;
		color: #64748b;
		margin-left: 8rpx;
	}

	.forgot-password {
		font-size: 24rpx;
		color: #059669;
		font-weight: 500;
	}

	.login-btn {
		width: 100%;
		background: #cbd5e1;
		color: #ffffff;
		border-radius: 999rpx;
		padding: 30rpx;
		font-size: 32rpx;
		font-weight: 600;
		border: none;
		margin-bottom: 28rpx;
		letter-spacing: 8rpx;
		transition: all 0.2s;
	}

	.login-btn-active {
		background: linear-gradient(135deg, #10b981 0%, #059669 50%, #047857 100%);
		box-shadow: 0 12rpx 32rpx rgba(5, 150, 105, 0.35);
	}

	.login-btn::after {
		border: none;
	}

	.login-btn[disabled] {
		opacity: 0.85;
	}

	.register-section {
		display: flex;
		justify-content: center;
		align-items: center;
		padding-top: 8rpx;
	}

	.register-text {
		font-size: 26rpx;
		color: #94a3b8;
		margin-right: 8rpx;
	}

	.register-link {
		font-size: 26rpx;
		color: #059669;
		font-weight: 600;
	}
</style>
