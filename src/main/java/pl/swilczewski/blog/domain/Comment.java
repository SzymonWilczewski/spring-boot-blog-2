package pl.swilczewski.blog.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Entity
@Table(name = "Comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "username")
    @Size(min=2, message="Username is too short")
    private String username;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_post")
    private Post post;

    @Column(name = "comment_content", columnDefinition = "text")
    @NotEmpty(message = "Comment cannot be empty")
    private String comment_content;

    public Comment(String username, Post post, String comment_content) {
        this.username = username;
        this.post = post;
        this.comment_content = comment_content;
    }
}
