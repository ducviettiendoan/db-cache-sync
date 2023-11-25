package com.sb.sb.student;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

//extend the JPA repository which is an interface Hibernate use to implement on. 
@Repository
public interface StudentRepository extends JpaRepository<Student,Long>{
    //Custom repository function
    @Query("SELECT s FROM Student s WHERE s.id = ?1")
    Optional<Student> findId(Long id);
}
