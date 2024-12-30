package edu.nnu.campus_qq.chat.Model;

import java.util.Date;
import java.util.List;

import lombok.Data;

@Data

public class BaseMessage {

    // 消息类型
    private String type;

    // 消息内容
    private String content;

    // 发送者
    private String sender;

    // 接受者 类型
    private String toType;
    //
//	// 接受者
    private String receiver;
    //
//	// 发送时间
    private Date date;
    //
    private List<String> receivers;


    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getToType() {
        return toType;
    }

    public void setToType(String toType) {
        this.toType = toType;
    }

    public List<String> getReceivers() {
        return receivers;
    }

    public void setReceivers(List<String> receivers) {
        this.receivers = receivers;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
