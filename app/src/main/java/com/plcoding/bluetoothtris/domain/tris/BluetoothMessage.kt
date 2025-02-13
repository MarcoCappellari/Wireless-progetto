package com.plcoding.bluetoothtris.domain.tris

data class BluetoothMessage(
    val message: String,
    val senderName: String,
    val isFromLocalUser: Boolean
)
