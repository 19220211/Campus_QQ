package edu.nnu.campus_qq.mongo.service;

import edu.nnu.campus_qq.api.CommonAPI.FriendRequestMessage;
import edu.nnu.campus_qq.mongo.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@lombok.extern.slf4j.Slf4j
public class RelationService {

    private final ConcurrentHashMap<String, List<String>> relations;
    private final ConcurrentHashMap<String, List<String>> pendingRequests;

    @Autowired
    private UserService userService;

    public RelationService() {
        relations = new ConcurrentHashMap<>();
        pendingRequests = new ConcurrentHashMap<>();
    }

    // 发送好友请求
    public void requestFriendship(String sender, String receiver) {
        User receiverUser = userService.getByUsername(receiver);
        if (receiverUser == null) {
            log.info("用户不存在：" + receiver);
            return;
        }
        if (sender.equals(receiver)) {
            log.info("不能添加自己为好友：" + receiver);
            return;
        }
        List<String> requests = pendingRequests.getOrDefault(receiver, new ArrayList<>());
        if (!requests.contains(sender)) {
            requests.add(sender);
            pendingRequests.put(receiver, requests);
        }
    }

    // 确认好友请求
    public void confirmFriendship(String sender, String receiver) {
        removePendingRequest(sender, receiver);
        establishRelation(sender, receiver);
        establishRelation(receiver, sender);
    }

    // 拒绝好友请求
    public void declineFriendship(String sender, String receiver) {
        removePendingRequest(sender, receiver);
    }

    private void removePendingRequest(String sender, String receiver) {
        List<String> requests = pendingRequests.get(receiver);
        if (requests != null && requests.contains(sender)) {
            requests.remove(sender);
            pendingRequests.put(receiver, requests);
        }
    }

    private void establishRelation(String username, String friendName) {
        List<String> friends = relations.get(username);
        if (friends == null) {
            friends = new ArrayList<>();
        }
        friends.add(friendName);
        relations.put(username, friends);
    }

    // 好友列表
    public List<User> listFriends(String username) {
        List<User> users = new ArrayList<>();
        List<String> friends = relations.get(username);
        if (friends != null) {
            for (String friend : friends) {
                User user = userService.getByUsername(friend);
                users.add(user);
            }
        }
        return users;
    }

    // 获取待处理的好友请求作为消息列表
    public List<FriendRequestMessage> getPendingFriendRequestsAsMessages(String username) {
        List<FriendRequestMessage> messages = new ArrayList<>();
        List<String> requests = pendingRequests.get(username);
        if (requests != null) {
            for (String request : requests) {
                User user = userService.getByUsername(request);
                messages.add(new FriendRequestMessage(user.getUsername(), user.getNickname()));
            }
        }
        return messages;
    }

    // 检查是否已经是好友
    public boolean isAlreadyFriend(String user1, String user2) {
        List<String> friendsOfUser1 = relations.get(user1);
        if (friendsOfUser1 != null && friendsOfUser1.contains(user2)) {
            return true;
        }
        List<String> friendsOfUser2 = relations.get(user2);
        if (friendsOfUser2 != null && friendsOfUser2.contains(user1)) {
            return true;
        }
        return false;
    }

    // 删除好友
    public boolean deleteFriend(String friendUsername) {
        User currentUser = getCurrentUser();
        User friendUser = userService.getByUsername(friendUsername);

        if (currentUser == null || friendUser == null) {
            return false;
        }

        String currentUserKey = currentUser.getUsername();
        String friendUserKey = friendUser.getUsername();

        if (relations.containsKey(currentUserKey)) {
            relations.get(currentUserKey).remove(friendUserKey);
        }

        if (relations.containsKey(friendUserKey)) {
            relations.get(friendUserKey).remove(currentUserKey);
        }

        return true;
    }

    // 获取当前用户
    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        return userService.getByUsername(username);
    }
}