package com.library.service;

import com.library.constant.LibraryConstants;
import com.library.entity.User;
import com.library.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

//用户服务（借阅权限检查）
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BookBorrowService bookBorrowService;

    //检查用户是否有权限借书（返回null表示通过，否则返回错误原因）
    public String checkBorrowPermission(Long userId, Long biblioId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return "用户不存在";
        }

        //检查用户状态
        if (!LibraryConstants.USER_STATUS_ACTIVE.equals(user.getStatus())) {
            return "用户状态异常（冻结或注销）";
        }

        //检查是否已达最大借阅数
        if (user.getCurrentBorrowCount() >= user.getMaxBorrowCount()) {
            return "已达最大借阅数限制（当前" + user.getCurrentBorrowCount() + "/" + user.getMaxBorrowCount() + "）";
        }

        //检查是否已借阅该书（防止同一用户重复借同一本书）
        if (bookBorrowService.hasBorrowedBiblio(userId, biblioId)) {
            return "您已借阅该书，不可重复借阅";
        }

        return null; //检查通过
    }

    //增加用户当前借阅数
    public void incrementBorrowCount(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setCurrentBorrowCount(user.getCurrentBorrowCount() + 1);
            userMapper.updateById(user);
        }
    }

    //减少用户当前借阅数（还书时用）
    public void decrementBorrowCount(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null && user.getCurrentBorrowCount() > 0) {
            user.setCurrentBorrowCount(user.getCurrentBorrowCount() - 1);
            userMapper.updateById(user);
        }
    }
}