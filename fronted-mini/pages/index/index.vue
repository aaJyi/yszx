<template>
	<view class="page-root">
		<view :class="['container', rootFontClass]">
		<!-- 顶部导航栏：菜单在左上角 -->
		<view class="navbar animate-slide-in-down">
			<view class="navbar-left" @tap="onMenuClick">
				<uni-icons type="bars" size="24" color="#333"></uni-icons>
			</view>
			<view class="navbar-center">
				<text class="navbar-title">医视智行</text>
			</view>
			<view class="navbar-right"></view>
		</view>

		<!-- 左上角菜单：服务列表弹窗 -->
		<view v-if="showServiceMenu" class="service-menu-mask" @tap="showServiceMenu = false"></view>
		<view v-if="showServiceMenu" class="service-menu-popup animate-slide-in-left">
			<view class="service-menu-title">全部服务</view>
			<scroll-view scroll-y class="service-menu-list">
				<view 
				class="service-menu-item animate-slide-in-up" 
				v-for="(s, i) in serviceList" 
				:key="i" 
				:class="'delay-' + (i * 100)"
				@tap="goToService(s.path)"
			>
				<view class="service-menu-icon-wrap">
					<uni-icons :type="s.icon" size="22" :color="getServiceColor(i)"></uni-icons>
				</view>
				<text class="service-menu-name">{{ s.name }}</text>
				<uni-icons type="right" size="14" color="#ccc"></uni-icons>
			</view>
			</scroll-view>
		</view>

		<!-- 介绍区域 -->
		<view class="intro-section animate-slide-in-up">
			<view class="intro-content">
				<view class="intro-title">
					<text class="title-text">医视智行</text>
					<text class="sparkle animate-pulse">✨</text>
				</view>
				<view class="intro-subtitle">
					<text class="subtitle-text">有健康问题随时问我</text>
				</view>
			</view>
			<view class="intro-image animate-float">
				<view class="doctor-placeholder">
					<image class="intro-logo-img" :src="appLogoUrl" mode="aspectFit" />
				</view>
			</view>
		</view>

		<!-- 健康数据：拍报告、就诊记录、拍皮肤、检查检验、拍三餐 -->
		<view class="health-card animate-slide-in-up delay-100">
			<view class="health-card-header">
				<view class="green-line"></view>
				<text class="health-card-title">健康数据</text>
			</view>
			<view class="health-data-grid">
				<view class="data-item animate-slide-in-up" v-for="(item, index) in healthDataList" :key="index" :class="'delay-' + (150 + index * 50)" @tap="onDataItemClick(index)">
					<view class="data-icon-wrapper">
						<uni-icons :type="item.icon" size="24" :color="item.color"></uni-icons>
					</view>
					<text class="data-text">{{ item.name }}</text>
				</view>
			</view>
		</view>

		<!-- 健康档案模块 -->
		<view class="health-archive-card animate-slide-in-up delay-200">
			<view class="health-archive-header">
				<view class="green-line"></view>
				<text class="health-archive-title">健康档案</text>
			</view>
			<view class="health-archive-button-wrapper">
				<view class="health-archive-button animate-float delay-300" @tap="onHealthArchiveClick">
					<view class="archive-icon-wrapper archive-logo">
						<uni-icons type="folder-add" size="36" color="#07C160"></uni-icons>
					</view>
					<text class="archive-button-text">健康档案</text>
					<uni-icons type="right" size="16" color="#999"></uni-icons>
				</view>
			</view>
		</view>

		<!-- 健康建议卡片（智能体分析后自动生成的三条建议，无则显示默认） -->
		<view class="task-section">
			<view class="task-card blue-card animate-slide-in-up delay-200" @tap="onHealthTipClick(0)">
				<view class="task-icon-wrapper blue-bg">
					<uni-icons type="checkmarkempty" size="20" color="#ffffff"></uni-icons>
				</view>
				<view class="task-content">
					<text class="task-text">{{ healthTips[0] || '每天保持8小时充足睡眠' }}</text>
				</view>
				<uni-icons type="right" size="16" color="#999"></uni-icons>
			</view>

			<view class="task-card purple-card animate-slide-in-up delay-300" @tap="onHealthTipClick(1)">
				<view class="task-icon-wrapper purple-bg">
					<uni-icons type="star" size="20" color="#ffffff"></uni-icons>
				</view>
				<view class="task-content">
					<text class="task-text">{{ healthTips[1] || '每天喝够2000ml水' }}</text>
				</view>
				<uni-icons type="right" size="16" color="#999"></uni-icons>
			</view>

			<view class="task-card purple-card animate-slide-in-up delay-400" @tap="onHealthTipClick(2)">
				<view class="task-icon-wrapper purple-bg">
					<uni-icons type="star" size="20" color="#ffffff"></uni-icons>
				</view>
				<view class="task-content">
					<text class="task-text">{{ healthTips[2] || '每周至少150分钟中等强度运动' }}</text>
				</view>
				<uni-icons type="right" size="16" color="#999"></uni-icons>
			</view>
		</view>

		<!-- 基本信息填写弹窗 -->
		<basic-info-form 
			ref="basicInfoForm" 
			:userId="currentUserId"
			@success="onBasicInfoSuccess"
			@cancel="onBasicInfoCancel"
		></basic-info-form>
		</view>
		<!-- 底部导航栏：放在 scale 容器外，保证 fixed 相对视口固定 -->
		<custom-tabbar :current="0"></custom-tabbar>
	</view>
</template>

<script>
	import customTabbar from '@/components/custom-tabbar/custom-tabbar.vue'
	import BasicInfoForm from '@/components/basic-info-form/basic-info-form.vue'
	import config from '@/utils/config.js'
	
	export default {
		components: {
			customTabbar,
			BasicInfoForm
		},
		data() {
			return {
				appLogoUrl: config.appLogoUrl,
				isLogin: false,
				currentUserId: null,
				hasCheckedBasicInfo: false, // 是否已检查过基本信息
				// 健康数据：拍报告、就诊记录、拍皮肤、检查检验、拍三餐
				healthDataList: [
					{ name: '拍报告', icon: 'paperplane', color: '#07C160' },
					{ name: '就诊记录', icon: 'calendar', color: '#1890ff' },
					{ name: '拍皮肤', icon: 'camera-filled', color: '#ff6b9d' },
					{ name: '检查检验', icon: 'checkmarkempty', color: '#9c27b0' },
					{ name: '拍三餐', icon: 'image', color: '#4CAF50' }
				],
				showServiceMenu: false,
				// 首页三条建议（智能体分析体检报告后自动生成，从接口拉取；无则用默认文案）
				healthTips: [],
				// 服务列表（与“服务”页一致，用于右上角菜单）
				serviceList: [
					{ name: 'AI问诊', path: '/pages/ai-consultation/index', icon: 'chatbubble' },
					{ name: '在线咨询', path: '/pages/consultation/index', icon: 'paperplane' },
					{ name: '慢病管理', path: '/pages/chronic-disease/index', icon: 'heart' },
					{ name: '情绪管理', path: '/pages/emotion-detection/index', icon: 'chatbubble-filled' },
					{ name: '快乐活动', path: '/pages/happy-activity/index', icon: 'person-filled' }
				]
			}
		},
		onLoad() {
			console.log('首页加载')
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
				this.isLogin = loginStatus === true
				
				// 如果已登录，检查是否需要填写基本信息，并拉取首页健康建议
				if (this.isLogin) {
					const userInfo = uni.getStorageSync('userInfo')
					if (userInfo && userInfo.userId) {
						this.currentUserId = parseInt(userInfo.userId)
						if (!this.hasCheckedBasicInfo) {
							// 检查用户是否已有健康档案
							this.checkHealthArchive()
						}
						// 拉取智能体生成的最新三条健康建议（生成健康档案后会自动分析并生成）
						this.loadHealthTips()
					}
				} else {
					this.healthTips = []
				}
			},
			// 默认建议池：无健康档案或接口无数据时随机取 3 条展示
			getRandomDefaultTips() {
				const pool = [
					'每天保持8小时充足睡眠',
					'每天喝够2000ml水',
					'每周至少150分钟中等强度运动',
					'饮食均衡，多吃蔬菜水果',
					'减少久坐，每小时活动5分钟',
					'保持心情愉悦，适当放松',
					'定期体检，关注身体指标',
					'少油少盐，控糖限酒'
				]
				const shuffled = pool.slice().sort(() => Math.random() - 0.5)
				return shuffled.slice(0, 3)
			},
			loadHealthTips() {
				if (!this.currentUserId) {
					this.healthTips = this.getRandomDefaultTips()
					return
				}
				uni.request({
					url: `${config.baseUrl}/health-ai-analysis/user/${this.currentUserId}/recommendations/latest`,
					method: 'GET',
					data: { limit: 3 },
					success: (res) => {
						if (res.statusCode === 200 && res.data && res.data.code === 200 && Array.isArray(res.data.data)) {
							const list = res.data.data
							const defaults = this.getRandomDefaultTips()
							this.healthTips = []
							for (let i = 0; i < 3; i++) {
								const item = list[i]
								const text = (item && (item.title || item.content)) ? (item.title || item.content).trim() : ''
								this.healthTips.push(text || defaults[i])
							}
							// 若接口返回不足 3 条或为空，用随机默认补足
							while (this.healthTips.length < 3) {
								this.healthTips.push(defaults[this.healthTips.length] || this.getRandomDefaultTips()[this.healthTips.length])
							}
						} else {
							this.healthTips = this.getRandomDefaultTips()
						}
					},
					fail: () => {
						this.healthTips = this.getRandomDefaultTips()
					}
				})
			},
			// 检查用户是否已有健康档案
			checkHealthArchive() {
				if (!this.currentUserId) {
					return
				}
				
				// 标记已检查，避免重复检查
				this.hasCheckedBasicInfo = true
				
				uni.request({
					url: `${config.baseUrl}/health-archive-process/list`,
					method: 'GET',
					data: {
						userId: this.currentUserId,
						asCreator: false
					},
					header: {
						'userId': this.currentUserId.toString(),
						'Content-Type': 'application/json'
					},
					success: (res) => {
						if (res.statusCode === 200 && res.data && res.data.code === 200) {
							const archiveList = res.data.data || []
							// 如果用户没有健康档案，弹出基本信息填写弹窗
							if (archiveList.length === 0) {
								setTimeout(() => {
									if (this.$refs.basicInfoForm) {
										this.$refs.basicInfoForm.open()
									}
								}, 1000) // 延迟1秒弹出，让页面先加载完成
							}
						}
					},
					fail: (err) => {
						console.error('检查健康档案失败:', err)
						// 如果检查失败，也弹出基本信息填写弹窗（保险起见）
						setTimeout(() => {
							if (this.$refs.basicInfoForm) {
								this.$refs.basicInfoForm.open()
							}
						}, 1000)
					}
				})
			},
			onBasicInfoSuccess(data) {
				// 基本信息保存成功
				console.log('基本信息保存成功', data)
				// 标记已检查，避免再次弹出
				this.hasCheckedBasicInfo = true
			},
			onBasicInfoCancel() {
				// 用户选择稍后填写
				console.log('用户选择稍后填写基本信息')
				// 标记已检查，避免再次弹出（但下次进入页面时还会检查）
				this.hasCheckedBasicInfo = true
			},
			// 右上角菜单：显示服务列表
			onMenuClick() {
			this.showServiceMenu = !this.showServiceMenu
		},
		goToService(path) {
			this.showServiceMenu = false
			if (path) uni.navigateTo({ url: path })
		},
		getServiceColor(index) {
			const colors = ['#07C160', '#1890ff', '#ff6b9d', '#fa8c16', '#9c27b0', '#ffc107', '#4ECDC4']
			return colors[index % colors.length]
		},
			onSpeakerClick() {
				uni.showToast({
					title: '语音功能开发中',
					icon: 'none'
				})
			},
			onMoreClick() {
				uni.showToast({
					title: '更多功能开发中',
					icon: 'none'
				})
			},
			// 健康数据卡片事件：拍报告、就诊记录、拍皮肤、检查检验、拍三餐
			onDataItemClick(index) {
				const item = this.healthDataList[index]
				if (!item) return
				if (item.name === '拍报告') {
					this.uploadReport()
					return
				}
				if (item.name === '就诊记录') {
					this.showMedicalRecordOptions()
					return
				}
				if (item.name === '拍皮肤') {
					this.uploadSkin()
					return
				}
				if (item.name === '检查检验') {
					this.showLabOptions()
					return
				}
				if (item.name === '拍三餐') {
					this.uploadMeal()
					return
				}
				uni.showToast({ title: `打开${item.name}`, icon: 'none' })
			},
			
			// 显示检查检验选项
			showLabOptions() {
				uni.showActionSheet({
					itemList: ['拍报告', '第三方导入'],
					success: (res) => {
						if (res.tapIndex === 0) {
							// 拍报告
							this.uploadLab()
						} else if (res.tapIndex === 1) {
							// 第三方导入
							uni.showToast({
								title: '功能待开发',
								icon: 'none'
							})
						}
					}
				})
			},
			
			// 检查检验：拍报告并上传
			uploadLab() {
				// 检查登录状态
				const loginStatus = uni.getStorageSync('isLogin')
				if (!loginStatus) {
					uni.showToast({
						title: '请先登录',
						icon: 'none'
					})
					// 跳转到登录页面
					setTimeout(() => {
						uni.navigateTo({
							url: '/pages/login/login'
						})
					}, 1500)
					return
				}
				
				// 获取用户token（如果需要的话）
				const token = uni.getStorageSync('token')
				
				// 调用uni.chooseImage选择图片或拍照
				uni.chooseImage({
					count: 1, // 只能选择一张图片
					sourceType: ['camera', 'album'], // 可以拍照或从相册选择
					success: (res) => {
						const tempFilePath = res.tempFilePaths[0]
						
						// 显示上传中提示
						uni.showLoading({
							title: '上传中...',
							mask: true
						})
						
						// 获取用户信息
						const userInfo = uni.getStorageSync('userInfo')
						const userId = userInfo ? userInfo.userId : null
						
						// 上传图片
						uni.uploadFile({
							url: `${config.baseUrl}/raw-health-data/LAB/upload`,
							filePath: tempFilePath,
							name: 'file',
							header: {
								'userId': userId || '', // 设置userId到请求头，拦截器会读取
								'token': token || '' // 如果有token，需要添加到请求头
							},
							success: (uploadRes) => {
								uni.hideLoading()
								
								try {
									const data = JSON.parse(uploadRes.data)
									if (data.code === 200) {
										uni.showToast({
											title: '已上传',
											icon: 'success'
										})
									} else {
										uni.showToast({
											title: data.message || '上传失败',
											icon: 'none'
										})
									}
								} catch (e) {
									console.error('解析响应数据失败', e)
									uni.showToast({
										title: '上传失败',
										icon: 'none'
									})
								}
							},
							fail: (err) => {
								uni.hideLoading()
								console.error('上传失败', err)
								uni.showToast({
									title: err.errMsg && err.errMsg.indexOf('timeout') !== -1 ? '上传超时，请重试' : '上传失败，请检查网络',
									icon: 'none'
								})
							}
						})
					},
					fail: (err) => {
						console.error('选择图片失败', err)
						uni.showToast({
							title: '选择图片失败',
							icon: 'none'
						})
					}
				})
			},
			
			// 显示情绪检测选项
			showEmotionOptions() {
				uni.showActionSheet({
					itemList: ['拍报告', '第三方导入'],
					success: (res) => {
						if (res.tapIndex === 0) {
							// 拍报告
							this.uploadEmotion()
						} else if (res.tapIndex === 1) {
							// 第三方导入
							uni.showToast({
								title: '功能待开发',
								icon: 'none'
							})
						}
					}
				})
			},
			
			// 情绪检测：拍报告并上传
			uploadEmotion() {
				// 检查登录状态
				const loginStatus = uni.getStorageSync('isLogin')
				if (!loginStatus) {
					uni.showToast({
						title: '请先登录',
						icon: 'none'
					})
					// 跳转到登录页面
					setTimeout(() => {
						uni.navigateTo({
							url: '/pages/login/login'
						})
					}, 1500)
					return
				}
				
				// 获取用户token（如果需要的话）
				const token = uni.getStorageSync('token')
				
				// 调用uni.chooseImage选择图片或拍照
				uni.chooseImage({
					count: 1, // 只能选择一张图片
					sourceType: ['camera', 'album'], // 可以拍照或从相册选择
					success: (res) => {
						const tempFilePath = res.tempFilePaths[0]
						
						// 显示上传中提示
						uni.showLoading({
							title: '上传中...',
							mask: true
						})
						
						// 获取用户信息
						const userInfo = uni.getStorageSync('userInfo')
						const userId = userInfo ? userInfo.userId : null
						
						// 上传图片
						uni.uploadFile({
							url: `${config.baseUrl}/raw-health-data/EMOTION/upload`,
							filePath: tempFilePath,
							name: 'file',
							header: {
								'userId': userId || '', // 设置userId到请求头，拦截器会读取
								'token': token || '' // 如果有token，需要添加到请求头
							},
							success: (uploadRes) => {
								uni.hideLoading()
								
								try {
									const data = JSON.parse(uploadRes.data)
									if (data.code === 200) {
										uni.showToast({
											title: '已上传',
											icon: 'success'
										})
									} else {
										uni.showToast({
											title: data.message || '上传失败',
											icon: 'none'
										})
									}
								} catch (e) {
									console.error('解析响应数据失败', e)
									uni.showToast({
										title: '上传失败',
										icon: 'none'
									})
								}
							},
							fail: (err) => {
								uni.hideLoading()
								console.error('上传失败', err)
								uni.showToast({
									title: err.errMsg && err.errMsg.indexOf('timeout') !== -1 ? '上传超时，请重试' : '上传失败，请检查网络',
									icon: 'none'
								})
							}
						})
					},
					fail: (err) => {
						console.error('选择图片失败', err)
						uni.showToast({
							title: '选择图片失败',
							icon: 'none'
						})
					}
				})
			},
			
			// 显示基因检测选项
			showGeneticOptions() {
				uni.showActionSheet({
					itemList: ['拍报告', '第三方导入'],
					success: (res) => {
						if (res.tapIndex === 0) {
							// 拍报告
							this.uploadGenetic()
						} else if (res.tapIndex === 1) {
							// 第三方导入
							uni.showToast({
								title: '功能待开发',
								icon: 'none'
							})
						}
					}
				})
			},
			
			// 基因检测：拍报告并上传
			uploadGenetic() {
				// 检查登录状态
				const loginStatus = uni.getStorageSync('isLogin')
				if (!loginStatus) {
					uni.showToast({
						title: '请先登录',
						icon: 'none'
					})
					// 跳转到登录页面
					setTimeout(() => {
						uni.navigateTo({
							url: '/pages/login/login'
						})
					}, 1500)
					return
				}
				
				// 获取用户token（如果需要的话）
				const token = uni.getStorageSync('token')
				
				// 调用uni.chooseImage选择图片或拍照
				uni.chooseImage({
					count: 1, // 只能选择一张图片
					sourceType: ['camera', 'album'], // 可以拍照或从相册选择
					success: (res) => {
						const tempFilePath = res.tempFilePaths[0]
						
						// 显示上传中提示
						uni.showLoading({
							title: '上传中...',
							mask: true
						})
						
						// 获取用户信息
						const userInfo = uni.getStorageSync('userInfo')
						const userId = userInfo ? userInfo.userId : null
						
						// 上传图片
						uni.uploadFile({
							url: `${config.baseUrl}/raw-health-data/GENETIC/upload`,
							filePath: tempFilePath,
							name: 'file',
							header: {
								'userId': userId || '', // 设置userId到请求头，拦截器会读取
								'token': token || '' // 如果有token，需要添加到请求头
							},
							success: (uploadRes) => {
								uni.hideLoading()
								
								try {
									const data = JSON.parse(uploadRes.data)
									if (data.code === 200) {
										uni.showToast({
											title: '已上传',
											icon: 'success'
										})
									} else {
										uni.showToast({
											title: data.message || '上传失败',
											icon: 'none'
										})
									}
								} catch (e) {
									console.error('解析响应数据失败', e)
									uni.showToast({
										title: '上传失败',
										icon: 'none'
									})
								}
							},
							fail: (err) => {
								uni.hideLoading()
								console.error('上传失败', err)
								uni.showToast({
									title: err.errMsg && err.errMsg.indexOf('timeout') !== -1 ? '上传超时，请重试' : '上传失败，请检查网络',
									icon: 'none'
								})
							}
						})
					},
					fail: (err) => {
						console.error('选择图片失败', err)
						uni.showToast({
							title: '选择图片失败',
							icon: 'none'
						})
					}
				})
			},
			
			// 显示可穿戴设备选项
			showWearableOptions() {
				uni.showActionSheet({
					itemList: ['绑定华为运动健康', '查看绑定状态', '同步数据'],
					success: (res) => {
						if (res.tapIndex === 0) {
							// 绑定华为运动健康
							this.bindHuaweiHealth()
						} else if (res.tapIndex === 1) {
							// 查看绑定状态
							this.checkHuaweiHealthStatus()
						} else if (res.tapIndex === 2) {
							// 同步数据
							this.syncHuaweiHealthData()
						}
					}
				})
			},
			
			// 绑定华为运动健康
			bindHuaweiHealth() {
				// 检查登录状态
				const loginStatus = uni.getStorageSync('isLogin')
				if (!loginStatus) {
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
				
				// 获取用户信息
				const userInfo = uni.getStorageSync('userInfo')
				const userId = userInfo ? userInfo.userId : null
				const token = uni.getStorageSync('token')
				
				uni.showLoading({
					title: '获取授权链接...',
					mask: true
				})
				
				// 获取授权URL
				uni.request({
					url: `${config.baseUrl}/huawei-health/authorize-url`,
					method: 'GET',
					header: {
						'userId': userId || '',
						'token': token || '',
						'Content-Type': 'application/json'
					},
					success: (res) => {
						uni.hideLoading()
						
						if (res.statusCode === 200 && res.data.code === 200) {
							const authorizeUrl = res.data.data.authorizeUrl
							
							// 在H5环境中，可以直接跳转
							// #ifdef H5
							window.location.href = authorizeUrl
							// #endif
							
							// 在微信小程序中，需要复制链接到浏览器打开
							// #ifdef MP-WEIXIN
							uni.setClipboardData({
								data: authorizeUrl,
								success: () => {
									uni.showModal({
										title: '提示',
										content: '授权链接已复制到剪贴板，请在浏览器中打开并完成授权',
										showCancel: false
									})
								}
							})
							// #endif
						} else {
							uni.showToast({
								title: res.data.message || '获取授权链接失败',
								icon: 'none'
							})
						}
					},
					fail: (err) => {
						uni.hideLoading()
						console.error('获取授权链接失败', err)
						uni.showToast({
							title: '获取授权链接失败',
							icon: 'none'
						})
					}
				})
			},
			
			// 查看绑定状态
			checkHuaweiHealthStatus() {
				// 检查登录状态
				const loginStatus = uni.getStorageSync('isLogin')
				if (!loginStatus) {
					uni.showToast({
						title: '请先登录',
						icon: 'none'
					})
					return
				}
				
				// 获取用户信息
				const userInfo = uni.getStorageSync('userInfo')
				const userId = userInfo ? userInfo.userId : null
				const token = uni.getStorageSync('token')
				
				uni.showLoading({
					title: '查询中...',
					mask: true
				})
				
				// 查询绑定状态
				uni.request({
					url: `${config.baseUrl}/huawei-health/bind-status`,
					method: 'GET',
					header: {
						'userId': userId || '',
						'token': token || '',
						'Content-Type': 'application/json'
					},
					success: (res) => {
						uni.hideLoading()
						
						if (res.statusCode === 200 && res.data.code === 200) {
							const data = res.data.data
							if (data.bindStatus) {
								uni.showModal({
									title: '绑定状态',
									content: `已绑定华为运动健康\n绑定手机号：${data.phone || '未知'}\n绑定时间：${data.bindTime || '未知'}`,
									showCancel: false
								})
							} else {
								uni.showModal({
									title: '绑定状态',
									content: '未绑定华为运动健康',
									showCancel: false
								})
							}
						} else {
							uni.showToast({
								title: res.data.message || '查询失败',
								icon: 'none'
							})
						}
					},
					fail: (err) => {
						uni.hideLoading()
						console.error('查询绑定状态失败', err)
						uni.showToast({
							title: '查询失败',
							icon: 'none'
						})
					}
				})
			},
			
			// 同步华为运动健康数据
			syncHuaweiHealthData() {
				// 检查登录状态
				const loginStatus = uni.getStorageSync('isLogin')
				if (!loginStatus) {
					uni.showToast({
						title: '请先登录',
						icon: 'none'
					})
					return
				}
				
				// 获取用户信息
				const userInfo = uni.getStorageSync('userInfo')
				const userId = userInfo ? userInfo.userId : null
				const token = uni.getStorageSync('token')
				
				uni.showLoading({
					title: '同步中...',
					mask: true
				})
				
				// 同步数据（默认同步最近7天）
				uni.request({
					url: `${config.baseUrl}/huawei-health/sync`,
					method: 'POST',
					header: {
						'userId': userId || '',
						'token': token || '',
						'Content-Type': 'application/json'
					},
					success: (res) => {
						uni.hideLoading()
						
						if (res.statusCode === 200 && res.data.code === 200) {
							const syncCount = res.data.data.syncCount || 0
							uni.showToast({
								title: `同步成功，共${syncCount}条数据`,
								icon: 'success'
							})
						} else {
							uni.showToast({
								title: res.data.message || '同步失败',
								icon: 'none'
							})
						}
					},
					fail: (err) => {
						uni.hideLoading()
						console.error('同步数据失败', err)
						uni.showToast({
							title: '同步失败，请检查网络',
							icon: 'none'
						})
					}
				})
			},
			
			// 显示就诊记录上传选项
			showMedicalRecordOptions() {
				uni.showActionSheet({
					itemList: ['拍照上传', '文件上传'],
					success: (res) => {
						if (res.tapIndex === 0) {
							// 拍照上传
							this.uploadMedicalRecordImage()
						} else if (res.tapIndex === 1) {
							// 文件上传
							this.uploadMedicalRecordFile()
						}
					}
				})
			},
			
			// 拍皮肤：拍照并上传
			uploadSkin() {
				// 检查登录状态
				const loginStatus = uni.getStorageSync('isLogin')
				if (!loginStatus) {
					uni.showToast({
						title: '请先登录',
						icon: 'none'
					})
					// 跳转到登录页面
					setTimeout(() => {
						uni.navigateTo({
							url: '/pages/login/login'
						})
					}, 1500)
					return
				}
				
				// 获取用户token（如果需要的话）
				const token = uni.getStorageSync('token')
				
				// 调用uni.chooseImage选择图片或拍照
				uni.chooseImage({
					count: 1, // 只能选择一张图片
					sourceType: ['camera', 'album'], // 可以拍照或从相册选择
					success: (res) => {
						const tempFilePath = res.tempFilePaths[0]
						
						// 显示上传中提示
						uni.showLoading({
							title: '上传中...',
							mask: true
						})
						
						// 获取用户信息
						const userInfo = uni.getStorageSync('userInfo')
						const userId = userInfo ? userInfo.userId : null
						
						// 上传图片
						uni.uploadFile({
							url: `${config.baseUrl}/raw-health-data/SKIN/upload`,
							filePath: tempFilePath,
							name: 'file',
							header: {
								'userId': userId || '', // 设置userId到请求头，拦截器会读取
								'token': token || '' // 如果有token，需要添加到请求头
							},
							success: (uploadRes) => {
								uni.hideLoading()
								
								try {
									const data = JSON.parse(uploadRes.data)
									if (data.code === 200) {
										uni.showToast({
											title: '已上传',
											icon: 'success'
										})
									} else {
										uni.showToast({
											title: data.message || '上传失败',
											icon: 'none'
										})
									}
								} catch (e) {
									console.error('解析响应数据失败', e)
									uni.showToast({
										title: '上传失败',
										icon: 'none'
									})
								}
							},
							fail: (err) => {
								uni.hideLoading()
								console.error('上传失败', err)
								uni.showToast({
									title: err.errMsg && err.errMsg.indexOf('timeout') !== -1 ? '上传超时，请重试' : '上传失败，请检查网络',
									icon: 'none'
								})
							}
						})
					},
					fail: (err) => {
						console.error('选择图片失败', err)
						uni.showToast({
							title: '选择图片失败',
							icon: 'none'
						})
					}
				})
			},
			
			// 拍三餐：拍照并上传
			uploadMeal() {
				// 检查登录状态
				const loginStatus = uni.getStorageSync('isLogin')
				if (!loginStatus) {
					uni.showToast({
						title: '请先登录',
						icon: 'none'
					})
					// 跳转到登录页面
					setTimeout(() => {
						uni.navigateTo({
							url: '/pages/login/login'
						})
					}, 1500)
					return
				}
				
				// 获取用户token（如果需要的话）
				const token = uni.getStorageSync('token')
				
				// 调用uni.chooseImage选择图片或拍照
				uni.chooseImage({
					count: 1, // 只能选择一张图片
					sourceType: ['camera', 'album'], // 可以拍照或从相册选择
					success: (res) => {
						const tempFilePath = res.tempFilePaths[0]
						
						// 显示上传中提示
						uni.showLoading({
							title: '上传并分析中...',
							mask: true
						})
						
						// 获取用户信息
						const userInfo = uni.getStorageSync('userInfo')
						const userId = userInfo ? userInfo.userId : null
						
						// 上传图片（后端落库后调用 food-train，CPU 推理可能较慢）
						uni.uploadFile({
							url: `${config.baseUrl}/raw-health-data/MEAL/upload`,
							filePath: tempFilePath,
							name: 'file',
							timeout: 120000,
							header: {
								'userId': userId || '', // 设置userId到请求头，拦截器会读取
								'token': token || '' // 如果有token，需要添加到请求头
							},
							success: (uploadRes) => {
								uni.hideLoading()
								
								try {
									const data = JSON.parse(uploadRes.data)
									if (data.code === 200) {
										const d = data.data || {}
										const n = d.nutrition
										const err = d.nutritionError
										if (n && typeof n === 'object' && n.energy_kcal != null) {
											const kcal = Number(n.energy_kcal)
											const kcalStr = Number.isFinite(kcal) ? kcal.toFixed(0) : String(n.energy_kcal)
											const lines = [
												`估算热量约 ${kcalStr} kcal`,
												n.protein_g != null ? `蛋白质 ${Number(n.protein_g).toFixed(1)} g` : '',
												n.fat_g != null ? `脂肪 ${Number(n.fat_g).toFixed(1)} g` : '',
												n.carbs_g != null ? `碳水 ${Number(n.carbs_g).toFixed(1)} g` : ''
											].filter(Boolean)
											uni.showModal({
												title: '营养估算',
												content: lines.join('\n') + '\n\n（模型预测，仅供参考，不构成医疗建议）',
												showCancel: false
											})
										} else {
											const tip = err ? `已保存图片。分析未成功：${err}` : '已保存。未返回热量（请确认已启动 food-train :5001）'
											uni.showModal({
												title: '拍三餐',
												content: tip,
												showCancel: false
											})
										}
									} else {
										uni.showToast({
											title: data.message || '上传失败',
											icon: 'none'
										})
									}
								} catch (e) {
									console.error('解析响应数据失败', e)
									uni.showToast({
										title: '上传失败',
										icon: 'none'
									})
								}
							},
							fail: (err) => {
								uni.hideLoading()
								console.error('上传失败', err)
								uni.showToast({
									title: err.errMsg && err.errMsg.indexOf('timeout') !== -1 ? '上传超时，请重试' : '上传失败，请检查网络',
									icon: 'none'
								})
							}
						})
					},
					fail: (err) => {
						console.error('选择图片失败', err)
						uni.showToast({
							title: '选择图片失败',
							icon: 'none'
						})
					}
				})
			},
			
			// 拍报告：拍照并上传
			uploadReport() {
				// 检查登录状态
				const loginStatus = uni.getStorageSync('isLogin')
				if (!loginStatus) {
					uni.showToast({
						title: '请先登录',
						icon: 'none'
					})
					// 跳转到登录页面
					setTimeout(() => {
						uni.navigateTo({
							url: '/pages/login/login'
						})
					}, 1500)
					return
				}
				
				// 获取用户token（如果需要的话）
				const token = uni.getStorageSync('token')
				
				// 调用uni.chooseImage选择图片或拍照
				uni.chooseImage({
					count: 1, // 只能选择一张图片
					sourceType: ['camera', 'album'], // 可以拍照或从相册选择
					success: (res) => {
						const tempFilePath = res.tempFilePaths[0]
						
						// 显示上传中提示
						uni.showLoading({
							title: '上传中...',
							mask: true
						})
						
						// 获取用户信息
						const userInfo = uni.getStorageSync('userInfo')
						const userId = userInfo ? userInfo.userId : null
						
						// 上传图片
						uni.uploadFile({
							url: `${config.baseUrl}/raw-health-data/REPORT/upload`,
							filePath: tempFilePath,
							name: 'file',
							timeout: 180000, // 3分钟，经公网/cpolar 上传大图易超时
							header: {
								'userId': userId || '', // 设置userId到请求头，拦截器会读取
								'token': token || '' // 如果有token，需要添加到请求头
							},
							success: (uploadRes) => {
								uni.hideLoading()
								
								try {
									const data = JSON.parse(uploadRes.data)
									if (data.code === 200) {
										uni.showToast({
											title: '已上传',
											icon: 'success'
										})
									} else {
										uni.showToast({
											title: data.message || '上传失败',
											icon: 'none'
										})
									}
								} catch (e) {
									console.error('解析响应数据失败', e)
									uni.showToast({
										title: '上传失败',
										icon: 'none'
									})
								}
							},
							fail: (err) => {
								uni.hideLoading()
								console.error('上传失败', err)
								uni.showToast({
									title: err.errMsg && err.errMsg.indexOf('timeout') !== -1 ? '上传超时，请重试' : '上传失败，请检查网络',
									icon: 'none'
								})
							}
						})
					},
					fail: (err) => {
						console.error('选择图片失败', err)
						uni.showToast({
							title: '选择图片失败',
							icon: 'none'
						})
					}
				})
			},
			
			// 就诊记录：拍照上传
			uploadMedicalRecordImage() {
				// 检查登录状态
				const loginStatus = uni.getStorageSync('isLogin')
				if (!loginStatus) {
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
				
				// 获取用户信息
				const userInfo = uni.getStorageSync('userInfo')
				const userId = userInfo ? userInfo.userId : null
				const token = uni.getStorageSync('token')
				
				// 调用uni.chooseImage选择图片或拍照
				uni.chooseImage({
					count: 1,
					sourceType: ['camera', 'album'],
					success: (res) => {
						const tempFilePath = res.tempFilePaths[0]
						
						uni.showLoading({
							title: '上传中...',
							mask: true
						})
						
						// 上传图片
						uni.uploadFile({
							url: `${config.baseUrl}/raw-health-data/MEDICAL_RECORD/upload-image`,
							filePath: tempFilePath,
							name: 'file',
							header: {
								'userId': userId || '',
								'token': token || ''
							},
							success: (uploadRes) => {
								uni.hideLoading()
								
								try {
									const data = JSON.parse(uploadRes.data)
									if (data.code === 200) {
										uni.showToast({
											title: '已上传',
											icon: 'success'
										})
									} else {
										uni.showToast({
											title: data.message || '上传失败',
											icon: 'none'
										})
									}
								} catch (e) {
									console.error('解析响应数据失败', e)
									uni.showToast({
										title: '上传失败',
										icon: 'none'
									})
								}
							},
							fail: (err) => {
								uni.hideLoading()
								console.error('上传失败', err)
								uni.showToast({
									title: err.errMsg && err.errMsg.indexOf('timeout') !== -1 ? '上传超时，请重试' : '上传失败，请检查网络',
									icon: 'none'
								})
							}
						})
					},
					fail: (err) => {
						console.error('选择图片失败', err)
						uni.showToast({
							title: '选择图片失败',
							icon: 'none'
						})
					}
				})
			},
			
			// 就诊记录：文件上传
			uploadMedicalRecordFile() {
				// 检查登录状态
				const loginStatus = uni.getStorageSync('isLogin')
				if (!loginStatus) {
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
				
				// 获取用户信息
				const userInfo = uni.getStorageSync('userInfo')
				const userId = userInfo ? userInfo.userId : null
				const token = uni.getStorageSync('token')
				
				// #ifdef H5
				// H5环境使用input file选择PDF文件
				const input = document.createElement('input')
				input.type = 'file'
				input.accept = '.pdf,application/pdf'
				input.onchange = (e) => {
					const file = e.target.files[0]
					if (!file) return
					
					uni.showLoading({
						title: '上传中...',
						mask: true
					})
					
					// 使用FormData上传
					const formData = new FormData()
					formData.append('file', file)
					
					uni.request({
						url: `${config.baseUrl}/raw-health-data/MEDICAL_RECORD/upload-file`,
						method: 'POST',
						header: {
							'userId': userId || '',
							'token': token || ''
						},
						data: formData,
						success: (res) => {
							uni.hideLoading()
							if (res.statusCode === 200 && res.data.code === 200) {
								uni.showToast({
									title: '已上传',
									icon: 'success'
								})
							} else {
								uni.showToast({
									title: res.data.message || '上传失败',
									icon: 'none'
								})
							}
						},
						fail: (err) => {
							uni.hideLoading()
							console.error('上传失败', err)
							uni.showToast({
								title: '上传失败，请检查网络',
								icon: 'none'
							})
						}
					})
				}
				input.click()
				// #endif
				
				// #ifdef MP-WEIXIN
				// 微信小程序使用uni.chooseMessageFile
				uni.chooseMessageFile({
					count: 1,
					type: 'file',
					extension: ['pdf'],
					success: (res) => {
						const filePath = res.tempFiles[0].path
						
						uni.showLoading({
							title: '上传中...',
							mask: true
						})
						
						// 上传文件
						uni.uploadFile({
							url: `${config.baseUrl}/raw-health-data/MEDICAL_RECORD/upload-file`,
							filePath: filePath,
							name: 'file',
							header: {
								'userId': userId || '',
								'token': token || ''
							},
							success: (uploadRes) => {
								uni.hideLoading()
								
								try {
									const data = JSON.parse(uploadRes.data)
									if (data.code === 200) {
										uni.showToast({
											title: '已上传',
											icon: 'success'
										})
									} else {
										uni.showToast({
											title: data.message || '上传失败',
											icon: 'none'
										})
									}
								} catch (e) {
									console.error('解析响应数据失败', e)
									uni.showToast({
										title: '上传失败',
										icon: 'none'
									})
								}
							},
							fail: (err) => {
								uni.hideLoading()
								console.error('上传失败', err)
								uni.showToast({
									title: '上传失败，请检查网络',
									icon: 'none'
								})
							}
						})
					},
					fail: (err) => {
						console.error('选择文件失败', err)
						uni.showToast({
							title: '选择文件失败',
							icon: 'none'
						})
					}
				})
				// #endif
			},
			// 任务卡片事件
			onHealthTipClick(index) {
				const healthTips = [
					'每天保持8小时充足睡眠有助于恢复体力和提高免疫力',
					'每天喝够2000ml水可以维持身体正常代谢和排毒',
					'每周至少150分钟中等强度运动可以增强心肺功能'
				]
				uni.showToast({
					title: healthTips[index],
					icon: 'none',
					duration: 2000
				})
			},
			// 服务网格事件
			
			onPlusClick() {
				uni.showToast({
					title: '更多功能',
					icon: 'none'
				})
			},
			onCameraClick() {
				uni.showToast({
					title: '拍照功能开发中',
					icon: 'none'
				})
			},
			// 健康档案按钮点击事件
			onHealthArchiveClick() {
				// 跳转到健康档案列表页面
				uni.navigateTo({
					url: '/pages/health-archive/list',
					fail: (err) => {
						console.error('跳转失败', err)
						uni.showToast({
							title: '跳转失败',
							icon: 'none'
						})
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
		padding-bottom: 120rpx;
		background: transparent;
		min-height: 100vh;
		position: relative;
	}
	/* 漂浮光斑：iOS26 风格动态细节 */
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
		width: 520rpx;
		height: 520rpx;
		left: -220rpx;
		top: 160rpx;
		background: radial-gradient(circle at 30% 30%, rgba(7,193,96,0.35), transparent 60%);
		animation: floaty 6.8s ease-in-out infinite;
	}
	.container::after {
		width: 600rpx;
		height: 600rpx;
		right: -260rpx;
		top: 420rpx;
		background: radial-gradient(circle at 30% 30%, rgba(24,144,255,0.28), transparent 60%);
		animation: floaty 8.2s ease-in-out infinite;
	}

	/* 让页面内容在光斑之上 */
	.navbar,
	.service-menu-mask,
	.service-menu-popup,
	.intro-section,
	.health-card,
	.health-archive-card,
	.task-section {
		position: relative;
		z-index: 1;
	}

	/* 顶部导航栏 */
	.navbar {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 24rpx 32rpx;
		background: rgba(255, 255, 255, 0.72);
		backdrop-filter: blur(14rpx);
		position: sticky;
		top: 0;
		z-index: 100;
		box-shadow: 0 10rpx 26rpx rgba(2, 6, 23, 0.08);
		border-bottom: 1rpx solid rgba(255, 255, 255, 0.65);
	}

	.navbar-left {
		width: 48rpx;
		height: 48rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.navbar-center {
		flex: 1;
		text-align: center;
	}

	.navbar-title {
		font-size: 36rpx;
		font-weight: 600;
		color: #0f172a;
		letter-spacing: 2rpx;
	}

	.navbar-right {
		width: 48rpx;
		height: 48rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	/* 左上角服务列表弹窗 */
	.service-menu-mask {
		position: fixed;
		left: 0;
		right: 0;
		top: 0;
		bottom: 0;
		background: rgba(0, 0, 0, 0.4);
		z-index: 998;
	}
	.service-menu-popup {
		position: fixed;
		left: 24rpx;
		top: 120rpx;
		width: 360rpx;
		max-height: 70vh;
		background: #fff;
		border-radius: 16rpx;
		box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.15);
		z-index: 999;
		overflow: hidden;
	}
	.service-menu-title {
		padding: 24rpx 24rpx 16rpx;
		font-size: 28rpx;
		font-weight: 600;
		color: #333;
		border-bottom: 1rpx solid #eee;
	}
	.service-menu-list {
		max-height: 60vh;
	}
	.service-menu-item {
		display: flex;
		align-items: center;
		padding: 24rpx 24rpx;
		border-bottom: 1rpx solid #f5f5f5;
	}
	.service-menu-item:active {
		background: #f5f5f5;
	}
	.service-menu-icon-wrap {
		width: 48rpx;
		height: 48rpx;
		border-radius: 50%;
		background: #e8f8f0;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-right: 20rpx;
	}
	.service-menu-name {
		flex: 1;
		font-size: 28rpx;
		color: #333;
	}

	/* 介绍区域 */
	.intro-section {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 48rpx 32rpx;
		background-color: #ffffff;
		margin: 0 24rpx 24rpx 24rpx;
		border-radius: 24rpx;
		box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
	}

	.intro-content {
		flex: 1;
	}

	.intro-title {
		display: flex;
		align-items: center;
		gap: 12rpx;
		margin-bottom: 16rpx;
	}

	.title-text {
		font-size: 48rpx;
		font-weight: 700;
		color: #07C160;
		letter-spacing: 2rpx;
	}

	.sparkle {
		font-size: 32rpx;
	}

	.subtitle-text {
		font-size: 28rpx;
		color: #666666;
		line-height: 1.5;
	}

	.intro-image {
		width: 160rpx;
		height: 120rpx;
		margin-left: 24rpx;
		flex-shrink: 0;
	}

	.doctor-placeholder {
		width: 100%;
		height: 100%;
		background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
		border-radius: 16rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 12rpx;
		box-sizing: border-box;
	}

	.intro-logo-img {
		width: 100%;
		height: 100%;
	}

	/* 我的健康卡片 */
	.health-card {
		background-color: #f8f8f8;
		border-radius: 24rpx;
		padding: 32rpx;
		margin: 0 24rpx 24rpx 24rpx;
		box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
	}

	.health-card-header {
		display: flex;
		align-items: center;
		margin-bottom: 32rpx;
	}

	.green-line {
		width: 6rpx;
		height: 32rpx;
		background: linear-gradient(180deg, #07C160 0%, #06A050 100%);
		border-radius: 3rpx;
		margin-right: 16rpx;
	}

	.health-card-title {
		font-size: 36rpx;
		font-weight: 600;
		color: #333333;
	}

	/* 健康数据网格布局 - 压缩为两行，每行5个 */
	.health-data-grid {
		display: grid;
		grid-template-columns: repeat(5, 1fr);
		gap: 20rpx 12rpx;
	}

	.data-item {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
	}

	.data-icon-wrapper {
			width: 80rpx;
			height: 80rpx;
			background-color: #ffffff;
			border-radius: 50%;
			display: flex;
			align-items: center;
			justify-content: center;
			margin-bottom: 12rpx;
			box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.1);
			transition: transform 0.2s;
		}

		.data-item:hover .data-icon-wrapper {
			transform: scale(1.1);
		}

		.data-item:active .data-icon-wrapper {
			transform: scale(0.95);
		}

	.data-text {
		font-size: 22rpx;
		color: #666666;
		text-align: center;
		line-height: 1.4;
		word-break: keep-all;
		white-space: nowrap;
	}

	/* 健康档案模块 */
	.health-archive-card {
		background-color: #f8f8f8;
		border-radius: 24rpx;
		padding: 32rpx;
		margin: 0 24rpx 24rpx 24rpx;
		box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
	}

	.health-archive-header {
		display: flex;
		align-items: center;
		margin-bottom: 24rpx;
	}

	.health-archive-title {
		font-size: 36rpx;
		font-weight: 600;
		color: #333333;
	}

	.health-archive-button-wrapper {
		display: flex;
		justify-content: center;
	}

	.health-archive-button {
		display: flex;
		align-items: center;
		justify-content: space-between;
		width: 100%;
		padding: 32rpx 40rpx;
		background-color: #ffffff;
		border-radius: 16rpx;
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
		transition: all 0.3s;
	}

	.health-archive-button:active {
		transform: scale(0.98);
		box-shadow: 0 2rpx 4rpx rgba(0, 0, 0, 0.1);
	}

	.archive-icon-wrapper {
		width: 64rpx;
		height: 64rpx;
		background-color: #f0f9f4;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-right: 24rpx;
	}

	.archive-logo {
		width: 72rpx;
		height: 72rpx;
		background: linear-gradient(135deg, #e8f7ed 0%, #d4f0dc 100%);
		box-shadow: 0 4rpx 12rpx rgba(7, 193, 96, 0.15);
	}

	.archive-button-text {
		flex: 1;
		font-size: 32rpx;
		font-weight: 600;
		color: #333333;
	}

	/* 任务和推荐卡片 */
	.task-section {
		margin: 0 24rpx 32rpx 24rpx;
	}

	.task-card {
		display: flex;
		align-items: center;
		padding: 24rpx;
		background-color: #ffffff;
		border-radius: 16rpx;
		margin-bottom: 16rpx;
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
	}

	.blue-card {
		border-left: 4rpx solid #1890ff;
	}

	.purple-card {
		border-left: 4rpx solid #722ed1;
	}

	.task-icon-wrapper {
		width: 72rpx;
		height: 72rpx;
		border-radius: 16rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-right: 24rpx;
	}

	.blue-bg {
		background-color: #1890ff;
	}

	.purple-bg {
		background-color: #722ed1;
	}


	.task-content {
		flex: 1;
	}

	.task-text {
		font-size: 28rpx;
		color: #333333;
		line-height: 1.5;
	}

	/* 服务网格 - 单行6个 */
	.service-grid {
		display: grid;
		grid-template-columns: repeat(6, 1fr);
		gap: 12rpx;
		padding: 0 24rpx 32rpx 24rpx;
	}

	.service-item {
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 20rpx 8rpx;
		background-color: #ffffff;
		border-radius: 16rpx;
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
		transition: transform 0.2s;
	}

	.service-item:active {
		transform: scale(0.95);
	}

	.service-icon-wrapper {
		width: 72rpx;
		height: 72rpx;
		background: linear-gradient(135deg, #f5f5f5 0%, #ffffff 100%);
		border-radius: 16rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 12rpx;
	}

	.service-text {
		font-size: 22rpx;
		color: #333333;
		text-align: center;
		line-height: 1.4;
	}

</style>
