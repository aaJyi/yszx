<template>
	<uni-popup ref="popup" type="center" :mask-click="false">
		<view class="basic-info-container">
			<view class="header">
				<text class="title">填写基本信息</text>
				<text class="subtitle">请填写您的基本信息，用于创建健康档案</text>
			</view>
			
			<scroll-view scroll-y class="form-content">
				<!-- 基本信息 -->
				<view class="form-section">
					<view class="section-title">基本信息</view>
					
					<view class="form-item">
						<text class="label">姓名 <text class="required">*</text></text>
						<input 
							class="input" 
							v-model="formData.fullName" 
							placeholder="请输入姓名"
							maxlength="50"
						/>
					</view>
					
					<view class="form-item">
						<text class="label">性别 <text class="required">*</text></text>
						<picker 
							mode="selector" 
							:range="genderOptions" 
							:value="genderIndex"
							@change="onGenderChange"
						>
							<view class="picker-view">
								<text :class="['picker-text', !formData.gender && 'placeholder']">
									{{ formData.gender || '请选择性别' }}
								</text>
								<uni-icons type="arrowdown" size="16" color="#999"></uni-icons>
							</view>
						</picker>
					</view>
					
					<view class="form-item">
						<text class="label">出生日期 <text class="required">*</text></text>
						<picker 
							mode="date" 
							:value="formData.birthDate" 
							:start="startDate"
							:end="endDate"
							@change="onBirthDateChange"
						>
							<view class="picker-view">
								<text :class="['picker-text', !formData.birthDate && 'placeholder']">
									{{ formData.birthDate || '请选择出生日期' }}
								</text>
								<uni-icons type="arrowdown" size="16" color="#999"></uni-icons>
							</view>
						</picker>
					</view>
					
					<view class="form-item">
						<text class="label">民族</text>
						<input 
							class="input" 
							v-model="formData.ethnicity" 
							placeholder="请输入民族（可选）"
							maxlength="20"
						/>
					</view>
					
					<view class="form-item">
						<text class="label">本人电话</text>
						<input 
							class="input" 
							v-model="formData.personalPhone" 
							type="number"
							placeholder="请输入本人电话（可选）"
							maxlength="20"
						/>
					</view>
				</view>
				
				<!-- 健康标识 -->
				<view class="form-section">
					<view class="section-title">健康标识</view>
					
					<view class="form-item">
						<text class="label">血型</text>
						<picker 
							mode="selector" 
							:range="bloodTypeOptions" 
							:value="bloodTypeIndex"
							@change="onBloodTypeChange"
						>
							<view class="picker-view">
								<text :class="['picker-text', !formData.bloodType && 'placeholder']">
									{{ formData.bloodType || '请选择血型（可选）' }}
								</text>
								<uni-icons type="arrowdown" size="16" color="#999"></uni-icons>
							</view>
						</picker>
					</view>
					
					<view class="form-item">
						<text class="label">体重状况</text>
						<picker 
							mode="selector" 
							:range="weightStatusOptions" 
							:value="weightStatusIndex"
							@change="onWeightStatusChange"
						>
							<view class="picker-view">
								<text :class="['picker-text', !formData.weightStatus && 'placeholder']">
									{{ formData.weightStatus || '请选择体重状况（可选）' }}
								</text>
								<uni-icons type="arrowdown" size="16" color="#999"></uni-icons>
							</view>
						</picker>
					</view>
					
					<view class="form-item checkbox-group">
						<text class="label">特殊人群</text>
						<view class="checkbox-item">
							<checkbox-group @change="onSpecialGroupChange">
								<label class="checkbox-label">
									<checkbox value="child" :checked="formData.isChild06" />
									<text>0-6岁儿童</text>
								</label>
								<label class="checkbox-label">
									<checkbox value="elderly" :checked="formData.isElderly65" />
									<text>65岁以上</text>
								</label>
								<label class="checkbox-label">
									<checkbox value="pregnant" :checked="formData.isPregnant" />
									<text>孕产妇</text>
								</label>
							</checkbox-group>
						</view>
					</view>
					
					<view class="form-item" v-if="formData.isPregnant">
						<text class="label">孕产妇风险等级</text>
						<picker 
							mode="selector" 
							:range="pregnancyRiskOptions" 
							:value="pregnancyRiskIndex"
							@change="onPregnancyRiskChange"
						>
							<view class="picker-view">
								<text :class="['picker-text', !formData.pregnancyRisk && 'placeholder']">
									{{ formData.pregnancyRisk || '请选择风险等级（可选）' }}
								</text>
								<uni-icons type="arrowdown" size="16" color="#999"></uni-icons>
							</view>
						</picker>
					</view>
				</view>
			</scroll-view>
			
			<view class="footer">
				<button class="btn cancel-btn" @tap="onCancel">稍后填写</button>
				<button class="btn submit-btn" @tap="onSubmit" :disabled="!canSubmit">保存</button>
			</view>
		</view>
	</uni-popup>
</template>

<script>
	import config from '@/utils/config.js'
	
	export default {
		name: 'BasicInfoForm',
		props: {
			userId: {
				type: Number,
				required: true
			}
		},
		data() {
			return {
				formData: {
					// 基本信息
					fullName: '',
					gender: '',
					birthDate: '',
					ethnicity: '',
					personalPhone: '',
					
					// 健康标识
					bloodType: '',
					weightStatus: '',
					isChild06: false,
					isElderly65: false,
					isPregnant: false,
					pregnancyRisk: ''
				},
				genderOptions: ['男', '女', '未说明的性别', '未知的性别'],
				bloodTypeOptions: ['A', 'B', 'O', 'AB', '不详'],
				weightStatusOptions: ['低', '正常', '超重', '肥胖'],
				pregnancyRiskOptions: ['低风险', '一般风险', '较高风险', '高风险'],
				startDate: '1900-01-01',
				endDate: ''
			}
		},
		computed: {
			genderIndex() {
				return this.genderOptions.indexOf(this.formData.gender);
			},
			bloodTypeIndex() {
				return this.bloodTypeOptions.indexOf(this.formData.bloodType);
			},
			weightStatusIndex() {
				return this.weightStatusOptions.indexOf(this.formData.weightStatus);
			},
			pregnancyRiskIndex() {
				return this.pregnancyRiskOptions.indexOf(this.formData.pregnancyRisk);
			},
			canSubmit() {
				return this.formData.fullName && 
					   this.formData.gender && 
					   this.formData.birthDate;
			}
		},
		mounted() {
			// 设置最大日期为今天
			const today = new Date();
			this.endDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
		},
		methods: {
			open() {
				this.$refs.popup.open();
			},
			close() {
				this.$refs.popup.close();
			},
			onGenderChange(e) {
				this.formData.gender = this.genderOptions[e.detail.value];
			},
			onBirthDateChange(e) {
				this.formData.birthDate = e.detail.value;
			},
			onBloodTypeChange(e) {
				this.formData.bloodType = this.bloodTypeOptions[e.detail.value];
			},
			onWeightStatusChange(e) {
				this.formData.weightStatus = this.weightStatusOptions[e.detail.value];
			},
			onPregnancyRiskChange(e) {
				this.formData.pregnancyRisk = this.pregnancyRiskOptions[e.detail.value];
			},
			onSpecialGroupChange(e) {
				const values = e.detail.value;
				this.formData.isChild06 = values.includes('child');
				this.formData.isElderly65 = values.includes('elderly');
				this.formData.isPregnant = values.includes('pregnant');
				
				// 如果不是孕产妇，清空风险等级
				if (!this.formData.isPregnant) {
					this.formData.pregnancyRisk = '';
				}
			},
			onCancel() {
				this.close();
				this.$emit('cancel');
			},
			onSubmit() {
				if (!this.canSubmit) {
					uni.showToast({
						title: '请填写必填项',
						icon: 'none'
					});
					return;
				}
				
				uni.showLoading({
					title: '保存中...',
					mask: true
				});
				
				// 构建请求数据
				const requestData = {
					healthArchive: {
						userId: this.userId,
						userName: this.formData.fullName,
						archiveDate: this.formData.birthDate, // 使用出生日期作为档案日期
						archiveYear: new Date(this.formData.birthDate).getFullYear()
					},
					userInfo: {
						fullName: this.formData.fullName,
						gender: this.formData.gender,
						birthDate: this.formData.birthDate,
						ethnicity: this.formData.ethnicity || null,
						personalPhone: this.formData.personalPhone || null
					},
					healthProfileTags: {
						isChild06: this.formData.isChild06 || false,
						isElderly65: this.formData.isElderly65 || false,
						isPregnant: this.formData.isPregnant || false,
						pregnancyRisk: this.formData.pregnancyRisk || null,
						bloodType: this.formData.bloodType || null,
						weightStatus: this.formData.weightStatus || null
					}
				};
				
				uni.request({
					url: `${config.baseUrl}/health-archive-process/basic-info`,
					method: 'POST',
					header: {
						'userId': this.userId.toString(),
						'Content-Type': 'application/json'
					},
					data: requestData,
					success: (res) => {
						uni.hideLoading();
						if (res.statusCode === 200 && res.data && res.data.code === 200) {
							uni.showToast({
								title: '保存成功',
								icon: 'success'
							});
							this.close();
							this.$emit('success', res.data.data);
						} else {
							uni.showToast({
								title: res.data?.message || '保存失败',
								icon: 'none'
							});
						}
					},
					fail: (err) => {
						uni.hideLoading();
						console.error('保存基本信息失败:', err);
						uni.showToast({
							title: '网络错误，请稍后重试',
							icon: 'none'
						});
					}
				});
			}
		}
	}
</script>

<style lang="scss" scoped>
	.basic-info-container {
		width: 90vw;
		max-width: 600rpx;
		max-height: 80vh;
		background-color: #ffffff;
		border-radius: 24rpx;
		display: flex;
		flex-direction: column;
		overflow: hidden;
	}
	
	.header {
		padding: 48rpx 32rpx 32rpx;
		border-bottom: 1rpx solid #f0f0f0;
	}
	
	.title {
		font-size: 36rpx;
		font-weight: 600;
		color: #333333;
		display: block;
		margin-bottom: 16rpx;
	}
	
	.subtitle {
		font-size: 26rpx;
		color: #999999;
		display: block;
	}
	
	.form-content {
		flex: 1;
		padding: 32rpx;
		max-height: 50vh;
	}
	
	.form-section {
		margin-bottom: 48rpx;
		
		&:last-child {
			margin-bottom: 0;
		}
	}
	
	.section-title {
		font-size: 32rpx;
		font-weight: 500;
		color: #333333;
		margin-bottom: 32rpx;
		padding-bottom: 16rpx;
		border-bottom: 2rpx solid #07C160;
	}
	
	.form-item {
		margin-bottom: 32rpx;
		
		&:last-child {
			margin-bottom: 0;
		}
	}
	
	.label {
		font-size: 28rpx;
		color: #333333;
		display: block;
		margin-bottom: 16rpx;
	}
	
	.required {
		color: #ff4d4f;
		margin-left: 4rpx;
	}
	
	.input {
		width: 100%;
		height: 88rpx;
		background-color: #f5f5f5;
		border-radius: 16rpx;
		padding: 0 24rpx;
		font-size: 28rpx;
		color: #333333;
		box-sizing: border-box;
	}
	
	.picker-view {
		width: 100%;
		height: 88rpx;
		background-color: #f5f5f5;
		border-radius: 16rpx;
		padding: 0 24rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
		box-sizing: border-box;
	}
	
	.picker-text {
		font-size: 28rpx;
		color: #333333;
		
		&.placeholder {
			color: #999999;
		}
	}
	
	.checkbox-group {
		.checkbox-item {
			margin-top: 16rpx;
		}
		
		.checkbox-label {
			display: flex;
			align-items: center;
			margin-bottom: 24rpx;
			font-size: 28rpx;
			color: #333333;
			
			&:last-child {
				margin-bottom: 0;
			}
		}
	}
	
	.footer {
		padding: 32rpx;
		border-top: 1rpx solid #f0f0f0;
		display: flex;
		gap: 24rpx;
	}
	
	.btn {
		flex: 1;
		height: 88rpx;
		border-radius: 16rpx;
		font-size: 32rpx;
		border: none;
		display: flex;
		align-items: center;
		justify-content: center;
	}
	
	.cancel-btn {
		background-color: #f5f5f5;
		color: #666666;
	}
	
	.submit-btn {
		background: linear-gradient(135deg, #07C160 0%, #06A050 100%);
		color: #ffffff;
	}
	
	.submit-btn[disabled] {
		background: #cccccc;
		color: #999999;
	}
</style>
