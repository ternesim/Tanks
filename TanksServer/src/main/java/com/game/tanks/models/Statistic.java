package com.game.tanks.models;

import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
@Table(name = "Statistics")
public class Statistic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    private String name;
 
    @Column
    private int shots;

    @Column
    private int hits;

    @Column
    private int misses;

    public Statistic(String name, int shots, int hits, int misses) {
        this.name = name;
        this.shots = shots;
        this.hits = hits;
        this.misses = misses;
    }

    public long getId() {return id;}
    public int getShots() {return shots;}
    public int getHits() {return hits;}
    public int getMisses() {return misses;}
    public String getName() {return name;}


    public void setShots(int shots) {this.shots = shots;}
    public void setHits(int hits) {this.hits = hits;}
    public void setMisses(int misses) {this.misses = misses;}

    @Override
    public String toString() {
        return "Statistic\n" + "shots: " + shots + ", hits: " + hits + ", misses: " + misses;
    }
}