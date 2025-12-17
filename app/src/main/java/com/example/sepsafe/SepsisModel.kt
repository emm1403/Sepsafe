package com.example.sepsafe

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class SepsisModel(context: Context) {

    private val interpreter: Interpreter = Interpreter(loadModelFile(context))

    // Función para ejecutar la predicción
    fun predict(input: Array<Array<FloatArray>>): Float {
        val output = Array(1) { FloatArray(1) } // output bidimensional [1,1]
        interpreter.run(input, output)
        return output[0][0]
    }

    // Cargar el modelo TFLite desde assets
    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("sepsis_model_android.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val channel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return channel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}
