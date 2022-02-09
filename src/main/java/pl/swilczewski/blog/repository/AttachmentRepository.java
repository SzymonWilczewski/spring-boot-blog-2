package pl.swilczewski.blog.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pl.swilczewski.blog.domain.Attachment;
import pl.swilczewski.blog.domain.Post;

import java.util.List;

@Repository
public interface AttachmentRepository extends CrudRepository<Attachment, Integer> {
    List<Attachment> findByPost(Post post);
}
