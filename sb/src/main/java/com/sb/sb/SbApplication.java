package com.sb.sb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.repository.support.Repositories;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sb.sb.kafkaMultiService.Consumer;
import com.sb.sb.kafkaSingleService.Producer;
import com.sb.sb.redis.RoleRedis;
import com.sb.sb.redis.RoleRedisRepository;
import com.sb.sb.redis.SDObject;
import com.sb.sb.redisson.WTProcess;
import com.sb.sb.student.Student;
import com.sb.sb.student.StudentService;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@SpringBootApplication
@EnableAsync
@RestController
@RequestMapping("/")
public class SbApplication{

	@Autowired
	StudentService studentService;

	@Autowired
    private SDObject sdo;

	private String STRING_KEY_PREFIX = "redi2read:strings:student";

	@Autowired 
	//Qualifier make sure template choosing is correct
	@Qualifier("redisTemplate")
	private RedisTemplate<String,String> template;

	@Autowired
	private RoleRedisRepository redisRepo;

	@Autowired
	public Consumer consumer;

	@Autowired
	public Producer producer;
	
	//Is this a place to use cache? If yes, sync up cache on cache miss.
	@GetMapping("/students")
	public List<Student> getAllStudents(){
		return studentService.getAllStudents();
	}

	@GetMapping("/student/id")
	public Optional<Student> getStudentById(@RequestParam Long id) throws IOException, ClassNotFoundException{
		//Lazy load 
		String hashK = "key"+id;
		//template.get() returns null herer -> fromString err.
		Object redisRes = template.opsForHash().get(STRING_KEY_PREFIX, hashK);
		if (redisRes != null){
			Student value = (Student)sdo.fromString((String)(redisRes));
			System.out.println("Cache Hit.");
			return Optional.of(value);
		}
		System.out.println("Cache miss will add "+id+" to cache");
		//write to cache
		Optional<Student> dbRes = studentService.getStudentById(id);
		//Student obj to write cache
		Student studentW = dbRes.get();
		String serializeS = sdo.toString(studentW);
		template.opsForHash().put(STRING_KEY_PREFIX,"key"+studentW.getId(),serializeS);
		System.out.println("Write key"+studentW+" complete");
		return dbRes;
	}

	//Implement Write-through
	@PostMapping(path="/student/add", consumes = MediaType.APPLICATION_JSON_VALUE)
	public HttpStatusCode addStudent(@RequestBody Student newStudent) throws IOException,ClassNotFoundException{
		// HttpStatusCode dbRes = studentService.addStudent(newStudent);
		String serializeS = sdo.toString(newStudent);
		try{
			template.opsForHash().put(STRING_KEY_PREFIX,"key"+newStudent.getId(),serializeS);
			System.out.println("Write through to cache success");
			producer.runProd(newStudent);
		}catch(Exception e){
				throw new IllegalStateException(e);
		}
		HttpStatusCode cacheRes = HttpStatusCode.valueOf(200);
		return cacheRes;
	}

	@PutMapping(path="/student/update")
	public HttpStatusCode updateStudent(@RequestBody Student newStudent) throws IOException,ClassNotFoundException{
		//write-aside
		HttpStatusCode dbRes = studentService.updateStudent(newStudent);
		if (dbRes.value() == 200){
			Long updateObjId = newStudent.getId();
			String hashK = "key"+updateObjId;
			//template.get() returns null herer -> fromString err.
			Object redisRes = template.opsForHash().get(STRING_KEY_PREFIX, hashK);
			if (redisRes != null){
				// Student value = (Student)sdo.fromString((String)(redisRes));
				// throw new IllegalStateException("key not found");
				String serializeS = sdo.toString(newStudent);
				template.opsForHash().put(STRING_KEY_PREFIX,hashK,serializeS);
				System.out.println("Cache Update Success.");
			}else{
				throw new IllegalStateException(hashK + " is not found double cache-db sync", null);
			}
		}
		return dbRes;
	}

	@DeleteMapping(path="/student/delete")
	public HttpStatusCode deleteStudent(@RequestParam Long id){
		//write-aside
		return studentService.deleteStudent(id);
	}

	//Test connection to redis (localhost:6379)
	@PostMapping(path="/redis/create")
	public Map.Entry<String, String> setString(@RequestBody Map.Entry<String, String> kvp) {
		template.opsForValue().set(STRING_KEY_PREFIX+kvp.getKey(),kvp.getValue());
		return kvp;
	}
	@GetMapping(path="/redis/{key}")
	public Map.Entry<String,String> getString(@PathVariable("key") String key){
		String value = template.opsForValue().get(STRING_KEY_PREFIX+key);
		System.out.println("Found key");
		if (value == null) {
			throw new IllegalStateException("key not found");
		}
		Entry<String,String> res = Map.entry(key, value);
		return res;
	}
	
	//Redis endpoints (could pack to a RedisService to make this class clean).
	@PostMapping(path="/redis/student/add")
	public Map.Entry<String, Student> setStudent(@RequestBody Map.Entry<String, Student> kvp) throws IOException, ClassNotFoundException {
		//key is fix, hashK format: "key{id}".
		String serializeS = sdo.toString(kvp.getValue());
		template.opsForHash().put(STRING_KEY_PREFIX,kvp.getKey(),serializeS);
		return kvp;
	}

	@GetMapping(path="/redis/student/{key}/{hashK}")
	public Map.Entry<String,Student> getHash(@PathVariable("key") String key, @PathVariable("hashK") String hashK) throws IOException, ClassNotFoundException, Exception{
		Object redisRes = template.opsForHash().get(STRING_KEY_PREFIX, hashK);
		if (redisRes == null){
			throw new Exception("Cannot find hash key in Redis", null);
		}
		Student value = (Student)sdo.fromString((String)(redisRes));
		if (value == null) {
			throw new IllegalStateException("key not found");
		}
		Entry<String,Student> res = Map.entry(hashK, value);
		return res;
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

	//test @RedisHash('role') with CRUDRepository.
	@PostMapping(path="/redis/role/create")
	public HttpStatusCode createRole(@RequestBody RoleRedis role){
		RoleRedis redisRes = redisRepo.save(role);
		if (redisRes == null){throw new IllegalStateException("Err create new role");}
		return HttpStatusCode.valueOf(200);
	}

	public static void main(String[] args) throws IOException , ClassNotFoundException, SQLException{
		//Run application (1 thread). context here acts as IOC of the application.
		ApplicationContext context = SpringApplication.run(SbApplication.class, args);
		Student s1 = context.getBean("initStudentBean", Student.class);
		System.out.println("IoC and BEANs is RUNNING "+s1.toString());
		//Run as Kafka consumer using Consumer Bean(seperate thread). Cannot use new Consumer() if need Bean 
		//functionalities such as @Autowired.
		Consumer c1 = context.getBean(Consumer.class);
		CompletableFuture<Consumer> t1 = c1.run();
		
		WTProcess wt1 = context.getBean(WTProcess.class);
		CompletableFuture<WTProcess> t2 = wt1.runWT();

		//join threads
		CompletableFuture.allOf(t1,t2).join();
		// CompletableFuture.allOf(t2).join();
	}
}
