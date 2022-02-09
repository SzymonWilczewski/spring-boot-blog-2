package pl.swilczewski.blog.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pl.swilczewski.blog.domain.Comment;

@Repository
public interface CommentRepository extends CrudRepository<Comment, Integer> {
}
