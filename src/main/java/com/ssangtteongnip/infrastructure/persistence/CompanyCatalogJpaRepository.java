package com.ssangtteongnip.infrastructure.persistence;

import com.ssangtteongnip.domain.company.CompanyCatalogEntry;
import com.ssangtteongnip.domain.company.CompanyCatalogRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CompanyCatalogJpaRepository extends JpaRepository<CompanyCatalogEntry, Long>, CompanyCatalogRepository {

    @Override
    @Query("""
            select e from CompanyCatalogEntry e
            where e.searchName like concat('%', :keyword, '%')
               or e.searchEngName like concat('%', :keyword, '%')
            order by case
                         when e.searchName = :keyword then 0
                         when e.searchName like concat(:keyword, '%') then 1
                         else 2
                     end,
                     e.listed desc, length(e.searchName), e.searchName
            """)
    List<CompanyCatalogEntry> findClosestMatches(@Param("keyword") String keyword, Pageable pageable);
}
