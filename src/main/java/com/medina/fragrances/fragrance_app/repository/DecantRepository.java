package com.medina.fragrances.fragrance_app.repository;

import com.medina.fragrances.fragrance_app.model.Decant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DecantRepository extends JpaRepository<Decant, Integer> {
}
