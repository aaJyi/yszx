<template>
	<view :class="['container', rootFontClass]">
		<!-- 自定义导航栏 -->
		<view class="custom-navbar">
			<view class="navbar-content">
				<view class="navbar-left" @click="goBack">
					<uni-icons type="left" size="20" color="#333333"></uni-icons>
				</view>
				<view class="navbar-title">AI健康助手</view>
				<view class="navbar-right"></view>
			</view>
		</view>

		<!-- 对话消息列表 -->
		<scroll-view 
			scroll-y 
			class="chat-scroll" 
			:scroll-top="scrollTop"
			:scroll-with-animation="true"
			@scrolltoupper="loadMoreHistory"
		>
			<view class="chat-messages">
				<!-- 欢迎消息 -->
				<view class="message-item message-ai" v-if="messages.length === 0">
					<view class="message-avatar">
						<uni-icons type="chatbubble-filled" size="24" color="#07C160"></uni-icons>
					</view>
					<view class="message-content">
						<view class="message-bubble ai-bubble">
							<text class="message-text">您好！我是AI健康助手，有什么健康问题可以咨询我。我会根据您的健康档案为您提供专业的健康建议。</text>
						</view>
					</view>
				</view>

				<!-- 消息列表 -->
				<view 
					class="message-item" 
					:class="message.role === 'user' ? 'message-user' : 'message-ai'"
					v-for="(message, index) in messages" 
					:key="index"
				>
					<view class="message-avatar" v-if="message.role === 'ai'">
						<uni-icons type="chatbubble-filled" size="24" color="#07C160"></uni-icons>
					</view>
					<view class="message-content">
						<view class="message-bubble" :class="message.role === 'user' ? 'user-bubble' : 'ai-bubble'">
							<text class="message-text">{{ message.content }}</text>
						</view>
						<view class="message-time">{{ formatTime(message.timestamp) }}</view>
					</view>
					<view class="message-avatar" v-if="message.role === 'user'">
						<uni-icons type="person-filled" size="24" color="#1890ff"></uni-icons>
					</view>
				</view>

				<!-- 加载中提示：正在分析中 -->
				<view class="message-item message-ai" v-if="loading">
					<view class="message-avatar">
						<uni-icons type="chatbubble-filled" size="24" color="#07C160"></uni-icons>
					</view>
					<view class="message-content">
						<view class="message-bubble ai-bubble loading-bubble">
							<text class="loading-text">正在分析中</text>
							<view class="typing-indicator">
								<view class="typing-dot"></view>
								<view class="typing-dot"></view>
								<view class="typing-dot"></view>
							</view>
						</view>
					</view>
				</view>
			</view>
		</scroll-view>

		<!-- 输入框区域 -->
		<view class="input-area">
			<view class="input-wrapper">
				<textarea 
					class="input-textarea" 
					v-model="inputText" 
					placeholder="请输入您的问题..."
					:auto-height="true"
					:maxlength="500"
					@confirm="sendMessage"
					:disabled="loading"
				></textarea>
				<view class="send-btn" @click="sendMessage" :class="{ 'disabled': !canSend }">
					<uni-icons type="paperplane-filled" size="20" color="#ffffff"></uni-icons>
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
				messages: [],
				inputText: '',
				loading: false,
				scrollTop: 0,
				userId: null
			}
		},
		computed: {
			canSend() {
				return this.inputText.trim().length > 0 && !this.loading
			}
		},
		onLoad() {
			const userInfo = uni.getStorageSync('userInfo')
			if (userInfo && userInfo.userId) {
				this.userId = userInfo.userId
			} else {
				uni.showToast({
					title: '请先登录',
					icon: 'none'
				})
				setTimeout(() => {
					uni.navigateBack()
				}, 1500)
			}
		},
		methods: {
			// 发送消息
			sendMessage() {
				if (!this.canSend) {
					return
				}

				const question = this.inputText.trim()
				if (!question) {
					return
				}

				// 添加用户消息
				const userMessage = {
					role: 'user',
					content: question,
					timestamp: new Date()
				}
				this.messages.push(userMessage)
				this.inputText = ''
				this.scrollToBottom()

				// 发送到后端
				this.loading = true
				const userInfo = uni.getStorageSync('userInfo')
				const token = uni.getStorageSync('token')

				// 构建对话历史（最近10条消息）
				const history = this.messages.slice(-10).map(msg => ({
					role: msg.role === 'user' ? 'user' : 'assistant',
					content: msg.content
				}))
				
				uni.request({
					url: `${config.baseUrl}/home-chat/ask`,
					method: 'POST',
					data: {
						question: question,
						userId: this.userId,
						history: history
					},
					header: {
						'userId': userInfo.userId,
						'token': token || '',
						'Content-Type': 'application/json'
					},
					success: (res) => {
						this.loading = false
						if (res.statusCode === 200 && res.data.code === 200) {
							// 添加AI回复
							const aiMessage = {
								role: 'ai',
								content: res.data.data.answer || '抱歉，我暂时无法回答您的问题。',
								timestamp: new Date()
							}
							this.messages.push(aiMessage)
							this.scrollToBottom()
						} else {
							// 添加错误消息
							const errorMessage = {
								role: 'ai',
								content: res.data.message || '抱歉，服务暂时不可用，请稍后再试。',
								timestamp: new Date()
							}
							this.messages.push(errorMessage)
							this.scrollToBottom()
						}
					},
					fail: (err) => {
						this.loading = false
						console.error('发送消息失败', err)
						// 添加错误消息
						const errorMessage = {
							role: 'ai',
							content: '网络错误，请检查网络连接后重试。',
							timestamp: new Date()
						}
						this.messages.push(errorMessage)
						this.scrollToBottom()
						uni.showToast({
							title: '网络错误',
							icon: 'none'
						})
					}
				})
			},

			// 滚动到底部
			scrollToBottom() {
				this.$nextTick(() => {
					// 使用一个很大的值来确保滚动到底部
					this.scrollTop = 99999
				})
			},

			// 格式化时间
			formatTime(timestamp) {
				if (!timestamp) return ''
				const date = new Date(timestamp)
				const hours = String(date.getHours()).padStart(2, '0')
				const minutes = String(date.getMinutes()).padStart(2, '0')
				return `${hours}:${minutes}`
			},

			// 加载历史消息（预留功能）
			loadMoreHistory() {
				// 可以在这里实现加载历史消息的功能
			},

			// 返回上一页
			goBack() {
				uni.navigateBack()
			}
		}
	}
</script>

<style lang="scss" scoped>
	.container {
		display: flex;
		flex-direction: column;
		height: 100vh;
		background-color: #f5f5f5;
	}

	/* 自定义导航栏 */
	.custom-navbar {
		position: fixed;
		top: 0;
		left: 0;
		right: 0;
		z-index: 999;
		background-color: #ffffff;
		border-bottom: 1px solid #e5e5e5;
	}

	.navbar-content {
		display: flex;
		align-items: center;
		justify-content: space-between;
		height: 88rpx;
		padding: 0 32rpx;
		padding-top: var(--status-bar-height, 0);
	}

	.navbar-left {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 80rpx;
		height: 88rpx;
	}

	.navbar-title {
		flex: 1;
		text-align: center;
		font-size: 32rpx;
		font-weight: 500;
		color: #333333;
	}

	.navbar-right {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 80rpx;
		height: 88rpx;
	}

	/* 对话消息区域 */
	.chat-scroll {
		flex: 1;
		padding: 20rpx;
		padding-top: calc(88rpx + var(--status-bar-height, 0) + 20rpx);
		padding-bottom: 120rpx;
	}

	.chat-messages {
		display: flex;
		flex-direction: column;
	}

	.message-item {
		display: flex;
		margin-bottom: 32rpx;
		align-items: flex-start;
	}

	.message-user {
		flex-direction: row-reverse;
	}

	.message-avatar {
		width: 64rpx;
		height: 64rpx;
		border-radius: 50%;
		background-color: #f0f0f0;
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		margin: 0 16rpx;
	}

	.message-content {
		flex: 1;
		max-width: 70%;
		display: flex;
		flex-direction: column;
	}

	.message-bubble {
		padding: 20rpx 24rpx;
		border-radius: 16rpx;
		word-break: break-all;
		line-height: 1.6;
	}

	.user-bubble {
		background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
		color: #ffffff;
		border-bottom-right-radius: 4rpx;
	}

	.ai-bubble {
		background-color: #ffffff;
		color: #333333;
		border-bottom-left-radius: 4rpx;
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.1);
	}

	.message-text {
		font-size: 28rpx;
	}

	.message-time {
		font-size: 22rpx;
		color: #999999;
		margin-top: 8rpx;
		padding: 0 8rpx;
	}

	.message-user .message-time {
		text-align: right;
	}

	/* 输入框区域 */
	.input-area {
		position: fixed;
		bottom: 0;
		left: 0;
		right: 0;
		background-color: #ffffff;
		border-top: 1px solid #e5e5e5;
		padding: 16rpx 20rpx;
		padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
	}

	.input-wrapper {
		display: flex;
		align-items: flex-end;
		gap: 16rpx;
	}

	.input-textarea {
		flex: 1;
		min-height: 60rpx;
		max-height: 200rpx;
		padding: 16rpx 20rpx;
		background-color: #f5f5f5;
		border-radius: 30rpx;
		font-size: 28rpx;
		line-height: 1.5;
	}

	.send-btn {
		width: 64rpx;
		height: 64rpx;
		border-radius: 50%;
		background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
	}

	.send-btn.disabled {
		background-color: #cccccc;
		opacity: 0.6;
	}

	.loading-text {
		font-size: 28rpx;
		color: #666666;
		margin-bottom: 12rpx;
	}

	/* 打字指示器 */
	.typing-indicator {
		display: flex;
		align-items: center;
		gap: 8rpx;
		padding: 0;
	}

	.typing-dot {
		width: 12rpx;
		height: 12rpx;
		border-radius: 50%;
		background-color: #999999;
		animation: typing 1.4s infinite;
	}

	.typing-dot:nth-child(2) {
		animation-delay: 0.2s;
	}

	.typing-dot:nth-child(3) {
		animation-delay: 0.4s;
	}

	@keyframes typing {
		0%, 60%, 100% {
			transform: translateY(0);
			opacity: 0.7;
		}
		30% {
			transform: translateY(-10rpx);
			opacity: 1;
		}
	}
</style>
