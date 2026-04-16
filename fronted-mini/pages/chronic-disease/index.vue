<template>
	<view :class="['container', rootFontClass]">
		<!-- 页面标题 -->
		<view class="page-header">
			<text class="page-title">我的慢病</text>
		</view>

		<!-- 慢病选择区域 -->
		<view class="disease-selector">
			<view class="loading-container" v-if="loading">
				<text class="loading-text">加载中...</text>
			</view>
			<view v-else class="disease-list">
				<view 
					class="disease-item" 
					v-for="disease in availableDiseases" 
					:key="disease.type"
					:class="{ active: selectedDisease === disease.type }" 
					@tap="selectDisease(disease.type)"
				>
					<view class="disease-icon" :class="disease.type + '-icon'">
						<text class="icon-text">{{disease.icon}}</text>
					</view>
					<text class="disease-name">{{disease.name}}</text>
				</view>
				<view class="empty-disease" v-if="availableDiseases.length === 0">
					<text class="empty-text">暂无慢病记录</text>
					<text class="empty-hint">请先创建健康档案并填写慢病信息</text>
				</view>
			</view>
		</view>

		<!-- 近7天平均值数据展示区域 -->
		<view class="average-card">
			<view class="average-header">
				<text class="average-title">近7天平均值</text>
				<view class="record-count">
					<text class="count-text">{{recordCount}}次记录</text>
				</view>
			</view>
			<view class="average-content">
				<text class="average-value">{{averageValue}}</text>
				<text class="average-unit">{{unit}}</text>
			</view>
			<view class="average-tip">
				<text class="tip-text">{{hasData ? '' : '暂无监测数据'}}</text>
			</view>
		</view>

		<!-- 监测数据区域 -->
		<view class="monitor-section">
			<view class="section-header">
				<text class="section-title">监测数据</text>
				<view class="add-record-btn" @tap="addRecord">
					<text class="btn-text">添加记录</text>
				</view>
			</view>
			<view class="monitor-card">
				<view class="monitor-empty" v-if="!hasMonitorData">
					<view class="empty-icon">
						<text class="icon-text">📊</text>
					</view>
					<view class="empty-content">
						<text class="empty-text">暂无监测数据</text>
						<text class="empty-hint">开始记录你的健康数据</text>
					</view>
					<view class="record-now-btn" @tap="recordNow">
						<text class="btn-text">立即记录</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 健康建议区域 -->
		<view class="advice-section">
			<view class="section-header">
				<text class="section-title">健康建议</text>
			</view>
			<view class="advice-grid">
				<view 
					class="advice-card" 
					v-for="advice in healthAdvice" 
					:key="advice.id"
				>
					<view class="advice-icon-wrapper">
						<text class="advice-icon">{{advice.icon}}</text>
					</view>
					<text class="advice-title">{{advice.title}}</text>
					<text class="advice-desc">{{advice.desc}}</text>
				</view>
			</view>
		</view>

		<!-- 底部功能按钮区域 -->
		<view class="bottom-actions">
			<view class="action-card" @tap="viewReport">
				<view class="action-icon report-icon">
					<text class="action-icon-text">📊</text>
				</view>
				<text class="action-text">管理报告</text>
			</view>
			<view class="action-card" @tap="adjustPlan">
				<view class="action-icon plan-icon">
					<text class="action-icon-text">⚙️</text>
				</view>
				<text class="action-text">调整方案</text>
			</view>
		</view>
	</view>
</template>

<script>
	import config from '@/utils/config.js'
	
	export default {
		components: {
			// uni-popup 是全局组件，不需要导入
		},
		data() {
			return {
				// 当前选中的慢病类型
				selectedDisease: null,
				
				// 从后端获取的慢病列表
				chronicDiseaseList: [],
				
				// 慢病类型映射（慢病名称 -> 类型）
				diseaseTypeMap: {
					'糖尿病': { type: 'diabetes', name: '糖尿病', icon: '💧' },
					'2型糖尿病': { type: 'diabetes', name: '糖尿病', icon: '💧' },
					'1型糖尿病': { type: 'diabetes', name: '糖尿病', icon: '💧' },
					'高血压': { type: 'hypertension', name: '高血压', icon: '❤️' },
					'原发性高血压': { type: 'hypertension', name: '高血压', icon: '❤️' },
					'冠心病': { type: 'coronary', name: '冠心病', icon: '💚' },
					'冠状动脉粥样硬化性心脏病': { type: 'coronary', name: '冠心病', icon: '💚' }
				},
				
				// 所有可用的慢病类型定义
				allDiseaseTypes: [
					{ type: 'diabetes', name: '糖尿病', icon: '💧' },
					{ type: 'hypertension', name: '高血压', icon: '❤️' },
					{ type: 'coronary', name: '冠心病', icon: '💚' }
				],
				
				// 加载状态
				loading: false,
				
				// 近7天平均值数据
				averageValue: '0',
				unit: 'mmol/L',
				recordCount: 0,
				hasData: false,
				
				// 监测数据
				hasMonitorData: false,
				
				// 健康建议
				healthAdvice: [
					{
						id: 1,
						icon: '🍎',
						title: '饮食控制',
						desc: '少食多餐，控制碳水化合物摄入'
					},
					{
						id: 2,
						icon: '🏃',
						title: '规律运动',
						desc: '每天30分钟有氧运动'
					},
					{
						id: 3,
						icon: '💊',
						title: '按时用药',
						desc: '遵医嘱按时服用降糖药物'
					},
					{
						id: 4,
						icon: '💧',
						title: '监测血糖',
						desc: '每天测量空腹和餐后血糖'
					}
				]
			}
		},
		computed: {
			// 根据慢病列表生成可用的慢病选项
			availableDiseases() {
				try {
					if (!this.chronicDiseaseList || this.chronicDiseaseList.length === 0) {
						return [];
					}
					
					// 将慢病名称映射到类型，去重
					const diseaseMap = new Map();
					this.chronicDiseaseList.forEach(diseaseName => {
						if (diseaseName && this.diseaseTypeMap[diseaseName]) {
							const diseaseInfo = this.diseaseTypeMap[diseaseName];
							if (diseaseInfo && !diseaseMap.has(diseaseInfo.type)) {
								diseaseMap.set(diseaseInfo.type, diseaseInfo);
							}
						}
					});
					
					// 转换为数组
					const diseases = Array.from(diseaseMap.values());
					
					return diseases;
				} catch (error) {
					console.error('计算可用慢病列表出错:', error);
					return [];
				}
			}
		},
		watch: {
			// 监听可用慢病列表变化，设置默认选中
			availableDiseases: {
				handler(newDiseases) {
					if (newDiseases.length > 0 && !this.selectedDisease) {
						this.selectedDisease = newDiseases[0].type;
						// 初始化单位
						const type = newDiseases[0].type;
						let unit = 'mmol/L';
						if (type === 'hypertension') {
							unit = 'mmHg';
						} else if (type === 'coronary') {
							unit = '次/分';
						}
						this.unit = unit;
					}
				},
				immediate: false
			}
		},
		onLoad(options) {
			console.log('慢病管理页面加载', options);
			try {
				this.loadChronicDiseaseList();
			} catch (error) {
				console.error('页面加载异常:', error);
				this.loading = false;
			}
		},
		onShow() {
			console.log('慢病管理页面显示');
		},
		onReady() {
			console.log('慢病管理页面渲染完成');
		},
		onError(err) {
			console.error('页面错误:', err);
		},
		methods: {
			/**
			 * 加载慢病列表
			 */
			loadChronicDiseaseList() {
				this.loading = true;
				
				try {
					const userInfo = uni.getStorageSync('userInfo');
					if (!userInfo || !userInfo.userId) {
						this.loading = false;
						uni.showToast({
							title: '请先登录',
							icon: 'none',
							duration: 2000
						});
						// 即使未登录也显示页面内容
						this.chronicDiseaseList = [];
						return;
					}
					
					const token = uni.getStorageSync('token');
					const userId = userInfo.userId;
					
					uni.request({
						url: `${config.baseUrl}/health-archive-process/chronic-disease/list`,
						method: 'GET',
						data: {
							userId: userId
						},
						header: {
							'userId': userId,
							'token': token || ''
						},
						timeout: 10000,
						success: (res) => {
							this.loading = false;
							console.log('慢病列表接口响应:', res);
							
							if (res.statusCode === 200 && res.data && res.data.code === 200) {
								this.chronicDiseaseList = res.data.data || [];
								console.log('慢病列表数据:', this.chronicDiseaseList);
								
								if (this.chronicDiseaseList.length === 0) {
									// 不显示toast，让用户看到空状态提示
									console.log('用户暂无慢病记录');
								}
							} else {
								this.chronicDiseaseList = [];
								const errorMsg = res.data?.message || '查询失败';
								console.error('查询慢病列表失败:', errorMsg);
								uni.showToast({
									title: errorMsg,
									icon: 'none',
									duration: 2000
								});
							}
						},
						fail: (err) => {
							this.loading = false;
							console.error('查询慢病列表网络请求失败:', err);
							this.chronicDiseaseList = [];
							uni.showToast({
								title: '网络请求失败，请检查网络连接',
								icon: 'none',
								duration: 2000
							});
						}
					});
				} catch (error) {
					this.loading = false;
					console.error('加载慢病列表异常:', error);
					this.chronicDiseaseList = [];
					uni.showToast({
						title: '加载失败，请重试',
						icon: 'none',
						duration: 2000
					});
				}
			},
			
			/**
			 * 选择慢病类型
			 */
			selectDisease(type) {
				this.selectedDisease = type;
				
				// 根据不同的慢病类型更新单位
				let unit = 'mmol/L';
				if (type === 'hypertension') {
					unit = 'mmHg';
				} else if (type === 'coronary') {
					unit = '次/分';
				}
				
				this.unit = unit;
				
				// 后续可以在这里调用接口获取对应慢病的数据
			},
			
			/**
			 * 添加记录
			 */
			addRecord() {
				uni.showToast({
					title: '跳转到添加记录页面',
					icon: 'none'
				});
				// 后续可以在这里跳转到添加记录页面
			},
			
			/**
			 * 立即记录
			 */
			recordNow() {
				uni.showToast({
					title: '跳转到记录页面',
					icon: 'none'
				});
				// 后续可以在这里跳转到记录页面
			},
			
			/**
			 * 查看管理报告
			 */
			viewReport() {
				uni.showToast({
					title: '跳转到管理报告页面',
					icon: 'none'
				});
				// 后续可以在这里跳转到管理报告页面
			},
			
			/**
			 * 调整方案
			 */
			adjustPlan() {
				uni.showToast({
					title: '跳转到调整方案页面',
					icon: 'none'
				});
				// 后续可以在这里跳转到调整方案页面
			}
		}
	}
</script>

<style lang="scss" scoped>
	.container {
		min-height: 100vh;
		background-color: #f5f5f5;
		padding-bottom: 20rpx;
	}

	/* 页面标题 */
	.page-header {
		padding: 32rpx 32rpx 24rpx 32rpx;
		background-color: #ffffff;
	}

	.page-title {
		font-size: 36rpx;
		font-weight: 600;
		color: #333333;
	}

	/* 慢病选择区域 */
	.disease-selector {
		padding: 24rpx 32rpx;
		background-color: #ffffff;
	}

	.disease-list {
		display: flex;
		gap: 16rpx;
	}

	.disease-item {
		flex: 1;
		display: flex;
		flex-direction: row;
		align-items: center;
		padding: 20rpx 16rpx;
		background-color: #ffffff;
		border: 2rpx solid #e5e5e5;
		border-radius: 24rpx;
		transition: all 0.3s;
	}

	.disease-item.active {
		background-color: #1890ff;
		border-color: #1890ff;
	}

	.disease-icon {
		width: 40rpx;
		height: 40rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-right: 12rpx;
		border-radius: 50%;
		flex-shrink: 0;
	}

	.diabetes-icon {
		background-color: rgba(24, 144, 255, 0.1);
	}

	.disease-item.active .diabetes-icon {
		background-color: rgba(255, 255, 255, 0.3);
	}

	.hypertension-icon {
		background-color: rgba(255, 77, 79, 0.1);
	}

	.disease-item.active .hypertension-icon {
		background-color: rgba(255, 255, 255, 0.3);
	}

	.coronary-icon {
		background-color: rgba(82, 196, 26, 0.1);
	}

	.disease-item.active .coronary-icon {
		background-color: rgba(255, 255, 255, 0.3);
	}

	.icon-text {
		font-size: 28rpx;
	}

	.disease-name {
		font-size: 28rpx;
		color: #333333;
		flex: 1;
	}

	.disease-item.active .disease-name {
		color: #ffffff;
	}

	.empty-disease {
		width: 100%;
		padding: 40rpx;
		text-align: center;
		background-color: #ffffff;
		border-radius: 16rpx;
	}

	.empty-text {
		font-size: 28rpx;
		color: #999999;
		margin-bottom: 8rpx;
		display: block;
	}

	.empty-hint {
		font-size: 24rpx;
		color: #cccccc;
		display: block;
	}

	.loading-container {
		width: 100%;
		padding: 40rpx;
		text-align: center;
	}

	.loading-text {
		font-size: 28rpx;
		color: #999999;
	}

	/* 近7天平均值数据展示区域 */
	.average-card {
		margin: 24rpx 32rpx;
		padding: 32rpx;
		background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
		border-radius: 24rpx;
		box-shadow: 0 4rpx 16rpx rgba(24, 144, 255, 0.3);
	}

	.average-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 32rpx;
	}

	.average-title {
		font-size: 28rpx;
		color: #ffffff;
		font-weight: 500;
	}

	.record-count {
		padding: 8rpx 16rpx;
		background-color: rgba(255, 255, 255, 0.2);
		border-radius: 12rpx;
	}

	.count-text {
		font-size: 24rpx;
		color: #ffffff;
	}

	.average-content {
		display: flex;
		align-items: baseline;
		justify-content: center;
		margin-bottom: 16rpx;
	}

	.average-value {
		font-size: 72rpx;
		font-weight: 700;
		color: #ffffff;
		line-height: 1;
	}

	.average-unit {
		font-size: 32rpx;
		color: #ffffff;
		margin-left: 8rpx;
		opacity: 0.9;
	}

	.average-tip {
		text-align: center;
	}

	.tip-text {
		font-size: 24rpx;
		color: #ffffff;
		opacity: 0.8;
	}

	/* 今日任务区域 */
	.task-section {
		margin: 24rpx 32rpx;
	}

	.section-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 24rpx;
	}

	.section-title {
		font-size: 32rpx;
		font-weight: 600;
		color: #333333;
	}

	.header-right {
		display: flex;
		align-items: center;
		gap: 16rpx;
	}
	
	.task-progress {
		font-size: 28rpx;
		color: #666666;
	}
	
	.add-task-btn {
		padding: 8rpx 20rpx;
		background-color: #1890ff;
		border-radius: 20rpx;
	}
	
	.add-btn-text {
		font-size: 24rpx;
		color: #ffffff;
	}
	
	.task-edit-btn {
		padding: 8rpx 20rpx;
		background-color: #f5f5f5;
		border-radius: 20rpx;
		margin-left: 12rpx;
	}
	
	.edit-btn-text {
		font-size: 24rpx;
		color: #666666;
	}
	
	/* 任务状态框 */
	.task-status-box {
		padding: 8rpx 20rpx;
		border-radius: 20rpx;
		margin-left: 12rpx;
		flex-shrink: 0;
		transition: all 0.3s;
	}
	
	.task-status-box.completed {
		background-color: #52c41a;
	}
	
	.task-status-box.uncompleted {
		background-color: #d9d9d9;
	}
	
	.status-text {
		font-size: 24rpx;
		color: #ffffff;
		white-space: nowrap;
	}
	
	.task-empty {
		padding: 60rpx 32rpx;
		text-align: center;
		background-color: #ffffff;
		border-radius: 12rpx;
	}
	
	.task-empty .empty-text {
		font-size: 28rpx;
		color: #999999;
	}
	
	/* 任务弹窗样式 */
	.task-dialog {
		width: 600rpx;
		background-color: #ffffff;
		border-radius: 24rpx;
		overflow: hidden;
	}
	
	.dialog-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 32rpx;
		border-bottom: 1rpx solid #f0f0f0;
	}
	
	.dialog-title {
		font-size: 32rpx;
		font-weight: 600;
		color: #333333;
	}
	
	.dialog-close {
		width: 48rpx;
		height: 48rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}
	
	.close-icon {
		font-size: 40rpx;
		color: #999999;
		line-height: 1;
	}
	
	.dialog-content {
		padding: 32rpx;
	}
	
	.form-item {
		margin-bottom: 32rpx;
	}
	
	.form-item:last-child {
		margin-bottom: 0;
	}
	
	.form-label {
		font-size: 28rpx;
		color: #333333;
		margin-bottom: 16rpx;
		display: block;
	}
	
	.form-input {
		width: 100%;
		padding: 20rpx;
		background-color: #f5f5f5;
		border-radius: 12rpx;
		font-size: 28rpx;
		color: #333333;
	}
	
	.picker-view {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 20rpx;
		background-color: #f5f5f5;
		border-radius: 12rpx;
	}
	
	.picker-text {
		font-size: 28rpx;
		color: #333333;
	}
	
	.picker-arrow {
		font-size: 24rpx;
		color: #999999;
	}
	
	.dialog-footer {
		display: flex;
		justify-content: flex-end;
		align-items: center;
		gap: 16rpx;
		padding: 24rpx 32rpx;
		border-top: 1rpx solid #f0f0f0;
	}
	
	.dialog-btn {
		padding: 16rpx 32rpx;
		border-radius: 20rpx;
	}
	
	.cancel-btn {
		background-color: #f5f5f5;
	}
	
	.delete-btn {
		background-color: #ff4d4f;
	}
	
	.confirm-btn {
		background-color: #1890ff;
	}
	
	.dialog-btn .btn-text {
		font-size: 28rpx;
		color: #ffffff;
	}
	
	.cancel-btn .btn-text {
		color: #666666;
	}

	.task-list {
		background-color: transparent;
	}

	.task-item {
		display: flex;
		align-items: center;
		padding: 28rpx 32rpx;
		background-color: #ffffff;
		margin-bottom: 12rpx;
		border-radius: 12rpx;
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
	}

	.task-item:last-child {
		margin-bottom: 0;
	}

	.task-text {
		flex: 1;
		font-size: 28rpx;
		color: #333333;
		text-align: left;
		margin-right: 12rpx;
	}

	.task-icon-wrapper {
		width: 48rpx;
		height: 48rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.task-icon {
		font-size: 32rpx;
	}

	/* 监测数据区域 */
	.monitor-section {
		margin: 24rpx 32rpx;
	}

	.add-record-btn {
		padding: 12rpx 24rpx;
		background-color: #1890ff;
		border-radius: 20rpx;
	}

	.btn-text {
		font-size: 24rpx;
		color: #ffffff;
	}

	.monitor-card {
		margin-top: 24rpx;
		background-color: #ffffff;
		border-radius: 16rpx;
		padding: 32rpx;
		min-height: 200rpx;
		position: relative;
	}

	.monitor-empty {
		display: flex;
		flex-direction: row;
		align-items: flex-start;
		position: relative;
		min-height: 160rpx;
	}

	.empty-icon {
		width: 80rpx;
		height: 80rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
	}

	.empty-icon .icon-text {
		font-size: 60rpx;
		opacity: 0.3;
	}

	.empty-content {
		flex: 1;
		display: flex;
		flex-direction: column;
		margin-left: 24rpx;
		padding-right: 120rpx;
	}

	.empty-text {
		font-size: 28rpx;
		color: #666666;
		margin-bottom: 8rpx;
		line-height: 1.5;
	}

	.empty-hint {
		font-size: 24rpx;
		color: #999999;
		line-height: 1.5;
	}

	.record-now-btn {
		padding: 16rpx 32rpx;
		background-color: #1890ff;
		border-radius: 20rpx;
		position: absolute;
		bottom: 0;
		right: 0;
		white-space: nowrap;
	}

	/* 健康建议区域 */
	.advice-section {
		margin: 24rpx 32rpx;
	}

	.advice-grid {
		display: grid;
		grid-template-columns: repeat(2, 1fr);
		gap: 16rpx;
	}

	.advice-card {
		background-color: #ffffff;
		border-radius: 16rpx;
		padding: 32rpx 24rpx;
		display: flex;
		flex-direction: column;
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
	}

	.advice-icon-wrapper {
		width: 64rpx;
		height: 64rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 16rpx;
	}

	.advice-icon {
		font-size: 48rpx;
	}

	.advice-title {
		font-size: 28rpx;
		font-weight: 600;
		color: #333333;
		margin-bottom: 12rpx;
	}

	.advice-desc {
		font-size: 24rpx;
		color: #666666;
		line-height: 1.5;
	}

	/* 管理团队区域 */
	.team-section {
		margin: 24rpx 32rpx;
	}

	.team-list {
		background-color: #ffffff;
		border-radius: 16rpx;
		overflow: hidden;
	}

	.team-item {
		display: flex;
		align-items: center;
		padding: 32rpx;
		border-bottom: 1rpx solid #f0f0f0;
	}

	.team-item:last-child {
		border-bottom: none;
	}

	.team-avatar {
		width: 80rpx;
		height: 80rpx;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-right: 24rpx;
		flex-shrink: 0;
	}

	.avatar-text {
		font-size: 32rpx;
		color: #ffffff;
		font-weight: 600;
	}

	.team-info {
		flex: 1;
		display: flex;
		flex-direction: column;
	}

	.team-name {
		font-size: 32rpx;
		font-weight: 600;
		color: #333333;
		margin-bottom: 8rpx;
	}

	.team-role {
		font-size: 24rpx;
		color: #666666;
	}

	.contact-btn {
		padding: 12rpx 32rpx;
		background-color: #f5f5f5;
		border-radius: 20rpx;
	}

	.contact-text {
		font-size: 24rpx;
		color: #666666;
	}
	
	.team-empty {
		padding: 60rpx 32rpx;
		text-align: center;
	}
	
	.team-empty .empty-text {
		font-size: 28rpx;
		color: #999999;
	}
	
	.team-loading {
		padding: 40rpx 32rpx;
		text-align: center;
	}
	
	.team-loading .loading-text {
		font-size: 28rpx;
		color: #999999;
	}

	/* 底部功能按钮区域 */
	.bottom-actions {
		display: flex;
		gap: 16rpx;
		padding: 24rpx 32rpx 40rpx 32rpx;
	}

	.action-card {
		flex: 1;
		background-color: #ffffff;
		border-radius: 16rpx;
		padding: 32rpx 24rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
	}

	.action-icon {
		width: 80rpx;
		height: 80rpx;
		border-radius: 16rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 16rpx;
	}

	.report-icon {
		background-color: rgba(24, 144, 255, 0.1);
	}

	.plan-icon {
		background-color: rgba(82, 196, 26, 0.1);
	}

	.action-icon-text {
		font-size: 48rpx;
	}

	.action-text {
		font-size: 28rpx;
		color: #333333;
		font-weight: 500;
	}
</style>
