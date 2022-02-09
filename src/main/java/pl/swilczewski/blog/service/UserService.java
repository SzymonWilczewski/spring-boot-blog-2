package pl.swilczewski.blog.service;

import pl.swilczewski.blog.domain.User;

public interface UserService {
    void save(User user);

    User findByUsername(String username);
}
