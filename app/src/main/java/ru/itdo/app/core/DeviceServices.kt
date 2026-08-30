package ru.itdo.app.core

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.android.gms.common.ConnectionResult as GmsConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.huawei.hms.api.ConnectionResult as HmsConnectionResult
import com.huawei.hms.api.HuaweiApiAvailability

private const val TAG = "DeviceServices"

enum class MobileServices { GMS, HMS, NONE }

/**
 * Определение мобильных сервисов на устройстве.
 *
 * Зачем это нужно: часть возможностей приложения (сейчас — Downloadable
 * Fonts для логотипа "ITDO", см. ui/theme/Type.kt; в будущем — пуш-
 * уведомления) физически требует Google Play Services. На Huawei/Honor с
 * EMUI/MagicOS без Google эти сервисы обычно отсутствуют, а вместо них
 * есть HMS Core — их и нужно проверять отдельно, а не считать отсутствие
 * GMS поломкой.
 *
 * Порядок: если есть GMS — используем GMS (даже на Huawei/Honor изредка
 * попадаются прошивки с Google). Если GMS нет, но устройство Huawei/Honor
 * и на нём есть HMS Core — используем HMS. Иначе — NONE (не Huawei/Honor
 * и Google-сервисов тоже нет; функции, завязанные на них, просто
 * выключаются, без падения).
 */
object DeviceServices {

    /** true для Huawei и Honor (Honor — отдельный бренд после продажи Huawei в 2020, но часть прошивок ещё репортит "HUAWEI"/"HONOR" по-разному в MANUFACTURER/BRAND). */
    fun isHuaweiOrHonor(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return manufacturer.contains("huawei") || manufacturer.contains("honor") ||
            brand.contains("huawei") || brand.contains("honor")
    }

    fun hasGms(context: Context): Boolean = runCatching {
        GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == GmsConnectionResult.SUCCESS
    }.getOrElse {
        Log.w(TAG, "GMS check failed", it)
        false
    }

    fun hasHms(context: Context): Boolean = runCatching {
        HuaweiApiAvailability.getInstance()
            .isHuaweiMobileServicesAvailable(context) == HmsConnectionResult.SUCCESS
    }.getOrElse {
        Log.w(TAG, "HMS check failed", it)
        false
    }

    fun detect(context: Context): MobileServices {
        val huaweiOrHonor = isHuaweiOrHonor()
        val gms = hasGms(context)
        val result = when {
            gms -> MobileServices.GMS
            huaweiOrHonor && hasHms(context) -> MobileServices.HMS
            else -> MobileServices.NONE
        }
        Log.i(TAG, "manufacturer=${Build.MANUFACTURER} brand=${Build.BRAND} huaweiOrHonor=$huaweiOrHonor gms=$gms -> $result")
        return result
    }
}
