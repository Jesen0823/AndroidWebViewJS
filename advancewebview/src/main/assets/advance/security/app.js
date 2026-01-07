/**
 * 安全防御测试 JS - 优化版
 * 优化点：
 * 1. 抽离通用工具方法，消除重复代码
 * 2. 增强原生对象注入的容错与重试逻辑
 * 3. 完善 XSS/URL 校验规则，联动原生校验
 * 4. 统一日志格式与错误处理
 * 5. 复用 AdvanceNativeCallJsManager 交互逻辑
 */

// 全局配置常量
const CONFIG = {
    RETRY_TIMES: 3,          // 原生对象重试次数
    RETRY_DELAY: 200,        // 重试延迟(ms)
    MALICIOUS_KEYWORDS: ["<script>", "javascript:", "eval(", "alert(", "document.cookie", "localStorage"], // 增强恶意关键字
    SAFE_PROTOCOLS: ["https:", "http:", "file:///android_asset/"] // 信任协议
};

/**
 * 通用工具类 - 抽离重复逻辑
 */
const AdvanceJsTool = {
    /**
     * 安全获取全局对象（带重试和时间限制）
     * @param {string} objName 全局对象名
     * @param {number} retryTimes 剩余重试次数
     * @param {number} totalDelay 已累计延迟(ms)
     * @returns {Promise<Object>}
     */
    getGlobalObject: function(objName, retryTimes = CONFIG.RETRY_TIMES, totalDelay = 0) {
        return new Promise((resolve) => {
            const target = window[objName];
            if (target) {
                console.log(`[AdvanceJsTool] ${objName} 注入成功`);
                resolve(target);
                return;
            }
            if (retryTimes <= 0 || totalDelay >= 2000) { // 最多重试2秒
                console.warn(`[AdvanceJsTool] ${objName} 未注入，已重试 ${CONFIG.RETRY_TIMES} 次，累计延迟 ${totalDelay}ms`);
                resolve(null);
                return;
            }
            // 递增延迟重试，避免频繁重试
            const nextDelay = Math.min(CONFIG.RETRY_DELAY * Math.pow(1.5, CONFIG.RETRY_TIMES - retryTimes), 1000);
            setTimeout(() => {
                this.getGlobalObject(objName, retryTimes - 1, totalDelay + nextDelay).then(resolve);
            }, nextDelay);
        });
    },

    /**
     * 统一日志输出（增强版）
     * @param {string} type 日志类型：info/success/warn/error
     * @param {string} content 日志内容
     */
    log: function(type, content) {
        const typeMap = {
            info: "INFO",
            success: "SUCCESS",
            warn: "WARN",
            error: "ERROR"
        };
        const colorClass = type === "success" ? "success" : type === "warn" || type === "error" ? "error" : "info";
        const html = `<span class="${colorClass}">[SECURITY-${typeMap[type]}] ${new Date().toLocaleTimeString()}：${content}</span>`;
        
        const resultDom = document.getElementById("advanceInjectResult");
        if (resultDom) {
            resultDom.innerHTML += `${html}<br/>`;
            resultDom.scrollTop = resultDom.scrollHeight;
        }
        // 同步调用原生日志（复用AdvanceNativeCallJsManager）
        this.getGlobalObject("AdvanceGlobalTool").then(tool => {
            tool && tool.log(`[JS-${typeMap[type]}] ${content}`);
        });
    },

    /**
     * URL 安全校验（增强版）
     * @param {string} url 待校验URL
     * @returns {Object} 校验结果
     */
    checkUrlSafety: function(url) {
        if (!url) return { valid: false, message: "URL 不能为空" };
        
        // 1. 协议校验
        const isSafeProtocol = CONFIG.SAFE_PROTOCOLS.some(protocol => url.startsWith(protocol));
        // 2. 本地文件风险校验
        const isDangerousFile = url.startsWith("file:///") && !url.startsWith("file:///android_asset/");
        // 3. 特殊字符校验
        const hasDangerousChar = /[<>|*?%&$#]/g.test(url);

        if (hasDangerousChar) {
            return { valid: false, message: "URL 包含危险特殊字符" };
        }
        if (isDangerousFile) {
            return { valid: false, message: "禁止访问非Assets本地文件" };
        }
        if (!isSafeProtocol) {
            return { valid: false, message: "URL 协议不在信任列表" };
        }
        return { valid: true, message: "URL 前端校验通过" };
    },

    /**
     * XSS 内容过滤（增强版）
     * @param {string} content 待过滤内容
     * @returns {Object} 过滤结果
     */
    filterXssContent: function(content) {
        if (!content) return { original: "", filtered: "", message: "XSS 内容不能为空" };
        
        let filtered = content;
        // 1. 移除script标签（含嵌套）
        filtered = filtered.replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, "");
        // 2. 移除javascript伪协议
        filtered = filtered.replace(/javascript:/gi, "");
        // 3. 移除危险函数
        filtered = filtered.replace(/(eval|alert|prompt|confirm)\(/gi, "");
        // 4. 转义HTML特殊字符
        filtered = filtered.replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#39;");
        // 5. 检测恶意关键字
        const hasMalicious = CONFIG.MALICIOUS_KEYWORDS.some(keyword => content.includes(keyword));

        return {
            original: content,
            filtered: filtered,
            hasMalicious: hasMalicious,
            message: hasMalicious ? "过滤出恶意内容" : "未发现恶意内容"
        };
    }
};

// ---------------------- 全局暴露安全测试方法 ----------------------
window.callCheckUrlSafety = async function() {
    const testUrl = document.getElementById("testUrl")?.value.trim() || "";
    // 前端初步校验
    const checkResult = AdvanceJsTool.checkUrlSafety(testUrl);
    if (!checkResult.valid) {
        AdvanceJsTool.log("error", checkResult.message);
        return;
    }
    AdvanceJsTool.log("info", `开始校验 URL 安全性：${testUrl}`);
    AdvanceJsTool.log("info", checkResult.message);

    // 调用原生校验（复用AdvanceNativeCallJsManager）
    const globalTool = await AdvanceJsTool.getGlobalObject("AdvanceGlobalTool");
    if (globalTool) {
        // 传递URL给原生，由AdvanceNativeCallJsManager处理后续逻辑
        globalTool.checkUrlSafety(testUrl, (nativeResult) => {
            // 原生回调结果处理
            if (nativeResult.valid) {
                AdvanceJsTool.log("success", `原生校验通过：${nativeResult.message}`);
            } else {
                AdvanceJsTool.log("warn", `原生校验失败：${nativeResult.message}`);
            }
        });
    } else {
        AdvanceJsTool.log("warn", "未注入AdvanceGlobalTool，仅完成前端校验");
    }
};

window.callFilterXssContent = async function() {
    const testXss = document.getElementById("testXss")?.value.trim() || "";
    // 前端过滤
    const filterResult = AdvanceJsTool.filterXssContent(testXss);
    if (!filterResult.original) {
        AdvanceJsTool.log("error", filterResult.message);
        return;
    }
    AdvanceJsTool.log("info", `开始过滤 XSS 恶意内容：${testXss}`);
    
    if (filterResult.hasMalicious) {
        AdvanceJsTool.log("success", `XSS 过滤完成：${filterResult.filtered}`);
    } else {
        AdvanceJsTool.log("info", filterResult.message);
    }

    // 调用原生过滤（复用AdvanceNativeCallJsManager）
    const globalTool = await AdvanceJsTool.getGlobalObject("AdvanceGlobalTool");
    if (globalTool) {
        globalTool.filterXssContent(testXss, (nativeFiltered) => {
            AdvanceJsTool.log("success", `原生 XSS 过滤结果：${nativeFiltered}`);
        });
    } else {
        AdvanceJsTool.log("warn", "未注入AdvanceGlobalTool，仅完成前端过滤");
    }
};

// ---------------------- 原生调用 JS 的方法（复用AdvanceNativeCallJsManager） ----------------------
window.callAdvanceJs = async function(methodName, params) {
    try {
        // 增强参数校验
        if (typeof methodName !== "string" || !methodName) {
            AdvanceJsTool.log("error", "原生调用方法名不能为空");
            return;
        }
        const paramsObj = typeof params === "string" ? JSON.parse(params) : params;
        if (typeof paramsObj !== "object" || paramsObj === null) {
            AdvanceJsTool.log("error", "原生调用参数必须为JSON对象");
            return;
        }

        switch (methodName) {
            case "updateAdvanceUi":
                if (paramsObj.originalContent && paramsObj.filteredContent) {
                    AdvanceJsTool.log("success", `原生 XSS 过滤结果：`);
                    AdvanceJsTool.log("info", `原始内容：${paramsObj.originalContent}`);
                    AdvanceJsTool.log("info", `过滤后内容：${paramsObj.filteredContent}`);
                } else {
                    AdvanceJsTool.log("warn", "updateAdvanceUi 参数缺失：originalContent/filteredContent");
                }
                break;
            case "notifyAdvanceCacheState":
                AdvanceJsTool.log("info", `安全提示：${paramsObj.message || paramsObj}，敏感信息已同步清理`);
                break;
            case "updateSecurityConfig":
                // 新增：原生推送配置更新
                initSecurityConfig(paramsObj);
                break;
            case "notifyUrlCheckResult":
                // 新增：原生URL校验结果通知
                if (paramsObj.url && typeof paramsObj.valid === "boolean") {
                    const resultType = paramsObj.valid ? "success" : "warn";
                    AdvanceJsTool.log(resultType, `URL 校验结果：${paramsObj.url} - ${paramsObj.message || (paramsObj.valid ? "安全" : "危险")}`);
                }
                break;
            case "notifyXssFilterResult":
                // 新增：原生XSS过滤结果通知
                if (paramsObj.originalContent && paramsObj.filteredContent) {
                    AdvanceJsTool.log("success", `原生 XSS 过滤结果：`);
                    AdvanceJsTool.log("info", `原始内容：${paramsObj.originalContent}`);
                    AdvanceJsTool.log("info", `过滤后内容：${paramsObj.filteredContent}`);
                }
                break;
            default:
                AdvanceJsTool.log("info", `未知原生调用方法：${methodName}`);
        }
    } catch (e) {
        AdvanceJsTool.log("error", `解析原生调用参数失败：${e.message}，原始参数：${params}`);
    }
};

// ---------------------- 初始化逻辑 ----------------------
async function initSecurityConfig(manualConfig = null) {
    try {
        // 优先使用手动传入的配置（原生推送），否则从原生对象获取
        const securityData = manualConfig || (await AdvanceJsTool.getGlobalObject("AdvanceBusinessLogic"))?.businessData;
        if (!securityData) {
            AdvanceJsTool.log("warn", "安全配置加载中：AdvanceBusinessLogic 未完全注入");
            document.getElementById("securityConfig").innerHTML = "安全配置加载中...";
            // 增加重试逻辑
            setTimeout(() => {
                initSecurityConfig();
            }, 500);
            return;
        }

        const configHtml = `
            信任域名白名单：${securityData.safeDomainWhitelist || "未配置"}<br/>
            XSS 过滤功能：${securityData.xssFilterEnabled ? "开启" : "关闭"}<br/>
            文件访问权限：${securityData.fileAccessEnabled ? "仅Assets" : "禁止"}<br/>
            混合内容支持：${securityData.mixedContentEnabled ? "允许" : "禁止"}<br/>
            安全浏览功能：${securityData.safeBrowsingEnabled ? "开启" : "关闭"}
        `;
        const configDom = document.getElementById("securityConfig");
        if (configDom) configDom.innerHTML = configHtml;
        AdvanceJsTool.log("success", "安全配置信息加载完成");
    } catch (e) {
        AdvanceJsTool.log("error", `初始化安全配置失败：${e.message}`);
    }
}

async function detectMaliciousScript() {
    AdvanceJsTool.log("info", "开始执行恶意脚本检测...");
    const pageSource = document.documentElement.outerHTML;
    let hasRisk = false;
    
    // 高级恶意脚本检测逻辑
    // 1. 检测未闭合的script标签
    const unclosedScriptPattern = /<script\b[^>]*>(?:(?!<\/script>)[\s\S])*$/gi;
    if (unclosedScriptPattern.test(pageSource)) {
        AdvanceJsTool.log("warn", "检测到未闭合的script标签（可能是恶意注入）");
        hasRisk = true;
    }
    
    // 2. 检测内联事件中的恶意代码
    const inlineEventPattern = /on\w+\s*=\s*['"](?:javascript:|eval\(|alert\()/gi;
    if (inlineEventPattern.test(pageSource)) {
        AdvanceJsTool.log("warn", "检测到内联事件中的恶意代码");
        hasRisk = true;
    }
    
    // 3. 检测不在合法属性内的javascript:伪协议
    const maliciousJsProtocolPattern = /(?<!href=['"]|src=['"])javascript:(?!void\s*\()/gi;
    if (maliciousJsProtocolPattern.test(pageSource)) {
        AdvanceJsTool.log("warn", "检测到不在合法属性内的javascript:伪协议");
        hasRisk = true;
    }
    
    // 4. 检测危险函数的动态调用
    const dangerousFunctionPattern = /eval\(.*\)|new\s+Function\(.*\)/gi;
    if (dangerousFunctionPattern.test(pageSource)) {
        AdvanceJsTool.log("warn", "检测到动态代码执行函数调用");
        hasRisk = true;
    }
    
    // 5. 检测对敏感信息的访问
    const sensitiveInfoPattern = /document\.cookie|localStorage|sessionStorage/gi;
    // 排除本页面的合法访问（比如检测脚本自身）
    const scriptTags = document.getElementsByTagName('script');
    let pageScriptsContent = '';
    for (let i = 0; i < scriptTags.length; i++) {
        if (scriptTags[i].textContent) {
            pageScriptsContent += scriptTags[i].textContent;
        }
    }
    
    // 只有在非脚本标签内容中检测到敏感信息访问才报告
    const sensitiveMatch = pageSource.match(sensitiveInfoPattern);
    if (sensitiveMatch && !pageScriptsContent.includes(sensitiveMatch[0])) {
        AdvanceJsTool.log("warn", "检测到对敏感信息的访问");
        hasRisk = true;
    }
    
    if (!hasRisk) {
        AdvanceJsTool.log("success", "恶意脚本检测完成，未发现高危风险");
    } else {
        AdvanceJsTool.log("warn", "恶意脚本检测完成，发现潜在风险");
    }
}

// DOM 就绪初始化（异步避免阻塞）
document.addEventListener('DOMContentLoaded', async function() {
    AdvanceJsTool.log("info", "安全测试页面初始化完成，开始加载安全配置...");
    await initSecurityConfig();
    await detectMaliciousScript();
});

// 暴露工具类供原生调试（可选）
window.AdvanceJsTool = AdvanceJsTool;