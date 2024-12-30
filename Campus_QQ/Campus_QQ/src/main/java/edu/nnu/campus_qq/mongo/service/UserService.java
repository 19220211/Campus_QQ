package edu.nnu.campus_qq.mongo.service;

import edu.nnu.campus_qq.mongo.model.User;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Getter
@Service
public class UserService {

    private final ConcurrentHashMap<String, User> users;

    public UserService() {
        users = new ConcurrentHashMap<>();
    }

    public boolean addUser(User user) {
        boolean isExist = users.containsKey(user.getUsername());
        if (isExist) {
            return false;
        }
        users.put(user.getUsername(), user);
        return true;
    }

    public User findByUsernameAndPassword(String username, String password) {
        if (users.containsKey(username)) {
            User u = users.get(username);
            if (password.equals(u.getPassword())) {
                return u;
            }
        }
        return null;
    }

    public User getByUsername(String username) {
        return users.get(username);
    }

    public User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        return getByUsername(username);
    }

    public boolean updateUserProfile(String newPassword, String newNickname) {
        User user = getCurrentUser();
        if (user == null) {
            return false;
        }
        user.setPassword(newPassword);
        user.setNickname(newNickname);
        users.put(user.getUsername(), user);
        return true;
    }


}