package com.sb.sb.redis;

import org.springframework.data.repository.CrudRepository;

import com.sb.sb.student.Student;

public interface RoleRedisRepository extends CrudRepository<RoleRedis, String>{
    
}
