package pl.swilczewski.blog.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pl.swilczewski.blog.domain.Post;

@Repository
public interface PostRepository extends CrudRepository<Post, Integer> {
}
