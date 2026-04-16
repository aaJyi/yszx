<template>
	<view class="page-root">
		<!-- 未识别：首页（说明 + 开始按钮） -->
		<view v-if="!recognizing" class="container">
			<view class="navbar">
				<view class="navbar-left"></view>
				<view class="navbar-center">
					<text class="navbar-title">运动</text>
				</view>
				<view class="navbar-right"></view>
			</view>
			<!-- 竖屏比例摄像头预览（9:16，便于全身入镜） -->
			<view class="preview-camera-section">
				<view class="preview-camera-header">
					<view class="green-line"></view>
					<text class="preview-camera-title">摄像头预览</text>
				</view>
				<view class="preview-camera-wrap">
					<camera
						id="exerciseCamera"
						class="preview-camera"
						device-position="front"
						flash="off"
						@error="onCameraError"
					></camera>
				</view>
				<view class="preview-camera-tip">将身体置于画面内，点击下方开始识别</view>
			</view>
			<view class="action-row">
				<button class="btn-start" @tap="startRecognize">开始识别</button>
			</view>
			<view class="action-row">
				<button class="btn-video" :disabled="videoUploading" @tap="chooseVideoAndUpload">
					{{ videoUploading ? '分析中…' : '导入视频识别' }}
				</button>
			</view>

			<!-- 此次运动记录（结束识别后展示） -->
				<view v-if="sessionSummary && sessionSummary.length > 0" class="result-card">
				<view class="result-header">
					<view class="green-line"></view>
					<text class="result-title">此次运动记录</text>
				</view>
				<view class="result-body">
					<view v-for="(item, idx) in sessionSummary" :key="idx" class="result-row">
						<text class="result-label">{{ item.name }}：</text>
						<text class="result-value highlight">{{ item.displayText }}</text>
					</view>
					<view class="result-row">
						<text class="result-label">总时长：</text>
						<text class="result-value">{{ sessionTotalText }}</text>
					</view>
				</view>
			</view>

			<!-- 视频识别结果 -->
			<view v-if="videoSummary && videoSummary.length > 0" class="result-card video-result">
				<view class="result-header">
					<view class="green-line"></view>
					<text class="result-title">视频识别结果</text>
				</view>
				<view class="result-body">
					<view v-for="(item, idx) in videoSummary" :key="idx" class="result-row">
						<text class="result-label">{{ item.name }}：</text>
						<text class="result-value highlight">{{ item.displayText }}</text>
					</view>
					<view class="result-row">
						<text class="result-label">视频时长：</text>
						<text class="result-value">{{ videoTotalDurationText }}</text>
					</view>
				</view>
			</view>
			<view v-if="videoPreviewUrl" class="result-card video-preview-card">
				<view class="result-header">
					<view class="green-line"></view>
					<text class="result-title">识别过程视频（标注）</text>
				</view>
				<video
					class="result-video"
					:src="videoPreviewUrl"
					controls
					show-center-play-btn
					:autoplay="false"
					:loop="false"
					object-fit="contain"
				></video>
			</view>
		</view>

		<!-- 识别中：竖屏全屏摄像头 + 右上角实时 + 结束按钮 -->
		<view v-else class="fullscreen-wrap">
			<camera
				id="exerciseCamera"
				class="fullscreen-camera"
				device-position="front"
				flash="off"
				@error="onCameraError"
			></camera>
			<!-- #ifdef MP-WEIXIN -->
			<cover-view class="overlay-marked" v-if="annotatedFrameDataUrl">
				<cover-view class="overlay-marked-title">识别过程标记</cover-view>
				<cover-image class="overlay-marked-image" :src="annotatedFrameDataUrl"></cover-image>
			</cover-view>
			<cover-view class="overlay-top-right">
				<cover-view class="overlay-title">当前：{{ displayCurrentText }}</cover-view>
				<cover-view class="overlay-list">
					<cover-view v-for="(item, idx) in liveStatsList" :key="idx" class="overlay-item">
						<cover-view class="overlay-name">{{ item.name }}</cover-view>
						<cover-view class="overlay-value">{{ item.text }}</cover-view>
					</cover-view>
				</cover-view>
				<cover-view class="overlay-duration">本次 {{ sessionDurationText }}</cover-view>
			</cover-view>
			<cover-view class="btn-end-wrap">
				<cover-view class="btn-end-cover" @tap="stopRecognize">结束运动</cover-view>
			</cover-view>
			<!-- #endif -->
			<!-- #ifndef MP-WEIXIN -->
			<view class="overlay-marked" v-if="annotatedFrameDataUrl">
				<view class="overlay-marked-title">识别过程标记</view>
				<image class="overlay-marked-image" :src="annotatedFrameDataUrl" mode="aspectFill"></image>
			</view>
			<view class="overlay-top-right">
				<view class="overlay-title">当前：{{ displayCurrentText }}</view>
				<view class="overlay-list">
					<view v-for="(item, idx) in liveStatsList" :key="idx" class="overlay-item">
						<text class="overlay-name">{{ item.name }}</text>
						<text class="overlay-value">{{ item.text }}</text>
					</view>
				</view>
				<view class="overlay-duration">本次 {{ sessionDurationText }}</view>
			</view>
			<view class="btn-end-wrap">
				<button class="btn-end" @tap="stopRecognize">结束运动</button>
			</view>
			<!-- #endif -->
		</view>

		<custom-tabbar v-show="!recognizing" :current="2"></custom-tabbar>
	</view>
</template>

<script>
	import customTabbar from '@/components/custom-tabbar/custom-tabbar.vue'
	import config from '@/utils/config.js'

	const CONF_THRESHOLD = 0.6
	const STABLE_FRAMES = 1
	const EXERCISE_CN = { high_jumps: '高抬腿', jumping_jacks: '开合跳', lunges: '弓箭步', squats: '深蹲' }

	function formatDuration(seconds) {
		if (seconds < 60) return Math.round(seconds) + ' 秒'
		const m = Math.floor(seconds / 60)
		const s = Math.round(seconds % 60)
		return s > 0 ? m + ' 分 ' + s + ' 秒' : m + ' 分钟'
	}

	function formatStat(reps, seconds) {
		const t = formatDuration(seconds)
		if (reps > 0) return reps + ' 个 / ' + t
		return t
	}

	export default {
		components: { customTabbar },
		data() {
			return {
				recognizing: false,
				timerId: null,
				sessionStart: 0,
				sessionId: '',
				lastResult: { exerciseType: null, exerciseTypeCn: '', confidence: 0, reps: 0, exerciseStats: [] },
				lastResultsBuffer: [],
				currentExercise: null,
				exerciseStartTime: 0,
				exerciseStats: {},
				sessionSummary: [],
				videoUploading: false,
				videoSummary: [],
				videoTotalDuration: 0,
				annotatedFrameDataUrl: '',
				videoPreviewUrl: ''
			}
		},
		computed: {
			videoTotalDurationText() {
				return formatDuration(this.videoTotalDuration || 0)
			},
			displayCurrentText() {
				if (this.lastResult.exerciseTypeCn) {
					const r = this.lastResult.reps
					return r > 0 ? this.lastResult.exerciseTypeCn + ' (' + r + ' 个)' : this.lastResult.exerciseTypeCn
				}
				return '等待识别…'
			},
			currentExerciseCn() {
				return this.currentExercise ? (EXERCISE_CN[this.currentExercise] || this.currentExercise) : ''
			},
			sessionDurationText() {
				if (!this.sessionStart || !this.recognizing) return '0 秒'
				const sec = Math.floor((Date.now() - this.sessionStart) / 1000)
				return formatDuration(sec)
			},
			liveStatsList() {
				if (this.lastResult.exerciseStats && this.lastResult.exerciseStats.length > 0) {
					return this.lastResult.exerciseStats.map((e) => ({
						name: e.exerciseTypeCn || EXERCISE_CN[e.exerciseType] || e.exerciseType,
						text: formatStat(e.reps || 0, e.seconds || 0)
					}))
				}
				const list = []
				for (const type in this.exerciseStats) {
					const s = this.exerciseStats[type]
					list.push({ name: EXERCISE_CN[type] || type, text: formatStat(s.reps || 0, s.seconds || 0) })
				}
				return list
			},
			sessionTotalText() {
				let total = 0
				this.sessionSummary.forEach((item) => { total += item.seconds })
				return formatDuration(total)
			},
		},
		methods: {
			onCameraError(e) {
				console.error('摄像头错误', e)
				uni.showToast({ title: '摄像头不可用或未授权', icon: 'none' })
			},
			startRecognize() {
				this.recognizing = true
				this.sessionStart = Date.now()
				this.exerciseStartTime = Date.now()
				this.exerciseStats = {}
				this.currentExercise = null
				this.lastResultsBuffer = []
				this.sessionId = ''
				this.lastResult = { exerciseType: null, exerciseTypeCn: '', confidence: 0, reps: 0, exerciseStats: [] }
				this.sessionSummary = []
				this.annotatedFrameDataUrl = ''
				this.$nextTick(() => {
					const interval = 2000
					const doCapture = () => {
						if (!this.recognizing) return
						this.doCaptureAndUpload()
					}
					doCapture()
					this.timerId = setInterval(doCapture, interval)
				})
				uni.showToast({ title: '已开始识别', icon: 'none' })
			},
			stopRecognize() {
				const now = Date.now()
				if (this.currentExercise && this.exerciseStartTime) {
					const dur = (now - this.exerciseStartTime) / 1000
					if (!this.exerciseStats[this.currentExercise]) this.exerciseStats[this.currentExercise] = { seconds: 0, reps: 0 }
					this.exerciseStats[this.currentExercise].seconds += dur
				}
				this.sessionSummary = []
				if (this.lastResult.exerciseStats && this.lastResult.exerciseStats.length > 0) {
					this.lastResult.exerciseStats.forEach((e) => {
						if ((e.seconds || 0) > 0 || (e.reps || 0) > 0) {
							this.sessionSummary.push({
								type: e.exerciseType,
								name: e.exerciseTypeCn || EXERCISE_CN[e.exerciseType] || e.exerciseType,
								seconds: e.seconds || 0,
								reps: e.reps || 0,
								displayText: formatStat(e.reps || 0, e.seconds || 0)
							})
						}
					})
				}
				if (this.sessionSummary.length === 0) {
					for (const type in this.exerciseStats) {
						const s = this.exerciseStats[type]
						const sec = s.seconds || 0
						if (sec > 0) this.sessionSummary.push({ type, name: EXERCISE_CN[type] || type, seconds: sec, reps: s.reps || 0, displayText: formatStat(s.reps || 0, sec) })
					}
				}
				this.recognizing = false
				this.annotatedFrameDataUrl = ''
				if (this.timerId) {
					clearInterval(this.timerId)
					this.timerId = null
				}
				uni.showToast({ title: '已结束运动', icon: 'none' })
			},
			applyStableFrames(res) {
				this.lastResult = {
					exerciseType: res.exerciseType,
					exerciseTypeCn: res.exerciseTypeCn || '',
					confidence: res.confidence || 0,
					reps: res.reps != null ? res.reps : 0,
					exerciseStats: res.exerciseStats || []
				}
				if (res.sessionId) this.sessionId = res.sessionId
				const type = res.exerciseType
				const conf = res.confidence || 0
				if (conf < CONF_THRESHOLD || !type) return
				this.lastResultsBuffer.push({ exerciseType: type, confidence: conf })
				if (this.lastResultsBuffer.length > STABLE_FRAMES) this.lastResultsBuffer.shift()
				if (this.lastResultsBuffer.length !== STABLE_FRAMES) return
				const allSame = this.lastResultsBuffer.every((r) => r.exerciseType === type)
				if (!allSame) return
				const now = Date.now()
				if (type !== this.currentExercise) {
					if (this.currentExercise && this.exerciseStartTime) {
						const dur = (now - this.exerciseStartTime) / 1000
						if (!this.exerciseStats[this.currentExercise]) this.exerciseStats[this.currentExercise] = { seconds: 0, reps: 0 }
						this.exerciseStats[this.currentExercise].seconds += dur
					}
					this.currentExercise = type
					this.exerciseStartTime = now
				}
			},
			doCaptureAndUpload() {
				// #ifdef MP-WEIXIN
				const ctx = uni.createCameraContext('exerciseCamera')
				ctx.takePhoto({
					quality: 'normal',
					success: (res) => this.uploadAndRecognize(res.tempImagePath),
					fail: (err) => console.error('拍照失败', err)
				})
				// #endif
				// #ifndef MP-WEIXIN
				uni.showToast({ title: '请使用微信小程序使用摄像头识别', icon: 'none' })
				// #endif
			},
			chooseVideoAndUpload() {
				if (this.videoUploading) return
				// #ifdef MP-WEIXIN
				uni.chooseMedia({
					count: 1,
					mediaType: ['video'],
					sourceType: ['album', 'camera'],
					maxDuration: 1800,
					success: (res) => {
						const file = res.tempFiles && res.tempFiles[0]
						if (!file || !file.tempFilePath) {
							uni.showToast({ title: '未选择视频', icon: 'none' })
							return
						}
						this.uploadVideoAndRecognize(file.tempFilePath)
					},
					fail: (err) => {
						const msg = (err && err.errMsg) ? String(err.errMsg) : ''
						// 某些机型/编码下 chooseMedia 会失败，回退到 chooseVideo
						if (msg && msg.includes('cancel')) return
						console.warn('chooseMedia 失败，回退 chooseVideo:', msg)
						uni.chooseVideo({
							sourceType: ['album', 'camera'],
							maxDuration: 1800,
							success: (v) => {
								if (v && v.tempFilePath) {
									this.uploadVideoAndRecognize(v.tempFilePath)
								} else {
									uni.showToast({ title: '未选择视频', icon: 'none' })
								}
							},
							fail: (e2) => {
								const m2 = (e2 && e2.errMsg) ? String(e2.errMsg) : '选择视频失败'
								uni.showToast({ title: m2.slice(0, 18), icon: 'none', duration: 3000 })
							}
						})
						if (msg) {
							uni.showToast({ title: msg.slice(0, 18), icon: 'none', duration: 2500 })
						}
					}
				})
				// #endif
				// #ifndef MP-WEIXIN
				uni.chooseVideo({
					sourceType: ['album', 'camera'],
					maxDuration: 300,
					success: (res) => {
						if (res.tempFilePath) this.uploadVideoAndRecognize(res.tempFilePath)
					}
				})
				// #endif
			},
			uploadVideoAndRecognize(filePath) {
				this.videoUploading = true
				this.videoSummary = []
				this.videoTotalDuration = 0
				this.videoPreviewUrl = ''
				uni.showLoading({ title: '上传并分析中…', mask: true })
				uni.uploadFile({
					url: config.baseUrl + '/exercise-train/recognize-video',
					filePath: filePath,
					name: 'file',
					header: { 'Content-Type': 'multipart/form-data' },
					timeout: 300000,
					success: (res) => {
						uni.hideLoading()
						this.videoUploading = false
						try {
							const data = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
							if (data.code === 200 && data.data) {
								const list = (data.data.exerciseStats || []).map((e) => ({
									name: e.exerciseTypeCn || EXERCISE_CN[e.exerciseType] || e.exerciseType,
									displayText: formatStat(e.reps || 0, e.seconds || 0)
								}))
								this.videoSummary = list
								this.videoTotalDuration = data.data.totalDuration || 0
								const token = data.data.annotatedToken
								if (token) {
									this.videoPreviewUrl = `${config.baseUrl}/exercise-train/annotated-video/${encodeURIComponent(token)}?t=${Date.now()}`
								}
								uni.showToast({ title: '分析完成', icon: 'success' })
							} else {
								uni.showToast({ title: data.message || data.data?.message || '分析失败', icon: 'none' })
							}
						} catch (e) {
							console.error('解析视频识别结果失败', e)
							uni.showToast({ title: '解析结果失败', icon: 'none' })
						}
					},
					fail: (err) => {
						uni.hideLoading()
						this.videoUploading = false
						console.error('视频上传/识别失败', err)
						uni.showToast({ title: err.errMsg || '上传或识别失败', icon: 'none', duration: 3000 })
					}
				})
			},
			uploadAndRecognize(filePath) {
				const url = config.baseUrl + '/exercise-train/recognize' + (this.sessionId ? '?sessionId=' + encodeURIComponent(this.sessionId) : '')
				uni.uploadFile({
					url: url,
					filePath: filePath,
					name: 'file',
					formData: this.sessionId ? { sessionId: this.sessionId } : {},
					header: { 'Content-Type': 'multipart/form-data' },
					success: (res) => {
						try {
							const data = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
							if (data.code === 200 && data.data) {
								if (data.data.annotatedImageBase64) {
									this.annotatedFrameDataUrl = 'data:image/jpeg;base64,' + data.data.annotatedImageBase64
								}
								this.applyStableFrames({
									exerciseType: data.data.exerciseType,
									exerciseTypeCn: data.data.exerciseTypeCn || '',
									confidence: data.data.confidence || 0,
									reps: data.data.reps,
									exerciseStats: data.data.exerciseStats,
									sessionId: data.data.sessionId
								})
							}
						} catch (e) {
							console.error('解析识别结果失败', e)
						}
					},
					fail: (err) => {
						console.error('上传识别失败', err)
						const msg = (err.errMsg || '').indexOf('CONNECTION_REFUSED') >= 0 || err.errno === 600001
							? '真机请将 utils/config.js 中 WEIXIN_BASE_URL 改为本机局域网 IP'
							: '识别请求失败，请检查后端与 run_server'
						uni.showToast({ title: msg, icon: 'none', duration: 3500 })
					}
				})
			}
		},
		onUnload() {
			if (this.recognizing) this.stopRecognize()
		}
	}
</script>

<style lang="scss" scoped>
	.page-root { min-height: 100vh; background: transparent; }
	.container {
		min-height: 100vh;
		padding-bottom: 120rpx;
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
	.preview-camera-section, .action-row, .result-card, .video-result-card { position: relative; z-index: 1; }
	/* 竖屏预览：控制高度，一屏内可露出下方两个按钮 */
	.preview-camera-section {
		margin: 16rpx;
		background: #fff;
		border-radius: 24rpx;
		overflow: hidden;
		box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
	}
	.preview-camera-header {
		display: flex;
		align-items: center;
		padding: 16rpx 24rpx;
		border-bottom: 1rpx solid #f0f0f0;
	}
	.preview-camera-title { font-size: 30rpx; font-weight: 600; color: #333; }
	.preview-camera-wrap {
		position: relative;
		width: 50%;
		margin: 0 auto;
		height: 380rpx;
		max-height: 40vh;
		overflow: hidden;
		background: #000;
		border-radius: 16rpx;
	}
	.preview-camera {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		display: block;
	}
	.preview-camera-tip {
		padding: 12rpx 24rpx 16rpx;
		font-size: 22rpx;
		color: #999;
		text-align: center;
	}
	.navbar {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 24rpx 32rpx;
		background: rgba(255, 255, 255, 0.92);
		border-bottom: 1rpx solid #e5e5e5;
	}
	.navbar-left, .navbar-right { width: 80rpx; }
	.navbar-center { flex: 1; text-align: center; }
	.navbar-title { font-size: 36rpx; font-weight: 600; color: #0f172a; }

	.green-line {
		width: 6rpx; height: 32rpx;
		background: linear-gradient(180deg, #07C160 0%, #06A050 100%);
		border-radius: 3rpx;
		margin-right: 16rpx;
	}
	.action-row { padding: 10rpx 32rpx; }
	.btn-start, .btn-video {
		width: 100%;
		height: 88rpx;
		line-height: 88rpx;
		border-radius: 44rpx;
		font-size: 32rpx;
		font-weight: 600;
		border: none;
	}
	.btn-start {
		background: linear-gradient(135deg, #07C160 0%, #06A050 100%);
		color: #fff;
	}
	.btn-video {
		background: #fff;
		color: #07C160;
		border: 2rpx solid #07C160;
	}
	.btn-video[disabled] {
		opacity: 0.7;
		color: #999;
		border-color: #ccc;
	}

	.result-card {
		margin: 24rpx;
		padding: 32rpx;
		background: #fff;
		border-radius: 24rpx;
		box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
	}
	.result-header { display: flex; align-items: center; margin-bottom: 20rpx; }
	.result-title { font-size: 34rpx; font-weight: 600; color: #333; }
	.result-body { font-size: 28rpx; color: #666; }
	.result-row { margin-bottom: 16rpx; }
	.result-label { color: #999; margin-right: 12rpx; }
	.result-value { color: #333; }
	.result-value.highlight { color: #07C160; font-weight: 600; font-size: 32rpx; }
	.result-video {
		width: 100%;
		height: 420rpx;
		background: #000;
		border-radius: 16rpx;
		overflow: hidden;
	}

	/* 竖屏全屏识别 */
	.fullscreen-wrap {
		position: fixed;
		top: 0;
		left: 0;
		right: 0;
		bottom: 0;
		z-index: 999;
		background: #000;
	}
	.fullscreen-camera {
		position: absolute;
		top: 0;
		left: 0;
		width: 100vw;
		height: 100vh;
	}
	.overlay-top-right {
		position: absolute;
		top: 80rpx;
		right: 24rpx;
		width: 280rpx;
		padding: 24rpx;
		background: rgba(0, 0, 0, 0.6);
		border-radius: 16rpx;
		color: #fff;
	}
	.overlay-marked {
		position: absolute;
		left: 24rpx;
		top: 80rpx;
		width: 300rpx;
		background: rgba(0, 0, 0, 0.6);
		border-radius: 16rpx;
		overflow: hidden;
	}
	.overlay-marked-title {
		padding: 10rpx 14rpx;
		font-size: 22rpx;
		color: #ddd;
	}
	.overlay-marked-image {
		width: 300rpx;
		height: 180rpx;
		display: block;
		background: #111;
	}
	.overlay-title {
		font-size: 28rpx;
		font-weight: 600;
		margin-bottom: 16rpx;
		color: #07C160;
	}
	.overlay-list { font-size: 24rpx; }
	.overlay-item {
		display: flex;
		justify-content: space-between;
		margin-bottom: 8rpx;
	}
	.overlay-name { color: #ccc; }
	.overlay-value { color: #fff; }
	.overlay-duration {
		margin-top: 12rpx;
		font-size: 22rpx;
		color: #999;
	}
	.btn-end-wrap {
		position: absolute;
		bottom: 0;
		left: 0;
		right: 0;
		padding: 32rpx 48rpx;
		padding-bottom: calc(32rpx + env(safe-area-inset-bottom));
	}
	.btn-end {
		width: 100%;
		height: 96rpx;
		line-height: 96rpx;
		border-radius: 48rpx;
		font-size: 34rpx;
		font-weight: 600;
		border: none;
		background: linear-gradient(135deg, #07C160 0%, #06A050 100%);
		color: #fff;
	}
	.btn-end-cover {
		width: 100%;
		height: 96rpx;
		line-height: 96rpx;
		border-radius: 48rpx;
		text-align: center;
		font-size: 34rpx;
		font-weight: 600;
		background: linear-gradient(135deg, #07C160 0%, #06A050 100%);
		color: #fff;
	}
</style>
