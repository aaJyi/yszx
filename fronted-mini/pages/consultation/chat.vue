<template>
	<view :class="['container', rootFontClass]">
		<!-- 自定义导航栏 -->
		<view class="custom-navbar">
			<view class="navbar-content">
				<view class="navbar-left" @click="goBack">
					<uni-icons type="left" size="20" color="#333333"></uni-icons>
				</view>
				<view class="navbar-title">AI在线问诊</view>
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
						<view 
							class="message-bubble" 
							:class="message.role === 'user' ? 'user-bubble' : 'ai-bubble'"
							@longpress="copyMessage(message.content)"
						>
							<text class="message-text" :selectable="true" :user-select="true">{{ message.content }}</text>
							<view v-if="message.streaming" class="typing-indicator">
								<view class="typing-dot"></view>
								<view class="typing-dot"></view>
								<view class="typing-dot"></view>
							</view>
						</view>
						<view class="message-time" v-if="!message.streaming">{{ formatTime(message.timestamp) }}</view>
					</view>
					<view class="message-avatar" v-if="message.role === 'user'">
						<uni-icons type="person-filled" size="24" color="#1890ff"></uni-icons>
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
					:show-confirm-bar="false"
					:adjust-position="true"
					:hold-keyboard="true"
					:enableNativeEmojiInput="true"
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
			// 发送消息（流式接收）
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

				// 创建AI消息占位符（用于流式更新）
				const aiMessage = {
					role: 'ai',
					content: '',
					timestamp: new Date(),
					streaming: true
				}
				this.messages.push(aiMessage)
				this.scrollToBottom()

				// 发送到后端（流式）
				this.loading = true
				const userInfo = uni.getStorageSync('userInfo')
				const token = uni.getStorageSync('token')

				// 构建对话历史（最近10条消息，排除当前正在流式输出的消息）
				const history = this.messages.slice(0, -1).slice(-10).map(msg => ({
					role: msg.role === 'user' ? 'user' : 'assistant',
					content: msg.content
				}))

				// 使用流式请求
				// #ifdef H5
				// H5环境使用EventSource
				this.sendMessageStreamH5(question, history, aiMessage, userInfo, token)
				// #endif

				// #ifndef H5
				// 非H5环境（小程序、APP）使用uni.request的流式处理
				this.sendMessageStreamUni(question, history, aiMessage, userInfo, token)
				// #endif
			},

			// H5环境使用EventSource接收流式数据
			sendMessageStreamH5(question, history, aiMessage, userInfo, token) {
				// 在H5中，uni.request不支持SSE，直接使用fetch API
				this.sendMessageStreamFetch(question, history, aiMessage, userInfo, token)
			},

			// 使用fetch API接收SSE流
			sendMessageStreamFetch(question, history, aiMessage, userInfo, token) {
				// #ifdef H5
				const requestData = {
					question: question,
					userId: this.userId,
					history: history
				}

				fetch(`${config.baseUrl}/consultation/chat/stream`, {
					method: 'POST',
					headers: {
						'Content-Type': 'application/json',
						'userId': userInfo.userId || '',
						'token': token || ''
					},
					body: JSON.stringify(requestData)
				}).then(response => {
					if (!response.ok) {
						throw new Error('Network response was not ok')
					}

					const reader = response.body.getReader()
					const decoder = new TextDecoder()
					let buffer = ''

					const readStream = () => {
						reader.read().then(({ done, value }) => {
							if (done) {
								this.loading = false
								aiMessage.streaming = false
								this.scrollToBottom()
								return
							}

							buffer += decoder.decode(value, { stream: true })
							const lines = buffer.split('\n')
							buffer = lines.pop() || ''

							for (const line of lines) {
								if (line.startsWith('data: ')) {
									const data = line.substring(6)
									try {
										const json = JSON.parse(data)
										if (json.error) {
											aiMessage.content = json.error
											aiMessage.streaming = false
											this.loading = false
										} else if (json.content || json.text || json.message) {
											aiMessage.content += (json.content || json.text || json.message)
											this.scrollToBottom()
										}
									} catch (e) {
										// 如果不是JSON，直接作为文本内容
										if (data.trim()) {
											aiMessage.content += data
											this.scrollToBottom()
										}
									}
								} else if (line.startsWith('event: ')) {
									const eventType = line.substring(7)
									if (eventType === 'error') {
										this.loading = false
										aiMessage.streaming = false
									}
								}
							}

							readStream()
						}).catch(err => {
							console.error('读取流式数据失败', err)
							this.loading = false
							aiMessage.streaming = false
							aiMessage.content = '接收AI响应失败，请重试。'
							this.scrollToBottom()
						})
					}

					readStream()
				}).catch(err => {
					console.error('发送流式请求失败', err)
					this.loading = false
					aiMessage.streaming = false
					aiMessage.content = '网络错误，请检查网络连接后重试。'
					this.scrollToBottom()
					uni.showToast({
						title: '网络错误',
						icon: 'none'
					})
				})
				// #endif

				// #ifndef H5
				// 非H5环境回退到普通请求
				this.sendMessageStreamUni(question, history, aiMessage, userInfo, token)
				// #endif
			},

			// uni.request方式（非H5环境或作为回退方案）
			// 使用XMLHttpRequest来接收流式数据（移动端支持）
			sendMessageStreamUni(question, history, aiMessage, userInfo, token) {
				const requestData = {
					question: question,
					userId: this.userId,
					history: history
				}
				
				// 使用XMLHttpRequest来接收流式数据
				// #ifdef H5
				// H5环境已经在sendMessageStreamFetch中处理，这里不应该被调用
				this.sendMessageStreamFetch(question, history, aiMessage, userInfo, token)
				// #endif
				
				// #ifndef H5
				// 移动端使用XMLHttpRequest接收流式数据
				try {
					const xhr = new XMLHttpRequest()
					xhr.open('POST', `${config.baseUrl}/consultation/chat/stream`)
					xhr.setRequestHeader('Content-Type', 'application/json')
					xhr.setRequestHeader('userId', userInfo.userId || '')
					xhr.setRequestHeader('token', token || '')
					xhr.setRequestHeader('Accept', 'text/event-stream')
					
					let receivedLength = 0
					let buffer = ''
					
					// 监听数据接收（流式）
					xhr.onreadystatechange = () => {
						if (xhr.readyState === 3) {
							// 正在接收数据
							const newData = xhr.responseText.substring(receivedLength)
							if (newData) {
								receivedLength = xhr.responseText.length
								buffer += newData
								
								// 解析SSE格式数据
								const lines = buffer.split('\n')
								buffer = lines.pop() || ''
								
								for (const line of lines) {
									if (line.startsWith('data: ')) {
										const data = line.substring(6).trim()
										if (data) {
											try {
												const json = JSON.parse(data)
												if (json.error) {
													aiMessage.content = json.error
													aiMessage.streaming = false
													this.loading = false
												} else if (json.content || json.text || json.message) {
													aiMessage.content += (json.content || json.text || json.message)
													this.scrollToBottom()
												}
											} catch (e) {
												// 如果不是JSON，直接作为文本内容
												if (data && data !== '[DONE]') {
													aiMessage.content += data
													this.scrollToBottom()
												}
											}
										}
									} else if (line.startsWith('event: ')) {
										const eventType = line.substring(7).trim()
										if (eventType === 'error' || eventType === 'complete') {
											this.loading = false
											aiMessage.streaming = false
										}
									}
								}
							}
						} else if (xhr.readyState === 4) {
							// 请求完成
							this.loading = false
							aiMessage.streaming = false
							
							// 处理剩余数据
							if (buffer.trim()) {
								const lines = buffer.split('\n')
								for (const line of lines) {
									if (line.startsWith('data: ')) {
										const data = line.substring(6).trim()
										if (data) {
											try {
												const json = JSON.parse(data)
												if (json.content || json.text || json.message) {
													aiMessage.content += (json.content || json.text || json.message)
												}
											} catch (e) {
												if (data && data !== '[DONE]') {
													aiMessage.content += data
												}
											}
										}
									}
								}
							}
							
							if (!aiMessage.content) {
								aiMessage.content = '抱歉，我暂时无法回答您的问题。'
							}
							
							this.scrollToBottom()
						}
					}
					
					xhr.onerror = () => {
						this.loading = false
						aiMessage.streaming = false
						aiMessage.content = '网络错误，请检查网络连接后重试。'
						this.scrollToBottom()
						uni.showToast({
							title: '网络错误',
							icon: 'none'
						})
					}
					
					xhr.ontimeout = () => {
						this.loading = false
						aiMessage.streaming = false
						aiMessage.content = '请求超时，请稍后再试。'
						this.scrollToBottom()
						uni.showToast({
							title: '请求超时',
							icon: 'none'
						})
					}
					
					xhr.timeout = 900000 // 15分钟
					xhr.send(JSON.stringify(requestData))
				} catch (e) {
					console.error('创建XMLHttpRequest失败', e)
					// 回退到普通请求
					uni.request({
						url: `${config.baseUrl}/consultation/chat`,
						method: 'POST',
						timeout: 900000,
						data: requestData,
						header: {
							'userId': userInfo.userId,
							'token': token || '',
							'Content-Type': 'application/json'
						},
						success: (res) => {
							this.loading = false
							aiMessage.streaming = false
							if (res.statusCode === 200 && res.data && res.data.code === 200) {
								aiMessage.content = res.data.data?.answer || '抱歉，我暂时无法回答您的问题。'
							} else {
								aiMessage.content = res.data?.message || '抱歉，服务暂时不可用，请稍后再试。'
							}
							this.scrollToBottom()
						},
						fail: (err) => {
							this.loading = false
							aiMessage.streaming = false
							aiMessage.content = '网络错误，请检查网络连接后重试。'
							this.scrollToBottom()
						}
					})
				}
				// #endif
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
			},

			// 复制消息内容
			copyMessage(content) {
				if (!content || content.trim() === '') {
					return
				}
				
				uni.setClipboardData({
					data: content,
					success: () => {
						uni.showToast({
							title: '已复制到剪贴板',
							icon: 'success',
							duration: 1500
						})
					},
					fail: () => {
						uni.showToast({
							title: '复制失败',
							icon: 'none'
						})
					}
				})
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
		word-wrap: break-word;
		word-break: break-all;
		white-space: pre-wrap;
		user-select: text;
		-webkit-user-select: text;
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
		word-wrap: break-word;
		word-break: break-all;
		white-space: pre-wrap;
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

	/* 打字指示器 */
	.typing-indicator {
		display: flex;
		align-items: center;
		gap: 8rpx;
		padding: 10rpx 0;
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
