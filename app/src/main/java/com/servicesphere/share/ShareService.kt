package com.servicesphere.share

interface ShareService {
    fun isAvailable(): Boolean
}

class AndroidShareService : ShareService {
    override fun isAvailable(): Boolean = true
}
