/**
 * 性能优化测试 JS - 修复版
 * 修复点：1. 提前定义 logImageLoad 避免图片onload找不到 2. 移除jQuery 3. 全局暴露方法
 */
//const AdvanceGlobalTool = window.AdvanceGlobalTool;
//const AdvanceBusinessLogic = window.AdvanceBusinessLogic;
const pageLoadStartTime = new Date().getTime();

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

// ---------------------- 关键修复：提前定义 logImageLoad ----------------------
window.logImageLoad = function(imageIndex) {
    logSuccess(`图片 ${imageIndex} 加载完成`);
    const globalTool = getAdvanceGlobalTool(); // 实时获取
    if (globalTool) {
        globalTool.log(`图片 ${imageIndex} 加载完成（性能监控）`);
    }
};

// ---------------------- 全局暴露方法 ----------------------
window.updatePageProgress = function(progress) {
    if (progress < 0) progress = 0;
    if (progress > 100) progress = 100;
    const progressDom = document.getElementById("loadProgress");
    const fillDom = document.getElementById("progressFill");
    if (progressDom && fillDom) {
        progressDom.innerText = `${progress}%`;
        fillDom.style.width = `${progress}%`;
    }
    progress === 100 ? logSuccess("页面加载完成，进度 100%") : logInfo(`页面加载中，进度 ${progress}%`);
};

// ---------------------- 辅助方法 ----------------------
function logInfo(content) {
    appendResult(`<span class="info">[PERF-INFO] ${new Date().toLocaleTimeString()}：${content}</span>`);
}

function logSuccess(content) {
    appendResult(`<span class="success">[PERF-SUCCESS] ${new Date().toLocaleTimeString()}：${content}</span>`);
}

function logError(content) {
    appendResult(`<span class="error">[PERF-ERROR] ${new Date().toLocaleTimeString()}：${content}</span>`);
}

function appendResult(html) {
    const resultDom = document.getElementById("advanceInjectResult");
    if (resultDom) {
        resultDom.innerHTML += `${html}<br/>`;
        resultDom.scrollTop = resultDom.scrollHeight;
    }
}

function initPerfMetrics() {
    const businessLogic = getAdvanceBusinessLogic(); // 实时获取
    if (businessLogic && businessLogic.businessData) {
        const perfData = businessLogic.businessData;
        const renderModeDom = document.getElementById("renderMode");
        const imageStateDom = document.getElementById("imageState");
        const cacheModeDom = document.getElementById("cacheMode");
        if (renderModeDom) renderModeDom.innerText = perfData.hardwareAcceleration === "true" ? "硬件加速" : "软件渲染";
        if (imageStateDom) imageStateDom.innerText = perfData.imageLoadingEnabled === "true" ? "已启用" : "已禁用";
        if (cacheModeDom) cacheModeDom.innerText = perfData.currentCacheMode || "未知";
        logSuccess(`渲染模式：${renderModeDom?.innerText}`);
        logSuccess(`图片加载状态：${imageStateDom?.innerText}`);
        logSuccess(`缓存模式：${cacheModeDom?.innerText}`);
    } else {
        logError("无法获取性能配置：AdvanceBusinessLogic 未注入");
    }
}

// ---------------------- 原生调用 JS 的方法 ----------------------
window.callAdvanceJs = function(methodName, params) {
    try {
        const paramsObj = JSON.parse(params);
        switch (methodName) {
            case "notifyAdvanceCacheState":
                logInfo(`性能影响：${paramsObj}`);
                break;
            case "updateAdvanceUi":
                if (paramsObj.hardwareAcceleration) {
                    const renderModeDom = document.getElementById("renderMode");
                    if (renderModeDom) {
                        renderModeDom.innerText = paramsObj.hardwareAcceleration === "true" ? "硬件加速" : "软件渲染";
                        logSuccess(`渲染模式更新：${renderModeDom.innerText}`);
                    }
                }
                if (paramsObj.imageLoadingEnabled) {
                    const imageStateDom = document.getElementById("imageState");
                    if (imageStateDom) {
                        imageStateDom.innerText = paramsObj.imageLoadingEnabled === "true" ? "已启用" : "已禁用";
                        logSuccess(`图片加载状态更新：${imageStateDom.innerText}`);
                    }
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
    logInfo("性能测试页面初始化完成，开始监控性能指标...");
    logInfo(`页面加载开始时间：${new Date(pageLoadStartTime).toLocaleTimeString()}`);
    initPerfMetrics();
    // 计算首屏加载时间
    setTimeout(() => {
        const firstLoadTime = (new Date().getTime() - pageLoadStartTime) / 1000;
        const firstLoadDom = document.getElementById("firstLoadTime");
        if (firstLoadDom) firstLoadDom.innerText = `${firstLoadTime.toFixed(2)} 秒`;
        logSuccess(`首屏加载完成，耗时：${firstLoadTime.toFixed(2)} 秒`);
    }, 2000);
});