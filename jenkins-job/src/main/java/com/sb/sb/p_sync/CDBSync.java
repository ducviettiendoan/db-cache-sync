
package com.sb.sb.p_sync;
 
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
 
import com.SDObject;
import com.sb.sb.student.Student;
import com.sb.sb.student.StudentService;

 
 
@Service
public class CDBSync {
 
    @Autowired
	StudentService studentService;
 
    @Autowired
    private SDObject sdo;
 
    @Autowired
    //Qualifier make sure template choosing is correct
    @Qualifier("redisTemplate")
	private RedisTemplate<String,String> template;
 
    private String STRING_KEY_PREFIX = "redi2read:strings:student";
 
    public void run(){
        //2-way: 
        // evoke element from cache if not in DB.
        List<Object> keys = new ArrayList<Object>(template.opsForHash().keys(STRING_KEY_PREFIX));
        List<Object> redisRes = new ArrayList<Object>(template.opsForHash().multiGet(STRING_KEY_PREFIX, keys));
        List<Student> res = redisRes.stream().map((serializer) -> {
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
        for (Student s: res){
            if (Objects.nonNull(s) && studentService.getStudentById(s.getId()).isEmpty()){
                template.opsForHash().delete(STRING_KEY_PREFIX, "key"+s.getId());
            }
        }
        // add element to the cache if in DB but not in cache
        List<Student> dbRes = studentService.getAllStudents();
        for (Student s: dbRes){
            try{
                Object cacheS = template.opsForHash().get(STRING_KEY_PREFIX, "key"+s.getId());
                if (Objects.isNull(cacheS)){
                    template.opsForHash().put(STRING_KEY_PREFIX, "key"+s.getId(), sdo.toString(s));
                }
            }catch(Exception e){
                if (Objects.nonNull(s)) {System.out.println("Error in processing id: "+s.getId());}
                continue;
            }
        }
    }   
}

