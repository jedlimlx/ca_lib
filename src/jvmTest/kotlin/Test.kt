import rules.int.transitions.R1MooreINT
import kotlin.test.Test

class Test {
    @Test
    fun test() {
        val transition = R1MooreINT("2-ace")
        println(transition.transitions)
        println(transition.transitionString)
    }
}