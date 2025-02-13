package com.plcoding.bluetoothtris.data.tris

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.plcoding.bluetoothtris.domain.tris.BluetoothDeviceDomain

@SuppressLint("MissingPermission")
fun BluetoothDevice.toBluetoothDeviceDomain(): BluetoothDeviceDomain {
    return BluetoothDeviceDomain(
        name = name,
        address = address
    )
}