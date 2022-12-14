package tech.portfolioshop.users.integration;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jobaggregator.kafka.config.KafkaTopics;
import org.jobaggregator.kafka.payload.UserCreated;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import tech.portfolioshop.users.UsersApplication;
import tech.portfolioshop.users.data.UserEntity;
import tech.portfolioshop.users.data.UserRepository;
import tech.portfolioshop.users.models.request.SignUpRequest;
import tech.portfolioshop.users.shared.UserDto;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = UsersApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(
        locations = "classpath:application.test.properties")
@EmbeddedKafka(
        partitions = 1,
        ports = 9092,
        zookeeperPort = 2181,
        topics = {KafkaTopics.USER_CREATED}
)
@DirtiesContext(
        classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD
)
public class SignupFlowTest {

    private final UserRepository userRepository;
    private final MockMvc mockMvc;
    private Consumer<String, String> consumer;
    private final EmbeddedKafkaBroker embeddedKafkaBroker;
    @Autowired
    public SignupFlowTest(
            UserRepository userRepository,
            MockMvc mockMvc,
            EmbeddedKafkaBroker embeddedKafkaBroker) {
        this.userRepository = userRepository;
        this.mockMvc = mockMvc;
        this.embeddedKafkaBroker = embeddedKafkaBroker;
    }

    @BeforeEach
    public void setup(){
        userRepository.deleteAll();
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("Test", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        ConsumerFactory<String, String> cf = new org.springframework.kafka.core.DefaultKafkaConsumerFactory<>(consumerProps);
        consumer = cf.createConsumer();
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(consumer);
    }

    @AfterEach
    public void tearDown() {
        consumer.close();
        // Kafka events after each test is cleared using dirties context

    }

    private UserDto getMockUser() {
        UserDto userDto = new UserDto();
        userDto.setName("test");
        userDto.setEmail("test@test.com");
        userDto.setPhone("1234567890");
        userDto.setPassword("test1234567890");
        return userDto;
    }

    @Test
    @DisplayName("Should create user and send user created event")
    public void signupSuccessfully() throws Exception {
        UserDto userDto = getMockUser();
        SignUpRequest signUpRequest = new ModelMapper().map(userDto, SignUpRequest.class);
        mockMvc.perform(post("/api/v1/user/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(signUpRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.AUTHORIZATION));
        UserEntity user = userRepository.findByEmail(signUpRequest.getEmail());
        assert user != null;
        // Test the user in database
        assert user.getName().equals(userDto.getName());
        assert user.getEmail().equals(userDto.getEmail());
        assert user.getPhone().equals(userDto.getPhone());
        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, KafkaTopics.USER_CREATED);
        assert record != null;
        // Test the user in kafka
        UserCreated userCreated = new UserCreated().deserialize(record.value());
        assert userCreated.getName().equals(userDto.getName());
        assert userCreated.getEmail().equals(userDto.getEmail());
        assert userCreated.getPhone().equals(userDto.getPhone());

        //test the UserID in kafka and repository
        assert userCreated.getUserId().equals(user.getUserId());
    }

    @Test
    @DisplayName("Can't create same user twice")
    public void signupTwice() throws Exception {
        UserDto userDto = getMockUser();
        SignUpRequest signUpRequest = new ModelMapper().map(userDto, SignUpRequest.class);
        mockMvc.perform(post("/api/v1/user/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(signUpRequest))
        ).andExpect(status().isCreated());
        UserEntity user = userRepository.findByEmail(signUpRequest.getEmail());
        assert user != null;
        mockMvc.perform(post("/api/v1/user/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(signUpRequest))
        ).andExpect(status().isInternalServerError());
        assert KafkaTestUtils.getRecords(consumer, 1000).count() == 1;
    }
}
