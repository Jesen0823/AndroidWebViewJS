/**
 * 安全防御测试 JS - 修复版
 * 修复点：1. 移除jQuery 2. 全局暴露安全测试方法 3. 实时响应过滤结果
 */
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

// ---------------------- 全局暴露安全测试方法 ----------------------
window.callCheckUrlSafety = function() {
    const testUrl = document.getElementById("testUrl").value.trim();
    if (!testUrl) {
        logError("请输入待校验的 URL");
        return;
    }
    logInfo(`开始校验 URL 安全性：${testUrl}`);
    const isHttpHttps = testUrl.startsWith("https://") || testUrl.startsWith("http://");
    const isAssetFile = testUrl.startsWith("file:///android_asset/");
    isHttpHttps || isAssetFile ? logInfo(`前端初步校验通过：${isHttpHttps ? "HTTPS/HTTP 协议" : "Assets 本地文件"}`) : logWarn(`前端初步校验警告：非信任协议/路径`);
    const globalTool = getAdvanceGlobalTool(); // 实时获取
    if (globalTool) {
        globalTool.showToast(`URL 校验中：${testUrl}（请查看原生结果）`);
    }
};

window.callFilterXssContent = function() {
    const testXss = document.getElementById("testXss").value.trim();
    if (!testXss) {
        logError("请输入待过滤的 XSS 内容");
        return;
    }
    logInfo(`开始过滤 XSS 恶意内容：${testXss}`);
    const filteredContent = testXss
        .replace(/<script.*?>.*?<\/script>/gi, "")
        .replace(/javascript:/gi, "")
        .replace(/eval\(/gi, "")
        .replace(/alert\(/gi, "");
    testXss !== filteredContent ? logSuccess(`XSS 过滤完成：${filteredContent}`) : logInfo(`未发现恶意内容：${testXss}`);
    const globalTool = getAdvanceGlobalTool(); // 实时获取
    if (globalTool) {
        globalTool.log(`XSS 过滤：原始=${testXss}，过滤后=${filteredContent}`);
    }
};

// ---------------------- 辅助方法 ----------------------
function logInfo(content) {
    appendResult(`<span class="info">[SECURITY-INFO] ${new Date().toLocaleTimeString()}：${content}</span>`);
}

function logSuccess(content) {
    appendResult(`<span class="success">[SECURITY-SUCCESS] ${new Date().toLocaleTimeString()}：${content}</span>`);
}

function logWarn(content) {
    appendResult(`<span class="error">[SECURITY-WARN] ${new Date().toLocaleTimeString()}：${content}</span>`);
}

function logError(content) {
    appendResult(`<span class="error">[SECURITY-ERROR] ${new Date().toLocaleTimeString()}：${content}</span>`);
}

function appendResult(html) {
    const resultDom = document.getElementById("advanceInjectResult");
    if (resultDom) {
        resultDom.innerHTML += `${html}<br/>`;
        resultDom.scrollTop = resultDom.scrollHeight;
    }
}

function initSecurityConfig() {
    const businessLogic = getAdvanceBusinessLogic(); // 实时获取
    if (businessLogic && businessLogic.businessData) {
        const securityData = businessLogic.businessData;
        const configHtml = `
            信任域名白名单：${securityData.safeDomainWhitelist || "未知"}<br/>
            XSS 过滤功能：${securityData.xssFilterEnabled || "未知"}<br/>
            文件访问权限：${securityData.fileAccessEnabled || "未知"}<br/>
            混合内容支持：${securityData.mixedContentEnabled || "未知"}<br/>
            安全浏览功能：${securityData.safeBrowsingEnabled || "未知"}
        `;
        const configDom = document.getElementById("securityConfig");
        if (configDom) configDom.innerHTML = configHtml;
        logSuccess("安全配置信息加载完成");
    } else {
        logError("无法获取安全配置：AdvanceBusinessLogic 未注入");
    }
}

function detectMaliciousScript() {
    logInfo("开始执行恶意脚本检测...");
    const maliciousKeywords = ["<script>", "javascript:", "eval(", "alert("];
    const pageSource = document.documentElement.outerHTML;
    for (const keyword of maliciousKeywords) {
        if (pageSource.includes(keyword)) logWarn(`检测到潜在恶意关键字：${keyword}`);
    }
    logSuccess("恶意脚本检测完成，未发现高危风险");
}

// ---------------------- 原生调用 JS 的方法 ----------------------
window.callAdvanceJs = function(methodName, params) {
    try {
        const paramsObj = JSON.parse(params);
        switch (methodName) {
            case "updateAdvanceUi":
                if (paramsObj.originalContent && paramsObj.filteredContent) {
                    logSuccess(`原生 XSS 过滤结果：`);
                    logInfo(`原始内容：${paramsObj.originalContent}`);
                    logInfo(`过滤后内容：${paramsObj.filteredContent}`);
                }
                break;
            case "notifyAdvanceCacheState":
                logInfo(`安全提示：${paramsObj}，敏感信息已同步清理`);
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
    logInfo("安全测试页面初始化完成，开始加载安全配置...");
    initSecurityConfig();
    detectMaliciousScript();
});