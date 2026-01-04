package org.dev.jesen.advancewebview.advance.helper

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * 反射工具类
 * 核心：安全查找方法/字段，不存在则返回null，调用失败则容错，不崩溃
 */
object AdvanceReflectUtils {

    /**
     * 安全查找对象的指定方法（无参/有参）
     * @param obj 目标对象
     * @param methodName 方法名
     * @param parameterTypes 方法参数类型数组
     * @return 找到的方法（null表示不存在）
     */
    fun findMethod(
        obj: Any,
        methodName: String,
        vararg parameterTypes: Class<*>
    ): Method? {
        return try {
            val clazz = obj.javaClass
            // 优先查找public方法，找不到则查找声明方法（包括protected/private）
            var method: Method? = clazz.getMethod(methodName, *parameterTypes)
            if (method == null) {
                method = clazz.getDeclaredMethod(methodName, *parameterTypes)
                method.isAccessible = true // 突破访问权限限制
            }
            AdvanceLogUtils.d("AdvanceReflectUtils", "找到方法：$methodName")
            method
        } catch (e: NoSuchMethodException) {
            AdvanceLogUtils.e("AdvanceReflectUtils", "未找到方法：$methodName，${e.message}")
            null
        } catch (e: Exception) {
            AdvanceLogUtils.e("AdvanceReflectUtils", "查找方法 $methodName 异常：${e.message}", e)
            null
        }
    }

    /**
     * 安全调用无返回值方法
     * @param obj 目标对象
     * @param methodName 方法名
     * @param params 方法参数
     * @return 是否调用成功（true=成功，false=失败/方法不存在）
     */
    fun invokeVoidMethod(
        obj: Any,
        methodName: String,
        vararg params: Any?
    ): Boolean {
        return try {
            // 构建参数类型数组
            val parameterTypes = params.mapNotNull { it?.javaClass }.toTypedArray()
            // 查找方法
            val method = findMethod(obj, methodName, *parameterTypes) ?: return false
            // 调用方法
            method.invoke(obj, *params)
            AdvanceLogUtils.d("AdvanceReflectUtils", "方法 $methodName 调用成功")
            true
        } catch (e: IllegalAccessException) {
            AdvanceLogUtils.e("AdvanceReflectUtils", "方法 $methodName 访问权限异常：${e.message}")
            false
        } catch (e: InvocationTargetException) {
            AdvanceLogUtils.e("AdvanceReflectUtils", "方法 $methodName 调用目标异常：${e.message}")
            false
        } catch (e: Exception) {
            AdvanceLogUtils.e("AdvanceReflectUtils", "方法 $methodName 调用失败：${e.message}", e)
            false
        }
    }

    /**
     * 安全调用有返回值方法
     * @param obj 目标对象
     * @param methodName 方法名
     * @param params 方法参数
     * @return 方法返回值（null表示调用失败/方法不存在/无返回值）
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> invokeReturnMethod(
        obj: Any,
        methodName: String,
        vararg params: Any?
    ): T? {
        return try {
            val parameterTypes = params.mapNotNull { it?.javaClass }.toTypedArray()
            val method = findMethod(obj, methodName, *parameterTypes) ?: return null
            val result = method.invoke(obj, *params) as? T
            AdvanceLogUtils.d("AdvanceReflectUtils", "方法 $methodName 调用成功，返回值：$result")
            result
        } catch (e: Exception) {
            AdvanceLogUtils.e("AdvanceReflectUtils", "方法 $methodName 调用失败：${e.message}", e)
            null
        }
    }
}