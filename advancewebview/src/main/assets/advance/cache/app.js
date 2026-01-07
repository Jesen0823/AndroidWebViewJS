'use strict';

/**
 * 缓存策略测试 JS - 规范优化版
 * 核心优化：修复Android交互逻辑+消除冗余+强化错误处理
 */
const CACHE_CONST = {
  TEST_KEY: "advance_cache_test_key",
  RETRY_DELAY: 100, // 重试延迟(ms)
  MAX_RETRY: 2      // 最大重试次数
};

// DOM元素缓存（避免重复查询）
const DOM_CACHE = {
  cacheMaxSize: document.getElementById('cacheMaxSize'),
  cacheExpireTime: document.getElementById('cacheExpireTime'),
  cacheDir: document.getElementById('cacheDir'),
  cacheMode: document.getElementById('cacheMode'),
  networkState: document.getElementById('networkState'),
  advanceInjectResult: document.getElementById('advanceInjectResult'),
  btnTestCacheLoad: document.getElementById('btnTestCacheLoad'),
  btnClearCache: document.getElementById('btnClearCache'),
  btnSaveCacheData: document.getElementById('btnSaveCacheData'),
  btnGetCacheData: document.getElementById('btnGetCacheData')
};

/**
 * 异步获取原生工具类（Promise化重试，适配Android注入延迟）
 * @returns {Promise<Object>} AdvanceGlobalTool
 */
async function getAdvanceGlobalTool() {
  let retryCount = 0;
  while (retryCount < CACHE_CONST.MAX_RETRY) {
    if (window.AdvanceGlobalTool) return window.AdvanceGlobalTool;
    await new Promise(resolve => setTimeout(resolve, CACHE_CONST.RETRY_DELAY));
    retryCount++;
  }
  console.warn('AdvanceGlobalTool 未注入（已重试最大次数）');
  throw new Error('AdvanceGlobalTool 注入失败');
}

/**
 * 异步获取原生业务逻辑类（Promise化重试）
 * @returns {Promise<Object>} AdvanceBusinessLogic
 */
async function getAdvanceBusinessLogic() {
  let retryCount = 0;
  while (retryCount < CACHE_CONST.MAX_RETRY) {
    if (window.AdvanceBusinessLogic) return window.AdvanceBusinessLogic;
    await new Promise(resolve => setTimeout(resolve, CACHE_CONST.RETRY_DELAY));
    retryCount++;
  }
  console.warn('AdvanceBusinessLogic 未注入（已重试最大次数）');
  throw new Error('AdvanceBusinessLogic 注入失败');
}

// ---------------------- 日志工具 ----------------------
function appendResult(html) {
  if (!DOM_CACHE.advanceInjectResult) return;
  DOM_CACHE.advanceInjectResult.innerHTML += `${html}<br/>`;
  DOM_CACHE.advanceInjectResult.scrollTop = DOM_CACHE.advanceInjectResult.scrollHeight;
}

function logInfo(content) {
  appendResult(`<span class="info">[CACHE-INFO] ${new Date().toLocaleTimeString()}：${content}</span>`);
}

function logSuccess(content) {
  appendResult(`<span class="success">[CACHE-SUCCESS] ${new Date().toLocaleTimeString()}：${content}</span>`);
}

function logError(content) {
  appendResult(`<span class="error">[CACHE-ERROR] ${new Date().toLocaleTimeString()}：${content}</span>`);
}

// ---------------------- UI更新核心方法（全局暴露给Android） ----------------------
window.updateAdvanceUi = function(params) {
  try {
    // 防御性解析Android传递的参数（兼容JSON字符串/对象）
    const config = typeof params === 'string' 
      ? JSON.parse(params.trim()) 
      : (typeof params === 'object' ? params : {});
    
    logInfo(`收到Android推送数据：${JSON.stringify(config)}`);

    // 更新UI（复用DOM缓存）
    if (DOM_CACHE.cacheMaxSize && config.cacheMaxSize) {
      DOM_CACHE.cacheMaxSize.innerText = config.cacheMaxSize;
    }
    if (DOM_CACHE.cacheExpireTime && config.cacheExpireTime) {
      DOM_CACHE.cacheExpireTime.innerText = config.cacheExpireTime;
    }
    if (DOM_CACHE.cacheDir && config.cacheDir) {
      DOM_CACHE.cacheDir.innerText = config.cacheDir;
    }
    if (DOM_CACHE.cacheMode && config.currentCacheMode) {
      DOM_CACHE.cacheMode.innerText = config.currentCacheMode;
    }
    if (DOM_CACHE.networkState && config.networkState) {
      DOM_CACHE.networkState.innerText = config.networkState;
    }
  } catch (e) {
    logError(`解析Android参数失败：${e.message}`);
  }
};

// ---------------------- 缓存操作方法（供按钮调用） ----------------------
async function callTestCacheLoad() {
  logInfo('开始测试缓存加载，即将刷新页面...');
  setTimeout(() => location.reload(), 300);
}

async function callClearCache() {
  const btn = DOM_CACHE.btnClearCache;
  if (btn) btn.disabled = true; // 防重复点击
  try {
    const globalTool = await getAdvanceGlobalTool();
    globalTool.clearCache();
    logSuccess('请求Android清理所有缓存');
  } catch (e) {
    logError(`调用clearCache失败：${e.message}`);
  } finally {
    if (btn) setTimeout(() => btn.disabled = false, 1000);
  }
}

async function callSaveCacheData() {
  const btn = DOM_CACHE.btnSaveCacheData;
  if (btn) btn.disabled = true;
  try {
    const cacheValue = `cache_test_value_${new Date().getTime()}`;
    const globalTool = await getAdvanceGlobalTool();
    globalTool.setStorage(CACHE_CONST.TEST_KEY, cacheValue);
    logSuccess(`保存测试缓存：${CACHE_CONST.TEST_KEY}=${cacheValue}`);
  } catch (e) {
    logError(`调用setStorage失败：${e.message}`);
  } finally {
    if (btn) setTimeout(() => btn.disabled = false, 1000);
  }
}

async function callGetCacheData() {
  const btn = DOM_CACHE.btnGetCacheData;
  if (btn) btn.disabled = true;
  try {
    const globalTool = await getAdvanceGlobalTool();
    const cacheValue = await globalTool.getStorage(CACHE_CONST.TEST_KEY);
    cacheValue 
      ? logSuccess(`获取缓存数据：${CACHE_CONST.TEST_KEY}=${cacheValue}`)
      : logInfo(`缓存数据不存在：${CACHE_CONST.TEST_KEY}`);
  } catch (e) {
    logError(`调用getStorage失败：${e.message}`);
  } finally {
    if (btn) setTimeout(() => btn.disabled = false, 1000);
  }
}

/**
 * 检测本地缓存数据
 */
async function checkLocalCacheData() {
  try {
    const globalTool = await getAdvanceGlobalTool();
    const cacheValue = await globalTool.getStorage(CACHE_CONST.TEST_KEY);
    cacheValue 
      ? logSuccess(`检测到缓存：${CACHE_CONST.TEST_KEY}=${cacheValue}`)
      : logInfo(`未检测到缓存：${CACHE_CONST.TEST_KEY}`);
  } catch (e) {
    logError(`检测缓存失败：${e.message}`);
  }
}

// ---------------------- Android调用JS的统一入口 ----------------------
window.callAdvanceJs = function(methodName, params) {
  try {
    const paramsObj = typeof params === 'string' 
      ? JSON.parse(params.trim()) 
      : (typeof params === 'object' ? params : {});
    
    switch (methodName) {
      case 'notifyAdvanceCacheState':
        logSuccess(`缓存状态通知：${JSON.stringify(paramsObj)}`);
        if (paramsObj.networkState && DOM_CACHE.networkState) {
          DOM_CACHE.networkState.innerText = paramsObj.networkState;
          logInfo(`网络状态更新：${paramsObj.networkState}`);
        }
        break;
      case 'updateAdvanceUi':
        // 复用全局UI更新方法，消除冗余
        window.updateAdvanceUi(paramsObj);
        break;
      default:
        logInfo(`未知Android调用方法：${methodName}`);
    }
  } catch (e) {
    logError(`解析Android调用参数失败：${e.message}`);
  }
};

// ---------------------- 初始化 ----------------------
document.addEventListener('DOMContentLoaded', async function() {
  logInfo('缓存测试页面初始化完成，等待Android交互...');

  // 绑定按钮事件（解耦HTML与JS）
  DOM_CACHE.btnTestCacheLoad?.addEventListener('click', callTestCacheLoad);
  DOM_CACHE.btnClearCache?.addEventListener('click', callClearCache);
  DOM_CACHE.btnSaveCacheData?.addEventListener('click', callSaveCacheData);
  DOM_CACHE.btnGetCacheData?.addEventListener('click', callGetCacheData);

  // 初始加载Android注入的缓存配置
  try {
    const businessLogic = await getAdvanceBusinessLogic();
    if (businessLogic?.businessData) {
      window.updateAdvanceUi(businessLogic.businessData);
      logSuccess('从Android BusinessLogic读取初始缓存配置');
    }
  } catch (e) {
    logInfo(`等待Android推送缓存配置：${e.message}`);
  }

  // 检测本地缓存
  await checkLocalCacheData();
});