package pl.swilczewski.blog.controllers.web;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import pl.swilczewski.blog.domain.*;
import pl.swilczewski.blog.repository.AttachmentRepository;
import pl.swilczewski.blog.repository.AuthorRepository;
import pl.swilczewski.blog.repository.CommentRepository;
import pl.swilczewski.blog.repository.PostRepository;
import pl.swilczewski.blog.service.UserService;
import pl.swilczewski.blog.storage.StorageService;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Controller
public class WebController {
    private final AuthorRepository authorRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final AttachmentRepository attachmentRepository;
    private final UserService userService;
    private final StorageService storageService;

    public WebController(AuthorRepository authorRepository, PostRepository postRepository, CommentRepository commentRepository, AttachmentRepository attachmentRepository, UserService userService, StorageService storageService) {
        this.authorRepository = authorRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.attachmentRepository = attachmentRepository;
        this.userService = userService;
        this.storageService = storageService;
    }

    // Access

    @GetMapping("/register")
    public String getRegister(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String postRegister(Model model, @Valid User user, BindingResult bindingResult) {
        if(bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "register";
        }
        try {
            userService.save(user);
            return "login";
        } catch (Exception e) {
            model.addAttribute("user", user);
            return "register";
        }
    }

    @GetMapping("/login")
    public String getLogin() {
        return "login";
    }

    // Posts

    @GetMapping("/")
    public String getPosts(Model model, @RequestParam(required=false) String c, @RequestParam(required=false) String f) {
        if (c != null && f != null) {
            switch (c) {
                case "pc":
                    List<Post> postsContent = StreamSupport.stream(postRepository.findAll().spliterator(), false)
                            .filter(p -> p.getPost_content().matches("(?i).*" + f + ".*"))
                            .sorted(Comparator.comparing(Post::getId).reversed())
                            .collect(Collectors.toList());
                    model.addAttribute("posts", postsContent);
                    return "posts";
                case "t":
                    List<Post> postsTags = StreamSupport.stream(postRepository.findAll().spliterator(), false)
                            .filter(p -> p.getTags().matches("(?i).*\\b" + f + "\\b.*"))
                            .sorted(Comparator.comparing(Post::getId).reversed())
                            .collect(Collectors.toList());
                    model.addAttribute("posts", postsTags);
                    return "posts";
                default:
                    model.addAttribute("posts", StreamSupport.stream(postRepository.findAll().spliterator(), false)
                            .sorted(Comparator.comparing(Post::getId).reversed())
                            .collect(Collectors.toList()));
                    return "posts";
            }
        } else {
            model.addAttribute("posts", StreamSupport.stream(postRepository.findAll().spliterator(), false)
                    .sorted(Comparator.comparing(Post::getId).reversed())
                    .collect(Collectors.toList()));
            return "posts";
        }
    }

    @GetMapping("/posts")
    public String getUserPosts(Model model, @RequestParam(required=false) String c, @RequestParam(required=false) String f) {
        if (c != null && f != null) {
            switch (c) {
                case "p":
                    if (f.matches("-?\\d+")) {
                        return "redirect:/post/" + f;
                    } else {
                        return "error";
                    }
                case "pc":
                    List<Post> postsContent = StreamSupport.stream(postRepository.findAll().spliterator(), false)
                            .filter(p -> p.getPost_content().matches("(?i).*" + f + ".*"))
                            .sorted(Comparator.comparing(Post::getId).reversed())
                            .collect(Collectors.toList());
                    model.addAttribute("posts", postsContent);
                    return "user/posts";
                case "t":
                    List<Post> postsTags = StreamSupport.stream(postRepository.findAll().spliterator(), false)
                            .filter(p -> p.getTags().matches("(?i).*\\b" + f + "\\b.*"))
                            .sorted(Comparator.comparing(Post::getId).reversed())
                            .collect(Collectors.toList());
                    model.addAttribute("posts", postsTags);
                    return "user/posts";
                case "ap":
                    if (f.matches("-?\\d+")) {
                        return "redirect:/author/" + f + "/posts";
                    } else {
                        return "error";
                    }
                case "uc":
                    return "redirect:/user/" + f + "/comments";
                default:
                    model.addAttribute("posts", StreamSupport.stream(postRepository.findAll().spliterator(), false)
                            .sorted(Comparator.comparing(Post::getId).reversed())
                            .collect(Collectors.toList()));
                    return "user/posts";
            }
        } else {
            model.addAttribute("posts", StreamSupport.stream(postRepository.findAll().spliterator(), false)
                    .sorted(Comparator.comparing(Post::getId).reversed())
                    .collect(Collectors.toList()));
            return "user/posts";
        }
    }

    // Post

    @GetMapping("/post/{id}")
    public String getPostId(Model model, @PathVariable int id) {
        Post post = StreamSupport.stream(postRepository.findAll().spliterator(), false)
                .filter(p -> p.getId() == id)
                .findAny()
                .orElse(null);

        if (post != null) {
            model.addAttribute("post", post);
            model.addAttribute("authors", post.getAuthors());
            model.addAttribute("comment", new Comment());
            model.addAttribute("comments", StreamSupport.stream(commentRepository.findAll().spliterator(), false)
                    .filter(c -> c.getPost().getId() == id)
                    .sorted(Comparator.comparing(Comment::getId).reversed())
                    .collect(Collectors.toList()));
            return "postId";
        } else {
            return "error";
        }
    }

    @GetMapping("/new/post")
    public String newPost(Model model) {
        PostForm postForm = new PostForm();
        postForm.setAuthors(StreamSupport.stream(authorRepository.findAll().spliterator(), false)
                .map(Author::getUsername)
                .collect(Collectors.toList()));
        postForm.setSelectedAuthors(new ArrayList<>());
        model.addAttribute("postForm", postForm);
        return "newPost";
    }

    @PostMapping("/new/post")
    public String postNewPost(Model model, @Valid PostForm postForm, Errors errors) {
        if (errors.hasErrors()) {
            postForm.setSelectedAuthors(postForm.getAuthors());
            postForm.setAuthors(StreamSupport.stream(authorRepository.findAll().spliterator(), false)
                    .map(Author::getUsername)
                    .collect(Collectors.toList()));
            model.addAttribute("postForm", postForm);
            return "newPost";
        }

        try {
            postRepository.save(new Post(postForm.getPost_content(), postForm.getTags()));
            Post post = StreamSupport.stream(postRepository.findAll().spliterator(), false)
                    .filter(p -> p.getPost_content().equals(postForm.getPost_content()))
                    .findAny()
                    .orElse(null);

            List<Author> authors = postForm.getAuthors().stream()
                    .map(authorRepository::findByUsername)
                    .collect(Collectors.toList());
            post.setAuthors(authors);

            List<Attachment> attachments = new ArrayList<>();
            Arrays.asList(postForm.getAttachments()).forEach(f -> {
                if (!f.isEmpty()) {
                    String filename = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS_").format(new Date(System.currentTimeMillis())) + f.getOriginalFilename();
                    attachments.add(new Attachment(post, filename));
                    storageService.store(f, filename);
                }
            });
            post.setAttachments(attachments);

            postRepository.save(post);

            return "redirect:/posts";
        } catch (Exception e) {
            System.out.println("Exception");
            e.printStackTrace();
            return "error";
        }
    }

    @GetMapping("/edit/post/{id}")
    public String editPostId(Model model, @PathVariable int id) {
        Post post = StreamSupport.stream(postRepository.findAll().spliterator(), false)
                .filter(p -> p.getId() == id)
                .findAny()
                .orElse(null);

        if (post != null) {
            PostForm postForm = new PostForm();
            postForm.setAuthors(StreamSupport.stream(authorRepository.findAll().spliterator(), false)
                    .map(Author::getUsername)
                    .collect(Collectors.toList()));
            postForm.setSelectedAuthors(StreamSupport.stream(authorRepository.findAll().spliterator(), false)
                    .filter(a -> post.getAuthors().contains(a))
                    .map(Author::getUsername)
                    .collect(Collectors.toList()));
            postForm.setId(post.getId());
            postForm.setPost_content(post.getPost_content());
            postForm.setTags(post.getTags());
            postForm.setFilenames(StreamSupport.stream(attachmentRepository.findAll().spliterator(), false)
                    .filter(a -> a.getPost().getId() == id)
                    .map(Attachment::getFilename)
                    .collect(Collectors.toList()));
            model.addAttribute("postForm", postForm);
            return "editPost";
        } else {
            return "error";
        }
    }

    @PostMapping("/edit/post/{id}")
    public String postEditPostId(Model model, @PathVariable int id, @Valid PostForm postForm, Errors errors) {
        if (errors.hasErrors()) {
            postForm.setSelectedAuthors(postForm.getAuthors());
            postForm.setAuthors(StreamSupport.stream(authorRepository.findAll().spliterator(), false)
                    .map(Author::getUsername)
                    .collect(Collectors.toList()));
            model.addAttribute("postForm", postForm);
            return "editPost";
        }

        try {
            if (postRepository.findById(postForm.getId()).isPresent()) {
                Post post = postRepository.findById(postForm.getId()).get();
                List<Author> authors = postForm.getAuthors().stream()
                        .map(authorRepository::findByUsername)
                        .collect(Collectors.toList());
                List<Attachment> attachments = new ArrayList<>();
                Arrays.asList(postForm.getAttachments()).forEach(f -> {
                    if (!f.isEmpty()) {
                        String filename = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS_").format(new Date(System.currentTimeMillis())) + f.getOriginalFilename();
                        attachments.add(new Attachment(post, filename));
                        storageService.store(f, filename);
                    }
                });

                post.setPost_content(postForm.getPost_content());
                post.setTags(postForm.getTags());
                post.setAuthors(authors);
                post.setAttachments(attachments);

                postRepository.save(post);

                return "redirect:/post/" + id;
            } else {
                return "error";
            }
        } catch (Exception e) {
            System.out.println("Exception");
            e.printStackTrace();
            return "error";
        }
    }

    @GetMapping("/delete/post/{id}")
    public String deletePostId(@PathVariable int id) {
        Post post = StreamSupport.stream(postRepository.findAll().spliterator(), false)
                .filter(p -> p.getId() == id)
                .findAny()
                .orElse(null);

        if (post != null) {
            postRepository.delete(post);
            return "redirect:/posts";
        } else {
            return "error";
        }
    }

    // Comment

    @PostMapping("/post/{id_post}")
    public String postComment(Model model, @PathVariable int id_post, @Valid Comment comment, Errors errors) {
        if (errors.hasErrors()) {
            model.addAttribute("post", StreamSupport.stream(postRepository.findAll().spliterator(), false)
                    .filter(p -> p.getId() == id_post)
                    .findAny()
                    .orElse(null));
            model.addAttribute("comment", comment);
            model.addAttribute("comments", StreamSupport.stream(commentRepository.findAll().spliterator(), false)
                    .filter(c -> c.getPost().getId() == id_post)
                    .sorted(Comparator.comparing(Comment::getId).reversed())
                    .collect(Collectors.toList()));
            return "postId";
        }

        Post post = StreamSupport.stream(postRepository.findAll().spliterator(), false)
                .filter(p -> p.getId() == id_post)
                .findAny()
                .orElse(null);

        if (post != null) {
            List<Comment> newComments = post.getComments();
            newComments.add(new Comment(comment.getUsername(), post, comment.getComment_content()));
            post.setComments(newComments);
            postRepository.save(post);

            return "redirect:/post/" + id_post;
        } else {
            model.addAttribute("post", StreamSupport.stream(postRepository.findAll().spliterator(), false)
                    .filter(p -> p.getId() == id_post)
                    .findAny()
                    .orElse(null));
            model.addAttribute("comment", comment);
            model.addAttribute("comments", StreamSupport.stream(commentRepository.findAll().spliterator(), false)
                    .filter(c -> c.getPost().getId() == id_post)
                    .sorted(Comparator.comparing(Comment::getId).reversed())
                    .collect(Collectors.toList()));
            return "postId";
        }
    }

    @GetMapping("/post/{id_post}/comment/{id}")
    public String editComment(Model model, @PathVariable int id_post, @PathVariable int id) {
        Post post = StreamSupport.stream(postRepository.findAll().spliterator(), false)
                .filter(p -> p.getId() == id_post)
                .findAny()
                .orElse(null);

        if (post != null) {
            Comment comment = StreamSupport.stream(commentRepository.findAll().spliterator(), false)
                    .filter(c -> c.getId() == id)
                    .findAny()
                    .orElse(null);
            if (comment != null) {
                model.addAttribute("comment", comment);
                return "editComment";
            } else {
                model.addAttribute("message", "Comment not found");
                return "error";
            }
        } else {
            model.addAttribute("message", "Post not found");
            return "error";
        }
    }

    @PostMapping("/post/{id_post}/comment/{id}")
    public String editComment(Model model, @PathVariable int id_post, @PathVariable int id, @Valid Comment comment, Errors errors) {
        if (errors.hasErrors()) {
            model.addAttribute("comment", comment);
            return "editComment";
        }

        if (postRepository.findById(id_post).isPresent()) {
            Post post = postRepository.findById(id_post).get();
            comment.setPost(post);
            if (post.getComments().stream().anyMatch(c -> c.getId() == id)) {
                post.setComments(post.getComments().stream()
                        .map(c -> c.getId() == id ? comment : c)
                        .collect(Collectors.toList())
                );
                postRepository.save(post);

                return "redirect:/post/" + id_post;
            } else {
                model.addAttribute("comment", comment);
                return "editComment";
            }
        } else {
            model.addAttribute("comment", comment);
            return "editComment";
        }
    }

    @GetMapping("/delete/comment/{id}")
    public String deleteCommentId(@PathVariable int id) {
        Comment comment = StreamSupport.stream(commentRepository.findAll().spliterator(), false)
                .filter(c -> c.getId() == id)
                .findAny()
                .orElse(null);

        if (comment != null) {
            if (postRepository.findById(comment.getPost().getId()).isPresent()) {
                Post post = postRepository.findById(comment.getPost().getId()).get();
                post.setComments(post.getComments().stream()
                        .filter(c -> !c.getId().equals(comment.getId()))
                        .collect(Collectors.toList()));
                postRepository.save(post);
                commentRepository.delete(comment);
                return "redirect:/post/" + comment.getPost().getId();
            } else {
                return "error";
            }
        } else {
            return "error";
        }
    }

    @GetMapping("/user/{username}/comments")
    public String getUserUsernameComments(Model model, @PathVariable String username) {
        List<Comment> comments_ = StreamSupport.stream(commentRepository.findAll().spliterator(), false)
                .filter(c -> c.getUsername().matches("(?i)\\b" + username + "\\b"))
                .sorted(Comparator.comparing(Comment::getId).reversed())
                .collect(Collectors.toList());

        if (!ObjectUtils.isEmpty(comments_)) {
            model.addAttribute("comments", comments_);
            return "userUsernameComments";
        } else {
            model.addAttribute("message", "No comments found");
            return "error";
        }
    }

    // Authors

    @GetMapping("/authors")
    public String getAuthors(Model model) {
        model.addAttribute("authors", authorRepository.findAll());
        return "authors";
    }

    @GetMapping("/author/{id}/posts")
    public String getAuthorIdPosts(Model model, @PathVariable int id) {
        Author author = StreamSupport.stream(authorRepository.findAll().spliterator(), false)
                .filter(a -> a.getId() == id)
                .findAny()
                .orElse(null);

        if (author != null) {
            model.addAttribute("posts", author.getPosts());
            return "authorIdPosts";
        } else {
            model.addAttribute("message", "No posts found");
            return "error";
        }
    }

//    // Stats
//
//    @GetMapping("/stats")
//    public String getStats(Model model) {
//        String[][] stats = {
//                {"Number of authors", String.valueOf(authors.size())},
//                {"Number of posts", String.valueOf(posts.size())},
//                {"Number of comments", String.valueOf(comments.size())},
//                {"Number of attachments", String.valueOf(attachments.size())},
//                {"Average post length", String.valueOf(posts.stream().mapToInt(p -> p.getPost_content().length()).sum() / posts.size())},
//                {"Average comment length", String.valueOf(comments.stream().mapToInt(p -> p.getComment_content().length()).sum() / comments.size())},
//        };
//        model.addAttribute("stats", stats);
//        return "stats";
//    }

    // Files

    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        Resource file = storageService.loadAsResource(filename);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + Objects.requireNonNull(file.getFilename()).substring(24) + "\"").body(file);
    }

    @GetMapping("/delete/file/{filename:.+}")
    public String deleteFile(@PathVariable String filename) throws IOException {
        Attachment attachment = StreamSupport.stream(attachmentRepository.findAll().spliterator(), false)
                .filter(a -> a.getFilename().equals(filename))
                .findFirst()
                .orElse(null);

        if (attachment != null) {
            Files.delete(Paths.get("upload/" + attachment.getFilename()));
            if (postRepository.findById(attachment.getPost().getId()).isPresent()) {
                Post post = postRepository.findById(attachment.getPost().getId()).get();
                post.setAttachments(post.getAttachments().stream()
                        .filter(a -> !a.getFilename().equals(attachment.getFilename()))
                        .collect(Collectors.toList()));
                postRepository.save(post);
                attachmentRepository.delete(attachment);
            }
            return "redirect:/edit/post/" + attachment.getPost().getId();
        } else {
            return "error";
        }
    }

//    @PostMapping("/import")
//    public String csvImport(Model model, @RequestParam("file") MultipartFile file) {
//        try {
//            String str = new String(file.getBytes());
//            ObjectMapper objectMapper = new ObjectMapper();
//            All all = objectMapper.readValue(str, All.class);
//            authors = all.getAuthors();
//            posts = all.getPosts();
//            postsAuthors = all.getPostsAuthors();
//            comments = all.getComments();
//            attachments = all.getAttachments();
//            return "redirect:/stats";
//        } catch (Exception e) {
//            model.addAttribute("message", "Cannot open file!");
//            return "error";
//        }
//    }
//
//
//    @GetMapping("/export")
//    @ResponseBody
//    public ResponseEntity<InputStreamResource> export() throws IOException {
//        String filename = "export-" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS").format(new Date(System.currentTimeMillis())) + ".csv";
//        Path path = Paths.get("upload/" + filename);
//        All all = new All();
//        all.setAuthors(authors);
//        all.setPosts(posts);
//        all.setPostsAuthors(postsAuthors);
//        all.setComments(comments);
//        all.setAttachments(attachments);
//        ObjectMapper objectMapper = new ObjectMapper();
//        Writer writer = new FileWriter(path.toString());
//        writer.write(objectMapper.writeValueAsString(all));
//        writer.close();
//
//        InputStreamResource isr = new InputStreamResource(new FileInputStream(path.toString()) {
//            @Override
//            public void close() throws IOException {
//                super.close();
//                Files.delete(path);
//            }
//        });
//        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
//                "attachment; filename=\"" + filename + "\"").body(isr);
//    }

}
