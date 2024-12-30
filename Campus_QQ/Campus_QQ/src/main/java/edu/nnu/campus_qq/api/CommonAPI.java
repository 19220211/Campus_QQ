package edu.nnu.campus_qq.api;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.nnu.campus_qq.config.security.UserPrincipal;
import edu.nnu.campus_qq.mongo.model.User;
import edu.nnu.campus_qq.mongo.service.RelationService;
import edu.nnu.campus_qq.mongo.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/common")
public class CommonAPI {

    @Autowired
    private UserService userService;

    @Autowired
    private RelationService relationService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping(value = "/register")
    public boolean register(HttpServletRequest request) {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String nickname = request.getParameter("nickname");
        User user = new User(username, password, nickname);
        // TODO 参数校验
        return userService.addUser(user);
    }

    @PostMapping(value = "/login")
    public boolean login(HttpServletRequest request) {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        User user = userService.findByUsernameAndPassword(username, password);
        if (user != null) {
            return true;
        } else {
            return false;
        }
    }

    @PostMapping("/add")
    public Response add(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestParam String friend) {
        User friendUser = userService.getByUsername(friend);
        if (friendUser == null) {
            return new Response(false, "用户不存在");
        }
        if (userPrincipal.getUsername().equals(friend)) {
            return new Response(false, "不能添加自己为好友");
        }
        if (relationService.isAlreadyFriend(userPrincipal.getUsername(), friend)) {
            return new Response(false, "该用户已经是您的好友");
        }
        relationService.requestFriendship(userPrincipal.getUsername(), friend);
        messagingTemplate.convertAndSendToUser(friend, "/topic/friendRequest", new FriendRequestMessage(userPrincipal.getUsername(), userPrincipal.getNickname()));
        return new Response(true, "好友请求已发送");
    }

    @GetMapping("/getPendingFriendRequests")
    public List<FriendRequestMessage> getPendingFriendRequests(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return relationService.getPendingFriendRequestsAsMessages(userPrincipal.getUsername());
    }

    @PostMapping("/confirm")
    public Response confirm(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestParam String from) {
        relationService.confirmFriendship(from, userPrincipal.getUsername());
        messagingTemplate.convertAndSendToUser(from, "/topic/friendConfirm", new FriendConfirmMessage(userPrincipal.getUsername(), userPrincipal.getNickname(), userPrincipal.getAvatar(),"已通过您的好友请求"));
        return new Response(true, "已接受好友请求");
    }

    @PostMapping("/decline")
    public Response decline(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestParam String from) {
        relationService.declineFriendship(from, userPrincipal.getUsername());
        messagingTemplate.convertAndSendToUser(from, "/topic/friendConfirm", new FriendConfirmMessage(userPrincipal.getUsername(), userPrincipal.getNickname(), userPrincipal.getAvatar(), "已拒绝您的好友请求"));
        return new Response(true, "已拒绝好友请求");
    }

    @PostMapping("/all")
    public void sendMessageToAll(@RequestBody ChatMessage chatMessage) {
        messagingTemplate.convertAndSend("/topic/notice", chatMessage);
    }

    @PostMapping("/chat")
    public void sendMessageToUser(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody ChatMessage chatMessage) {
        chatMessage.setSender(userPrincipal.getUsername());
        chatMessage.setAvatar(userService.getByUsername(chatMessage.getSender()).getAvatar());
        chatMessage.setNickname(userService.getByUsername(chatMessage.getSender()).getNickname());
        messagingTemplate.convertAndSendToUser(chatMessage.getReceiver(), "/user/topic/privateChat", chatMessage);
    }

    @PostMapping("/group")
    public ResponseEntity<?> sendMessageToGroup(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody GroupChatMessage groupChatMessage) {
        try {
            String sender = userPrincipal.getUsername();
            User user = userService.getByUsername(sender);

            if (user == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            groupChatMessage.setSender(sender);
            groupChatMessage.setAvatar(user.getAvatar());
            groupChatMessage.setNickname(user.getNickname());

            for (String receiver : groupChatMessage.getReceivers()) {
                messagingTemplate.convertAndSendToUser(receiver, "/user/topic/groupChat", groupChatMessage);
            }

            return ResponseEntity.ok("Message sent successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to send message: " + e.getMessage());
        }
    }



    @GetMapping("/getUserProfile")
    public ResponseEntity<User> getUserProfile() {
        User user = userService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    @PostMapping("/updateProfile")
    public ResponseEntity<String> updateProfile(@RequestParam String newPassword, @RequestParam String newNickname) {
        boolean success = userService.updateUserProfile(newPassword, newNickname);
        if (success) {
            return ResponseEntity.ok("个人信息已更新！");
        } else {
            return ResponseEntity.badRequest().body("操作失败：更新失败");
        }
    }

    @PostMapping("/deleteFriend")
    public ResponseEntity<String> deleteFriend(@RequestParam String friendUsername) {
        boolean success = relationService.deleteFriend(friendUsername);
        if (success) {
            return ResponseEntity.ok("已删除好友！");
        } else {
            return ResponseEntity.badRequest().body("操作失败：删除好友失败");
        }
    }


    static class Response {
        private boolean success;
        private String message;

        public Response(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class FriendRequestMessage {
        private String fromUsername;
        private String fromNickname;

        public FriendRequestMessage(String fromUsername, String fromNickname) {
            this.fromUsername = fromUsername;
            this.fromNickname = fromNickname;
        }

        public String getFromUsername() {
            return fromUsername;
        }

        public void setFromUsername(String fromUsername) {
            this.fromUsername = fromUsername;
        }

        public String getFromNickname() {
            return fromNickname;
        }

        public void setFromNickname(String fromNickname) {
            this.fromNickname = fromNickname;
        }
    }

    static class FriendConfirmMessage {
        private String fromUsername;
        private String fromNickname;
        private String avatar;
        private String message;

        public FriendConfirmMessage(String fromUsername, String fromNickname, String avatar,String message) {
            this.fromUsername = fromUsername;
            this.fromNickname = fromNickname;
            this.avatar=avatar;
            this.message = message;
        }

        public String getFromUsername() {
            return fromUsername;
        }

        public void setFromUsername(String fromUsername) {
            this.fromUsername = fromUsername;
        }

        public String getFromNickname() {
            return fromNickname;
        }

        public void setFromNickname(String fromNickname) {
            this.fromNickname = fromNickname;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    static class ChatMessage {
        private String sender;
        private String avatar;
        private String nickname;
        private String content;
        private String sendTime;
        private String receiver;

        public String getSender() {
            return sender;
        }

        public void setSender(String sender) {
            this.sender = sender;
        }

        public String getAvatar() {
            return avatar;
        }

        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getSendTime() {
            return sendTime;
        }

        public void setSendTime(String sendTime) {
            this.sendTime = sendTime;
        }

        public String getReceiver() {
            return receiver;
        }

        public void setReceiver(String receiver) {
            this.receiver = receiver;
        }
    }

    public class GroupChatMessage {
        private String sender;
        private String avatar;
        private String nickname;
        private List<String> receivers;
        private String content;
        private String sendTime;

        // Getters and Setters
        public String getSender() {
            return sender;
        }

        public void setSender(String sender) {
            this.sender = sender;
        }

        public String getAvatar() {
            return avatar;
        }

        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public List<String> getReceivers() {
            return receivers;
        }

        public void setReceivers(List<String> receivers) {
            this.receivers = receivers;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getSendTime() {
            return sendTime;
        }

        public void setSendTime(String sendTime) {
            this.sendTime = sendTime;
        }
    }

}