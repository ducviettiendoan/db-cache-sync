package com.jk.jk.student;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class StudentService {
    @Autowired
    private StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository){
        this.studentRepository = studentRepository;
    }
    public List<Student>getAllStudents(){
        return studentRepository.findAll();
    }
    public Optional<Student> getStudentById(Long id){
        return studentRepository.findId(id);
    }
    public HttpStatusCode addStudent(Student newStudent){
        Optional<Student> foundS = studentRepository.findId(newStudent.getId());
        if (foundS.isPresent()){throw new IllegalStateException("Student id: " + newStudent.getId() + " already exists.");}
        Student commitStudent = studentRepository.save(newStudent);
        if (commitStudent == null){
            return HttpStatusCode.valueOf(401);
        }
        return HttpStatusCode.valueOf(200);
    }

    public HttpStatusCode updateStudent(Student newStudent){
        if (newStudent.getId()==null){throw new IllegalStateException("Id is required");}
        Optional<Student> foundS = studentRepository.findId(newStudent.getId());
        if (foundS.isEmpty()){
            throw new IllegalStateException("Student id: " + newStudent.getId() + " does exists.");
        }
        Student oldS = foundS.get();
        oldS.setAge(newStudent.getAge());
        oldS.setName(newStudent.getName());
        studentRepository.save(oldS);
        return HttpStatusCode.valueOf(200);
    }
    public HttpStatusCode deleteStudent(Long id){
        Optional<Student> foundS = studentRepository.findId(id);
        if (foundS.isEmpty()){throw new IllegalStateException("Student id: " + id + " does exists.");}
        studentRepository.deleteById(id);
        return HttpStatusCode.valueOf(200);
    }
}
