 /**
 * 应用配置文件
 * 真机调试说明：手机和电脑须在同一 WiFi；真机上 127.0.0.1 指向手机本机。
 * 请用「无线局域网 WLAN」的 IPv4，不要用 VMware 虚拟网卡（如 192.168.223.1）。
 * 校园网/公司网 IP 可能变化，连不上时重新 ipconfig 后改此处。
 */
// 当前本机 WLAN：172.27.26.92（与 ipconfig 一致；换网络需改）
const WEIXIN_BASE_URL = 'http://192.168.43.196:8080'

const config = {
	baseUrl: (() => {
		// #ifdef H5
		return 'http://127.0.0.1:8080'
		// #endif
		// #ifdef MP-WEIXIN
		return WEIXIN_BASE_URL
		// #endif
		// #ifndef H5 || MP-WEIXIN
		return 'http://127.0.0.1:8080'
		// #endif
	})(),
	aiDoctorRagUrl: (() => {
		// #ifdef H5
		return 'http://127.0.0.1:8765'
		// #endif
		// #ifdef MP-WEIXIN
		return WEIXIN_BASE_URL.replace(':8080', ':8765')
		// #endif
		return 'http://127.0.0.1:8765'
	})(),
	/** 英文 RAG（docs.csv + english_bge），默认端口 8766，需单独进程启动 */
	aiDoctorRagEnUrl: (() => {
		// #ifdef H5
		return 'http://127.0.0.1:8766'
		// #endif
		// #ifdef MP-WEIXIN
		return WEIXIN_BASE_URL.replace(':8080', ':8766')
		// #endif
		return 'http://127.0.0.1:8766'
	})(),
	/** 应用 Logo（小程序登录页、首页等；需在小程序后台配置该 OSS 域名为 download 合法域名） */
	appLogoUrl:
		'https://medical-vision-intelligent-travel.oss-cn-beijing.aliyuncs.com/Snipaste_2026-04-09_21-42-29.png'
}

export default config
