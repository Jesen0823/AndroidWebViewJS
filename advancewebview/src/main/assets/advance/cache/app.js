/**
 * 缓存策略测试 JS - 修复版
 * 修复点：1. 移除jQuery 2. 全局暴露所有缓存操作方法 3. 实时更新缓存状态文案
 */
//const AdvanceGlobalTool = window.AdvanceGlobalTool;
//const AdvanceBusinessLogic = window.AdvanceBusinessLogic;
const CACHE_TEST_KEY = "advance_cache_test_key";

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

// ---------------------- 全局暴露缓存操作方法 ----------------------
window.callTestCacheLoad = function() {
    logInfo("开始测试缓存加载，即将刷新页面...");
    location.reload();
};

window.callClearCache = function() {
    const globalTool = getAdvanceGlobalTool(); // 实时获取
    if (globalTool) {
        globalTool.clearCache();
        logSuccess("请求原生清理所有缓存");
    } else {
        logError("无法调用 clearCache：AdvanceGlobalTool 未注入");
    }
};

window.callSaveCacheData = function() {
    const cacheValue = "cache_test_value_" + new Date().getTime();
    const globalTool = getAdvanceGlobalTool(); // 实时获取
    if (globalTool) {
        globalTool.setStorage(CACHE_TEST_KEY, cacheValue);
        logSuccess(`保存测试缓存数据：${CACHE_TEST_KEY}=${cacheValue}`);
    } else {
        logError("无法调用 setStorage：AdvanceGlobalTool 未注入");
    }
};

window.callGetCacheData = function() {
    const globalTool = getAdvanceGlobalTool(); // 实时获取
    if (globalTool) {
        globalTool.getStorage(CACHE_TEST_KEY);
        logInfo(`获取测试缓存数据：${CACHE_TEST_KEY}`);
    } else {
        logError("无法调用 getStorage：AdvanceGlobalTool 未注入");
    }
};

// ---------------------- 辅助方法 ----------------------
function logInfo(content) {
    appendResult(`<span class="info">[CACHE-INFO] ${new Date().toLocaleTimeString()}：${content}</span>`);
}

function logSuccess(content) {
    appendResult(`<span class="success">[CACHE-SUCCESS] ${new Date().toLocaleTimeString()}：${content}</span>`);
}

function logError(content) {
    appendResult(`<span class="error">[CACHE-ERROR] ${new Date().toLocaleTimeString()}：${content}</span>`);
}

function appendResult(html) {
    const resultDom = document.getElementById("advanceInjectResult");
    if (resultDom) {
        resultDom.innerHTML += `${html}<br/>`;
        resultDom.scrollTop = resultDom.scrollHeight;
    }
}

function initCacheConfig() {
    if (AdvanceBusinessLogic && AdvanceBusinessLogic.businessData) {
        const cacheData = AdvanceBusinessLogic.businessData;
        const maxSizeDom = document.getElementById("cacheMaxSize");
        const expireDom = document.getElementById("cacheExpireTime");
        const dirDom = document.getElementById("cacheDir");
        const modeDom = document.getElementById("cacheMode");
        if (maxSizeDom) maxSizeDom.innerText = cacheData.cacheMaxSize || "未知";
        if (expireDom) expireDom.innerText = cacheData.cacheExpireTime || "未知";
        if (dirDom) dirDom.innerText = cacheData.cacheDir || "未知";
        if (modeDom) modeDom.innerText = cacheData.currentCacheMode || "未知";
        logSuccess("缓存配置信息加载完成");
    } else {
        logError("无法获取缓存配置：AdvanceBusinessLogic 未注入");
    }
}

function checkLocalCacheData() {
    const globalTool = getAdvanceGlobalTool(); // 实时获取
    if (globalTool) {
        const cacheValue = globalTool.getStorage(CACHE_TEST_KEY);
        cacheValue ? logSuccess(`检测到本地缓存数据：${CACHE_TEST_KEY}=${cacheValue}`) : logInfo(`未检测到本地缓存数据：${CACHE_TEST_KEY}`);
    }
}

// ---------------------- 原生调用 JS 的方法 ----------------------
window.callAdvanceJs = function(methodName, params) {
    try {
        const paramsObj = JSON.parse(params);
        switch (methodName) {
            case "notifyAdvanceCacheState":
                logSuccess(`缓存状态通知：${paramsObj}`);
                if (paramsObj.networkState) {
                    const networkDom = document.getElementById("networkState");
                    if (networkDom) networkDom.innerText = paramsObj.networkState;
                    logInfo(`网络状态更新：${paramsObj.networkState}`);
                }
                break;
            case "updateAdvanceUi":
                if (paramsObj.currentCacheMode) {
                    const modeDom = document.getElementById("cacheMode");
                    if (modeDom) modeDom.innerText = paramsObj.currentCacheMode;
                    logSuccess(`缓存模式更新：${paramsObj.currentCacheMode}`);
                }
                break;
            default:
                logInfo(`未知原生调用方法：${methodName}`);
        }
    } catch (e) {
        logError(`解析原生调用参数失败：${e.message}`);
    }
};

// ---------------------- DOM 就绪初始化 ----------------------
document.addEventListener('DOMContentLoaded', function() {
    logInfo("缓存测试页面初始化完成，开始加载缓存配置...");
    initCacheConfig();
    checkLocalCacheData();
});