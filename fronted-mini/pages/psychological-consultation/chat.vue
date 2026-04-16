<template>
	<view :class="['container', rootFontClass]">
		<!-- 顶部导航栏 -->
		<view class="navbar">
			<view class="back-btn" @click="goBack">
				<uni-icons type="back" size="24" color="#ffffff"></uni-icons>
			</view>
			<text class="navbar-title">心理咨询</text>
			<view class="navbar-right">
				<view class="clear-btn" @click="clearChatHistory">
					<uni-icons type="trash" size="24" color="#ffffff"></uni-icons>
				</view>
			</view>
		</view>

		<!-- 聊天内容区域 -->
		<view class="chat-content" ref="chatContent">
			<!-- 系统消息 -->
			<view class="system-message" v-if="messages.length === 0">
				<text class="system-text">你好，我是心理咨询AI助手，有什么可以帮助你的？</text>
			</view>

			<!-- 聊天消息 -->
			<view 
				v-for="(message, index) in messages" 
				:key="index"
				:class="['message-item', message.role === 'user' ? 'user-message' : 'ai-message']"
			>
				<view class="message-avatar">
					<uni-icons 
						:type="message.role === 'user' ? 'person' : 'chatbubble'
						:size="36"
						:color="message.role === 'user' ? '#1890ff' : '#07C160'"
					></uni-icons>
				</view>
				<view class="message-content">
					<text class="message-text">{{message.content}}</text>
				</view>
			</view>

			<!-- 加载中提示 -->
			<view class="loading-message" v-if="isLoading">
				<view class="loading-dot"></view>
				<text class="loading-text">AI正在思考...</text>
			</view>
		</view>

		<!-- 输入区域 -->
		<view class="input-area">
			<view class="input-container">
				<input 
					v-model="inputMessage" 
					class="input-box" 
					placeholder="请输入你的问题..."
					@keyup.enter="sendMessage"
					:disabled="isLoading"
				>
				<view class="send-btn" @click="sendMessage" :class="{ disabled: !inputMessage.trim() || isLoading }">
					<uni-icons type="send" size="28" color="#ffffff"></uni-icons>
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
				// 聊天消息列表
				messages: [],
				// 输入框内容
				inputMessage: '',
				// 加载状态
				isLoading: false,
				// 历史对话记录键名
				historyKey: 'psychologicalConsultationChatHistory'
			}
		},
		onLoad() {
			console.log('心理咨询页面加载')
			// 加载历史聊天记录
			this.loadChatHistory()
		},
		methods: {
			/**
			 * 加载历史聊天记录
			 */
			loadChatHistory() {
				const history = uni.getStorageSync(this.historyKey)
				if (history && Array.isArray(history)) {
					this.messages = history
				}
			},

			/**
			 * 保存聊天记录
			 */
			saveChatHistory() {
				uni.setStorageSync(this.historyKey, this.messages)
			},

			/**
			 * 清空聊天记录
			 */
			clearChatHistory() {
				uni.showModal({
					title: '确认清空',
					content: '确定要清空所有聊天记录吗？',
					confirmText: '确定',
					cancelText: '取消',
					success: (res) => {
						if (res.confirm) {
							this.messages = []
							uni.removeStorageSync(this.historyKey)
							uni.showToast({
								title: '聊天记录已清空',
								icon: 'success'
							})
						}
					}
				})
			},

			/**
			 * 返回上一页
			 */
			goBack() {
				uni.navigateBack()
			},

			/**
			 * 发送消息
			 */
			sendMessage() {
				const message = this.inputMessage.trim()
				if (!message || this.isLoading) return

				// 添加用户消息到聊天列表
				const userMessage = {
					role: 'user',
					content: message
				}
				this.messages.push(userMessage)
				this.inputMessage = ''
				this.scrollToBottom()
				this.saveChatHistory()

				// 显示加载状态
				this.isLoading = true

				// 调用AI接口获取回复
				this.getAIResponse(message)
			},

			/**
			 * 调用AI接口获取回复
			 */
			getAIResponse(message) {
				const self = this
				// 心理咨询对接智能体：使用 /psychological-chat/chat 接口（后端转发到智能体）
				self.sendRegularRequest(message)
			},

			/**
			 * 发送普通请求获取AI回复（调用智能体）
			 */
			sendRegularRequest(message) {
				const self = this
				const userInfo = uni.getStorageSync('userInfo')
				if (!userInfo || !userInfo.userId) {
					self.isLoading = false
					self.messages.push({ role: 'assistant', content: '请先登录后再使用心理咨询。' })
					self.scrollToBottom()
					self.saveChatHistory()
					return
				}

				uni.request({
					url: config.baseUrl + '/psychological-chat/chat',
					method: 'POST',
					data: { message: message },
					header: {
						'Content-Type': 'application/json',
						'userId': String(userInfo.userId)
					},
					success: function(res) {
						self.isLoading = false
						const data = res.data && res.data.data
						const content = data && data.content
						if (res.statusCode === 200 && content) {
							self.messages.push({ role: 'assistant', content: content })
						} else {
							self.messages.push({
								role: 'assistant',
								content: (res.data && res.data.message) || '抱歉，AI助手暂时无法响应，请稍后再试。'
							})
						}
						self.scrollToBottom()
						self.saveChatHistory()
					},
					fail: function(err) {
						console.error('调用心理咨询接口失败:', err)
						self.isLoading = false
						self.messages.push({
							role: 'assistant',
							content: '抱歉，网络连接失败，请稍后再试。'
						})
						self.scrollToBottom()
						self.saveChatHistory()
					}
				})
			},

			/**
			 * 滚动到底部
			 */
			scrollToBottom() {
				uni.nextTick(() => {
					const chatContent = this.$refs.chatContent
					if (chatContent) {
						chatContent.scrollTop = chatContent.scrollHeight
					}
				})
			}
		}
	}
</script>

<style lang="scss" scoped>
	.container {
		min-height: 100vh;
		background-color: #f5f5f5;
		display: flex;
		flex-direction: column;
	}

	/* 顶部导航栏 */
	.navbar {
		height: 88rpx;
		background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 0 32rpx;
		position: relative;
		z-index: 10;
	}

	.back-btn {
		width: 48rpx;
		height: 48rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.navbar-title {
		font-size: 32rpx;
		font-weight: 600;
		color: #ffffff;
		flex: 1;
		text-align: center;
		margin: 0 48rpx;
	}

	.navbar-right {
		width: 48rpx;
		display: flex;
		align-items: center;
		justify-content: flex-end;
	}

	.clear-btn {
		width: 48rpx;
		height: 48rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	/* 聊天内容区域 */
	.chat-content {
		flex: 1;
		padding: 32rpx;
		overflow-y: auto;
		display: flex;
		flex-direction: column;
		gap: 24rpx;
	}

	/* 系统消息 */
	.system-message {
		align-self: center;
		background-color: rgba(0, 0, 0, 0.05);
		padding: 16rpx 24rpx;
		border-radius: 20rpx;
		margin-top: 16rpx;
	}

	.system-text {
		font-size: 24rpx;
		color: #666666;
	}

	/* 聊天消息 */
	.message-item {
		display: flex;
		gap: 16rpx;
		align-items: flex-start;
	}

	.user-message {
		flex-direction: row-reverse;
	}

	.message-avatar {
		width: 64rpx;
		height: 64rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 50%;
		background-color: #f0f0f0;
		flex-shrink: 0;
	}

	.message-content {
		flex: 1;
		max-width: 70%;
	}

	.user-message .message-content {
		align-items: flex-end;
	}

	.message-text {
		font-size: 28rpx;
		color: #333333;
		line-height: 1.5;
		word-wrap: break-word;
	}

	.user-message .message-text {
		background-color: #1890ff;
		color: #ffffff;
		padding: 16rpx 24rpx;
		border-radius: 20rpx 20rpx 4rpx 20rpx;
	}

	.ai-message .message-text {
		background-color: #ffffff;
		padding: 16rpx 24rpx;
		border-radius: 20rpx 20rpx 20rpx 4rpx;
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
	}

	/* 加载中提示 */
	.loading-message {
		align-self: flex-start;
		display: flex;
		align-items: center;
		gap: 12rpx;
		padding: 16rpx 24rpx;
		background-color: #ffffff;
		border-radius: 20rpx;
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
	}

	.loading-dot {
		width: 20rpx;
		height: 20rpx;
		border-radius: 50%;
		background-color: #1890ff;
		animation: pulse 1.5s infinite ease-in-out;
	}

	@keyframes pulse {
		0%, 100% {
			transform: scale(0.8);
			opacity: 0.6;
		}
		50% {
			transform: scale(1.2);
			opacity: 1;
		}
	}

	.loading-text {
		font-size: 24rpx;
		color: #666666;
	}

	/* 输入区域 */
	.input-area {
		padding: 24rpx 32rpx;
		background-color: #ffffff;
		border-top: 1rpx solid #f0f0f0;
	}

	.input-container {
		display: flex;
		align-items: flex-end;
		gap: 16rpx;
	}

	.input-box {
		flex: 1;
		height: 80rpx;
		border: 2rpx solid #e0e0e0;
		border-radius: 40rpx;
		padding: 0 32rpx;
		font-size: 28rpx;
		background-color: #f9f9f9;
		resize: none;
		box-sizing: border-box;
	}

	.input-box:focus {
		border-color: #1890ff;
		background-color: #ffffff;
	}

	.send-btn {
		width: 80rpx;
		height: 80rpx;
		background-color: #1890ff;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		transition: all 0.3s;
	}

	.send-btn.disabled {
		background-color: #cccccc;
		cursor: not-allowed;
	}

	.send-btn:active {
		transform: scale(0.95);
	}

	/* 滚动条样式 */
	.chat-content::-webkit-scrollbar {
		width: 6rpx;
	}

	.chat-content::-webkit-scrollbar-track {
		background: #f1f1f1;
		border-radius: 3rpx;
	}

	.chat-content::-webkit-scrollbar-thumb {
		background: #c1c1c1;
		border-radius: 3rpx;
	}

	.chat-content::-webkit-scrollbar-thumb:hover {
		background: #a8a8a8;
	}
</style>