package com.magnossao.health;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    private final JdbcTemplate jdbc;

    public HealthService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean databaseOk() {
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (DataAccessException e) {
            return false;
        }
    }
}
