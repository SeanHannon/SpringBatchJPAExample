package com.example.jpabatch.batchprocessing;

import com.example.jpabatch.domain.User;
import com.example.jpabatch.domain.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import java.text.Normalizer;

@Configuration
@EnableBatchProcessing
@Slf4j
public class BatchConfiguration {
    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Autowired
    public UserRepository userRepository;

    @Bean
    public FlatFileItemReader<User> reader(){
        return new FlatFileItemReaderBuilder<User>()
            .name("userItemReader")
            .resource(new FileSystemResource("sample.csv"))
            .delimited()
            .names("firstName", "lastName")
            .fieldSetMapper(new BeanWrapperFieldSetMapper<>(){{
                setTargetType(User.class);
            }})
            .build();
    }

    @Bean
    public ItemProcessor<User, User> processor(){
        return user -> {
            final var firstName = Normalizer.normalize(user.getFirstName().toUpperCase(), Normalizer.Form.NFKC);
            final var lastName = Normalizer.normalize(user.getLastName().toUpperCase(), Normalizer.Form.NFKC);
            final var transformedUser = new User(firstName, lastName);

            log.info("before converted : " + user);
            log.info("transformed user : " + transformedUser);
            return transformedUser;
        };
    }

    @Bean
    public RepositoryItemWriter<User> writer() {
        return new RepositoryItemWriterBuilder<User>()
            .repository(userRepository)
            .methodName("save")
            .build();
    }

    @Bean
    public Job importUserJob() {
        return jobBuilderFactory.get("importUserJob")
                                .incrementer(new RunIdIncrementer())
                                .flow(step1())
                                .end()
                                .build();
    }

    @Bean
    public Step step1() {
        return stepBuilderFactory.get("step1")
                                 .<User, User>chunk(10)
                                 .reader(reader())
                                 .processor(processor())
                                 .writer(writer())
                                 .build();
    }
}
