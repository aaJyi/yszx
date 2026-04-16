<template>
	<view class="page-root">
		<view :class="['container', rootFontClass]">
		<!-- 顶部导航栏 -->
		<view class="navbar animate-slide-in-down">
			<view class="navbar-left"></view>
			<view class="navbar-center">
				<text class="navbar-title">健康商城</text>
			</view>
			<view class="navbar-right">
				<uni-icons type="search" size="24" color="#333" @tap="onSearchClick"></uni-icons>
			</view>
		</view>

		<!-- 商城内容 -->
		<view class="content">
			<!-- 商品分类标题 -->
			<view class="section-header animate-slide-in-up">
				<view class="header-line" style="background: linear-gradient(180deg, #FF6B6B 0%, #FF8E8E 100%);"></view>
				<text class="section-title">健康商品</text>
			</view>

			<!-- 商品网格 -->
			<view class="product-grid">
				<!-- 商品卡片 -->
				<view class="product-card animate-slide-in-up" v-for="(product, index) in products" :key="index" :class="'delay-' + (100 + index * 100)" @tap="onProductClick(product)">
					<view class="product-image animate-float" :class="'delay-' + (index * 100)">
						<image :src="product.image" mode="aspectFill"></image>
					</view>
					<view class="product-info">
						<text class="product-name">{{ product.name }}</text>
						<text class="product-price">¥{{ product.price }}</text>
						<view class="product-badge animate-pulse" v-if="product.badge">{{ product.badge }}</view>
					</view>
				</view>
			</view>

			<!-- 健康服务标题 -->
			<view class="section-header animate-slide-in-up delay-200" style="margin-top: 60rpx;">
				<view class="header-line" style="background: linear-gradient(180deg, #4ECDC4 0%, #45B7AA 100%);"></view>
				<text class="section-title">健康服务</text>
			</view>

			<!-- 服务网格 -->
			<view class="service-grid">
				<view class="service-card animate-slide-in-up" v-for="(service, index) in services" :key="index" :class="'delay-' + (300 + index * 100)" @tap="onServiceClick(service)">
					<view class="service-icon-wrapper animate-float" :class="'delay-' + (index * 100)" :style="{ backgroundColor: service.bgColor }">
						<uni-icons :type="service.icon" size="24" :color="service.color"></uni-icons>
					</view>
					<text class="service-name">{{ service.name }}</text>
					<text class="service-desc">{{ service.desc }}</text>
				</view>
			</view>
		</view>
		</view>
		<!-- 底部导航栏：放在 scale 容器外，保证 fixed 固定 -->
		<custom-tabbar :current="2"></custom-tabbar>
	</view>
</template>

<script>
	import customTabbar from '@/components/custom-tabbar/custom-tabbar.vue'

	export default {
		components: {
			customTabbar
		},
		data() {
			return {
				products: [
					{
						name: '维生素C片',
						price: 69.9,
						image: 'https://via.placeholder.com/200x200',
						badge: '热销'
					},
					{
						name: '蛋白粉',
						price: 199.0,
						image: 'https://via.placeholder.com/200x200'
					},
					{
						name: '鱼油胶囊',
						price: 129.0,
						image: 'https://via.placeholder.com/200x200',
						badge: '新品'
					},
					{
						name: '钙片',
						price: 89.9,
						image: 'https://via.placeholder.com/200x200'
					}
				],
				services: [
					{
						name: '健康体检',
						desc: '全面体检套餐',
						icon: 'medkit',
						color: '#FF6B6B',
						bgColor: '#fff5f5'
					},
					{
						name: '营养咨询',
						desc: '专业营养师指导',
						icon: 'food',
						color: '#4ECDC4',
						bgColor: '#f0fdfa'
					},
					{
						name: '运动计划',
						desc: '个性化运动方案',
						icon: 'flag',
						color: '#45B7D1',
						bgColor: '#f0f9ff'
					}
				]
			}
		},
		onLoad() {
			console.log('商城页面加载')
		},
		methods: {
			onSearchClick() {
				uni.showToast({
					title: '搜索功能开发中',
					icon: 'none'
				})
			},
			onProductClick(product) {
				uni.showToast({
					title: `查看${product.name}`,
					icon: 'none'
				})
			},
			onServiceClick(service) {
				uni.showToast({
					title: `查看${service.name}`,
					icon: 'none'
				})
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page-root {
		min-height: 100vh;
		background-color: transparent;
	}
	.container {
		min-height: 100vh;
		background-color: transparent;
		padding-bottom: 120rpx;
	}

	/* 导航栏 */
	.navbar {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 40rpx 30rpx 20rpx;
	}

	.navbar-left {
		width: 60rpx;
	}

	.navbar-center {
		flex: 1;
		text-align: center;
	}

	.navbar-title {
		font-size: 36rpx;
		font-weight: 600;
		color: #333333;
	}

	.navbar-right {
		width: 60rpx;
		display: flex;
		justify-content: flex-end;
	}

	.content {
		padding: 40rpx 30rpx;
		padding-top: 20rpx;
	}

	.section-header {
		display: flex;
		align-items: center;
		margin-bottom: 40rpx;
	}

	.header-line {
		width: 6rpx;
		height: 32rpx;
		border-radius: 3rpx;
		margin-right: 16rpx;
	}

	.section-title {
		font-size: 36rpx;
		font-weight: 600;
		color: #333333;
	}

	/* 商品网格 */
	.product-grid {
		display: grid;
		grid-template-columns: repeat(2, 1fr);
		gap: 24rpx;
	}

	.product-card {
		background: rgba(255, 255, 255, 0.82);
		backdrop-filter: blur(12rpx);
		border: 1rpx solid rgba(255, 255, 255, 0.7);
		border-radius: 16rpx;
		overflow: hidden;
		box-shadow: 0 12rpx 30rpx rgba(2, 6, 23, 0.08);
		transition: all 0.3s;
	}

	.product-card:hover {
		transform: scale(1.03);
		box-shadow: 0 16rpx 36rpx rgba(2, 6, 23, 0.12), 0 0 25rpx rgba(255, 107, 157, 0.15);
	}

	.product-card:active {
		transform: scale(0.98);
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.1);
	}

	.product-image {
		width: 100%;
		height: 240rpx;
		overflow: hidden;
	}

	.product-image image {
		width: 100%;
		height: 100%;
	}

	.product-info {
		padding: 24rpx;
		position: relative;
	}

	.product-name {
		font-size: 28rpx;
		font-weight: 500;
		color: #333333;
		margin-bottom: 12rpx;
		line-height: 1.4;
	}

	.product-price {
		font-size: 32rpx;
		font-weight: 600;
		color: #FF6B6B;
	}

	.product-badge {
		position: absolute;
		top: 24rpx;
		right: 24rpx;
		background-color: #FF6B6B;
		color: #ffffff;
		font-size: 20rpx;
		padding: 4rpx 12rpx;
		border-radius: 12rpx;
	}

	/* 服务网格 */
	.service-grid {
		display: grid;
		grid-template-columns: repeat(2, 1fr);
		gap: 24rpx;
	}

	.service-card {
		background: rgba(255, 255, 255, 0.82);
		backdrop-filter: blur(12rpx);
		border: 1rpx solid rgba(255, 255, 255, 0.7);
		border-radius: 16rpx;
		padding: 32rpx 24rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
		box-shadow: 0 12rpx 30rpx rgba(2, 6, 23, 0.08);
		transition: all 0.3s;
	}

	.service-card:hover {
		transform: scale(1.03);
		box-shadow: 0 16rpx 36rpx rgba(2, 6, 23, 0.12), 0 0 25rpx rgba(78, 205, 196, 0.15);
	}

	.service-card:active {
		transform: scale(0.98);
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.1);
	}

	.service-icon-wrapper {
		width: 60rpx;
		height: 60rpx;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 20rpx;
		transition: transform 0.2s;
	}

	.service-card:hover .service-icon-wrapper {
		transform: scale(1.1);
	}

	.service-name {
		font-size: 32rpx;
		font-weight: 600;
		color: #333333;
		margin-bottom: 12rpx;
	}

	.service-desc {
		font-size: 24rpx;
		color: #999999;
		text-align: center;
		line-height: 1.5;
	}
</style>