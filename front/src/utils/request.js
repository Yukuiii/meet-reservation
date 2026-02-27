/**
 * 后端服务地址。
 * @description 本地开发环境默认使用 8080 端口。
 */
export const BASE_URL = "http://localhost:8080";

/**
 * 将接口返回的资源路径转换为可访问地址。
 * @param {string} rawUrl 原始资源路径
 * @returns {string} 可访问地址
 */
export function buildAssetUrl(rawUrl) {
  if (!rawUrl) {
    return ""
  }
  if (/^https?:\/\//i.test(rawUrl)) {
    return rawUrl
  }

  const normalizedPath = rawUrl.startsWith("/") ? rawUrl : `/${rawUrl}`
  return `${BASE_URL}${normalizedPath}`
}

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

/**
 * 通用文件上传方法。
 * @param {Object} options 上传参数
 * @param {string} options.url 上传路径
 * @param {string} options.filePath 本地文件路径
 * @param {string} [options.name] 文件字段名
 * @param {Object} [options.formData] 附加表单参数
 * @returns {Promise<any>} 业务数据
 */
export function uploadFile(options) {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${BASE_URL}${options.url}`,
      filePath: options.filePath,
      name: options.name || "file",
      formData: options.formData || {},
      success: (res) => {
        if (res.statusCode < 200 || res.statusCode >= 300) {
          reject(new Error("上传失败，请稍后重试"))
          return
        }

        let body = {}
        if (typeof res.data === "string") {
          try {
            body = JSON.parse(res.data || "{}")
          } catch (error) {
            reject(new Error("上传响应解析失败"))
            return
          }
        } else {
          body = res.data || {}
        }

        if (body.code !== 0) {
          reject(new Error(body.message || "上传失败"))
          return
        }

        resolve(body.data)
      },
      fail: (err) => {
        reject(new Error(err.errMsg || "上传失败，请检查网络"))
      }
    })
  })
}
