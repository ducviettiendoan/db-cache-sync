package com.jk.jk.student;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Student implements Serializable{
    @Id
    private Long id;
    private String name;
    private Integer age;
    
    protected Student(){
        super();
    }

    public Student(Long id, String name, Integer age){
        this.id = id;
        this.name = name;
        this.age = age;
    }
    public Long getId() {
        return id;
    }
    public Integer getAge() {
        return age;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setAge(Integer age) {
        this.age = age;
    }
    @Override
    public String toString(){
        return "Student{" + "id=" + id + ", name='" + name + ", age=" + age + '\'' + '}';
    }
}
