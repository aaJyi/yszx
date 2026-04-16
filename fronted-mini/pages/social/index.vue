<template>
	<view class="page-root">
		<view class="container">
			<view class="navbar">
				<view class="navbar-left"></view>
				<view class="navbar-center">
					<text class="navbar-title">好友社交</text>
				</view>
				<view class="navbar-right" @tap="onRefresh" v-if="subTab === 0">
					<uni-icons type="refreshempty" size="22" color="#333"></uni-icons>
				</view>
				<view class="navbar-right" v-else></view>
			</view>

			<view class="sub-tabs">
				<view
					class="sub-tab"
					:class="{ active: subTab === i }"
					v-for="(t, i) in subTabs"
					:key="i"
					@tap="subTab = i"
				>{{ t }}</view>
			</view>

			<!-- 咨询：健康资讯（原页面） -->
			<view v-show="subTab === 0">
				<view v-if="!isLogin" class="hint-box">
					<text class="hint-text">登录后可查看基于您健康状况推荐的资讯</text>
					<button class="hint-btn" @tap="goLogin">去登录</button>
				</view>

				<view v-else>
					<view class="tag-section" v-if="diseaseTags.length">
						<text class="tag-label">关注方向</text>
						<view class="tag-wrap">
							<text class="tag" v-for="(t, i) in diseaseTags" :key="i">{{ t.diseaseName }}</text>
						</view>
					</view>
					<scroll-view scroll-y class="feed-scroll" :refresher-enabled="true" :refresher-triggered="refreshing" @refresherrefresh="onPullRefresh">
						<view v-if="articles.length === 0" class="empty">
							<uni-icons type="info" size="48" color="#ccc"></uni-icons>
							<text class="empty-txt">暂无推荐，请稍后重试或先刷新</text>
						</view>
						<view v-else>
							<view class="card" v-for="(a, idx) in articles" :key="idx" @tap="openArticle(a)">
								<image v-if="a.coverUrl" class="cover" :src="a.coverUrl" mode="aspectFill"></image>
								<view class="card-body">
									<text class="card-title">{{ a.title }}</text>
									<text class="card-sub" v-if="a.summary">{{ a.summary }}</text>
									<text class="card-kw" v-if="a.keyword">关键词：{{ a.keyword }}</text>
								</view>
							</view>
						</view>
					</scroll-view>
				</view>
			</view>

			<!-- 推荐 -->
			<view v-show="subTab === 1" class="panel-social">
				<view v-if="!isLogin" class="hint-box">
					<text class="hint-text">登录后根据推断疾病与 NCSS 社团推荐相似病友</text>
					<button class="hint-btn" @tap="goLogin">去登录</button>
				</view>
				<scroll-view v-else scroll-y class="reco-scroll">
					<view class="pref-card">
						<text class="pref-title">推荐授权</text>
						<switch :checked="allowRecommend" color="#07C160" @change="onToggleAllowRecommend" />
					</view>
					<text class="reco-tip">同社团用户优先展示；可互相关注后进入「交流」发消息。</text>
					<view v-if="recoLoading" class="reco-loading">加载中…</view>
					<view v-else-if="recommendList.length === 0" class="empty">
						<text class="empty-txt">暂无推荐，请先在档案中完善健康数据或等待后台更新社团划分</text>
					</view>
					<view v-else>
						<view class="user-card" v-for="u in recommendList" :key="u.userId">
							<view class="user-main">
								<image v-if="u.avatarUrl" class="avatar" :src="u.avatarUrl" mode="aspectFill"></image>
							<view v-else class="avatar avatar-ph"><uni-icons type="person-filled" size="28" color="#bbb"></uni-icons></view>
								<view class="user-info">
									<text class="uname">{{ u.nickname || '用户' + u.userId }}</text>
									<text class="u-sub" v-if="u.diseaseSimilarity != null">疾病相似度 {{ (u.diseaseSimilarity * 100).toFixed(0) }}%</text>
									<text class="u-sub" v-if="u.profileSimilarity != null">画像相似度 {{ (u.profileSimilarity * 100).toFixed(0) }}%</text>
									<text class="u-tags" v-if="u.sharedDiseaseHints && u.sharedDiseaseHints.length">共有：{{ u.sharedDiseaseHints.join('、') }}</text>
									<text class="u-reason" v-if="u.matchType">{{ u.matchType }}</text>
								</view>
							</view>
							<button
								v-if="!u.mutualFollow"
								class="follow-btn"
								:class="{ ghost: u.followed }"
								size="mini"
								@tap.stop="toggleFollow(u)"
							>{{ u.followed ? '已关注' : '关注' }}</button>
							<text v-else class="mutual-tag">已互关</text>
						</view>
						<view class="pager-row">
							<button class="page-btn" :disabled="recoPage <= 1" @tap="changeRecoPage(recoPage - 1)">上一页</button>
							<text class="page-text">第 {{ recoPage }} / {{ recoTotalPage }} 页</text>
							<button class="page-btn" :disabled="recoPage >= recoTotalPage" @tap="changeRecoPage(recoPage + 1)">下一页</button>
						</view>
					</view>
				</scroll-view>
			</view>

			<!-- 交流 -->
			<view v-show="subTab === 2" class="panel-social">
				<view v-if="!isLogin" class="hint-box">
					<text class="hint-text">登录后与互相关注的好友交流</text>
					<button class="hint-btn" @tap="goLogin">去登录</button>
				</view>
				<scroll-view v-else scroll-y class="reco-scroll">
					<text class="reco-tip">仅展示已互相关注的好友，点击进入会话。</text>
					<view v-if="partnersLoading" class="reco-loading">加载中…</view>
					<view v-else-if="partners.length === 0" class="empty">
						<text class="empty-txt">暂无互关好友，请在「推荐」中关注对方并等待对方回关</text>
					</view>
					<view v-else>
						<view class="partner-row" v-for="p in partners" :key="p.userId" @tap="openChat(p)">
							<image v-if="p.avatarUrl" class="avatar" :src="p.avatarUrl" mode="aspectFill"></image>
							<view v-else class="avatar avatar-ph"><uni-icons type="person-filled" size="28" color="#bbb"></uni-icons></view>
							<text class="uname">{{ p.nickname || '用户' + p.userId }}</text>
							<uni-icons type="right" size="18" color="#ccc"></uni-icons>
						</view>
					</view>
				</scroll-view>
			</view>
		</view>
		<custom-tabbar :current="4"></custom-tabbar>
	</view>
</template>

<script>
	import customTabbar from '@/components/custom-tabbar/custom-tabbar.vue'
	import config from '@/utils/config.js'

	export default {
		components: { customTabbar },
		data() {
			return {
				subTabs: ['咨询', '推荐', '交流'],
				subTab: 0,
				isLogin: false,
				userId: null,
				diseaseTags: [],
				articles: [],
				refreshing: false,
				recommendList: [],
				recoLoading: false,
				partners: [],
				partnersLoading: false,
				recoPage: 1,
				recoPageSize: 20,
				recoTotal: 0,
				allowRecommend: true
			}
		},
		computed: {
			recoTotalPage() {
				return Math.max(1, Math.ceil((this.recoTotal || 0) / this.recoPageSize))
			}
		},
		watch: {
			subTab(n) {
				if (!this.isLogin || !this.userId) return
				if (n === 1) this.loadRecommend()
				if (n === 2) this.loadPartners()
			}
		},
		onShow() {
			const login = uni.getStorageSync('isLogin')
			const userInfo = uni.getStorageSync('userInfo') || {}
			this.isLogin = login === true
			this.userId = userInfo.userId != null ? userInfo.userId : null
			if (this.isLogin && this.userId != null) {
				this.loadDiseases()
				this.loadArticles()
				this.loadPreference()
				if (this.subTab === 1) this.loadRecommend()
				if (this.subTab === 2) this.loadPartners()
			} else {
				this.diseaseTags = []
				this.articles = []
				this.recommendList = []
				this.partners = []
			}
		},
		methods: {
			goLogin() {
				uni.navigateTo({ url: '/pages/login/login' })
			},
			loadDiseases() {
				if (this.userId == null) return
				uni.request({
					url: `${config.baseUrl}/social/inferred-diseases/user/${this.userId}`,
					method: 'GET',
					success: (res) => {
						if (res.statusCode === 200 && res.data && res.data.code === 200) {
							this.diseaseTags = res.data.data || []
						}
					}
				})
			},
			loadArticles() {
				if (this.userId == null) return
				uni.request({
					url: `${config.baseUrl}/social/articles/user/${this.userId}`,
					method: 'GET',
					success: (res) => {
						if (res.statusCode === 200 && res.data && res.data.code === 200) {
							this.articles = res.data.data || []
						}
					}
				})
			},
			loadRecommend() {
				if (this.userId == null) return
				this.recoLoading = true
				uni.request({
					url: `${config.baseUrl}/social/peer/recommendations/user/${this.userId}?page=${this.recoPage}&pageSize=${this.recoPageSize}`,
					method: 'GET',
					success: (res) => {
						this.recoLoading = false
						if (res.statusCode === 200 && res.data && res.data.code === 200) {
							const payload = res.data.data || {}
							this.recommendList = payload.list || []
							this.recoTotal = payload.total || 0
						}
					},
					fail: () => {
						this.recoLoading = false
					}
				})
			},
			loadPreference() {
				if (this.userId == null) return
				uni.request({
					url: `${config.baseUrl}/social/peer/preference/user/${this.userId}`,
					method: 'GET',
					success: (res) => {
						if (res.statusCode === 200 && res.data && res.data.code === 200) {
							const p = res.data.data || {}
							this.allowRecommend = p.allowRecommend !== false
						}
					}
				})
			},
			onToggleAllowRecommend(e) {
				const val = !!(e && e.detail && e.detail.value)
				this.allowRecommend = val
				uni.request({
					url: `${config.baseUrl}/social/peer/preference`,
					method: 'POST',
					header: { 'Content-Type': 'application/json' },
					data: { userId: this.userId, allowRecommend: val },
					success: () => {
						this.recoPage = 1
						this.loadRecommend()
					}
				})
			},
			changeRecoPage(p) {
				this.recoPage = p
				this.loadRecommend()
			},
			loadPartners() {
				if (this.userId == null) return
				this.partnersLoading = true
				uni.request({
					url: `${config.baseUrl}/social/peer/partners/user/${this.userId}`,
					method: 'GET',
					success: (res) => {
						this.partnersLoading = false
						if (res.statusCode === 200 && res.data && res.data.code === 200) {
							this.partners = res.data.data || []
						}
					},
					fail: () => {
						this.partnersLoading = false
					}
				})
			},
			toggleFollow(u) {
				if (u.followed) {
					uni.request({
						url: `${config.baseUrl}/social/peer/follow/cancel`,
						method: 'POST',
						header: { 'Content-Type': 'application/json' },
						data: { followerUserId: this.userId, followeeUserId: u.userId },
						success: (r) => {
							if (r.statusCode === 200 && r.data && r.data.code === 200) {
								u.followed = false
								u.mutualFollow = false
								this.loadRecommend()
							}
						}
					})
				} else {
					uni.request({
						url: `${config.baseUrl}/social/peer/follow`,
						method: 'POST',
						header: { 'Content-Type': 'application/json' },
						data: { followerUserId: this.userId, followeeUserId: u.userId },
						success: (r) => {
							if (r.statusCode === 200 && r.data && r.data.code === 200) {
								u.followed = true
								if (u.followsMe) u.mutualFollow = true
								this.loadRecommend()
								this.loadPartners()
							}
						}
					})
				}
			},
			openChat(p) {
				const name = encodeURIComponent(p.nickname || '')
				const av = p.avatarUrl ? encodeURIComponent(p.avatarUrl) : ''
				uni.navigateTo({
					url: `/pages/social/peer-chat?peerId=${p.userId}&peerName=${name}${av ? '&peerAvatar=' + av : ''}`
				})
			},
			onRefresh() {
				if (this.userId == null) {
					uni.showToast({ title: '请先登录', icon: 'none' })
					return
				}
				uni.request({
					url: `${config.baseUrl}/social/articles/refresh`,
					method: 'POST',
					header: { 'Content-Type': 'application/json' },
					data: { userId: this.userId },
					success: (res) => {
						const msg = (res.data && res.data.message) ? res.data.message : '完成'
						uni.showToast({ title: msg, icon: 'none' })
						this.loadArticles()
						this.loadDiseases()
					},
					fail: () => {
						uni.showToast({ title: '刷新失败', icon: 'none' })
					}
				})
			},
			onPullRefresh() {
				this.refreshing = true
				if (this.userId == null) {
					this.refreshing = false
					return
				}
				uni.request({
					url: `${config.baseUrl}/social/articles/refresh`,
					method: 'POST',
					header: { 'Content-Type': 'application/json' },
					data: { userId: this.userId },
					complete: () => {
						this.refreshing = false
					},
					success: () => {
						this.loadArticles()
						this.loadDiseases()
					}
				})
			},
			openArticle(a) {
				const url = a && a.articleUrl
				if (!url) return
				uni.navigateTo({
					url: '/pages/social/webview?url=' + encodeURIComponent(url)
				})
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page-root { min-height: 100vh; background: #f5f6f8; }
	.container { min-height: 100vh; padding-bottom: 120rpx; }
	.navbar {
		display: flex; align-items: center; justify-content: space-between;
		padding: 40rpx 30rpx 16rpx;
		background: #fff;
	}
	.navbar-left { width: 60rpx; }
	.navbar-center { flex: 1; text-align: center; }
	.navbar-title { font-size: 36rpx; font-weight: 600; color: #333; }
	.navbar-right { width: 60rpx; display: flex; justify-content: flex-end; }

	.sub-tabs {
		display: flex;
		margin: 0 24rpx 16rpx;
		background: #fff;
		border-radius: 12rpx;
		overflow: hidden;
		border: 1rpx solid #eee;
	}
	.sub-tab {
		flex: 1;
		text-align: center;
		padding: 20rpx 0;
		font-size: 28rpx;
		color: #666;
		background: #fff;
	}
	.sub-tab.active {
		color: #fff;
		background: #07C160;
		font-weight: 600;
	}

	.hint-box {
		margin: 24rpx 30rpx; padding: 40rpx; background: #fff; border-radius: 16rpx; text-align: center;
	}
	.hint-text { font-size: 28rpx; color: #666; display: block; margin-bottom: 24rpx; }
	.hint-btn { background: #07C160; color: #fff; font-size: 28rpx; border-radius: 40rpx; }

	.tag-section { margin: 24rpx 30rpx; padding: 24rpx; background: #fff; border-radius: 16rpx; }
	.tag-label { font-size: 24rpx; color: #999; display: block; margin-bottom: 12rpx; }
	.tag-wrap { display: flex; flex-wrap: wrap; gap: 16rpx; }
	.tag {
		font-size: 24rpx; color: #07C160; background: rgba(7, 193, 96, 0.1);
		padding: 8rpx 20rpx; border-radius: 8rpx;
	}
	.feed-scroll { height: calc(100vh - 420rpx); }
	.panel-social .reco-scroll { height: calc(100vh - 380rpx); }
	.reco-tip { display: block; font-size: 24rpx; color: #888; margin: 0 30rpx 16rpx; }
	.reco-loading { text-align: center; color: #999; padding: 40rpx; }
	.pref-card {
		margin: 12rpx 30rpx;
		padding: 18rpx 22rpx;
		background: #fff;
		border-radius: 12rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}
	.pref-title { font-size: 26rpx; color: #333; }

	.empty { padding: 80rpx; text-align: center; }
	.empty-txt { display: block; margin-top: 16rpx; font-size: 26rpx; color: #999; }

	.card {
		margin: 24rpx 30rpx; background: #fff; border-radius: 16rpx; overflow: hidden;
		box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.06);
	}
	.cover { width: 100%; height: 280rpx; background: #eee; }
	.card-body { padding: 24rpx; }
	.card-title { font-size: 32rpx; font-weight: 600; color: #222; line-height: 1.4; }
	.card-sub { font-size: 26rpx; color: #666; margin-top: 12rpx; display: -webkit-box; -webkit-line-clamp: 3; line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
	.card-kw { font-size: 22rpx; color: #aaa; margin-top: 12rpx; display: block; }

	.user-card {
		margin: 16rpx 30rpx;
		padding: 24rpx;
		background: #fff;
		border-radius: 16rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
		box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.05);
	}
	.user-main { display: flex; align-items: center; flex: 1; min-width: 0; }
	.avatar { width: 88rpx; height: 88rpx; border-radius: 50%; background: #eee; margin-right: 20rpx; }
	.avatar-ph { display: flex; align-items: center; justify-content: center; }
	.user-info { flex: 1; min-width: 0; }
	.uname { font-size: 30rpx; font-weight: 600; color: #222; display: block; }
	.u-sub { font-size: 24rpx; color: #07C160; margin-top: 6rpx; display: block; }
	.u-tags { font-size: 22rpx; color: #999; margin-top: 6rpx; display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
	.u-reason { font-size: 22rpx; color: #666; margin-top: 6rpx; display: block; }
	.follow-btn { margin: 0; font-size: 24rpx; background: #07C160; color: #fff; }
	.follow-btn.ghost { background: #e8f8ef; color: #07C160; }
	.mutual-tag { font-size: 24rpx; color: #07C160; }
	.pager-row { display: flex; align-items: center; justify-content: center; gap: 20rpx; margin: 24rpx 30rpx; }
	.page-btn { font-size: 22rpx; padding: 0 26rpx; height: 60rpx; line-height: 60rpx; border-radius: 10rpx; }
	.page-text { font-size: 22rpx; color: #666; }

	.partner-row {
		margin: 12rpx 30rpx;
		padding: 24rpx;
		background: #fff;
		border-radius: 16rpx;
		display: flex;
		align-items: center;
		gap: 20rpx;
	}
	.partner-row .uname { flex: 1; font-size: 30rpx; }
</style>
