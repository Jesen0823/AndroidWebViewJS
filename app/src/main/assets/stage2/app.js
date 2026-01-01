/**
 * 原生-JS 互调 JS 逻辑
 * 桥接名称：AndroidJsBridge（与原生保持一致）
 */

 /**
  * 优化后：健壮的JS桥接对象获取方法
  * 1. 实时从window对象获取（避免缓存undefined）
  * 2. 支持重试机制，应对注入延迟
  * 3. 完善错误兜底
  */
 function getAndroidJsBridge(){
   return window.AndroidJsBridge;
 }

 const resultDom = document.getElementById("result");

 /**
 * 工具函数：更新结果展示
 */
 function updateResult(content){
    resultDom.innerHTML = content;
    console.log("H5 日志：",content);
 }

 /**
 * 工具函数：JS 调用原生统一入口（带重试）
 * @param methodName 原生方法名
 * @param params 传递参数
 * @param retryCount 剩余重试次数
 */
 function callAndroid(methodName,params,retryCount = 3){
    const bridge = getAndroidJsBridge();
    if (bridge) {
            try {
                // 确保参数是字符串类型（兼容Kotlin的String参数）
                const finalParams = typeof params === 'string' ? params : JSON.stringify(params);
                bridge.callAndroid(methodName, finalParams);
                updateResult(`已调用原生方法：${methodName}，参数：${finalParams}`);
            } catch (e) {
                updateResult(`调用原生方法失败：${e.message}`);
                console.error("JS调用原生异常：", e);
            }
        } else {
            if (retryCount > 0) {
                updateResult(`未找到AndroidJsBridge，${retryCount}秒后重试...`);
                // 延迟1秒重试，给原生接口注入留时间
                setTimeout(() => callAndroid(methodName, params, retryCount - 1), 1000);
            } else {
                updateResult("错误：多次重试后仍未找到AndroidJsBridge");
                console.error("JS调用原生失败：未找到AndroidJsBridge");
            }
        }
 }

 /**
 * 1. JS 调用原生：获取用户信息
 */
function getUserInfo() {
    updateResult("正在请求用户信息...");
    callAndroid("getUserInfo", "");
}

/**
 * 2. JS 调用原生：打开原生页面
 */
function openNativePage() {
    updateResult("正在请求打开原生页面...");
    callAndroid("openNativePage", "HomePage");
}

/**
 * 3. 原生调用 JS：显示 Toast
 */
function showToastFromNative() {
    // 原生主动调用 JS 方法（此处模拟原生调用，实际由原生触发）
    updateResult("正在请求原生显示 Toast...");
    // 原生调用 JS 的方法名：showToast
    if (window.callJs) {
        window.callJs("showToast", "这是来自 H5 的 Toast 提示");
    }
}

/**
 * 4. 原生调用 JS 的统一入口（供原生调用）
 */
function callJs(methodName, params) {
    switch (methodName) {
        case "getUserInfo":
            // 接收原生返回的用户信息
            const userInfo = JSON.parse(params);
            let userInfoStr = "用户信息：<br/>";
            for (const key in userInfo) {
                userInfoStr += key + "：" + userInfo[key] + "<br/>";
            }
            updateResult(userInfoStr);
            break;
        case "showToast":
            updateResult("Toast 提示：" + params);
            break;
        case "updateUi":
            updateResult("UI 更新：" + params);
            break;
        default:
            updateResult("未知 JS 方法：" + methodName);
            break;
    }
}