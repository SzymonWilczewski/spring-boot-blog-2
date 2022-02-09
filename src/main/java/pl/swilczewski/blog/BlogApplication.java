package pl.swilczewski.blog;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pl.swilczewski.blog.domain.*;
import pl.swilczewski.blog.repository.*;
import pl.swilczewski.blog.storage.StorageProperties;
import pl.swilczewski.blog.storage.StorageService;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class BlogApplication {
	Dotenv dotenv = Dotenv.load();
	private final AuthorRepository authorRepository;
	private final PostRepository postRepository;
	private final CommentRepository commentRepository;
	private final AttachmentRepository attachmentRepository;
	private final BCryptPasswordEncoder bCryptPasswordEncoder;
	private final UserRepository userRepository;

	public BlogApplication(AuthorRepository authorRepository, PostRepository postRepository, CommentRepository commentRepository, AttachmentRepository attachmentRepository, BCryptPasswordEncoder bCryptPasswordEncoder, UserRepository userRepository) {
		this.authorRepository = authorRepository;
		this.postRepository = postRepository;
		this.commentRepository = commentRepository;
		this.attachmentRepository = attachmentRepository;
		this.bCryptPasswordEncoder = bCryptPasswordEncoder;
		this.userRepository = userRepository;
	}

	public static void main(String[] args) {
		SpringApplication.run(BlogApplication.class, args);
	}

	@Bean
	public CommandLineRunner init(StorageService storageService) {
		return (args) -> {
			storageService.deleteAll();
			storageService.init();

//			Create admin
			if (dotenv.get("ADMIN_USERNAME") != null && userRepository.findByUsername(dotenv.get("ADMIN_USERNAME")) == null) {
				userRepository.save(new User(dotenv.get("ADMIN_EMAIL"), dotenv.get("ADMIN_USERNAME"), bCryptPasswordEncoder.encode(dotenv.get("ADMIN_PASSWORD")), "ADMIN"));
			}

//			Authors.csv
			try (CSVReader reader = new CSVReaderBuilder(new FileReader("src/main/resources/csv/Authors.csv")).withSkipLines(1).build()) {

				List<String[]> r = reader.readAll();
				r.forEach(row -> {
					Author author = new Author(Integer.parseInt(row[0]), row[1], row[2], row[3]);
					if (authorRepository.findByUsername(author.getUsername()) == null) {
						authorRepository.save(author);
					}
				});
			} catch (IOException | CsvException e) {
				e.printStackTrace();
			}

//			Posts.csv
			List<Post> posts = new ArrayList<>();
			try (CSVReader reader = new CSVReaderBuilder(new FileReader("src/main/resources/csv/Posts.csv")).withSkipLines(1).build()) {
				List<String[]> r = reader.readAll();
				r.forEach(row -> posts.add(new Post(Integer.parseInt(row[0]), row[1], row[2])));
			} catch (IOException | CsvException e) {
				e.printStackTrace();
			}

//			Posts_Authors.csv
			try (CSVReader reader = new CSVReaderBuilder(new FileReader("src/main/resources/csv/Posts_Authors.csv")).withSkipLines(1).build()) {
				List<String[]> r = reader.readAll();
				r.forEach(row -> {
					if (authorRepository.findById(Integer.parseInt(row[1])).isPresent()) {
						Author author = authorRepository.findById(Integer.parseInt(row[1])).get();
						posts.stream().filter(p -> p.getId().equals(Integer.parseInt(row[0]))).forEach(p -> p.addAuthor(author));
					}
				});
			} catch (IOException | CsvException e) {
				e.printStackTrace();
			}

			posts.forEach(p -> {
				if (!postRepository.findById(p.getId()).isPresent()) {
					postRepository.save(p);
				}
			});

//			Comments.csv
			try (CSVReader reader = new CSVReaderBuilder(new FileReader("src/main/resources/csv/Comments.csv")).withSkipLines(1).build()) {
				List<String[]> r = reader.readAll();
				r.forEach(row -> {
					if (posts.stream().anyMatch(p -> p.getId().equals(Integer.parseInt(row[2])))) {
						Post post = posts.stream().filter(p -> p.getId().equals(Integer.parseInt(row[2]))).findFirst().orElse(null);
						Comment comment = new Comment(Integer.parseInt(row[0]), row[1], post, row[3]);
						commentRepository.save(comment);
					}
				});
			} catch (IOException | CsvException e) {
				e.printStackTrace();
			}

//			Attachments.csv
			try (CSVReader reader = new CSVReaderBuilder(new FileReader("src/main/resources/csv/Attachments.csv")).withSkipLines(1).build()) {
				List<String[]> r = reader.readAll();
				r.forEach(row -> {
					if (posts.stream().anyMatch(p -> p.getId().equals(Integer.parseInt(row[0])))) {
						Post post = posts.stream().filter(p -> p.getId().equals(Integer.parseInt(row[0]))).findFirst().orElse(null);
						List<Attachment> attachments = attachmentRepository.findByPost(post);
						if (attachments.stream().noneMatch(a -> a.getFilename().equals(row[1]))) {
							Attachment attachment = new Attachment(post, row[1]);
							attachmentRepository.save(attachment);
						}
					}
				});
			} catch (IOException | CsvException e) {
				e.printStackTrace();
			}
		};
	}
}
