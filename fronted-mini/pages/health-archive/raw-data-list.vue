<template>
	<view class="page-root">
	<view :class="['container', rootFontClass]">
		<!-- 顶部标题栏 -->
		<view class="header">
			<view class="header-content">
				<uni-icons type="left" size="24" color="#333333" @tap="goBack"></uni-icons>
				<text class="header-title">健康报告</text>
				<view style="width: 24px;"></view>
			</view>
		</view>

		<!-- 数据列表 -->
		<view class="data-section">
			<!-- 空状态 -->
			<view v-if="dataList.length === 0 && !loading" class="empty-state">
				<uni-icons type="folder" size="120" color="#cccccc"></uni-icons>
				<text class="empty-text">暂无健康数据</text>
				<text class="empty-hint">请先上传健康数据</text>
			</view>

			<!-- 数据列表 -->
			<view v-else class="data-list">
				<view 
					class="data-item" 
					v-for="(item, index) in dataList" 
					:key="item.id"
					@tap="onItemClick(item)"
				>
					<view class="data-content">
						<view class="data-header">
							<text class="data-owner">{{ item.ownerName || '未知' }}</text>
							<text class="data-type">{{ getDataTypeName(item.dataType) }}</text>
						</view>
						<view class="data-info">
							<text class="data-file-name" v-if="item.fileName">{{ item.fileName }}</text>
							<text class="data-time" v-if="item.uploadTime">{{ formatTime(item.uploadTime) }}</text>
						</view>
						<view class="data-footer">
							<text class="data-format">{{ getFormatTypeName(item.formatType) }}</text>
							<text class="data-size" v-if="item.fileSize">{{ formatFileSize(item.fileSize) }}</text>
						</view>
					</view>
					<view class="data-action">
						<uni-icons type="download" size="20" color="#07C160"></uni-icons>
					</view>
				</view>
			</view>

			<!-- 加载中 -->
			<view v-if="loading" class="loading-state">
				<uni-icons type="spinner-cycle" size="40" color="#07C160"></uni-icons>
				<text class="loading-text">加载中...</text>
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
				dataList: [],
				loading: false
			}
		},
		onLoad() {
			this.loadDataList()
		},
		onShow() {
			// 每次显示页面时刷新列表
			this.loadDataList()
		},
		methods: {
			/**
			 * 加载原始数据列表
			 */
			loadDataList() {
				const loginStatus = uni.getStorageSync('isLogin')
				if (!loginStatus) {
					this.dataList = []
					return
				}

				const userInfo = uni.getStorageSync('userInfo')
				if (!userInfo || !userInfo.userId) {
					this.dataList = []
					return
				}

				const token = uni.getStorageSync('token')
				const userId = userInfo.userId

				this.loading = true

				uni.request({
					url: `${config.baseUrl}/raw-health-data/list/user/${userId}`,
					method: 'GET',
					header: {
						'userId': userId,
						'token': token || '',
						'Content-Type': 'application/json'
					},
					success: (res) => {
						this.loading = false
						if (res.statusCode === 200 && res.data && res.data.code === 200) {
							this.dataList = res.data.data || []
						} else {
							this.dataList = []
							uni.showToast({
								title: res.data?.message || '查询失败',
								icon: 'none'
							})
						}
					},
					fail: (err) => {
						this.loading = false
						console.error('查询原始数据列表失败', err)
						this.dataList = []
						uni.showToast({
							title: '网络错误，请重试',
							icon: 'none'
						})
					}
				})
			},

			/**
			 * 点击数据项，下载文件
			 */
			onItemClick(item) {
				const ownerName = item.ownerName || ''
				const downloadUrl = `${config.baseUrl}/raw-health-data/download/${item.id}?ownerName=${encodeURIComponent(ownerName)}`
				
				// 显示下载提示
				uni.showLoading({
					title: '正在下载...',
					mask: true
				})

				// 下载文件
				uni.downloadFile({
					url: downloadUrl,
					header: {
						'userId': uni.getStorageSync('userInfo')?.userId || '',
						'token': uni.getStorageSync('token') || ''
					},
					success: (res) => {
						if (res.statusCode === 200) {
							// 打开文件（文件名已在后端设置好）
							uni.openDocument({
								filePath: res.tempFilePath,
								showMenu: true,
								success: () => {
									uni.hideLoading()
									uni.showToast({
										title: '打开成功',
										icon: 'success'
									})
								},
								fail: (err) => {
									uni.hideLoading()
									console.error('打开文件失败', err)
									// 如果打开失败，尝试保存文件
									this.saveFile(res.tempFilePath, item)
								}
							})
						} else {
							uni.hideLoading()
							uni.showToast({
								title: '下载失败',
								icon: 'none'
							})
						}
					},
					fail: (err) => {
						uni.hideLoading()
						console.error('下载文件失败', err)
						uni.showToast({
							title: '下载失败，请重试',
							icon: 'none'
						})
					}
				})
			},

			/**
			 * 保存文件到本地
			 */
			saveFile(tempFilePath, item) {
				const ownerName = item.ownerName || ''
				const fileExtension = this.getFileExtension(item.formatType, item.fileName)
				const fileName = (ownerName || '健康数据') + '健康数据' + fileExtension
				
				// #ifdef APP-PLUS
				// APP端保存文件
				plus.io.resolveLocalFileSystemURL(tempFilePath, (entry) => {
					plus.io.resolveLocalFileSystemURL('_downloads/', (dirEntry) => {
						entry.copyTo(dirEntry, fileName, () => {
							uni.hideLoading()
							uni.showToast({
								title: '保存成功',
								icon: 'success'
							})
						}, (err) => {
							uni.hideLoading()
							console.error('保存文件失败', err)
							uni.showToast({
								title: '保存失败',
								icon: 'none'
							})
						})
					})
				})
				// #endif
				
				// #ifndef APP-PLUS
				// 非APP端提示用户
				uni.hideLoading()
				uni.showToast({
					title: '请使用浏览器下载',
					icon: 'none'
				})
				// #endif
			},

			/**
			 * 获取文件扩展名
			 */
			getFileExtension(formatType, fileName) {
				if (formatType === 'PDF') {
					return '.pdf'
				} else if (formatType === 'IMAGE') {
					if (fileName) {
						const lowerFileName = fileName.toLowerCase()
						if (lowerFileName.endsWith('.jpg') || lowerFileName.endsWith('.jpeg')) {
							return '.jpg'
						} else if (lowerFileName.endsWith('.png')) {
							return '.png'
						} else if (lowerFileName.endsWith('.gif')) {
							return '.gif'
						}
					}
					return '.jpg'
				} else if (formatType === 'JSON') {
					return '.json'
				} else if (formatType === 'CSV') {
					return '.csv'
				} else if (formatType === 'TEXT') {
					return '.txt'
				}
				return ''
			},

			/**
			 * 获取数据类型名称
			 */
			getDataTypeName(dataType) {
				const typeMap = {
					'REPORT': '体检报告',
					'MEDICAL_RECORD': '就诊记录',
					'SKIN': '皮肤照片',
					'LAB': '检查检验',
					'EMOTION': '情绪检测',
					'GENETIC': '基因检测',
					'SLEEP': '睡眠监测',
					'MEAL': '拍三餐',
					'EXERCISE': '运动数据'
				}
				return typeMap[dataType] || dataType || '未知类型'
			},

			/**
			 * 获取格式类型名称
			 */
			getFormatTypeName(formatType) {
				const formatMap = {
					'IMAGE': '图片',
					'PDF': 'PDF',
					'JSON': 'JSON',
					'CSV': 'CSV',
					'TEXT': '文本'
				}
				return formatMap[formatType] || formatType || '未知格式'
			},

			/**
			 * 格式化文件大小
			 */
			formatFileSize(size) {
				if (!size) return ''
				if (size < 1024) {
					return size + 'B'
				} else if (size < 1024 * 1024) {
					return (size / 1024).toFixed(2) + 'KB'
				} else {
					return (size / (1024 * 1024)).toFixed(2) + 'MB'
				}
			},

			/**
			 * 格式化时间
			 */
			formatTime(timeStr) {
				if (!timeStr) return ''
				try {
					const date = new Date(timeStr)
					const year = date.getFullYear()
					const month = String(date.getMonth() + 1).padStart(2, '0')
					const day = String(date.getDate()).padStart(2, '0')
					const hours = String(date.getHours()).padStart(2, '0')
					const minutes = String(date.getMinutes()).padStart(2, '0')
					return `${year}-${month}-${day} ${hours}:${minutes}`
				} catch (e) {
					return timeStr
				}
			},

			/**
			 * 返回上一页
			 */
			goBack() {
				uni.navigateBack()
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page-root { min-height: 100vh; background: transparent; }
	.container {
		min-height: 100vh;
		background: transparent;
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
	.header, .data-section { position: relative; z-index: 1; }

	/* 顶部标题栏 */
	.header {
		background-color: #ffffff;
		padding: 20rpx 0;
		border-bottom: 1rpx solid #eeeeee;
		position: sticky;
		top: 0;
		z-index: 100;
	}

	.header-content {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 0 32rpx;
	}

	.header-title {
		font-size: 36rpx;
		font-weight: 600;
		color: #333333;
	}

	/* 数据列表区域 */
	.data-section {
		padding: 24rpx;
	}

	/* 空状态 */
	.empty-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 120rpx 0;
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

	/* 数据列表 */
	.data-list {
		display: flex;
		flex-direction: column;
		gap: 20rpx;
	}

	.data-item {
		background-color: #ffffff;
		border-radius: 16rpx;
		padding: 32rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
	}

	.data-content {
		flex: 1;
	}

	.data-header {
		display: flex;
		align-items: center;
		gap: 16rpx;
		margin-bottom: 16rpx;
	}

	.data-owner {
		font-size: 32rpx;
		font-weight: 600;
		color: #07C160;
	}

	.data-type {
		font-size: 24rpx;
		color: #666666;
		background-color: #f0f0f0;
		padding: 4rpx 12rpx;
		border-radius: 8rpx;
	}

	.data-info {
		display: flex;
		flex-direction: column;
		gap: 8rpx;
		margin-bottom: 16rpx;
	}

	.data-file-name {
		font-size: 28rpx;
		color: #333333;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.data-time {
		font-size: 24rpx;
		color: #999999;
	}

	.data-footer {
		display: flex;
		align-items: center;
		gap: 16rpx;
	}

	.data-format {
		font-size: 24rpx;
		color: #666666;
		background-color: #f0f0f0;
		padding: 4rpx 12rpx;
		border-radius: 8rpx;
	}

	.data-size {
		font-size: 24rpx;
		color: #999999;
	}

	.data-action {
		margin-left: 24rpx;
	}

	/* 加载中 */
	.loading-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 80rpx 0;
	}

	.loading-text {
		font-size: 28rpx;
		color: #999999;
		margin-top: 24rpx;
	}
</style>
