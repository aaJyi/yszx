<template>
	<view class="page-root">
		<view :class="['container', rootFontClass]">
			<!-- 顶部导航栏 -->
			<view class="navbar">
				<view class="navbar-left"></view>
				<view class="navbar-center">
					<text class="navbar-title">AI对话</text>
				</view>
				<view class="navbar-right">
					<view class="clear-btn" @tap="clearChatHistory">
						<uni-icons type="trash" size="22" color="#666"></uni-icons>
					</view>
				</view>
			</view>

			<!-- 中文 / 英文 RAG 切换（分别请求 8765 / 8766） -->
			<view class="lang-tabs">
				<view
					class="lang-tab"
					:class="{ active: chatLang === 'zh' }"
					@tap="setChatLang('zh')"
				>中文 AI</view>
				<view
					class="lang-tab"
					:class="{ active: chatLang === 'en' }"
					@tap="setChatLang('en')"
				>English AI</view>
			</view>

			<!-- 图四风格：问候区 -->
			<view class="greeting-card">
				<view class="greeting-main">
					<text class="greeting-title">医视智行</text>
					<text class="greeting-sparkle">✨</text>
				</view>
				<text class="greeting-sub">{{ greetingSub }}</text>
			</view>

			<!-- 图四风格：推荐问题（圆角白卡片 + 竖条标题） -->
			<view class="section-card" v-if="messages.length === 0">
				<view class="section-header">
					<view class="green-line"></view>
					<text class="section-title">{{ sectionSuggestTitle }}</text>
				</view>
				<view 
					class="suggest-card" 
					v-for="(q, i) in suggestQuestions" 
					:key="i"
					@tap="onSuggestTap(q)"
				>
					<view class="suggest-hash">#</view>
					<text class="suggest-text">{{ q }}</text>
					<uni-icons type="right" size="14" color="#999"></uni-icons>
				</view>
			</view>

			<!-- 对话消息列表 -->
			<scroll-view 
				scroll-y 
				class="chat-scroll" 
				:scroll-top="scrollTop"
				:scroll-with-animation="true"
			>
				<view class="chat-messages">
					<!-- 无消息时显示欢迎语 -->
					<view class="message-item message-ai" v-if="messages.length === 0">
						<view class="message-avatar">
							<uni-icons type="chatbubble-filled" size="24" color="#07C160"></uni-icons>
						</view>
						<view class="message-content">
							<view class="message-bubble ai-bubble">
								<text class="message-text">{{ welcomeAiText }}</text>
							</view>
						</view>
					</view>

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
								<text v-if="message.role === 'user'" class="message-text">{{ getMessageContent(message) }}</text>
								<rich-text v-else class="message-richtext" :nodes="mdToHtml(getMessageContent(message))"></rich-text>
							</view>
							<view class="message-time">{{ formatTime(message.timestamp) }}</view>
						</view>
						<view class="message-avatar" v-if="message.role === 'user'">
							<uni-icons type="person-filled" size="24" color="#1890ff"></uni-icons>
						</view>
					</view>

					<view class="message-item message-ai" v-if="loading && !streamingResponse">
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

			<!-- 提问框固定在屏幕下方，回车发送 -->
			<view class="input-area">
				<view class="input-row">
					<textarea 
						class="input-textarea" 
						v-model="inputText" 
						:placeholder="inputPlaceholder"
						:auto-height="true"
						:maxlength="2000"
						@confirm="sendMessage"
						:disabled="loading"
						confirm-type="send"
					></textarea>
					<view class="send-btn" @tap="onSendTap" :class="{ 'disabled': !canSend }">
						<uni-icons type="paperplane-filled" size="22" color="#ffffff"></uni-icons>
					</view>
				</view>
			</view>
		</view>
		<custom-tabbar :current="1"></custom-tabbar>
	</view>
</template>

<script>
	import customTabbar from '@/components/custom-tabbar/custom-tabbar.vue'
	import config from '@/utils/config.js'

	export default {
		components: { customTabbar },
		data() {
			return {
				chatLang: 'zh',
				messagesZh: [],
				messagesEn: [],
				inputText: '',
				loading: false,
				scrollTop: 0,
				streamingContent: '',
				streamingResponse: false,
				_simulateStreamTimer: null,
				suggestQuestionsZh: [
					'吃褪黑素真的能睡得更好吗？',
					'睡觉的最佳姿势是什么？',
					'怎么减肚子上的赘肉最快？',
					'每天运动多久比较合适？',
					'血压高平时饮食要注意什么？'
				],
				suggestQuestionsEn: [
					'Can melatonin really help me sleep better?',
					'What is the best sleeping position?',
					'How can I lose belly fat safely?',
					'How much exercise per day is recommended?',
					'What should I eat if I have high blood pressure?'
				]
			}
		},
		computed: {
			messages() {
				return this.chatLang === 'zh' ? this.messagesZh : this.messagesEn
			},
			ragBaseUrl() {
				return this.chatLang === 'en' ? config.aiDoctorRagEnUrl : config.aiDoctorRagUrl
			},
			suggestQuestions() {
				return this.chatLang === 'zh' ? this.suggestQuestionsZh : this.suggestQuestionsEn
			},
			greetingSub() {
				return this.chatLang === 'zh'
					? '有健康问题随时问我'
					: 'Ask me health questions in English anytime.'
			},
			sectionSuggestTitle() {
				return this.chatLang === 'zh' ? '健康问答' : 'Health Q&A'
			},
			welcomeAiText() {
				return this.chatLang === 'zh'
					? '您好 我是您的AI健康助手，有什么健康问题可以咨询我'
					: "Hi — I'm your AI health assistant. Ask me any health question in English."
			},
			inputPlaceholder() {
				return this.chatLang === 'zh' ? '请输入问题' : 'Type your question in English'
			},
			ragPortHint() {
				return this.chatLang === 'en' ? '8766' : '8765'
			},
			canSend() {
				return this.inputText.trim().length > 0 && !this.loading
			}
		},
		beforeDestroy() {
			if (this._simulateStreamTimer) {
				clearInterval(this._simulateStreamTimer)
				this._simulateStreamTimer = null
			}
		},
		methods: {
			setChatLang(lang) {
				if (lang === this.chatLang) return
				if (this.loading) {
					uni.showToast({ title: this.chatLang === 'zh' ? '请等待当前回复完成' : 'Please wait for the reply', icon: 'none' })
					return
				}
				this.chatLang = lang
				this.inputText = ''
				this.scrollToBottom()
			},
			sendMessage(suggestText) {
				// 推荐问题：明确传入非空字符串；输入框发送：用 inputText；@confirm 可能传入 event.detail.value
				let query = ''
				if (typeof suggestText === 'string' && suggestText.trim() !== '') {
					query = suggestText.trim()
				} else if (suggestText && typeof suggestText === 'object' && suggestText.detail && typeof suggestText.detail.value === 'string') {
					query = suggestText.detail.value.trim()
				} else {
					query = (this.inputText || '').trim()
				}
				const isSuggest = typeof suggestText === 'string' && suggestText.trim() !== ''
				if (!query) return
				if (!isSuggest && this.loading) return

				const userMessage = { role: 'user', content: query, timestamp: new Date() }
				this.messages.push(userMessage)
				this.inputText = ''
				this.scrollToBottom()

				this.loading = true
				this.streamingContent = ''
				const history = this.messages.slice(-10).map(m => ({
					role: m.role === 'user' ? 'user' : 'assistant',
					content: typeof m.content === 'string' ? m.content : String(m.content != null ? m.content : '')
				}))

				const body = JSON.stringify({ query, history, top_k: 5 })

				// H5：真实流式；小程序：先请求 /chat 再模拟逐字显示，既有效果又不乱码
				const useFetchStream = typeof fetch !== 'undefined' && typeof ReadableStream !== 'undefined'
				if (useFetchStream) {
					this.requestStream(body)
				} else {
					this.requestOneShotThenSimulateStream(body)
				}
			},
			async requestStream(body) {
				const url = `${this.ragBaseUrl}/chat/stream`
				this.streamingResponse = true
				this.messages.push({ role: 'ai', content: '', timestamp: new Date() })
				let fullContent = ''
				try {
					const res = await fetch(url, {
						method: 'POST',
						headers: { 'Content-Type': 'application/json' },
						body
					})
					if (!res.ok) {
						const err = await res.json().catch(() => ({}))
						throw new Error(err.detail || res.statusText)
					}
					const reader = res.body.getReader()
					const decoder = new TextDecoder()
					let buffer = ''
					while (true) {
						const { value, done } = await reader.read()
						if (done) break
						buffer += decoder.decode(value, { stream: true })
						const lines = buffer.split('\n')
						buffer = lines.pop() || ''
						for (const line of lines) {
							if (!line.trim()) continue
							try {
								const obj = JSON.parse(line)
								if (obj.type === 'chunk' && obj.content) {
									fullContent += obj.content
									this.streamingContent = fullContent
									const last = this.messages[this.messages.length - 1]
									if (last && last.role === 'ai') last.content = fullContent
									this.scrollToBottom()
								}
							} catch (e) {}
						}
					}
					const last = this.messages[this.messages.length - 1]
					if (last && last.role === 'ai') last.content = fullContent || '暂无回复。'
				} catch (e) {
					const errMsg = e && e.message ? e.message : '请求失败'
					const last = this.messages[this.messages.length - 1]
					if (last && last.role === 'ai') last.content = errMsg
					else this.messages.push({ role: 'ai', content: errMsg, timestamp: new Date() })
				}
				this.loading = false
				this.streamingContent = ''
				this.streamingResponse = false
				this.scrollToBottom()
			},
			// UTF-8 解码：只解码完整字符，末尾不完整字节留给下次（避免分片边界乱码）
			decodeUtf8Safe(arr) {
				if (!arr || arr.length === 0) return { str: '', remaining: new Uint8Array(0) }
				const a = arr instanceof Uint8Array ? arr : new Uint8Array(arr)
				let s = ''
				let i = 0
				while (i < a.length) {
					const b = a[i]
					if (b < 128) {
						s += String.fromCharCode(b)
						i += 1
					} else if (b >= 192 && b < 224) {
						if (i + 1 >= a.length) return { str: s, remaining: a.subarray(i) }
						s += String.fromCharCode(((b & 31) << 6) | (a[i + 1] & 63))
						i += 2
					} else if (b >= 224 && b < 240) {
						if (i + 2 >= a.length) return { str: s, remaining: a.subarray(i) }
						s += String.fromCharCode(((b & 15) << 12) | ((a[i + 1] & 63) << 6) | (a[i + 2] & 63))
						i += 3
					} else if (b >= 240 && b < 248) {
						if (i + 3 >= a.length) return { str: s, remaining: a.subarray(i) }
						const code = ((b & 7) << 18) | ((a[i + 1] & 63) << 12) | ((a[i + 2] & 63) << 6) | (a[i + 3] & 63)
						s += String.fromCharCode(0xD800 + (code - 0x10000 >> 10), 0xDC00 + (code & 0x3FF))
						i += 4
					} else {
						s += String.fromCharCode(b)
						i += 1
					}
				}
				return { str: s, remaining: new Uint8Array(0) }
			},
			requestStreamMiniProgram(body) {
				const url = `${this.ragBaseUrl}/chat/stream`
				this.streamingResponse = true
				this.messages.push({ role: 'ai', content: '', timestamp: new Date() })
				let fullContent = ''
				let lineBuffer = ''
				let byteBuffer = new Uint8Array(0)
				let dataObj = body
				try { dataObj = typeof body === 'string' ? JSON.parse(body) : body } catch (e) {}
				const self = this
				const req = uni.request({
					url,
					method: 'POST',
					timeout: 90000,
					data: dataObj,
					header: { 'Content-Type': 'application/json' },
					enableChunked: true,
					success: (res) => {
						if (res.statusCode !== 200) {
							const errMsg = (res.data && res.data.detail) ? String(res.data.detail) : '服务暂时不可用'
							const last = self.messages[self.messages.length - 1]
							if (last && last.role === 'ai') last.content = errMsg
						} else {
							const last = self.messages[self.messages.length - 1]
							if (last && last.role === 'ai') last.content = fullContent || '暂无回复。'
						}
					},
					fail: (err) => {
						const errMsg = (err.errMsg && err.errMsg.includes('timeout')) ? '请求超时，请稍后再试。' : `请确保 ai-doctor-rag 服务已启动（端口${this.ragPortHint}）。`
						const last = self.messages[self.messages.length - 1]
						if (last && last.role === 'ai') last.content = errMsg
						else self.messages.push({ role: 'ai', content: errMsg, timestamp: new Date() })
						uni.showToast({ title: err.errMsg && err.errMsg.includes('timeout') ? '请求超时' : '网络错误', icon: 'none' })
					},
					complete: () => {
						self.loading = false
						self.streamingContent = ''
						self.streamingResponse = false
						self.scrollToBottom()
					}
				})
				if (req && typeof req.onChunkReceived === 'function') {
					req.onChunkReceived((res) => {
						const raw = res.data
						if (!raw) return
						const newBytes = raw instanceof Uint8Array ? raw : new Uint8Array(raw)
						const combined = new Uint8Array(byteBuffer.length + newBytes.length)
						combined.set(byteBuffer)
						combined.set(newBytes, byteBuffer.length)
						const { str: chunk, remaining } = self.decodeUtf8Safe(combined)
						byteBuffer = remaining
						lineBuffer += chunk
						const lines = lineBuffer.split('\n')
						lineBuffer = lines.pop() || ''
						for (const line of lines) {
							if (!line.trim()) continue
							try {
								const obj = JSON.parse(line)
								if (obj.type === 'chunk' && obj.content) {
									fullContent += obj.content
									const last = self.messages[self.messages.length - 1]
									if (last && last.role === 'ai') last.content = fullContent
									self.scrollToBottom()
								}
							} catch (e) {}
						}
					})
				}
			},
			// 小程序：先请求 /chat 拿到完整回复，再定时逐段显示，实现“流式”效果且不乱码
			requestOneShotThenSimulateStream(body) {
				const dataObj = typeof body === 'string' ? (() => { try { return JSON.parse(body) } catch (e) { return {} } })() : body
				this.streamingResponse = true
				this.messages.push({ role: 'ai', content: '', timestamp: new Date() })
				const self = this
				uni.request({
					url: `${this.ragBaseUrl}/chat`,
					method: 'POST',
					timeout: 60000,
					data: dataObj,
					header: { 'Content-Type': 'application/json' },
					success: (res) => {
						let fullText = ''
						if (res.statusCode === 200 && res.data) {
							const raw = res.data.answer
							fullText = typeof raw === 'string' ? raw : (raw != null ? String(raw) : '抱歉，暂无回复。')
						} else {
							fullText = (res.data && res.data.detail) ? (typeof res.data.detail === 'string' ? res.data.detail : JSON.stringify(res.data.detail)) : '服务暂时不可用'
						}
						const last = self.messages[self.messages.length - 1]
						if (!last || last.role !== 'ai') return
						// 模拟流式：每 40ms 显示 2 个字符（中文友好）
						const step = 2
						const interval = 40
						let index = 0
						if (self._simulateStreamTimer) clearInterval(self._simulateStreamTimer)
						self._simulateStreamTimer = setInterval(() => {
							if (index >= fullText.length) {
								clearInterval(self._simulateStreamTimer)
								self._simulateStreamTimer = null
								last.content = fullText
								self.loading = false
								self.streamingResponse = false
								self.scrollToBottom()
								return
							}
							index += step
							last.content = fullText.slice(0, index)
							self.scrollToBottom()
						}, interval)
					},
					fail: (err) => {
						const errMsg = (err.errMsg && err.errMsg.includes('timeout')) ? '请求超时，请稍后再试。' : `请确保 ai-doctor-rag 服务已启动（端口${this.ragPortHint}）。`
						const last = self.messages[self.messages.length - 1]
						if (last && last.role === 'ai') last.content = errMsg
						else self.messages.push({ role: 'ai', content: errMsg, timestamp: new Date() })
						self.loading = false
						self.streamingResponse = false
						self.scrollToBottom()
						uni.showToast({ title: err.errMsg && err.errMsg.includes('timeout') ? '请求超时' : '网络错误', icon: 'none' })
					}
				})
			},
			requestOneShot(body) {
				const dataObj = typeof body === 'string' ? (() => { try { return JSON.parse(body) } catch (e) { return {} } })() : body
				uni.request({
					url: `${this.ragBaseUrl}/chat`,
					method: 'POST',
					timeout: 60000,
					data: dataObj,
					header: { 'Content-Type': 'application/json' },
					success: (res) => {
						this.loading = false
						if (res.statusCode === 200 && res.data) {
							const raw = res.data.answer
							const answer = typeof raw === 'string' ? raw : (raw != null ? String(raw) : '抱歉，暂无回复。')
							this.messages.push({ role: 'ai', content: answer, timestamp: new Date() })
						} else {
							const errMsg = (res.data && res.data.detail) ? (typeof res.data.detail === 'string' ? res.data.detail : JSON.stringify(res.data.detail)) : '服务暂时不可用'
							this.messages.push({ role: 'ai', content: errMsg, timestamp: new Date() })
						}
						this.scrollToBottom()
					},
					fail: (err) => {
						this.loading = false
						const errMsg = (err.errMsg && err.errMsg.includes('timeout')) ? '请求超时，请稍后再试。' : `请确保 ai-doctor-rag 服务已启动（端口${this.ragPortHint}）。`
						this.messages.push({ role: 'ai', content: errMsg, timestamp: new Date() })
						this.scrollToBottom()
						uni.showToast({ title: err.errMsg && err.errMsg.includes('timeout') ? '请求超时' : '网络错误', icon: 'none' })
					}
				})
			},
			mdToHtml(text) {
				if (!text || typeof text !== 'string') return ''
				let s = text
					.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
				s = s.replace(/^### (.+)$/gm, '<h3 style="font-size:1.1em;margin:0.6em 0 0.3em;">$1</h3>')
				s = s.replace(/^## (.+)$/gm, '<h2 style="font-size:1.2em;margin:0.7em 0 0.35em;">$1</h2>')
				s = s.replace(/^# (.+)$/gm, '<h1 style="font-size:1.3em;margin:0.8em 0 0.4em;">$1</h1>')
				s = s.replace(/\*\*(.+?)\*\*/g, '<b>$1</b>')
				s = s.replace(/\n/g, '<br/>')
				return s
			},
			onSendTap() {
				this.sendMessage()
			},
			scrollToBottom() {
				this.$nextTick(() => { this.scrollTop = 99999 })
			},
			formatTime(timestamp) {
				if (!timestamp) return ''
				const d = new Date(timestamp)
				return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
			},
			clearChatHistory() {
				uni.showModal({
					title: '确认清空',
					content: '确定要清空所有聊天记录吗？',
					success: (res) => {
						if (res.confirm) {
							if (this.chatLang === 'zh') {
								this.messagesZh = []
							} else {
								this.messagesEn = []
							}
							uni.showToast({ title: this.chatLang === 'zh' ? '已清空' : 'Cleared', icon: 'success' })
						}
					}
				})
			},
			getMessageContent(msg) {
				if (!msg || msg.content == null) return ''
				return typeof msg.content === 'string' ? msg.content : String(msg.content)
			},
			onSuggestTap(q) {
				const text = typeof q === 'string' ? q : ''
				if (text) this.sendMessage(text)
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page-root { min-height: 100vh; background: transparent; }
	.container {
		display: flex;
		flex-direction: column;
		height: 100vh;
		min-height: 100vh;
		padding-bottom: 0;
		background: transparent;
		position: relative;
	}
	/* 与首页一致的漂浮光斑背景 */
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
	.navbar, .greeting-card, .section-card, .chat-scroll, .input-area { position: relative; z-index: 1; }

	.navbar {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 24rpx 32rpx;
		background: rgba(255, 255, 255, 0.92);
		backdrop-filter: blur(14rpx);
		border-bottom: 1rpx solid #e5e5e5;
	}
	.navbar-left, .navbar-right { width: 80rpx; }
	.navbar-center { flex: 1; text-align: center; }
	.navbar-title { font-size: 36rpx; font-weight: 600; color: #0f172a; }
	.clear-btn { padding: 8rpx; }

	.lang-tabs {
		display: flex;
		margin: 16rpx 24rpx 0;
		padding: 6rpx;
		background: rgba(255, 255, 255, 0.88);
		border-radius: 999rpx;
		box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.06);
		position: relative;
		z-index: 1;
	}
	.lang-tab {
		flex: 1;
		text-align: center;
		padding: 18rpx 16rpx;
		font-size: 28rpx;
		color: #64748b;
		border-radius: 999rpx;
	}
	.lang-tab.active {
		background: linear-gradient(135deg, #07C160 0%, #06A050 100%);
		color: #fff;
		font-weight: 600;
	}

	/* 图四风格：问候卡片 */
	.greeting-card {
		margin: 24rpx;
		padding: 32rpx;
		background: rgba(255, 255, 255, 0.92);
		border-radius: 24rpx;
		box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
	}
	.greeting-main { display: flex; align-items: center; gap: 8rpx; margin-bottom: 12rpx; }
	.greeting-title { font-size: 36rpx; font-weight: 700; color: #07C160; }
	.greeting-sparkle { font-size: 32rpx; }
	.greeting-sub { font-size: 28rpx; color: #666; }

	/* 图四风格：推荐问题区块 */
	.section-card {
		margin: 0 24rpx 24rpx;
		padding: 24rpx;
		background: #fff;
		border-radius: 24rpx;
		box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
	}
	.section-header { display: flex; align-items: center; margin-bottom: 20rpx; }
	.green-line { width: 6rpx; height: 32rpx; background: #07C160; border-radius: 3rpx; margin-right: 12rpx; }
	.section-title { font-size: 32rpx; font-weight: 600; color: #333; }
	.suggest-card {
		display: flex;
		align-items: center;
		padding: 24rpx 20rpx;
		background: #fff;
		border-radius: 16rpx;
		margin-bottom: 12rpx;
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
		border: 1rpx solid #f0f0f0;
	}
	.suggest-card:last-child { margin-bottom: 0; }
	.suggest-hash { font-size: 28rpx; color: #722ed1; margin-right: 16rpx; font-weight: 600; }
	.suggest-text { flex: 1; font-size: 28rpx; color: #333; }
	.suggest-card .uni-icons { flex-shrink: 0; }

	.chat-scroll {
		flex: 1;
		height: 0;
		min-height: 0;
		padding: 20rpx 24rpx;
		/* 留出底部空间，避免最后一条消息被固定聊天框遮住 */
		padding-bottom: 220rpx;
		box-sizing: border-box;
	}
	.chat-messages { display: flex; flex-direction: column; padding-bottom: 24rpx; }
	.message-item { display: flex; margin-bottom: 32rpx; align-items: flex-start; }
	.message-user { flex-direction: row-reverse; }
	.message-avatar {
		width: 56rpx; height: 56rpx; border-radius: 50%;
		background: #f0f0f0;
		display: flex; align-items: center; justify-content: center;
		flex-shrink: 0; margin: 0 12rpx;
	}
	/* 用户气泡在右、AI 在左，宽度适配手机，单行不超约 78% */
	.message-content {
		flex: 1;
		min-width: 0;
		max-width: 78%;
	}
	.message-bubble {
		padding: 20rpx 24rpx; border-radius: 20rpx;
		word-break: break-word; line-height: 1.6;
	}
	.user-bubble {
		background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
		color: #fff; border-bottom-right-radius: 6rpx;
	}
	.ai-bubble {
		background: #fff; color: #333;
		border-bottom-left-radius: 6rpx;
		box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.08);
	}
	.message-text { font-size: 28rpx; white-space: pre-wrap; }
	.message-richtext { font-size: 28rpx; line-height: 1.6; word-break: break-word; display: block; }
	.message-time { font-size: 22rpx; color: #999; margin-top: 8rpx; }
	.message-user .message-time { text-align: right; }

	/* 聊天框始终固定在 AI 聊天页面下方（在 tabbar 上方），不遮挡问答内容 */
	.input-area {
		position: fixed;
		left: 0; right: 0; bottom: 98rpx;
		background: rgba(255, 255, 255, 0.98);
		backdrop-filter: blur(12rpx);
		border-top: 1rpx solid #e5e5e5;
		padding: 20rpx 24rpx;
		padding-bottom: calc(20rpx + env(safe-area-inset-bottom, 0));
		z-index: 10;
	}
	.input-row { display: flex; align-items: flex-end; gap: 20rpx; width: 100%; box-sizing: border-box; }
	.input-textarea {
		flex: 1;
		min-width: 0;
		min-height: 72rpx;
		max-height: 240rpx;
		padding: 20rpx 24rpx;
		background: #f5f5f5;
		border-radius: 36rpx;
		font-size: 28rpx;
		line-height: 1.5;
		box-sizing: border-box;
	}
	.send-btn {
		width: 80rpx; height: 80rpx; border-radius: 50%;
		background: linear-gradient(135deg, #07C160 0%, #06A050 100%);
		display: flex; align-items: center; justify-content: center;
		flex-shrink: 0;
	}
	.send-btn.disabled { background: #ccc; opacity: 0.6; }
	.loading-text { font-size: 28rpx; color: #666; margin-bottom: 8rpx; }
	.typing-indicator { display: flex; gap: 8rpx; }
	.typing-dot {
		width: 12rpx; height: 12rpx; border-radius: 50%;
		background: #999;
		animation: typing 1.4s infinite;
	}
	.typing-dot:nth-child(2) { animation-delay: 0.2s; }
	.typing-dot:nth-child(3) { animation-delay: 0.4s; }
	@keyframes typing {
		0%, 60%, 100% { transform: translateY(0); opacity: 0.7; }
		30% { transform: translateY(-10rpx); opacity: 1; }
	}
</style>
