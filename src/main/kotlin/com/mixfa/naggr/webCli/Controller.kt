package com.mixfa.naggr.webCli

import com.mixfa.naggr.webCli.request.InputCommand
import jakarta.ws.rs.core.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/cli")
class Controller(
    private val cliService: CliService
) {

    @GetMapping
    fun homePageCli(model: Model): String {
        model.addAttribute("identifier", cliService.generateIdentifier())
        return "cli"
    }

    @ResponseBody
    @PostMapping("/handle", consumes = [MediaType.APPLICATION_JSON])
    fun handleCommand(@RequestBody inputCommand: InputCommand): String {
        return cliService.forwardCommand(inputCommand)
    }
}