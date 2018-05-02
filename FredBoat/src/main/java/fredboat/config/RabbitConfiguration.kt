package fredboat.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.amqp.rabbit.AsyncRabbitTemplate
import org.springframework.amqp.rabbit.core.RabbitTemplate

@Configuration
open class RabbitConfiguration {

    @Bean
    open fun jsonMessageConverter(): MessageConverter {
        // We must register this Kotlin module to get deserialization to work with data classes
        return Jackson2JsonMessageConverter(ObjectMapper().registerKotlinModule())
    }

    @Bean
    open fun asyncTemplate(rabbitTemplate: RabbitTemplate): AsyncRabbitTemplate {
        return AsyncRabbitTemplate(rabbitTemplate)
    }


}