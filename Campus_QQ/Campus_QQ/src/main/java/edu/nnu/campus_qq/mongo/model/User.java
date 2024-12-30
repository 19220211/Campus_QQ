package edu.nnu.campus_qq.mongo.model;


import java.util.Date;
import java.util.Random;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
public class User {


    private String username;

    // 密码
    private String password;

    // 昵称
    private String nickname;

    // 头像
    private String avatar;

    // 登录时间
    private Date joinTime;
    private boolean online;

    public User(String username, String password, String nickname) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.avatar = "/image/user.png";
        this.joinTime = new Date();
    }



}
