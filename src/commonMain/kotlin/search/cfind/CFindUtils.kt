package search.cfind

internal fun lcm(a: Int, b: Int): Int {
    val larger = if (a > b) a else b
    val maxLcm = a * b
    var lcm = larger
    while (lcm <= maxLcm) {
        if (lcm % a == 0 && lcm % b == 0) {
            return lcm
        }

        lcm += larger
    }

    return maxLcm
}

internal fun repeat(num: Int, base: Int, power: Int): Int {
    return if (num == 0) 0
    else (power - 1) / (base - 1) * num
}

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
            x /= base
        }
    }

    return temp
}

internal fun getDigit(number: Int, power: Int, base: Int): Int {
    if (base == 2) return if (number and power == 0) 0 else 1
    return number.floorDiv(power).mod(base)
}

internal fun pow(base: Int, exponent: Int): Int {
    if (base == 2 && exponent >= 0) return 1 shl exponent
    if (exponent <= 0) return 1
    val temp = pow(base, exponent / 2)
    return if (exponent % 2 == 0) temp * temp else base * temp * temp
}