import kotlinx.cinterop.*
import platform.posix.*


actual val PLATFORM = "NATIVE"

// @OptIn(ExperimentalForeignApi::class)
actual fun readResource(resource: String): String {
    throw NotImplementedError("Cannot read resources in Kotlin Native.")

    /*
    var file: CPointer<FILE>? = fopen("./src/commonMain/resources/$resource", "r")
    if (file == null)
        file = fopen("./src/commonTest/resources/$resource", "r")

    fseek(file, 0, SEEK_END)
    val size = ftell(file)
    rewind(file)

    return memScoped {
        val tmp = allocArray<ByteVar>(size)
        fread(tmp, sizeOf<ByteVar>().convert(), size.convert(), file)
        tmp.toKString()
    }
     */
}