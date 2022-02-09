package pl.swilczewski.blog.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "Authors")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "first_name")
    private String first_name;

    @Column(name = "last_name")
    private String last_name;

    @Column(name = "username")
    private String username;

    @ManyToMany(mappedBy = "authors", fetch = FetchType.EAGER)
    private List<Post> posts;

    public Author(Integer id, String first_name, String last_name, String username) {
        this.id = id;
        this.first_name = first_name;
        this.last_name = last_name;
        this.username = username;
    }

    public void addPost(Post post) {
        this.posts.add(post);
    }
}
