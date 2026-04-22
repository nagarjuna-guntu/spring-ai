package com.example.tools.domain.game;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameRepository extends ListCrudRepository<Game, Long> {

    Optional<Game> findBySlug(String slug);
}
