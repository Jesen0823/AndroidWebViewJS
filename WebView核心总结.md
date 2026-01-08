# Android WebView 核心总结

## 1. Android WebView 与 H5 JS 互相通信

### 核心机制
WebView 与 JS 通信通过 **桥接机制** 实现，分为两种方向：

### (1) 原生调用 JS

#### 关键步骤
| 版本要求 | 方法 | 特点 |
|---------|------|------|
| Android 4.4+ | `evaluateJavascript()` | 异步执行，支持返回值，无弹窗 |
| Android 4.4- | `loadUrl("javascript:...")` | 同步执行，无返回值，可能有弹窗 |

#### 实现示例
```kotlin
// Android 4.4+
webView.evaluateJavascript("javascript:callJsFunction('参数')") { result ->
    // 处理JS返回结果
}

// Android 4.4-
webView.loadUrl("javascript:callJsFunction('参数')")
```

### (2) JS 调用原生

#### 关键步骤
1. **创建通信接口**：定义包含 `@JavascriptInterface` 注解的方法
2. **注入对象**：使用 `addJavascriptInterface()` 将接口注入WebView
3. **JS调用**：通过注入的对象名调用原生方法

#### 实现示例
```kotlin
// 1. 定义通信接口
class JsBridge {
    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

// 2. 注入对象
webView.addJavascriptInterface(JsBridge(), "AndroidBridge")
```

```javascript
// 3. JS调用原生
AndroidBridge.showToast("Hello from JS");
```

### 安全注意事项
- **Android 4.2+**：必须使用 `@JavascriptInterface` 注解
- **版本兼容**：4.2以下存在安全漏洞，避免使用
- **参数校验**：过滤XSS等恶意输入

## 2. WebView 性能与缓存优化

### (1) 性能优化策略

| 优化方向 | 具体措施 | 版本注意 |
|---------|---------|----------|
| **渲染优化** | 启用硬件加速（`setLayerType(LAYER_TYPE_HARDWARE)`） | Android 3.0+支持，4.0+稳定 |
| **资源加载** | 图片延迟加载、JS/CSS压缩合并 | 全版本适用 |
| **内存管理** | `onDestroy()` 时销毁WebView，释放资源 | 避免内存泄漏 |
| **网络优化** | DNS预解析、HTTP/2、资源预加载 | Android 5.0+支持HTTP/2 |
| **线程管理** | JS执行在WebView线程，避免阻塞UI | 全版本适用 |

### (2) 缓存优化策略

#### 缓存类型与控制
| 缓存类型 | 配置方法 | 适用场景 |
|---------|---------|----------|
| **页面缓存** | `webSettings.cacheMode` | LOAD_DEFAULT（默认）、LOAD_CACHE_ONLY等 |
| **应用缓存** | `setAppCacheEnabled(true)` | 静态资源缓存 |
| **DOM存储** | `setDomStorageEnabled(true)` | 页面数据存储 |
| **数据库缓存** | `setDatabaseEnabled(true)` | 结构化数据存储 |
| **文件系统** | `setAllowFileAccess(true)` | 文件访问控制 |

#### 缓存模式选择
- `LOAD_DEFAULT`：有网加载新数据，无网加载缓存
- `LOAD_CACHE_ONLY`：仅加载缓存，不访问网络
- `LOAD_NO_CACHE`：不加载缓存，仅访问网络
- `LOAD_CACHE_ELSE_NETWORK`：优先加载缓存，无缓存再访问网络

### (3) 版本差异与注意事项

| Android版本 | WebView特点 | 注意事项 |
|------------|------------|----------|
| **4.4+** | 基于Chromium内核 | 性能大幅提升，支持现代Web标准 |
| **5.0+** | 多进程架构 | 资源隔离，内存占用增加 |
| **6.0+** | 权限模型变更 | 危险权限需要动态申请 |
| **7.0+** | 混合内容限制 | 默认阻止HTTP资源在HTTPS页面加载 |
| **8.0+** | 安全增强 | 限制非HTTPS页面的地理定位等功能 |
| **9.0+** | 明文流量限制 | 默认禁止HTTP，需配置networkSecurityConfig |

### 优化最佳实践
1. **按需启用功能**：关闭不必要的WebView特性（如地理位置、传感器）
2. **合理使用缓存**：根据业务场景选择合适的缓存策略
3. **监控性能**：使用WebView的性能API监控加载时间和资源消耗
4. **安全优先**：遵循最新的安全规范，避免安全漏洞
5. **版本适配**：针对不同Android版本采用相应的优化策略

## 记忆要点总结

### WebView与JS通信
- **原生→JS**：`evaluateJavascript`(4.4+) 或 `loadUrl`
- **JS→原生**：`@JavascriptInterface` + `addJavascriptInterface`
- **安全**：4.2+必须用注解，参数需校验

### 性能与缓存优化
- **性能**：渲染(硬件加速)、资源(压缩)、内存(正确销毁)
- **缓存**：页面缓存(LOAD_*)、应用缓存、DOM存储
- **版本**：4.4+Chromium、5.0+多进程、9.0+禁止明文

以上要点覆盖了Android WebView的核心问题。