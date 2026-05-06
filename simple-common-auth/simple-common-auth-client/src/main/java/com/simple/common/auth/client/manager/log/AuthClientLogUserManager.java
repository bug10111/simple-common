//package com.simple.common.auth.client.manager.log;
//
//import com.simple.common.auth.client.util.LoginUserUtils;
//import com.simple.common.logs.client.common.manager.LogUserManager;
//import org.springframework.context.annotation.Primary;
//import org.springframework.stereotype.Component;
//
///**
// * Created with IntelliJ IDEA
// *
// * @author qty
// */
//@Component
//@Primary
//public class AuthClientLogUserManager implements LogUserManager {
//
//    @Override
//    public String loginNickName() {
//        return LoginUserUtils.getUserTemporary().getNickname();
//    }
//
//    @Override
//    public String loginUserId() {
//        return LoginUserUtils.getUserTemporary().getUserId();
//    }
//}
