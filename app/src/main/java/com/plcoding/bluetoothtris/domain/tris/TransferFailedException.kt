package com.plcoding.bluetoothtris.domain.tris

import java.io.IOException

class TransferFailedException: IOException("Reading incoming data failed")