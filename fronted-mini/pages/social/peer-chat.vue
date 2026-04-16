<template>
	<view class="page-root">
		<view class="custom-navbar">
			<view class="navbar-content">
				<view class="navbar-left" @tap="goBack">
					<uni-icons type="left" size="20" color="#333"></uni-icons>
				</view>
				<view class="navbar-title">{{ peerName || '病友交流' }}</view>
				<view class="navbar-right"></view>
			</view>
		</view>

		<scroll-view scroll-y class="chat-scroll" :scroll-top="scrollTop" scroll-with-animation>
			<view class="chat-messages">
				<view v-if="messages.length === 0" class="hint">互相关注后可交流病情，消息仅双方可见。</view>
				<view
					class="msg-row"
					:class="m.fromUserId === userId ? 'msg-row-me' : 'msg-row-peer'"
					v-for="m in messages"
					:key="m.id"
				>
					<view class="avatar-wrap" v-if="m.fromUserId !== userId">
						<image v-if="peerAvatar" class="msg-avatar" :src="peerAvatar" mode="aspectFill" />
						<view v-else class="msg-avatar msg-avatar-placeholder">
							<uni-icons type="person-filled" size="22" color="#94a3b8"></uni-icons>
						</view>
					</view>
					<view class="msg-col">
						<view class="bubble" :class="m.fromUserId === userId ? 'b-me' : 'b-peer'">
							<text class="txt">{{ m.content }}</text>
						</view>
						<text class="time">{{ formatTime(m.createdAt || m.createTime) }}</text>
					</view>
					<view class="avatar-wrap" v-if="m.fromUserId === userId">
						<image v-if="myAvatar" class="msg-avatar" :src="myAvatar" mode="aspectFill" />
						<view v-else class="msg-avatar msg-avatar-placeholder is-me">
							<uni-icons type="person-filled" size="22" color="#fff"></uni-icons>
						</view>
					</view>
				</view>
			</view>
		</scroll-view>

		<view class="input-area">
			<view class="input-inner">
				<textarea
					class="input-textarea"
					v-model="inputText"
					placeholder="发送病情交流消息..."
					:auto-height="true"
					:maxlength="2000"
					:show-confirm-bar="false"
					:cursor-spacing="20"
				/>
				<view class="send-btn" :class="{ disabled: !canSend }" @tap="send">
					<text class="send-text">发送</text>
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
				userId: null,
				peerId: null,
				peerName: '',
				peerAvatar: '',
				myAvatar: '',
				messages: [],
				inputText: '',
				scrollTop: 0,
				pollTimer: null,
				lastMsgId: 0
			}
		},
		computed: {
			canSend() {
				return this.inputText.trim().length > 0 && this.peerId != null && this.userId != null
			}
		},
		onLoad(q) {
			const userInfo = uni.getStorageSync('userInfo') || {}
			this.userId = userInfo.userId != null ? userInfo.userId : null
			this.myAvatar = userInfo.avatarUrl || ''
			this.peerId = q.peerId ? parseInt(q.peerId, 10) : null
			this.peerName = q.peerName ? decodeURIComponent(q.peerName) : ''
			this.peerAvatar = q.peerAvatar ? decodeURIComponent(q.peerAvatar) : ''
			if (!this.userId || !this.peerId) {
				uni.showToast({ title: '参数错误', icon: 'none' })
				setTimeout(() => uni.navigateBack(), 800)
			}
		},
		onShow() {
			this.loadMessages(true)
			this.pollTimer = setInterval(() => this.loadMessages(false), 4000)
		},
		onHide() {
			if (this.pollTimer) {
				clearInterval(this.pollTimer)
				this.pollTimer = null
			}
		},
		onUnload() {
			if (this.pollTimer) clearInterval(this.pollTimer)
		},
		methods: {
			goBack() {
				uni.navigateBack()
			},
			parseMessageDate(t) {
				if (t == null || t === '') return null
				if (typeof t === 'number') {
					const ms = t < 1e12 ? t * 1000 : t
					const d = new Date(ms)
					return isNaN(d.getTime()) ? null : d
				}
				if (Array.isArray(t) && t.length >= 3) {
					const y = t[0]
					const mo = t[1]
					const day = t[2]
					const h = t.length > 3 ? t[3] : 0
					const mi = t.length > 4 ? t[4] : 0
					const s = t.length > 5 ? t[5] : 0
					return new Date(y, mo - 1, day, h, mi, s)
				}
				if (typeof t === 'object' && t !== null) {
					if (typeof t.getTime === 'function') {
						const d = t
						return isNaN(d.getTime()) ? null : d
					}
				}
				const s = String(t).trim()
				if (!s) return null
				const d = new Date(s.includes('T') ? s : s.replace(/-/g, '/'))
				return isNaN(d.getTime()) ? null : d
			},
			formatTime(t) {
				const d = this.parseMessageDate(t)
				if (!d) return ''
				const h = d.getHours().toString().padStart(2, '0')
				const min = d.getMinutes().toString().padStart(2, '0')
				return `${h}:${min}`
			},
			loadMessages(reset) {
				if (!this.userId || !this.peerId) return
				const data = {
					userId: this.userId,
					peerId: this.peerId,
					limit: 200
				}
				if (!reset && this.lastMsgId > 0) {
					data.sinceId = this.lastMsgId
				}
				uni.request({
					url: `${config.baseUrl}/social/peer/messages/conversation`,
					method: 'GET',
					data,
					success: (res) => {
						if (res.statusCode !== 200 || !res.data || res.data.code !== 200) return
						const list = res.data.data || []
						if (reset) {
							this.messages = list
						} else if (list.length) {
							const existing = new Set(this.messages.map((m) => m.id))
							for (const m of list) {
								if (!existing.has(m.id)) this.messages.push(m)
							}
							this.messages.sort((a, b) => (a.id || 0) - (b.id || 0))
						}
						if (this.messages.length) {
							this.lastMsgId = this.messages[this.messages.length - 1].id
						}
						this.$nextTick(() => {
							this.scrollTop = 999999
						})
					}
				})
			},
			send() {
				if (!this.canSend) return
				const content = this.inputText.trim()
				this.inputText = ''
				uni.request({
					url: `${config.baseUrl}/social/peer/messages`,
					method: 'POST',
					header: { 'Content-Type': 'application/json' },
					data: {
						fromUserId: this.userId,
						toUserId: this.peerId,
						content
					},
					success: (res) => {
						if (res.statusCode === 200 && res.data && res.data.code === 200) {
							this.loadMessages(true)
						} else {
							const msg = (res.data && res.data.message) || '发送失败'
							uni.showToast({ title: msg, icon: 'none' })
						}
					},
					fail: () => uni.showToast({ title: '网络错误', icon: 'none' })
				})
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page-root {
		min-height: 100vh;
		background: linear-gradient(180deg, #eef2f7 0%, #e8edf3 40%, #f0f4f8 100%);
		display: flex;
		flex-direction: column;
	}
	.custom-navbar {
		padding-top: env(safe-area-inset-top);
		background: rgba(255, 255, 255, 0.92);
		backdrop-filter: blur(12px);
		border-bottom: 1rpx solid rgba(0, 0, 0, 0.06);
	}
	.navbar-content {
		height: 88rpx;
		display: flex;
		align-items: center;
		padding: 0 24rpx;
	}
	.navbar-left { width: 80rpx; }
	.navbar-title { flex: 1; text-align: center; font-size: 32rpx; font-weight: 600; color: #1a1a1a; }
	.navbar-right { width: 80rpx; }
	.chat-scroll { flex: 1; height: 0; padding: 24rpx 20rpx 220rpx; box-sizing: border-box; }
	.hint {
		font-size: 24rpx;
		color: #8a94a6;
		text-align: center;
		padding: 48rpx 32rpx;
		line-height: 1.6;
	}
	.msg-row {
		display: flex;
		align-items: flex-end;
		margin-bottom: 28rpx;
		gap: 16rpx;
	}
	.msg-row-me {
		flex-direction: row;
		justify-content: flex-end;
	}
	.msg-row-peer {
		flex-direction: row;
		justify-content: flex-start;
	}
	.avatar-wrap {
		flex-shrink: 0;
	}
	.msg-avatar {
		width: 72rpx;
		height: 72rpx;
		border-radius: 50%;
		background: #e8ecf0;
	}
	.msg-avatar-placeholder {
		display: flex;
		align-items: center;
		justify-content: center;
		background: linear-gradient(145deg, #e2e8f0, #cbd5e1);
	}
	.msg-avatar-placeholder.is-me {
		background: linear-gradient(145deg, #34d399, #059669);
	}
	.msg-col {
		max-width: calc(100% - 120rpx);
		display: flex;
		flex-direction: column;
		align-items: inherit;
	}
	.msg-row-me .msg-col {
		align-items: flex-end;
	}
	.msg-row-peer .msg-col {
		align-items: flex-start;
	}
	.bubble {
		padding: 22rpx 26rpx;
		border-radius: 20rpx;
		box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
	}
	.b-me {
		background: linear-gradient(135deg, #22c55e 0%, #16a34a 100%);
		border-bottom-right-radius: 6rpx;
	}
	.b-peer {
		background: #fff;
		border: 1rpx solid rgba(0, 0, 0, 0.06);
		border-bottom-left-radius: 6rpx;
	}
	.txt {
		font-size: 28rpx;
		line-height: 1.55;
		color: #1e293b;
		word-break: break-word;
	}
	.b-me .txt {
		color: #fff;
	}
	.time {
		font-size: 20rpx;
		color: #94a3b8;
		margin-top: 10rpx;
		padding: 0 4rpx;
	}
	.input-area {
		position: fixed;
		left: 0;
		right: 0;
		bottom: 0;
		padding: 12rpx 20rpx calc(12rpx + env(safe-area-inset-bottom));
		background: rgba(255, 255, 255, 0.96);
		backdrop-filter: blur(16px);
		border-top: 1rpx solid rgba(0, 0, 0, 0.06);
		box-shadow: 0 -8rpx 32rpx rgba(15, 23, 42, 0.06);
	}
	.input-inner {
		display: flex;
		align-items: flex-end;
		gap: 16rpx;
	}
	.input-textarea {
		flex: 1;
		min-height: 80rpx;
		max-height: 220rpx;
		font-size: 28rpx;
		padding: 20rpx 24rpx;
		background: #f1f5f9;
		border-radius: 20rpx;
		border: 1rpx solid transparent;
	}
	.send-btn {
		min-width: 120rpx;
		height: 80rpx;
		padding: 0 24rpx;
		background: linear-gradient(135deg, #22c55e 0%, #16a34a 100%);
		border-radius: 20rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 8rpx 20rpx rgba(22, 163, 74, 0.35);
	}
	.send-text {
		font-size: 28rpx;
		color: #fff;
		font-weight: 600;
	}
	.send-btn.disabled {
		opacity: 0.45;
		box-shadow: none;
	}
</style>
