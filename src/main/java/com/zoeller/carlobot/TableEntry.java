package com.zoeller.carlobot;

import java.io.Serializable;

import org.jnosql.artemis.Entity;
import org.jnosql.artemis.Id;

@Entity
public class TableEntry implements Serializable {
    
    @Id
    private String userId;

    private String userName;

    private String score;

    public TableEntry() {

    }
}
