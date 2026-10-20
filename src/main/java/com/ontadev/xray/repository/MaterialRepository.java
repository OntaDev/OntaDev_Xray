package com.ontadev.xray.repository;

import com.ontadev.libs.orm.OrmRepository;
import com.ontadev.libs.orm.annotation.Modifying;
import com.ontadev.libs.orm.annotation.Query;
import com.ontadev.xray.domain.MaterialEntry;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository extends OrmRepository<MaterialEntry, Integer> {
    @Query("SELECT * FROM materials ORDER BY id")
    List<MaterialEntry> findAllOrdered();

    @Query("SELECT * FROM materials WHERE id = ?")
    Optional<MaterialEntry> findById(Integer id);

    @Modifying("DELETE FROM materials WHERE id = ?")
    void deleteById(Integer id);
}
