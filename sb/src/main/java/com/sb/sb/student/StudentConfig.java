package com.sb.sb.student;
import java.util.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StudentConfig {
    @Bean
    Student initStudentBean(StudentRepository studentRepository){
        return new Student(Long.valueOf(1),"Default",20);
    }
}
