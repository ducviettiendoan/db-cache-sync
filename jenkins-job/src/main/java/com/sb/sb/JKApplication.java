package com.sb.sb;

import java.io.IOException;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Qualifier;

import com.SDObject;
import com.sb.sb.p_sync.CDBSync;
import com.sb.sb.student.Student;
import com.sb.sb.student.StudentService;

import org.springframework.http.MediaType;

@SpringBootApplication
@RestController
@ComponentScan({"com", "com.jk.jk"})
@RequestMapping("/")
public class JKApplication {
	@Autowired
	StudentService studentService;

	// @Autowired
	// Producer prod;
	@Autowired
	private SDObject sdo;

	private String STRING_KEY_PREFIX = "redi2read:strings:student";

	@Autowired 
	//Qualifier make sure template choosing is correct
	@Qualifier("redisTemplate")
	private RedisTemplate<String,String> template;

	@GetMapping("/students")
	public List<Student> getAllStudents(){
		return studentService.getAllStudents();
	}

	@GetMapping(path="/redis/students")
	public List<Student>getAllStudent() throws IOException, ClassNotFoundException{
		List<Object> keys = new ArrayList<Object>(template.opsForHash().keys(STRING_KEY_PREFIX));
		List<Object> response = template.opsForHash().multiGet(STRING_KEY_PREFIX, keys);
		List<Student> res = response.stream().map((serializer) -> {
			try{
				if (serializer != null){
					return (Student)sdo.fromString(serializer.toString());
				}else{
					System.out.println("Some serializer not working");
					return null;
				}
			}catch(IOException | ClassNotFoundException e){
				throw new IllegalStateException(e);
			}
		}).toList();
		return res;
	}

	// //do not need to write to cache because the next read to this added student to db will be CACHE miss.
	// @PostMapping(path="/student/add", consumes = MediaType.APPLICATION_JSON_VALUE)
	// public HttpStatusCode addStudent(@RequestBody Student newStudent) throws IOException{
	// 	HttpStatusCode dbRes = studentService.addStudent(newStudent);
	// 	if (dbRes.value() == 200){
	// 		Producer prod = new Producer();
	// 		prod.configProd();
	// 		prod.runProd(newStudent);
	// 	}
	// 	return dbRes;
	// }

	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = SpringApplication.run(JKApplication.class, args);
		CDBSync cdb = ctx.getBean(CDBSync.class);
		cdb.run();
		System.out.println("Hello World");
		ctx.close();
	}

}
