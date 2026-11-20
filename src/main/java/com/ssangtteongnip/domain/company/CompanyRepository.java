package com.ssangtteongnip.domain.company;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository {
    Company save(Company company);
    Optional<Company> findById(Long id);
    List<Company> findAll();
    boolean existsByName(String name);
    List<Company> findByNameIn(Collection<String> names);
}
