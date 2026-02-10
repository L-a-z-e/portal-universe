package com.portal.universe.authservice.user.event;

import com.portal.universe.event.auth.AuthTopics;
import com.portal.universe.event.auth.UserSignedUpEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSignupEventHandler 테스트")
class UserSignupEventHandlerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private UserSignupEventHandler userSignupEventHandler;

    @Nested
    @DisplayName("handleUserSignup")
    class HandleUserSignup {

        @Test
        @DisplayName("should_sendKafkaMessage_when_eventReceived")
        void should_sendKafkaMessage_when_eventReceived() {
            // given
            UserSignedUpEvent event = new UserSignedUpEvent(
                    "test-uuid", "test@example.com", "testNick"
            );

            // when
            userSignupEventHandler.handleUserSignup(event);

            // then
            verify(kafkaTemplate).send(AuthTopics.USER_SIGNED_UP, event);
        }

        @Test
        @DisplayName("should_sendToCorrectTopic_when_eventPublished")
        void should_sendToCorrectTopic_when_eventPublished() {
            // given
            UserSignedUpEvent event = new UserSignedUpEvent(
                    "another-uuid", "another@example.com", "anotherNick"
            );

            // when
            userSignupEventHandler.handleUserSignup(event);

            // then
            verify(kafkaTemplate).send(AuthTopics.USER_SIGNED_UP, event);
        }
    }
}
