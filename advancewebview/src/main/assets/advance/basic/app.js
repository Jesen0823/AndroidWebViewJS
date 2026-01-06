/**
 * 基础功能测试 JS - 修复版
 * 修复点：1. 移除jQuery依赖 2. 全局暴露所有外部调用方法 3. 统一桥接命名 4. 实时更新页面文案
 */
const AdvanceAndroidJsBridge = window.AdvanceAndroidJsBridge;
//const AdvanceGlobalTool = window.AdvanceGlobalTool;
//const AdvanceBusinessLogic = window.AdvanceBusinessLogic;

/**
 * 实时获取注入的全局工具类（带重试）
 * @param {number} retryTimes 重试次数（默认1次）
 * @returns {Object|null}
 */
function getAdvanceGlobalTool(retryTimes = 1) {
    const tool = window.AdvanceGlobalTool;
    if (tool) return tool;
    
    // 重试逻辑：若未获取到，延迟 100ms 重试（仅重试1次）
    if (retryTimes > 0) {
        setTimeout(() => {
            return getAdvanceGlobalTool(retryTimes - 1);
        }, 100);
    }
    console.warn ("AdvanceGlobalTool 未注入");
    return null;
}

/**
 * 实时获取注入的业务逻辑类（带重试）
 * @param {number} retryTimes 重试次数（默认1次）
 * @returns {Object|null} 注入的 AdvanceBusinessLogic 或 null
 */
function getAdvanceBusinessLogic(retryTimes = 1) {
    const tool = window.AdvanceBusinessLogic;
    if (tool) return tool;
    
    // 重试逻辑：若未获取到，延迟 100ms 重试（仅重试1次）
    if (retryTimes > 0) {
        setTimeout(() => {
            return getAdvanceBusinessLogic(retryTimes - 1);
        }, 100);
    }
    console.warn ("AdvanceBusinessLogic 未注入");
    return null;
}

// ---------------------- 全局暴露方法（HTML按钮调用）----------------------
window.callShowToast = function() {
    const message = "Hello Advance WebView! 这是 JS 调用原生 Toast";
    const globalTool = getAdvanceGlobalTool(); // 实时获取
    if (globalTool) {
        globalTool.showToast(message);
        logSuccess(`调用 AdvanceGlobalTool.showToast：${message}`);
    } else {
        logError("无法调用 showToast：AdvanceGlobalTool 未注入");
    }
};

window.callGetDeviceInfo = function() {
    const businessLogic = getAdvanceBusinessLogic(); // 实时获取
    if (businessLogic) {
        businessLogic.getDeviceInfo();
        logInfo("调用 AdvanceBusinessLogic.getDeviceInfo（请求原生返回设备信息）");
    } else {
        logError("无法调用 getDeviceInfo：AdvanceBusinessLogic 未注入");
    }
};

window.callSetStorage = function() {
    const key = "advance_test_key";
    const value = "advance_test_value_" + new Date().getTime();
    const globalTool = getAdvanceGlobalTool(); // 实时获取
    if (globalTool) {
        globalTool.setStorage(key, value);
        logSuccess(`调用 AdvanceGlobalTool.setStorage：${key}=${value}`);
    } else {
        logError("无法调用 setStorage：AdvanceGlobalTool 未注入");
    }
};

window.callGetStorage = function() {
    const key = "advance_test_key";
    const globalTool = getAdvanceGlobalTool(); // 实时获取
    if (globalTool) {
        globalTool.getStorage(key);
        logInfo(`调用 AdvanceGlobalTool.getStorage：${key}`);
    } else {
        logError("无法调用 getStorage：AdvanceGlobalTool 未注入");
    }
};

window.callClearCache = function() {
    const globalTool = getAdvanceGlobalTool(); // 实时获取
    if (globalTool) {
        globalTool.clearCache();
        logInfo("调用 AdvanceGlobalTool.clearCache（请求原生清理缓存）");
    } else {
        logError("无法调用 clearCache：AdvanceGlobalTool 未注入");
    }
};

window.clearResultArea = function() {
    const resultDom = document.getElementById("advanceInjectResult");
    if (resultDom) {
        resultDom.innerHTML = '<p class="info">结果区域已清空，等待执行操作...</p>';
    }
};

// ---------------------- 辅助日志方法 ----------------------
function logInfo(content) {
    appendResult(`<span class="info">[INFO] ${new Date().toLocaleTimeString()}：${content}</span>`);
}

function logSuccess(content) {
    appendResult(`<span class="success">[SUCCESS] ${new Date().toLocaleTimeString()}：${content}</span>`);
}

function logError(content) {
    appendResult(`<span class="error">[ERROR] ${new Date().toLocaleTimeString()}：${content}</span>`);
}

function appendResult(html) {
    const resultDom = document.getElementById("advanceInjectResult");
    if (resultDom) {
        resultDom.innerHTML += `${html}<br/>`;
        resultDom.scrollTop = resultDom.scrollHeight;
    }
}

// ---------------------- 原生调用 JS 的方法（全局暴露）----------------------
window.callAdvanceJs = function(methodName, params) {
    logInfo(`收到原生调用：method=${methodName}，params=${params}`);
    try {
        const paramsObj = JSON.parse(params);
        switch (methodName) {
            case "updateAdvanceUi":
                for (const [key, value] of Object.entries(paramsObj)) {
                    logSuccess(`设备信息 - ${key}：${value}`);
                }
                break;
            case "notifyAdvanceCacheState":
                logSuccess(`缓存状态通知：${paramsObj}`);
                break;
            default:
                logInfo(`未知原生调用方法：${methodName}`);
        }
    } catch (e) {
        logError(`解析原生调用参数失败：${e.message}，原始参数：${params}`);
    }
};

// ---------------------- DOM 就绪初始化 ----------------------
document.addEventListener('DOMContentLoaded', function() {
    logInfo("页面初始化完成，检测注入模块状态...");
    if (getAdvanceGlobalTool()) logSuccess("AdvanceGlobalTool 注入成功（全局工具类）");
    else logError("AdvanceGlobalTool 注入失败");
    if (getAdvanceBusinessLogic()) logSuccess("AdvanceBusinessLogic 注入成功（业务逻辑类）");
    else logError("AdvanceBusinessLogic 注入失败");
    if (AdvanceAndroidJsBridge) logSuccess("AdvanceAndroidJsBridge 桥接成功（原生通信入口）");
    else logError("AdvanceAndroidJsBridge 桥接失败");
});