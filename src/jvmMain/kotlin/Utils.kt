actual fun readResource(resource: String): String {
    return object {}.javaClass.getResource(resource)!!.readText()
}