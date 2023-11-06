package com.sb.sb;

import java.io.IOException;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sb.sb.kafka.Producer;
import com.sb.sb.student.Student;
import com.sb.sb.student.StudentService;

import org.springframework.http.MediaType;

@SpringBootApplication
@RestController
@RequestMapping("/")
public class SaApplication {
	@Autowired
	StudentService studentService;

	@Autowired
	Producer prod;

	@GetMapping("/students")
	public List<Student> getAllStudents(){
		return studentService.getAllStudents();
	}

	//do not need to write to cache because the next read to this added student to db will be CACHE miss.
	@PostMapping(path="/student/add", consumes = MediaType.APPLICATION_JSON_VALUE)
	public HttpStatusCode addStudent(@RequestBody Student newStudent) throws IOException{
		HttpStatusCode dbRes = studentService.addStudent(newStudent);
		if (dbRes.value() == 200){
			Producer prod = new Producer();
			prod.configProd();
			prod.runProd(newStudent);
		}
		return dbRes;
	}

	public static void main(String[] args) {
		SpringApplication.run(SaApplication.class, args);
	}

}
