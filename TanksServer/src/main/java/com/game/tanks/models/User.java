package com.game.tanks.models;

import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
@Table(name = "Users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    
    @Column
    private String name;

    @Column
    private String password;

    @Column
    private int statistics_id;

    public User(String name, String password) {
        this.name = name;
        this.password = password;
    }

    public User(String name, String password, int statistics_id) {
        this.name = name;
        this.password = password;
        this.statistics_id = statistics_id;
    }

    public long getId() {return id;}
    public String getname(){return name;}
    public String getPassword() {return password;}
    public int getStatistics_id() {return statistics_id;}

    public void setId(long id) {this.id = id;}
    public void setName(String name) {this.name = name;}
    public void setPassword(String password) {this.password = password;}
    public void setStatistics_id(int statistics_id) {this.statistics_id = statistics_id;}

    @Override
    public String toString() {
        return "User: " + name;
    }
}