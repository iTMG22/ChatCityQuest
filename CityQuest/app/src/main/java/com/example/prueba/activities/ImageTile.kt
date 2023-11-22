package com.example.prueba.activities

import android.os.Parcel
import android.os.Parcelable

data class ImageTile(val byteArray: ByteArray) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.createByteArray() ?: byteArrayOf())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByteArray(byteArray)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ImageTile> {
        override fun createFromParcel(parcel: Parcel): ImageTile {
            return ImageTile(parcel)
        }

        override fun newArray(size: Int): Array<ImageTile?> {
            return arrayOfNulls(size)
        }
    }
}

