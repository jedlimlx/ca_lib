package search.cfind

internal inline fun reverseDigits(x: Int, base: Int = 2, length: Int = -1): Int {
    var temp = 0
    var x = x
    if (base == 2) {
        if (length != -1) x += 1 shl length
        while (x != 0) {
            temp = (temp shl 1) + (x and 1)
            x = x shr 1
        }
        if (length != -1) temp = temp shr 1
    } else {
        while (x != 0) {
            temp = temp * base + x.mod(base)
            x = x / base
        }
    }

    return temp
}

internal inline fun getDigit(number: Int, power: Int, base: Int): Int {
    if (base == 2) return if (number and power == 0) 0 else 1
    return number.floorDiv(power).mod(base)
}

internal fun pow(base: Int, exponent: Int): Int {
    if (base == 2 && exponent >= 0) return 1 shl exponent
    if (exponent <= 0) return 1
    val temp = pow(base, exponent / 2)
    return if (exponent % 2 == 0) temp * temp else base * temp * temp
}