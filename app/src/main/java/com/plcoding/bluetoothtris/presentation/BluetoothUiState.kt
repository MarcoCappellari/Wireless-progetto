package com.plcoding.bluetoothtris.presentation

import com.plcoding.bluetoothtris.domain.tris.BluetoothDevice
import com.plcoding.bluetoothtris.domain.tris.BluetoothMessage

data class BluetoothUiState(
    val scannedDevices: List<BluetoothDevice> = emptyList(),
    val pairedDevices: List<BluetoothDevice> = emptyList(),
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
    val messages: List<BluetoothMessage> = emptyList()
)
