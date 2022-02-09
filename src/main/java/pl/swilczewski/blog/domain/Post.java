package pl.swilczewski.blog.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "post_content", columnDefinition = "text")
    private String post_content;

    @Column(name = "tags")
    private String tags;

    @ManyToMany(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @JoinTable(
            name = "PostsAuthors",
            joinColumns = @JoinColumn(name = "id_post"),
            inverseJoinColumns = @JoinColumn(name = "id_author")
    )
    private List<Author> authors;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    private List<Comment> comments;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    private List<Attachment> attachments;

    public Post(String post_content, String tags) {
        this.post_content = post_content;
        this.tags = tags;
        this.authors = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.attachments = new ArrayList<>();
    }

    public Post(Integer id, String post_content, String tags) {
        this.id = id;
        this.post_content = post_content;
        this.tags = tags;
        this.authors = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.attachments = new ArrayList<>();
    }

    public Post(String post_content, String tags, List<Author> authors, List<Attachment> attachments) {
        this.post_content = post_content;
        this.tags = tags;
        this.authors = authors;
        this.comments = new ArrayList<>();
        this.attachments = attachments;
    }

    public void addAuthor(Author author) {
        this.authors.add(author);
    }
}
