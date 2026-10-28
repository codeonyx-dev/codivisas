package com.codeonyx.bcvaqui.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateManifest(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String? = null,
    /** SHA-256 (hex) del APK publicado. Si está, se verifica antes de instalar. */
    val sha256: String? = null
)
