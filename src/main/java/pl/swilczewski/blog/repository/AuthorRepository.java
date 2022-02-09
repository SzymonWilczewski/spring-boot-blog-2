package pl.swilczewski.blog.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pl.swilczewski.blog.domain.Author;

@Repository
public interface AuthorRepository extends CrudRepository<Author, Integer> {
    Author findByUsername(String username);
}
