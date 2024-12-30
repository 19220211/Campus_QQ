package edu.nnu.campus_qq.chat.controller;

import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;

import com.alibaba.fastjson.JSON;

import edu.nnu.campus_qq.chat.Model.BaseMessage;
import edu.nnu.campus_qq.chat.Model.ChatMessage;
import edu.nnu.campus_qq.mongo.model.User;
import edu.nnu.campus_qq.mongo.service.UserService;

@Controller
public class ChatController {

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd HH:mm");

    @Autowired
    private SimpMessagingTemplate template;

    @Autowired
    private UserService userService;

    @MessageMapping("/all")
    public void all(Principal principal, String message) {
        BaseMessage baseMessage = JSON.parseObject(message, BaseMessage.class);
        baseMessage.setSender(principal.getName());
        ChatMessage chatMessage = createMessage(baseMessage.getSender(), baseMessage.getContent());
        template.convertAndSend("/topic/notice",JSON.toJSONString(chatMessage));
    }

    @MessageMapping("/chat")
    public void chat(Principal principal, String message) {
        BaseMessage baseMessage = JSON.parseObject(message, BaseMessage.class);
        baseMessage.setSender(principal.getName());
        this.send(baseMessage);
    }

    @MessageMapping("/groupChat")
    public void groupChat(Principal principal, String message) {
        BaseMessage baseMessage = JSON.parseObject(message, BaseMessage.class);
        baseMessage.setSender(principal.getName());
        this.sendToGroup(baseMessage);
    }

    private void sendToGroup(BaseMessage baseMessage) {
        List<String> receivers = baseMessage.getReceivers();
        for (String receiver : receivers) {
            ChatMessage chatMessage = createMessage(baseMessage.getSender(), baseMessage.getContent());
            template.convertAndSendToUser(receiver, "/topic/chat", JSON.toJSONString(chatMessage));
        }
    }

    @Async
    protected void send(BaseMessage message) {
        message.setDate(new Date());
        ChatMessage chatMessage = createMessage(message.getSender(), message.getContent());
        template.convertAndSendToUser(message.getReceiver(), "/topic/chat", JSON.toJSONString(chatMessage));
    }

    private ChatMessage createMessage(String username, String message) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setUsername(username);
        User user = userService.getByUsername(username);
        chatMessage.setAvatar(user.getAvatar());
        chatMessage.setNickname(user.getNickname());
        chatMessage.setContent(message);
        chatMessage.setSendTime(simpleDateFormat.format(new Date()));
        return chatMessage;
    }

}