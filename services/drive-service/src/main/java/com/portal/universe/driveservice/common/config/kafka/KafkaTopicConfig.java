package com.portal.universe.driveservice.common.config.kafka;

import com.portal.universe.event.drive.DriveTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topic.partitions:3}")
    private int partitions;

    @Value("${app.kafka.topic.replicas:1}")
    private int replicas;

    @Bean
    public NewTopic fileUploadedTopic() {
        return buildTopic(DriveTopics.FILE_UPLOADED);
    }

    @Bean
    public NewTopic fileDeletedTopic() {
        return buildTopic(DriveTopics.FILE_DELETED);
    }

    @Bean
    public NewTopic folderCreatedTopic() {
        return buildTopic(DriveTopics.FOLDER_CREATED);
    }

    private NewTopic buildTopic(String name) {
        return TopicBuilder.name(name)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }
}
