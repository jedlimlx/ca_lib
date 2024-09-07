import cli.CFindCommand
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal

expect fun test()

class MainCommand: CliktCommand(name="ca_lib") {
    override fun run() = Unit
}

fun main(args: Array<String>) {
    MainCommand().subcommands(CFindCommand()).main(args)
}