package pl.swilczewski.blog.service;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import pl.swilczewski.blog.domain.User;
import pl.swilczewski.blog.repository.UserRepository;
import sendinblue.ApiClient;
import sendinblue.Configuration;
import sendinblue.auth.ApiKeyAuth;
import sibApi.TransactionalEmailsApi;
import sibModel.SendSmtpEmail;
import sibModel.SendSmtpEmailReplyTo;
import sibModel.SendSmtpEmailSender;
import sibModel.SendSmtpEmailTo;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    Dotenv dotenv = Dotenv.load();
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public UserServiceImpl(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Override
    public void save(User user) {
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        sendConfirmation(user);
    }

    public void sendConfirmation(User user) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        ApiKeyAuth apiKey = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
        apiKey.setApiKey(dotenv.get("API_KEY"));

        try {
            TransactionalEmailsApi api = new TransactionalEmailsApi();
            SendSmtpEmailSender sender = new SendSmtpEmailSender();
            sender.setEmail(dotenv.get("SENDER_EMAIL"));
            sender.setName(dotenv.get("SENDER_NAME"));
            List<SendSmtpEmailTo> toList = new ArrayList<>();
            SendSmtpEmailTo to = new SendSmtpEmailTo();
            to.setEmail(user.getEmail());
            to.setName(user.getUsername());
            toList.add(to);
            SendSmtpEmailReplyTo replyTo = new SendSmtpEmailReplyTo();
            replyTo.setEmail(dotenv.get("SENDER_EMAIL"));
            replyTo.setName(dotenv.get("SENDER_NAME"));
            SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
            sendSmtpEmail.setSender(sender);
            sendSmtpEmail.setTo(toList);
            sendSmtpEmail.setHtmlContent(
                    "<h1>Thank you for joining Spring Blog!</h1>\n" +
                    "<br>\n" +
                    "<h4>If you have any questions, just reply to this email.</h4>\n" +
                    "<br>\n" +
                    "<h4>Regards</h4>\n" +
                    "<h4>Spring Blog Team</h4>"
            );
            sendSmtpEmail.setSubject("Welcome " + user.getUsername());
            sendSmtpEmail.setReplyTo(replyTo);
            api.sendTransacEmail(sendSmtpEmail);
        } catch (Exception e) {
            System.out.println("Exception occurred:- " + e.getMessage());
        }
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
