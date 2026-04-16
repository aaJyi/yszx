<template>
	<view :class="['register-container', rootFontClass]">
		<view class="bg-blob bg-blob-1"></view>
		<view class="bg-blob bg-blob-2"></view>

		<view class="back-button" @tap="onBack">
			<uni-icons type="left" size="24" color="#ffffff"></uni-icons>
		</view>

		<view class="logo-section">
			<view class="logo-ring">
				<view class="logo-inner">
					<image class="logo-img" :src="appLogoUrl" mode="aspectFit" />
				</view>
			</view>
			<text class="app-name">医视智行</text>
			<text class="app-slogan">您的健康管理专家</text>
		</view>

		<view class="register-form">
			<view class="form-head">
				<text class="form-title">创建账号</text>
				<text class="form-sub">填写信息完成注册，即可开始使用</text>
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
						placeholder="至少6位密码"
						placeholder-style="color: #94a3b8"
					/>
					<view class="eye-icon" @tap="togglePassword">
						<uni-icons :type="showPassword ? 'eye' : 'eye-slash'" size="20" color="#94a3b8"></uni-icons>
					</view>
				</view>
			</view>

			<view class="form-item">
				<text class="field-label">昵称（可选）</text>
				<view class="input-wrapper">
					<uni-icons type="person" size="20" color="#94a3b8"></uni-icons>
					<input
						class="input-field"
						type="text"
						v-model="nickname"
						placeholder="用于展示的名称"
						placeholder-style="color: #94a3b8"
					/>
				</view>
			</view>

			<button class="register-btn" :class="{ 'register-btn-active': canRegister }" @tap="onRegister" :disabled="!canRegister">注 册</button>

			<view class="login-section">
				<text class="login-text">已有账号？</text>
				<text class="login-link" @tap="goToLogin">立即登录</text>
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
				nickname: '',
				showPassword: false
			}
		},
		computed: {
			canRegister() {
				// 简单的验证：手机号11位，密码至少6位
				return this.phone.length === 11 && this.password.length >= 6
			}
		},
		methods: {
			onBack() {
				uni.navigateBack({
					delta: 1,
					fail: () => {
						uni.reLaunch({
							url: '/pages/index/index'
						})
					}
				})
			},
			togglePassword() {
				this.showPassword = !this.showPassword
			},
			goToLogin() {
				uni.navigateTo({
					url: '/pages/login/login'
				})
			},
			onRegister() {
				if (!this.canRegister) {
					uni.showToast({
						title: '请填写完整的注册信息',
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

				// 验证密码长度
				if (this.password.length < 6) {
					uni.showToast({
						title: '密码长度不能少于6位',
						icon: 'none'
					})
					return
				}

				// 显示加载提示
				uni.showLoading({
					title: '注册中...',
					mask: true
				})

				// 调用后端注册接口
				uni.request({
					url: config.baseUrl + '/t-user/register',
					method: 'POST',
					header: {
						'Content-Type': 'application/json'
					},
					data: {
						phone: this.phone,
						password: this.password,
						nickname: this.nickname || undefined
					},
					success: (res) => {
						uni.hideLoading()
						
						if (res.statusCode === 200 && res.data) {
							// 检查是否有错误信息
							if (res.data.code && res.data.code !== 200) {
								uni.showToast({
									title: res.data.message || '注册失败，请稍后重试',
									icon: 'none',
									duration: 2000
								})
								return
							}
							
							// 注册成功，自动登录
							const userId = res.data.userId;
							
							if (userId) {
								// 自动调用登录接口
								this.autoLogin();
							} else {
								// 如果没有userId，跳转到登录页面
								uni.showToast({
									title: '注册成功，请登录',
									icon: 'success'
								});
								
								setTimeout(() => {
									uni.navigateTo({
										url: '/pages/login/login?phone=' + this.phone
									})
								}, 1500);
							}
						} else {
							// 注册失败
							const errorMsg = res.data?.message || '注册失败，请稍后重试'
							uni.showToast({
								title: errorMsg,
								icon: 'none',
								duration: 2000
							})
						}
					},
					fail: (err) => {
						uni.hideLoading()
						console.error('注册请求失败：', err)
						uni.showToast({
							title: '网络错误，请稍后重试',
							icon: 'none',
							duration: 2000
						})
					}
				})
			},
			// 自动登录
			autoLogin() {
				uni.showLoading({
					title: '登录中...',
					mask: true
				});
				
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
					success: (loginRes) => {
						uni.hideLoading();
						
						if (loginRes.statusCode === 200 && loginRes.data) {
							// 检查是否有错误信息
							if (loginRes.data.code && loginRes.data.code !== 200) {
								uni.showToast({
									title: '自动登录失败，请手动登录',
									icon: 'none',
									duration: 2000
								});
								setTimeout(() => {
									uni.navigateTo({
										url: '/pages/login/login?phone=' + this.phone
									})
								}, 2000);
								return;
							}
							
							// 登录成功
							const loginData = loginRes.data.data || loginRes.data;
							const userInfo = {
								userId: loginData.userId,
								phone: loginData.phone,
								nickname: loginData.nickname || '用户' + this.phone.substring(7),
								avatarUrl: loginData.avatarUrl || '',
								token: loginData.token
							};
							
							// 保存到本地存储
							uni.setStorageSync('userInfo', userInfo);
							uni.setStorageSync('isLogin', true);
							
							uni.showToast({
								title: '注册成功',
								icon: 'success',
								duration: 1000
							});
							
							// 跳转到首页
							setTimeout(() => {
								uni.reLaunch({
									url: '/pages/index/index'
								});
							}, 1000);
						} else {
							// 登录失败，跳转到登录页面
							uni.showToast({
								title: '自动登录失败，请手动登录',
								icon: 'none'
							});
							setTimeout(() => {
								uni.navigateTo({
									url: '/pages/login/login?phone=' + this.phone
								})
							}, 2000);
						}
					},
					fail: (err) => {
						uni.hideLoading();
						console.error('自动登录失败:', err);
						uni.showToast({
							title: '自动登录失败，请手动登录',
							icon: 'none'
						});
						setTimeout(() => {
							uni.navigateTo({
								url: '/pages/login/login?phone=' + this.phone
							})
						}, 2000);
					}
				});
			}
		}
	}
</script>

<style lang="scss" scoped>
	.register-container {
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

	.register-form {
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

	.register-btn {
		width: 100%;
		background: #cbd5e1;
		color: #ffffff;
		border-radius: 999rpx;
		padding: 30rpx;
		font-size: 32rpx;
		font-weight: 600;
		border: none;
		margin-top: 8rpx;
		margin-bottom: 28rpx;
		letter-spacing: 8rpx;
		transition: all 0.2s;
	}

	.register-btn-active {
		background: linear-gradient(135deg, #10b981 0%, #059669 50%, #047857 100%);
		box-shadow: 0 12rpx 32rpx rgba(5, 150, 105, 0.35);
	}

	.register-btn::after {
		border: none;
	}

	.register-btn[disabled] {
		opacity: 0.85;
	}

	.login-section {
		display: flex;
		justify-content: center;
		align-items: center;
		padding-top: 8rpx;
	}

	.login-text {
		font-size: 26rpx;
		color: #94a3b8;
		margin-right: 8rpx;
	}

	.login-link {
		font-size: 26rpx;
		color: #059669;
		font-weight: 600;
	}
</style>
