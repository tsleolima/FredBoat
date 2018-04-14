package fredboat.main

import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan(basePackages = ["fredboat"])
open class Launcher

fun main(args: Array<String>) {
    System.setProperty("spring.config.name", "fredboat")
    val app = SpringApplication(Launcher::class.java)
    app.webApplicationType = WebApplicationType.NONE
    app.run(*args)
}