package com.library.constant;

//系统常量定义（便于维护和修改）
public class LibraryConstants {

    //用户相关
    public static final int DEFAULT_MAX_BORROW = 10;
    public static final String USER_STATUS_ACTIVE = "ACTIVE";
    public static final String USER_STATUS_FROZEN = "FROZEN";

    //图书副本状态
    public static final String COPY_STATUS_AVAILABLE = "AVAILABLE";
    public static final String COPY_STATUS_BORROWED = "BORROWED";
    //public static final String COPY_STATUS_IN_TRANSIT = "IN_TRANSIT";
    public static final String COPY_STATUS_RESERVED = "RESERVED";      //新增：预留状态（调拨后等待用户取书）
    public static final String COPY_STATUS_LOST = "LOST";

    //借阅记录状态
    public static final String BORROW_STATUS_RESERVED = "RESERVED";    //已预留（等待取书，24小时有效）
    public static final String BORROW_STATUS_BORROWING = "BORROWING";  //借阅中
    public static final String BORROW_STATUS_RETURNED = "RETURNED";    //已归还

    //预约状态
    public static final String RESERVATION_STATUS_PENDING = "PENDING";
    public static final String RESERVATION_STATUS_FULFILLED = "FULFILLED";
    public static final String RESERVATION_STATUS_CANCELED = "CANCELED";
    public static final String RESERVATION_STATUS_EXPIRED = "EXPIRED";

    //调拨状态
    public static final String TRANSFER_STATUS_PENDING = "PENDING";
    public static final String TRANSFER_STATUS_IN_TRANSIT = "IN_TRANSIT";
    public static final String TRANSFER_STATUS_COMPLETED = "COMPLETED";

    //业务规则
    public static final int RESERVE_HOURS = 24;        //调拨后预留24小时
    public static final int BORROW_DAYS = 30;          //默认借阅期限30天
    public static final int RESERVATION_EXPIRE_DAYS = 3; //预约有效期3天

    //信用扣分规则
    public static final int BORROW_DAILY_DEDUCTION = 2;  //借书每逾期1天扣2分
    public static final int BORROW_CEILING_DEDUCTION = 20; //借书逾期上限扣20分

}