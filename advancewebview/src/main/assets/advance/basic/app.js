/**
 * 基础功能测试 JS - 优化版
 * 优化点：1. DOM元素缓存 2. 重复代码抽象 3. 异步重试逻辑优化 4. 性能优化
 */
const AdvanceAndroidJsBridge = window.AdvanceAndroidJsBridge;

// DOM元素缓存
const resultDom = document.getElementById("advanceInjectResult");
const loadingMask = document.getElementById("loadingMask");

/**
 * 获取注入的全局对象（抽象通用方法，避免重复代码）
 * @param {string} objName 全局对象名称
 * @param {number} retryTimes 重试次数（默认1次）
 * @returns {Object|null}
 */
function getInjectedGlobalObject(objName, retryTimes = 1) {
    const obj = window[objName];
    if (obj) return obj;
    
    // 重试逻辑：若未获取到，延迟 100ms 重试（仅重试1次）
    if (retryTimes > 0) {
        console.warn(`${objName} 未注入，将尝试重试`);
        setTimeout(() => {
            // 重试获取并记录结果
            const retriedObj = window[objName];
            if (retriedObj) {
                console.log(`${objName} 注入成功（重试后）`);
            } else {
                console.warn(`${objName} 未注入（重试后）`);
            }
        }, 100);
    }
    return null;
}

/**
 * 实时获取注入的全局工具类（带重试）
 * @param {number} retryTimes 重试次数（默认1次）
 * @returns {Object|null}
 */
function getAdvanceGlobalTool(retryTimes = 1) {
    return getInjectedGlobalObject("AdvanceGlobalTool", retryTimes);
}

/**
 * 实时获取注入的业务逻辑类（带重试）
 * @param {number} retryTimes 重试次数（默认1次）
 * @returns {Object|null} 注入的 AdvanceBusinessLogic 或 null
 */
function getAdvanceBusinessLogic(retryTimes = 1) {
    return getInjectedGlobalObject("AdvanceBusinessLogic", retryTimes);
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
        showLoading(); // 显示加载状态
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
    if (resultDom) {
        resultDom.innerHTML = '<p class="info">结果区域已清空，等待执行操作...</p>';
    }
};

// ---------------------- 辅助日志方法 ----------------------
// 缓存日志类型和对应的CSS类名
const LOG_TYPES = {
    INFO: { prefix: '[INFO]', className: 'info' },
    SUCCESS: { prefix: '[SUCCESS]', className: 'success' },
    ERROR: { prefix: '[ERROR]', className: 'error' }
};

// 生成日志条目
function createLogEntry(type, content) {
    const logType = LOG_TYPES[type];
    const time = new Date().toLocaleTimeString();
    return `<span class="${logType.className}">${logType.prefix} ${time}：${content}</span>`;
}

// 日志方法封装
function logInfo(content) {
    appendResult(createLogEntry('INFO', content));
}

function logSuccess(content) {
    appendResult(createLogEntry('SUCCESS', content));
}

function logError(content) {
    appendResult(createLogEntry('ERROR', content));
}

// 加载状态控制方法
function showLoading() {
    if (loadingMask) {
        loadingMask.style.display = 'flex';
    }
}

function hideLoading() {
    if (loadingMask) {
        loadingMask.style.display = 'none';
    }
}

function appendResult(html) {
    if (resultDom) {
        resultDom.innerHTML += `${html}<br/>`;
        resultDom.scrollTop = resultDom.scrollHeight;
    }
}

// ---------------------- 原生调用 JS 的方法（全局暴露）----------------------
window.callAdvanceJs = function(methodName, params) {
    logInfo(`收到原生调用：method=${methodName}，params=${params}`);
    let returnResult = {
        code: 200,
        message: "success",
        data: null
    };
    
    try {
        // 缓存常用方法名，避免重复字符串比较
        const METHOD_UPDATE_UI = "updateAdvanceUi";
        const METHOD_NOTIFY_CACHE = "notifyAdvanceCacheState";
        const METHOD_GET_DEVICE_INFO = "getAdvanceDeviceInfo";
        
        // 解析参数（仅当有参数时）
        const paramsObj = params ? JSON.parse(params) : {};
        
        if (methodName === METHOD_UPDATE_UI) {
            for (const [key, value] of Object.entries(paramsObj)) {
                logSuccess(`设备信息 - ${key}：${value}`);
            }
            returnResult.data = paramsObj;
        } else if (methodName === METHOD_NOTIFY_CACHE) {
            logSuccess(`缓存状态通知：${paramsObj}`);
            returnResult.data = { cacheState: paramsObj };
        } else if (methodName === METHOD_GET_DEVICE_INFO) {
            // 处理设备信息返回，关闭加载状态
            hideLoading();
            for (const [key, value] of Object.entries(paramsObj)) {
                logSuccess(`设备信息 - ${key}：${value}`);
            }
            // 返回设备信息数据
            returnResult.data = paramsObj;
        } else {
            logInfo(`未知原生调用方法：${methodName}`);
            returnResult.code = 404;
            returnResult.message = "unknown_method";
        }
    } catch (e) {
        logError(`解析原生调用参数失败：${e.message}，原始参数：${params}`);
        // 发生错误时也要关闭加载状态
        hideLoading();
        returnResult.code = 500;
        returnResult.message = e.message;
    }
    
    // 返回JSON字符串给原生
    return JSON.stringify(returnResult);
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