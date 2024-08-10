package org.example.fourtreesproject.user.model.response;

import lombok.Builder;
import lombok.Getter;
import org.example.fourtreesproject.coupon.model.Coupon;
import org.example.fourtreesproject.coupon.model.response.CouponResponse;
import org.example.fourtreesproject.delivery.model.DeliveryAddress;
import org.example.fourtreesproject.delivery.model.response.DeliveryAddressResponse;

import java.time.LocalDate;
import java.util.List;

@Builder
@Getter
public class UserInfoResponse {
    private String name;
    private String password;
    private LocalDate birth;
    private String email;
    private String sex;
    private String address;
    private int postCode;
    private String phoneNumber;
    private List<CouponResponse> couponList;
    private List<DeliveryAddressResponse> deliveryAddressList;
    private Integer userPoint;
}
