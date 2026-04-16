<template>
	<view class="page-root">
	<view :class="['container', rootFontClass]">
		<view class="header" :style="{ paddingTop: statusBarPadding }">
			<view class="header-bar">
				<view class="header-back" @tap="goBack">
					<uni-icons type="left" size="22" color="#ffffff"></uni-icons>
				</view>
				<text class="title">家人管理</text>
				<view class="header-back placeholder"></view>
			</view>
		</view>

		<scroll-view scroll-y class="family-scroll" :show-scrollbar="false">
			<view class="family-list">
				<view 
					class="family-item" 
					v-for="(item, index) in familyList" 
					:key="item.memberId"
				>
					<view class="family-info">
						<image 
							class="family-avatar" 
							:src="item.avatarUrl || '/static/default-avatar.png'"
							mode="aspectFill"
						></image>
						<view class="family-details">
							<text class="family-name">{{ item.fullName }}</text>
							<text class="family-phone">{{ item.phone }}</text>
							<text class="family-relation" v-if="item.relation">{{ item.relation }}</text>
						</view>
					</view>
					<view class="family-actions">
						<text class="delete-btn" @tap="deleteFamily(item.memberId)">删除</text>
					</view>
				</view>

				<view v-if="familyList.length === 0" class="empty-state">
					<uni-icons type="person" size="80" color="#cccccc"></uni-icons>
					<text class="empty-text">暂无家人</text>
					<text class="empty-hint">点击下方按钮添加家人</text>
				</view>
			</view>
			<view class="scroll-bottom-spacer"></view>
		</scroll-view>

		<!-- 添加家人按钮 -->
		<view class="add-family-btn" @tap="showAddDialog">
			<uni-icons type="plus" size="24" color="#ffffff"></uni-icons>
			<text class="add-text">添加家人</text>
		</view>

		<!-- 为家人生成健康档案按钮 -->
		<view class="create-archive-btn" @tap="goToCreateArchive" v-if="familyList.length > 0">
			<uni-icons type="folder-add" size="24" color="#ffffff"></uni-icons>
			<text class="create-text">为家人生成健康档案</text>
		</view>

		<!-- 添加家人弹窗 -->
		<uni-popup ref="addDialog" type="center">
			<view class="add-modal">
				<view class="add-modal-hero">
					<view class="add-modal-hero-icon">
						<uni-icons type="heart" size="36" color="#fff"></uni-icons>
					</view>
					<text class="add-modal-title">添加家人</text>
					<text class="add-modal-sub">填写信息后，家人账号将关联到您的家庭</text>
					<view class="add-modal-close" @tap="cancelAdd">
						<uni-icons type="close" size="22" color="rgba(255,255,255,0.85)"></uni-icons>
					</view>
				</view>
				<view class="add-modal-body">
					<view class="add-field">
						<view class="add-field-label">
							<uni-icons type="phone" size="18" color="#667eea"></uni-icons>
							<text>手机号</text>
						</view>
						<input
							class="add-input"
							type="number"
							v-model="addForm.phone"
							placeholder="请输入家人手机号"
							maxlength="11"
						/>
					</view>
					<view class="add-field">
						<view class="add-field-label">
							<uni-icons type="locked" size="18" color="#667eea"></uni-icons>
							<text>密码</text>
						</view>
						<input
							class="add-input"
							password
							v-model="addForm.password"
							placeholder="用于家人登录的密码"
						/>
					</view>
					<view class="add-field">
						<view class="add-field-label">
							<uni-icons type="person" size="18" color="#667eea"></uni-icons>
							<text>关系</text>
							<text class="add-optional">选填</text>
						</view>
						<input
							class="add-input"
							v-model="addForm.relation"
							placeholder="如：父亲、母亲、子女等"
						/>
					</view>
				</view>
				<view class="add-modal-actions">
					<button class="add-btn ghost" @tap="cancelAdd">取消</button>
					<button class="add-btn primary" @tap="confirmAdd">保存</button>
				</view>
			</view>
		</uni-popup>

		<!-- 自定义底部导航栏：入口在「我的」，高亮应为「我的」(索引 5) -->
		<custom-tabbar :current="5"></custom-tabbar>
	</view>
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
				statusBarPadding: '0px',
				familyList: [],
				addForm: {
					phone: '',
					password: '',
					relation: ''
				}
			}
		},
		onLoad() {
			try {
				const sys = uni.getSystemInfoSync()
				const h = sys.statusBarHeight || 0
				this.statusBarPadding = `${h}px`
			} catch (e) {
				this.statusBarPadding = 'env(safe-area-inset-top)'
			}
			this.loadFamilyList()
		},
		onShow() {
			this.loadFamilyList()
		},
		methods: {
			goBack() {
				const pages = getCurrentPages()
				if (pages.length > 1) {
					uni.navigateBack()
				} else {
					uni.switchTab({ url: '/pages/profile/profile' })
				}
			},
			// 加载家人列表
			loadFamilyList() {
				const userInfo = uni.getStorageSync('userInfo')
				if (!userInfo || !userInfo.userId) {
					uni.showToast({
						title: '请先登录',
						icon: 'none'
					})
					setTimeout(() => {
						uni.navigateTo({
							url: '/pages/login/login'
						})
					}, 1500)
					return
				}

				uni.request({
					url: `${config.baseUrl}/family-member/list`,
					method: 'GET',
					data: {
						userId: userInfo.userId
					},
					header: {
						'userId': userInfo.userId
					},
					success: (res) => {
						if (res.statusCode === 200 && res.data.code === 200) {
							this.familyList = res.data.data || []
						} else {
							uni.showToast({
								title: res.data.message || '加载失败',
								icon: 'none'
							})
						}
					},
					fail: (err) => {
						console.error('加载家人列表失败', err)
						uni.showToast({
							title: '网络错误',
							icon: 'none'
						})
					}
				})
			},

			// 显示添加对话框
			showAddDialog() {
				this.addForm = {
					phone: '',
					password: '',
					relation: ''
				}
				this.$refs.addDialog.open()
			},

			// 确认添加
			confirmAdd() {
				if (!this.addForm.phone) {
					uni.showToast({
						title: '请输入手机号',
						icon: 'none'
					})
					return
				}
				if (!this.addForm.password) {
					uni.showToast({
						title: '请输入密码',
						icon: 'none'
					})
					return
				}

				const userInfo = uni.getStorageSync('userInfo')
				if (!userInfo || !userInfo.userId) {
					uni.showToast({
						title: '请先登录',
						icon: 'none'
					})
					return
				}

				uni.request({
					url: `${config.baseUrl}/family-member/add`,
					method: 'POST',
					data: {
						phone: this.addForm.phone,
						password: this.addForm.password,
						relation: this.addForm.relation || ''
					},
					header: {
						'userId': userInfo.userId,
						'Content-Type': 'application/json'
					},
					success: (res) => {
						if (res.statusCode === 200 && res.data.code === 200) {
							uni.showToast({
								title: '添加成功',
								icon: 'success'
							})
							this.$refs.addDialog.close()
							this.loadFamilyList()
						} else {
							uni.showToast({
								title: res.data.message || '添加失败',
								icon: 'none'
							})
						}
					},
					fail: (err) => {
						console.error('添加家人失败', err)
						uni.showToast({
							title: '网络错误',
							icon: 'none'
						})
					}
				})
			},

			// 取消添加
			cancelAdd() {
				this.addForm = {
					phone: '',
					password: '',
					relation: ''
				}
				this.$refs.addDialog.close()
			},

			// 删除家人
			deleteFamily(memberId) {
				uni.showModal({
					title: '确认删除',
					content: '确定要删除该家人吗？',
					success: (res) => {
						if (res.confirm) {
							const userInfo = uni.getStorageSync('userInfo')
							if (!userInfo || !userInfo.userId) {
								uni.showToast({
									title: '请先登录',
									icon: 'none'
								})
								return
							}

							uni.request({
								url: `${config.baseUrl}/family-member/${memberId}`,
								method: 'DELETE',
								data: {
									userId: userInfo.userId
								},
								header: {
									'userId': userInfo.userId
								},
								success: (res) => {
									if (res.statusCode === 200 && res.data.code === 200) {
										uni.showToast({
											title: '删除成功',
											icon: 'success'
										})
										this.loadFamilyList()
									} else {
										uni.showToast({
											title: res.data.message || '删除失败',
											icon: 'none'
										})
									}
								},
								fail: (err) => {
									console.error('删除家人失败', err)
									uni.showToast({
										title: '网络错误',
										icon: 'none'
									})
								}
							})
						}
					}
				})
			},

			// 跳转到为家人生成健康档案页面
			goToCreateArchive() {
				if (this.familyList.length === 0) {
					uni.showToast({
						title: '请先添加家人',
						icon: 'none'
					})
					return
				}
				uni.navigateTo({
					url: '/pages/family/create-archive'
				})
			}
		}
	}
</script>

<style scoped>
	.page-root {
		height: 100vh;
		max-height: 100vh;
		box-sizing: border-box;
		display: flex;
		flex-direction: column;
		overflow: hidden;
		background: transparent;
	}
	.container {
		flex: 1;
		min-height: 0;
		display: flex;
		flex-direction: column;
		background: transparent;
		padding-bottom: calc(98rpx + constant(safe-area-inset-bottom));
		padding-bottom: calc(98rpx + env(safe-area-inset-bottom));
		box-sizing: border-box;
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
	.header, .family-scroll, .add-family-btn, .create-archive-btn, .navbar { position: relative; z-index: 1; }

	.header {
		flex-shrink: 0;
		background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
		padding-left: 24rpx;
		padding-right: 24rpx;
		padding-bottom: 28rpx;
		color: #ffffff;
	}
	.header-bar {
		display: flex;
		align-items: center;
		justify-content: space-between;
		min-height: 72rpx;
	}
	.header-back {
		width: 72rpx;
		height: 72rpx;
		display: flex;
		align-items: center;
		justify-content: flex-start;
	}
	.header-back.placeholder {
		pointer-events: none;
		opacity: 0;
	}
	.title {
		flex: 1;
		text-align: center;
		font-size: 36rpx;
		font-weight: bold;
	}

	.family-scroll {
		flex: 1;
		min-height: 0;
		height: 0;
	}
	.family-list {
		padding: 20rpx;
	}
	.scroll-bottom-spacer {
		height: 280rpx;
	}

	.family-item {
		background-color: #ffffff;
		border-radius: 16rpx;
		padding: 30rpx;
		margin-bottom: 20rpx;
		display: flex;
		justify-content: space-between;
		align-items: center;
		box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
	}

	.family-info {
		display: flex;
		align-items: center;
		flex: 1;
	}

	.family-avatar {
		width: 100rpx;
		height: 100rpx;
		border-radius: 50%;
		margin-right: 20rpx;
		background-color: #f0f0f0;
	}

	.family-details {
		display: flex;
		flex-direction: column;
	}

	.family-name {
		font-size: 32rpx;
		font-weight: bold;
		color: #333333;
		margin-bottom: 8rpx;
	}

	.family-phone {
		font-size: 28rpx;
		color: #666666;
		margin-bottom: 4rpx;
	}

	.family-relation {
		font-size: 24rpx;
		color: #999999;
	}

	.family-actions {
		display: flex;
		align-items: center;
	}

	.delete-btn {
		color: #ff4757;
		font-size: 28rpx;
		padding: 10rpx 20rpx;
	}

	.empty-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 100rpx 0;
	}

	.empty-text {
		font-size: 32rpx;
		color: #999999;
		margin-top: 20rpx;
	}

	.empty-hint {
		font-size: 24rpx;
		color: #cccccc;
		margin-top: 10rpx;
	}

	.add-family-btn,
	.create-archive-btn {
		position: fixed;
		left: 30rpx;
		right: 30rpx;
		z-index: 100;
		bottom: calc(98rpx + constant(safe-area-inset-bottom) + 20rpx);
		bottom: calc(98rpx + env(safe-area-inset-bottom) + 20rpx);
		background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
		color: #ffffff;
		border-radius: 50rpx;
		padding: 24rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 32rpx;
		font-weight: bold;
		box-shadow: 0 4rpx 20rpx rgba(102, 126, 234, 0.4);
	}

	.create-archive-btn {
		bottom: calc(98rpx + constant(safe-area-inset-bottom) + 120rpx);
		bottom: calc(98rpx + env(safe-area-inset-bottom) + 120rpx);
		background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
	}

	.add-text,
	.create-text {
		margin-left: 10rpx;
	}

	.add-modal {
		width: 620rpx;
		max-width: 92vw;
		border-radius: 28rpx;
		overflow: hidden;
		background: #fff;
		box-shadow: 0 24rpx 80rpx rgba(102, 126, 234, 0.35);
	}
	.add-modal-hero {
		position: relative;
		padding: 44rpx 36rpx 36rpx;
		background: linear-gradient(135deg, #667eea 0%, #764ba2 55%, #a78bfa 100%);
	}
	.add-modal-hero-icon {
		width: 88rpx;
		height: 88rpx;
		border-radius: 24rpx;
		background: rgba(255, 255, 255, 0.22);
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 20rpx;
	}
	.add-modal-title {
		display: block;
		font-size: 36rpx;
		font-weight: 700;
		color: #fff;
		letter-spacing: 1rpx;
	}
	.add-modal-sub {
		display: block;
		margin-top: 12rpx;
		font-size: 24rpx;
		color: rgba(255, 255, 255, 0.88);
		line-height: 1.5;
		padding-right: 48rpx;
	}
	.add-modal-close {
		position: absolute;
		top: 28rpx;
		right: 28rpx;
		padding: 12rpx;
	}
	.add-modal-body {
		padding: 32rpx 32rpx 8rpx;
	}
	.add-field {
		margin-bottom: 28rpx;
	}
	.add-field-label {
		display: flex;
		align-items: center;
		gap: 10rpx;
		margin-bottom: 14rpx;
		font-size: 26rpx;
		color: #475569;
		font-weight: 600;
	}
	.add-optional {
		margin-left: auto;
		font-size: 22rpx;
		font-weight: 400;
		color: #94a3b8;
	}
	.add-input {
		width: 100%;
		box-sizing: border-box;
		min-height: 88rpx;
		padding: 0 28rpx;
		font-size: 28rpx;
		color: #1e293b;
		background: #f8fafc;
		border-radius: 18rpx;
		border: 2rpx solid #e2e8f0;
	}
	.add-input:focus {
		border-color: #a5b4fc;
		background: #fff;
	}
	.add-modal-actions {
		display: flex;
		gap: 20rpx;
		padding: 16rpx 32rpx 36rpx;
	}
	.add-btn {
		flex: 1;
		height: 92rpx;
		line-height: 92rpx;
		font-size: 30rpx;
		border-radius: 999rpx;
		border: none;
		margin: 0;
	}
	.add-btn::after {
		border: none;
	}
	.add-btn.ghost {
		background: #f1f5f9;
		color: #64748b;
		font-weight: 600;
	}
	.add-btn.primary {
		background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
		color: #fff;
		font-weight: 700;
		box-shadow: 0 12rpx 32rpx rgba(102, 126, 234, 0.4);
	}
</style>