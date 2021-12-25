infix fun Double.fmod(other: Double) = ((this % other) + other) % other

infix fun Int.fmod(other: Int) = ((this % other) + other) % other

infix fun Double.fmod(other: Int) = ((this % other) + other) % other

infix fun Int.fmod(other: Double) = ((this % other) + other) % other