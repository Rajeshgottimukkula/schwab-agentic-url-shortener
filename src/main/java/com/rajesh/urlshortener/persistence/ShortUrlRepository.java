package com.rajesh.urlshortener.persistence;

import com.rajesh.urlshortener.domain.ShortUrl;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/** Persistence access for short URL mappings. */
public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByShortCode(String shortCode);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE ShortUrl s SET s.clickCount = s.clickCount + 1 " +
            "WHERE s.shortCode = :shortCode AND (s.expiresAt IS NULL OR s.expiresAt >= :now)")
    int incrementClickCountIfActive(@Param("shortCode") String shortCode, @Param("now") Instant now);
}
