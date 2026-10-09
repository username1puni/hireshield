package com.hireshield.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hireshield.entity.BlacklistEntry;
import com.hireshield.entity.BlacklistType;

public interface BlacklistEntryRepository extends JpaRepository<BlacklistEntry, Long> {

    Optional<BlacklistEntry> findByTypeAndValue(BlacklistType type, String value);

    List<BlacklistEntry> findByValueIn(Collection<String> values);

    @Query("select b from BlacklistEntry b where lower(b.value) like lower(concat('%', :q, '%')) order by b.reportCount desc")
    List<BlacklistEntry> search(@Param("q") String query, Pageable pageable);
}
