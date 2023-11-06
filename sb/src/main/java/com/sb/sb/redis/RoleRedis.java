package com.sb.sb.redis;

import java.io.Serializable;

import org.springframework.data.redis.core.RedisHash;

@RedisHash("role")
public class RoleRedis implements Serializable {
    private Long id;
    private String name;
    private String role;

    public RoleRedis(Long id, String name, String role){
        this.id = id;
        this.name = name;
        this.role = role;
    }
    public Long getId() {
        return id;
    }
    public String getRole() {
        return role;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setRole(String role) {
        this.role = role;
    }
    @Override
    public String toString(){
        return "Student{" + "id=" + id + ", name=" + name + ", role=" + role + '}';
    }
}
