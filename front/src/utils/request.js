/**
 * 后端服务地址。
 * @description 本地开发环境默认使用 8080 端口。
 */
const BASE_URL = "http://localhost:8080";

/**
 * 通用请求方法。
 * @param {Object} options 请求参数
 * @param {string} options.url 请求路径
 * @param {string} [options.method] 请求方法
 * @param {Object} [options.data] 请求体
 * @param {Object} [options.header] 请求头
 * @returns {Promise<any>} 业务数据
 */
export function request(options) {
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${BASE_URL}${options.url}`,
      method: options.method || "GET",
      data: options.data || {},
      header: {
        "content-type": "application/json",
        ...(options.header || {}),
      },
      success: (res) => {
        if (res.statusCode < 200 || res.statusCode >= 300) {
          reject(new Error("请求失败，请稍后重试"));
          return;
        }

        const body = res.data || {};
        if (body.code !== 0) {
          reject(new Error(body.message || "请求失败"));
          return;
        }

        resolve(body.data);
      },
      fail: (err) => {
        reject(new Error(err.errMsg || "网络异常，请检查后端服务"));
      },
    });
  });
}
