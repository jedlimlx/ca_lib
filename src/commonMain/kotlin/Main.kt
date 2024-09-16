import cli.CFindCommand
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands

expect fun test()

class MainCommand: CliktCommand(name="ca_lib") {
    override fun run() = Unit
}

fun main(args: Array<String>) {
    MainCommand().subcommands(CFindCommand()).main(args)
}