<template>
	<view :class="['container', rootFontClass]">
		<!-- 选择家人 -->
		<view class="section">
			<view class="section-title">选择家人</view>
			<view class="family-selector">
				<view 
					class="family-option" 
					v-for="item in familyList" 
					:key="item.memberId"
					:class="{ 'selected': selectedFamily && selectedFamily.memberId === item.memberId }"
					@tap="selectFamily(item)"
				>
					<text class="family-name">{{ item.fullName }}</text>
					<text class="family-phone">{{ item.phone }}</text>
					<uni-icons v-if="selectedFamily && selectedFamily.memberId === item.memberId" type="checkmarkempty" size="20" color="#667eea"></uni-icons>
				</view>
			</view>
		</view>

		<!-- 上传体检报告 -->
		<view class="section" v-if="selectedFamily">
			<view class="section-title">上传体检报告</view>
			<view class="upload-area">
				<uni-file-picker 
					v-model="fileList"
					file-mediatype="image"
					mode="grid"
					:limit="5"
					@select="onFileSelect"
				></uni-file-picker>
			</view>
		</view>

		<!-- 填写基本信息 -->
		<view class="section" v-if="selectedFamily">
			<view class="section-title">填写基本信息</view>
			<view class="form">
				<!-- 健康档案主表信息 -->
				<view class="form-group">
					<text class="group-title">健康档案主表</text>
					<view class="form-item">
						<text class="label">档案名称：</text>
						<input class="input" v-model="archiveForm.archiveName" placeholder="如：2025年度体检档案" />
					</view>
					<view class="form-item">
						<text class="label">姓名：</text>
						<input class="input" v-model="archiveForm.userName" :placeholder="selectedFamily.fullName" />
					</view>
				</view>

				<!-- 用户基本信息 -->
				<view class="form-group">
					<text class="group-title">用户基本信息</text>
					<view class="form-item">
						<text class="label">性别：</text>
						<picker mode="selector" :range="genderOptions" :value="genderIndex" @change="onGenderChange">
							<view class="picker">{{ archiveForm.gender || '请选择' }}</view>
						</picker>
					</view>
					<view class="form-item">
						<text class="label">出生日期：</text>
						<picker mode="date" :value="archiveForm.birthDate" @change="onDateChange">
							<view class="picker">{{ archiveForm.birthDate || '请选择' }}</view>
						</picker>
					</view>
					<view class="form-item">
						<text class="label">联系电话：</text>
						<input class="input" v-model="archiveForm.phone" :placeholder="selectedFamily.phone" />
					</view>
				</view>

				<!-- 紧急联系人 -->
				<view class="form-group">
					<text class="group-title">紧急联系人</text>
					<view class="form-item">
						<text class="label">联系人姓名：</text>
						<input class="input" v-model="archiveForm.emergencyName" placeholder="请输入紧急联系人姓名" />
					</view>
					<view class="form-item">
						<text class="label">联系人电话：</text>
						<input class="input" type="number" v-model="archiveForm.emergencyPhone" placeholder="请输入紧急联系人电话" maxlength="11" />
					</view>
					<view class="form-item">
						<text class="label">关系：</text>
						<input class="input" v-model="archiveForm.emergencyRelation" placeholder="如：配偶、子女等" />
					</view>
				</view>

				<!-- 健康服务凭证 -->
				<view class="form-group">
					<text class="group-title">健康服务凭证（可选）</text>
					<view class="form-item">
						<text class="label">凭证类型：</text>
						<input class="input" v-model="archiveForm.certificateType" placeholder="如：医保卡、健康卡等" />
					</view>
					<view class="form-item">
						<text class="label">凭证号码：</text>
						<input class="input" v-model="archiveForm.certificateNumber" placeholder="请输入凭证号码" />
					</view>
				</view>
			</view>
		</view>

		<!-- 提交按钮 -->
		<view class="submit-btn" v-if="selectedFamily" @tap="submitArchive">
			<text class="submit-text">生成健康档案</text>
		</view>

		<!-- 加载提示 -->
		<uni-load-more v-if="loading" status="loading"></uni-load-more>
	</view>
</template>

<script>
	import config from '@/utils/config.js'
	
	export default {
		data() {
			return {
				familyList: [],
				selectedFamily: null,
				fileList: [],
				archiveForm: {
					archiveName: '',
					userName: '',
					gender: '',
					birthDate: '',
					phone: '',
					emergencyName: '',
					emergencyPhone: '',
					emergencyRelation: '',
					certificateType: '',
					certificateNumber: ''
				},
				genderOptions: ['男', '女'],
				genderIndex: 0,
				loading: false
			}
		},
		onLoad() {
			this.loadFamilyList()
		},
		methods: {
			// 加载家人列表
			loadFamilyList() {
				const userInfo = uni.getStorageSync('userInfo')
				if (!userInfo || !userInfo.userId) {
					uni.showToast({
						title: '请先登录',
						icon: 'none'
					})
					setTimeout(() => {
						uni.navigateBack()
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
							if (this.familyList.length === 0) {
								uni.showToast({
									title: '请先添加家人',
									icon: 'none'
								})
								setTimeout(() => {
									uni.navigateBack()
								}, 1500)
							}
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

			// 选择家人
			selectFamily(family) {
				this.selectedFamily = family
				// 初始化表单数据
				this.archiveForm.userName = family.fullName
				this.archiveForm.phone = family.phone
				// 设置默认档案名称
				const year = new Date().getFullYear()
				this.archiveForm.archiveName = `${year}年度体检档案`
				// 清空其他字段
				this.archiveForm.emergencyName = ''
				this.archiveForm.emergencyPhone = ''
				this.archiveForm.emergencyRelation = ''
				this.archiveForm.certificateType = ''
				this.archiveForm.certificateNumber = ''
			},

			// 文件选择
			onFileSelect(e) {
				this.fileList = e.tempFiles || []
			},

			// 性别选择
			onGenderChange(e) {
				this.genderIndex = e.detail.value
				this.archiveForm.gender = this.genderOptions[e.detail.value]
			},

			// 日期选择
			onDateChange(e) {
				this.archiveForm.birthDate = e.detail.value
			},

			// 提交健康档案
			submitArchive() {
				if (!this.selectedFamily) {
					uni.showToast({
						title: '请选择家人',
						icon: 'none'
					})
					return
				}

				if (!this.archiveForm.archiveName) {
					uni.showToast({
						title: '请输入档案名称',
						icon: 'none'
					})
					return
				}

				if (!this.archiveForm.userName) {
					uni.showToast({
						title: '请输入姓名',
						icon: 'none'
					})
					return
				}

				this.loading = true

				const userInfo = uni.getStorageSync('userInfo')
				if (!userInfo || !userInfo.userId) {
					uni.showToast({
						title: '请先登录',
						icon: 'none'
					})
					this.loading = false
					return
				}

				// 先上传文件（如果有）
				const uploadPromises = []
				if (this.fileList.length > 0) {
					this.fileList.forEach(file => {
						uploadPromises.push(this.uploadFile(file))
					})
				}

				// 等待所有文件上传完成
				Promise.all(uploadPromises).then(rawDataIds => {
					// 构建健康档案请求数据
					const requestData = {
						healthArchive: {
							userName: this.archiveForm.userName,
							archiveName: this.archiveForm.archiveName,
							archiveDate: this.archiveForm.birthDate || new Date().toISOString().split('T')[0],
							archiveYear: new Date().getFullYear()
						},
						userInfo: {
							fullName: this.archiveForm.userName,
							gender: this.archiveForm.gender === '男' ? true : (this.archiveForm.gender === '女' ? false : null),
							birthDate: this.archiveForm.birthDate,
							personalPhone: this.archiveForm.phone || this.selectedFamily.phone
						}
					}

					// 添加紧急联系人（如果有填写）
					if (this.archiveForm.emergencyName || this.archiveForm.emergencyPhone) {
						requestData.userEmergencyContacts = {
							contactName: this.archiveForm.emergencyName,
							contactPhone: this.archiveForm.emergencyPhone,
							relation: this.archiveForm.emergencyRelation || '家人'
						}
					}

					// 添加健康服务凭证（如果有填写）
					if (this.archiveForm.certificateType || this.archiveForm.certificateNumber) {
						requestData.userCertificates = {
							certificateType: this.archiveForm.certificateType,
							certificateNumber: this.archiveForm.certificateNumber
						}
					}

					// 提交健康档案
					uni.request({
						url: `${config.baseUrl}/health-archive-process/process?familyMemberId=${this.selectedFamily.memberId}`,
						method: 'POST',
						data: requestData,
						header: {
							'userId': userInfo.userId,
							'Content-Type': 'application/json'
						},
						success: (res) => {
							this.loading = false
							if (res.statusCode === 200 && res.data.code === 200) {
								uni.showToast({
									title: '创建成功',
									icon: 'success'
								})
								setTimeout(() => {
									uni.navigateBack()
								}, 1500)
							} else {
								uni.showToast({
									title: res.data.message || '创建失败',
									icon: 'none'
								})
							}
						},
						fail: (err) => {
							this.loading = false
							console.error('创建健康档案失败', err)
							uni.showToast({
								title: '网络错误',
								icon: 'none'
							})
						}
					})
				}).catch(err => {
					this.loading = false
					console.error('上传文件失败', err)
					uni.showToast({
						title: '上传文件失败',
						icon: 'none'
					})
				})
			},

			// 上传文件
			uploadFile(file) {
				return new Promise((resolve, reject) => {
					const userInfo = uni.getStorageSync('userInfo')
					const familyUserId = this.selectedFamily.registeredUserId

					uni.uploadFile({
						url: `${config.baseUrl}/raw-health-data/upload`,
						filePath: file.path,
						name: 'file',
						formData: {
							userId: familyUserId.toString(), // 使用家人的userId
							dataType: 'REPORT'
						},
						header: {
							'userId': userInfo.userId
						},
						success: (res) => {
							try {
								const data = JSON.parse(res.data)
								if (data.code === 200) {
									resolve(data.data.id)
								} else {
									reject(new Error(data.message))
								}
							} catch (e) {
								reject(e)
							}
						},
						fail: reject
					})
				})
			}
		}
	}
</script>

<style scoped>
	.container {
		min-height: 100vh;
		background-color: #f5f5f5;
		padding: 20rpx;
		padding-bottom: 120rpx;
	}

	.section {
		background-color: #ffffff;
		border-radius: 16rpx;
		padding: 30rpx;
		margin-bottom: 20rpx;
	}

	.section-title {
		font-size: 32rpx;
		font-weight: bold;
		color: #333333;
		margin-bottom: 20rpx;
	}

	.family-selector {
		display: flex;
		flex-direction: column;
	}

	.family-option {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 20rpx;
		border: 2rpx solid #e0e0e0;
		border-radius: 12rpx;
		margin-bottom: 15rpx;
		background-color: #fafafa;
	}

	.family-option.selected {
		border-color: #667eea;
		background-color: #f0f4ff;
	}

	.family-name {
		font-size: 30rpx;
		color: #333333;
		margin-right: 20rpx;
	}

	.family-phone {
		font-size: 26rpx;
		color: #666666;
		flex: 1;
	}

	.upload-area {
		margin-top: 20rpx;
	}

	.form {
		margin-top: 20rpx;
	}

	.form-item {
		display: flex;
		align-items: center;
		margin-bottom: 30rpx;
	}

	.label {
		width: 160rpx;
		font-size: 28rpx;
		color: #333333;
	}

	.input,
	.picker {
		flex: 1;
		height: 60rpx;
		padding: 0 20rpx;
		border: 1rpx solid #e0e0e0;
		border-radius: 8rpx;
		font-size: 28rpx;
		line-height: 60rpx;
	}

	.picker {
		color: #333333;
	}

	.form-group {
		margin-bottom: 30rpx;
		padding-bottom: 20rpx;
		border-bottom: 1rpx solid #f0f0f0;
	}

	.form-group:last-child {
		border-bottom: none;
	}

	.group-title {
		font-size: 28rpx;
		font-weight: bold;
		color: #667eea;
		margin-bottom: 20rpx;
		display: block;
	}

	.submit-btn {
		position: fixed;
		bottom: 30rpx;
		left: 30rpx;
		right: 30rpx;
		background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
		color: #ffffff;
		border-radius: 50rpx;
		padding: 24rpx;
		text-align: center;
		font-size: 32rpx;
		font-weight: bold;
		box-shadow: 0 4rpx 20rpx rgba(102, 126, 234, 0.4);
	}

	.submit-text {
		color: #ffffff;
	}
</style>