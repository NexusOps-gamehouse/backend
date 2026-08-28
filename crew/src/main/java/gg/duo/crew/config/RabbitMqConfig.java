package gg.duo.crew.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String CREW_CHAT_EXCHANGE = "crew.chat.exchange";
    public static final String CREW_CHAT_QUEUE = "crew.chat.queue";
    public static final String ROUTING_KEY = "room.*";

    // Topic Exchange 생성
    @Bean
    public TopicExchange crewChatExchange() {
        return new TopicExchange(CREW_CHAT_EXCHANGE);
    }

    // Queue 생성
    @Bean
    public Queue crewChatQueue() {
        return new Queue(CREW_CHAT_QUEUE, true);
    }

    // Exchange와 Queue 바인딩
    @Bean
    public Binding binding(Queue crewChatQueue, TopicExchange crewChatExchange) {
        return BindingBuilder.bind(crewChatQueue).to(crewChatExchange).with(ROUTING_KEY);
    }
}