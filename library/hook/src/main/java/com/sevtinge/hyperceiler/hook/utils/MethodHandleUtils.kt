package com.sevtinge.hyperceiler.hook.utils

import de.robv.android.xposed.XposedHelpers
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method

/**
 * 方法句柄工具类
 */
@Suppress("unused")
object MethodHandleUtils {
    @JvmStatic
    fun invokeSuperMethod(
        obj: Any,
        methodName: String,
        vararg toArgs: Any?
    ): Any? = invokeSpecialMethod(
        obj,
        XposedHelpers.findMethodBestMatch(obj.javaClass.superclass, methodName, *toArgs),
        *toArgs
    )

    @JvmStatic
    fun invokeSpecialMethod(
        obj: Any,
        refClass: Class<*>,
        methodName: String,
        vararg args: Any?
    ): Any? = invokeSpecialMethod(
        obj,
        XposedHelpers.findMethodBestMatch(refClass, methodName, *args),
        *args
    )

    @JvmStatic
    fun invokeSpecialMethod(
        obj: Any,
        method: Method,
        vararg args: Any?
    ): Any? = MethodHandles.privateLookupIn(obj.javaClass, MethodHandles.lookup())
        .unreflectSpecial(method, obj.javaClass)
        .invokeWithArguments(obj, *args)
}
