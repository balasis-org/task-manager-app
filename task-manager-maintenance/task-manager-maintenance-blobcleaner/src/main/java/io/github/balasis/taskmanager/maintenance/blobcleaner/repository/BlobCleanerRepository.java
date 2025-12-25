package io.github.balasis.taskmanager.maintenance.blobcleaner.repository;

import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@AllArgsConstructor
public class BlobCleanerRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean existsById(BlobContainerType type, long id) {
        String sql = String.format(
                "SELECT COUNT(*) FROM %s WHERE id = ?",
                type.getTableName()
        );

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }
}

