package com.portal.universe.driveservice.common.config.kafka;

import com.portal.universe.event.drive.DriveTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic fileUploadedTopic() {
        return TopicBuilder.name(DriveTopics.FILE_UPLOADED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic fileDeletedTopic() {
        return TopicBuilder.name(DriveTopics.FILE_DELETED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic folderCreatedTopic() {
        return TopicBuilder.name(DriveTopics.FOLDER_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
