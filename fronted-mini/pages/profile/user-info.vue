<template>
	<view :class="['user-info-page', rootFontClass]">
		<view class="header">
			<view class="back" @tap="goBack">
				<uni-icons type="left" size="20" color="#0f172a"></uni-icons>
			</view>
			<text class="title">用户信息</text>
			<view class="placeholder"></view>
		</view>

		<view class="card">
			<view class="avatar-wrap" @tap="chooseAvatar">
				<image v-if="form.avatarUrl" :src="form.avatarUrl" class="avatar" mode="aspectFill"></image>
				<view v-else class="avatar avatar-placeholder">
					<uni-icons type="person" size="36" color="#94a3b8"></uni-icons>
				</view>
				<text class="avatar-tip">点击更换头像</text>
			</view>

			<view class="field">
				<text class="label">用户昵称</text>
				<input class="input" v-model="form.nickname" placeholder="请输入昵称" />
			</view>

			<view class="field">
				<text class="label">手机号</text>
				<input class="input" v-model="form.phone" type="number" maxlength="11" placeholder="请输入手机号" />
			</view>

			<view class="field">
				<text class="label">密码（可修改）</text>
				<input class="input" v-model="form.password" :password="!showPassword" placeholder="请输入新密码，不改可留空" />
				<text class="small-link" @tap="showPassword = !showPassword">{{ showPassword ? '隐藏' : '显示' }}</text>
			</view>

			<view class="field">
				<text class="label">性别</text>
				<picker mode="selector" :range="genderOptions" :value="genderIndex" @change="onGenderChange">
					<view class="picker">{{ genderText }}</view>
				</picker>
			</view>

			<view class="field">
				<text class="label">状态</text>
				<picker mode="selector" :range="statusOptions" :value="statusIndex" @change="onStatusChange">
					<view class="picker">{{ statusText }}</view>
				</picker>
			</view>

			<button class="save-btn" @tap="saveProfile" :disabled="saving">{{ saving ? '保存中...' : '保存修改' }}</button>
		</view>
	</view>
</template>

<script>
import config from '@/utils/config.js'

export default {
	data() {
		return {
			userId: null,
			showPassword: false,
			saving: false,
			genderOptions: ['未知', '男', '女'],
			statusOptions: ['禁用', '正常'],
			form: {
				nickname: '',
				avatarUrl: '',
				phone: '',
				password: '',
				gender: 0,
				status: 1
			}
		}
	},
	computed: {
		genderIndex() {
			return Number(this.form.gender || 0)
		},
		statusIndex() {
			return Number(this.form.status || 0)
		},
		genderText() {
			return this.genderOptions[this.genderIndex] || '未知'
		},
		statusText() {
			return this.statusOptions[this.statusIndex] || '正常'
		}
	},
	onLoad() {
		const userInfo = uni.getStorageSync('userInfo') || {}
		this.userId = userInfo.userId
		if (!this.userId) {
			uni.showToast({ title: '请先登录', icon: 'none' })
			setTimeout(() => uni.navigateTo({ url: '/pages/login/login' }), 800)
			return
		}
		this.loadProfile()
	},
	onShow() {
		if (this.userId) {
			this.loadProfile()
		}
	},
	methods: {
		goBack() {
			uni.navigateBack({
				delta: 1,
				fail: () => {
					uni.reLaunch({
						url: '/pages/profile/profile'
					})
				}
			})
		},
		onGenderChange(e) {
			this.form.gender = Number(e.detail.value)
		},
		onStatusChange(e) {
			this.form.status = Number(e.detail.value)
		},
		loadProfile() {
			uni.showLoading({ title: '加载中...' })
			uni.request({
				url: config.baseUrl + '/t-user/profile',
				method: 'GET',
				header: { userId: String(this.userId) },
				success: (res) => {
					uni.hideLoading()
					const body = res.data || {}
					if (res.statusCode !== 200 || body.code !== 200 || !body.data) {
						uni.showToast({ title: body.message || '加载失败', icon: 'none' })
						return
					}
					const d = body.data
					this.form.nickname = d.nickname || ''
					this.form.avatarUrl = d.avatarUrl || ''
					this.form.phone = d.phone || ''
					this.form.gender = d.gender == null ? 0 : Number(d.gender)
					this.form.status = d.status == null ? 1 : Number(d.status)

					// 历史密码为不可逆加密摘要，无法从数据库解密回明文；
					// 若本地记住了密码，则优先展示本地记住的值用于“回显”体验。
					const rememberedPhone = uni.getStorageSync('rememberedPhone')
					const rememberedPassword = uni.getStorageSync('rememberedPassword')
					if (rememberedPhone && rememberedPassword && rememberedPhone === this.form.phone) {
						this.form.password = rememberedPassword
					} else {
						this.form.password = ''
					}
				},
				fail: () => {
					uni.hideLoading()
					uni.showToast({ title: '网络错误，请稍后重试', icon: 'none' })
				}
			})
		},
		chooseAvatar() {
			uni.chooseImage({
				count: 1,
				sizeType: ['compressed'],
				sourceType: ['album', 'camera'],
				success: (res) => {
					const filePath = res.tempFilePaths && res.tempFilePaths[0]
					if (!filePath) return
					uni.showLoading({ title: '上传头像中...' })
					uni.uploadFile({
						url: config.baseUrl + '/t-user/avatar',
						filePath,
						name: 'file',
						header: { userId: String(this.userId) },
						success: (uploadRes) => {
							uni.hideLoading()
							try {
								const body = JSON.parse(uploadRes.data || '{}')
								if (body.code === 200 && body.data && body.data.avatarUrl) {
									this.form.avatarUrl = body.data.avatarUrl
									const userInfo = uni.getStorageSync('userInfo') || {}
									userInfo.avatarUrl = body.data.avatarUrl
									uni.setStorageSync('userInfo', userInfo)
									uni.showToast({ title: '头像上传成功', icon: 'success' })
									// 上传接口已落库，立即回查一次，确保页面显示与数据库一致
									this.loadProfile()
								} else {
									uni.showToast({ title: body.message || '上传失败', icon: 'none' })
								}
							} catch (e) {
								uni.showToast({ title: '上传结果解析失败', icon: 'none' })
							}
						},
						fail: () => {
							uni.hideLoading()
							uni.showToast({ title: '上传失败，请重试', icon: 'none' })
						}
					})
				}
			})
		},
		saveProfile() {
			if (!/^1[3-9]\d{9}$/.test(this.form.phone)) {
				uni.showToast({ title: '请输入正确手机号', icon: 'none' })
				return
			}
			if (this.form.password && this.form.password.length < 6) {
				uni.showToast({ title: '密码至少6位', icon: 'none' })
				return
			}

			const payload = {
				nickname: this.form.nickname,
				avatarUrl: this.form.avatarUrl,
				phone: this.form.phone,
				gender: Number(this.form.gender),
				status: Number(this.form.status)
			}
			if (this.form.password) {
				payload.password = this.form.password
			}

			this.saving = true
			uni.request({
				url: config.baseUrl + '/t-user/profile',
				method: 'PUT',
				header: {
					userId: String(this.userId),
					'Content-Type': 'application/json'
				},
				data: payload,
				success: (res) => {
					this.saving = false
					const body = res.data || {}
					if (res.statusCode !== 200 || body.code !== 200 || !body.data) {
						uni.showToast({ title: body.message || '保存失败', icon: 'none' })
						return
					}
					const userInfo = uni.getStorageSync('userInfo') || {}
					userInfo.nickname = body.data.nickname || ''
					userInfo.avatarUrl = body.data.avatarUrl || ''
					userInfo.phone = body.data.phone || ''
					uni.setStorageSync('userInfo', userInfo)
					if (payload.password) {
						uni.setStorageSync('rememberedPassword', payload.password)
						uni.setStorageSync('rememberedPhone', payload.phone)
					}
					uni.showToast({ title: '保存成功', icon: 'success' })
					setTimeout(() => {
						uni.reLaunch({
							url: '/pages/profile/profile'
						})
					}, 450)
				},
				fail: () => {
					this.saving = false
					uni.showToast({ title: '网络错误，请稍后重试', icon: 'none' })
				}
			})
		}
	}
}
</script>

<style lang="scss" scoped>
.user-info-page {
	min-height: 100vh;
	padding: 24rpx;
	background: #f4f8fb;
	display: flex;
	flex-direction: column;
	align-items: center;
	box-sizing: border-box;
}
.header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 20rpx 8rpx 28rpx;
	width: 100%;
	max-width: 690rpx;
	margin: 0 auto;
	box-sizing: border-box;
}
.back, .placeholder {
	width: 52rpx;
	height: 52rpx;
}
.back {
	display: flex;
	align-items: center;
	justify-content: center;
	background: #ffffff;
	border-radius: 50%;
}
.title {
	font-size: 34rpx;
	font-weight: 700;
	color: #0f172a;
}
.card {
	background: #ffffff;
	border-radius: 24rpx;
	padding: 30rpx 24rpx;
	box-shadow: 0 10rpx 30rpx rgba(2, 6, 23, 0.08);
	width: 100%;
	max-width: 690rpx;
	margin: 0 auto;
	box-sizing: border-box;
}
.avatar-wrap {
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-bottom: 28rpx;
}
.avatar {
	width: 140rpx;
	height: 140rpx;
	border-radius: 50%;
}
.avatar-placeholder {
	display: flex;
	align-items: center;
	justify-content: center;
	background: #f1f5f9;
}
.avatar-tip {
	margin-top: 12rpx;
	font-size: 24rpx;
	color: #64748b;
}
.field {
	margin-bottom: 20rpx;
}
.label {
	font-size: 24rpx;
	color: #334155;
	display: block;
	margin-bottom: 10rpx;
}
.input, .picker {
	width: 100%;
	height: 84rpx;
	line-height: 84rpx;
	background: #f8fafc;
	border: 2rpx solid #e2e8f0;
	border-radius: 16rpx;
	padding: 0 20rpx;
	font-size: 28rpx;
	color: #0f172a;
	box-sizing: border-box;
}
.small-link {
	font-size: 22rpx;
	color: #10b981;
	margin-top: 8rpx;
	display: inline-block;
}
.save-btn {
	margin-top: 18rpx;
	background: linear-gradient(135deg, #07C160 0%, #0d9488 100%);
	color: #ffffff;
	border-radius: 999rpx;
	border: none;
	font-size: 30rpx;
}
.save-btn::after {
	border: none;
}
.save-btn[disabled] {
	opacity: 0.75;
}
</style>

