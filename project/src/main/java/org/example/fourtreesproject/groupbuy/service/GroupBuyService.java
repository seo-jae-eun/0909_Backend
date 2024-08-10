package org.example.fourtreesproject.groupbuy.service;


import com.siot.IamportRestClient.exception.IamportResponseException;
import lombok.RequiredArgsConstructor;
import org.example.fourtreesproject.bid.model.entity.Bid;
import org.example.fourtreesproject.bid.repository.BidRepository;
import org.example.fourtreesproject.common.BaseResponseStatus;
import org.example.fourtreesproject.coupon.model.UserCoupon;
import org.example.fourtreesproject.coupon.repository.UserCouponRepository;
import org.example.fourtreesproject.exception.custom.InvalidGroupBuyException;
import org.example.fourtreesproject.exception.custom.InvalidUserException;
import org.example.fourtreesproject.groupbuy.model.entity.Category;
import org.example.fourtreesproject.groupbuy.model.entity.GroupBuy;
import org.example.fourtreesproject.groupbuy.model.entity.Likes;
import org.example.fourtreesproject.groupbuy.model.request.GroupBuyCreateRequest;
import org.example.fourtreesproject.groupbuy.model.request.GroupBuySearchRequest;
import org.example.fourtreesproject.groupbuy.model.response.*;
import org.example.fourtreesproject.groupbuy.repository.CategoryRepository;
import org.example.fourtreesproject.groupbuy.repository.GroupBuyRepository;
import org.example.fourtreesproject.groupbuy.repository.LikesRepository;
import org.example.fourtreesproject.orders.model.entity.Orders;
import org.example.fourtreesproject.orders.repository.OrdersRepository;
import org.example.fourtreesproject.orders.service.OrdersService;
import org.example.fourtreesproject.product.model.entity.Product;
import org.example.fourtreesproject.product.model.entity.ProductImg;
import org.example.fourtreesproject.user.model.dto.CustomUserDetails;
import org.example.fourtreesproject.user.model.entity.User;
import org.example.fourtreesproject.user.model.entity.UserDetail;
import org.example.fourtreesproject.user.repository.UserDetailRepository;
import org.example.fourtreesproject.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GroupBuyService {

    private final GroupBuyRepository gpbuyRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BidRepository bidRepository;
    private final LikesRepository likesRepository;
    private final OrdersRepository ordersRepository;
    private final UserCouponRepository userCouponRepository;
    private final OrdersService ordersService;
    private final UserDetailRepository userDetailRepository;

    public GroupBuyRegisterResponse save(Long user_idx, GroupBuyCreateRequest request) {
        User user = userRepository.findById(user_idx).orElseThrow(() -> new InvalidUserException(BaseResponseStatus.USER_INFO_DETAIL_FAIL));
        Category category = categoryRepository.findById(request.getCategoryIdx()).orElseThrow(() -> new InvalidGroupBuyException(BaseResponseStatus.REQUEST_FAIL));
        GroupBuy groupbuy = GroupBuy.builder()
                .user(user)
                .category(category)
                .gpbuyTitle(request.getTitle())
                .gpbuyQuantity(request.getGpbuyQuantity())
                .gpbuyRemainQuantity(request.getGpbuyQuantity())
                .gpbuyContent(request.getContent())
                .gpbuyRegedAt(LocalDateTime.now())
                .gpbuyBidEndedAt(LocalDateTime.now().plusDays(2))
                .build();

        GroupBuy res = gpbuyRepository.save(groupbuy);
        if (res == null){
            return null;
        }
        return GroupBuyRegisterResponse.builder().groupBuyIdx(res.getIdx()).build();
    }
    //공구 시작 기능

    public List<RegisteredBidListResponse> findBidList(Long gpbuyIdx) {
        GroupBuy groupBuy = gpbuyRepository.findById(gpbuyIdx).orElseThrow(
                () -> new InvalidGroupBuyException(BaseResponseStatus.GROUPBUY_EMPTY)
        );
        List<Bid> bidList = bidRepository.findAllByGpbuyIdx(gpbuyIdx).orElseThrow(
                () -> new InvalidGroupBuyException(BaseResponseStatus.GROUPBUY_LIST_REGISTERD_BID_EMPTY)
        );
        List<RegisteredBidListResponse> responseList = new ArrayList<>();
        for (Bid bid: bidList){
            String productThumbnailImg = "";
            for (ProductImg img : bid.getProduct().getProductImgList()){
                if (img.getProductImgSequence()==0){
                    productThumbnailImg = img.getProductImgUrl();
                }
            }
            RegisteredBidListResponse response = RegisteredBidListResponse.builder()
                    .bidIdx(bid.getIdx())
                    .productIdx(bid.getProduct().getIdx())
                    .productName(bid.getProduct().getProductName())
                    .productThumbnailImg(productThumbnailImg)
                    .bidPrice(bid.getBidPrice())
                    .companyName(bid.getProduct().getCompany().getCompanyName())
                    .build();
            responseList.add(response);
        }
        return responseList;
    }

    public List<GroupBuyListResponse> list(Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "idx"));
        Slice<GroupBuy> result = gpbuyRepository.findSliceByGpbuyStatus(pageable,"진행");
        List<GroupBuy> slicedResult = result.getContent();
        List<GroupBuyListResponse> responseList = new ArrayList<>();
        Product selectedProduct = null;
        ProductImg selectedProductThumbnailImg = null;
        Bid selectedBid = null;


        for (GroupBuy g : slicedResult){
            //선정된 상품 확인 (입찰 목록에서 선정 상태 확인)
            for (Bid b: g.getBidList()){
                if (isSelected(b)){
                    selectedBid = b;
                    selectedProduct = selectedBid.getProduct();
                    selectedProductThumbnailImg = extractThumbnailImg(selectedProduct.getProductImgList());
                    break;
                }
            }

            GroupBuyListResponse response = GroupBuyListResponse.builder()
                    .gpbuyIdx(g.getIdx())
                    .gpbuyQuantity(g.getGpbuyQuantity())
                    .gpbuyRemainQuantity(g.getGpbuyRemainQuantity())
                    .productThumbnailImg(selectedProductThumbnailImg.getProductImgUrl())
                    .productName(selectedProduct.getProductName())
                    .bidPrice(selectedBid.getBidPrice())
                    .companyName(selectedProduct.getCompany().getCompanyName())
                    .gpbuyStartedAt(g.getGpbuyStartedAt())
                    .gpbuyEndedAt(g.getGpbuyEndedAt())
                    .duration(calcDuration(g.getGpbuyEndedAt()))
                    .build();
            responseList.add(response);
        }

        return responseList;
    }

    //공구 상세조회 기능
    public GroupBuyDetailResponse findByGpbuyIdx(Long gpbuyIdx, CustomUserDetails customUserDetails) {
        Boolean isLiked = false;
        if (customUserDetails != null){
            Optional<Likes> optionalLikes = likesRepository.findByGpbuyIdxAndUserIdx(gpbuyIdx, customUserDetails.getUser().getIdx());
            if (optionalLikes.isPresent()){
                isLiked = true;
            }
        }
            GroupBuy groupBuy = gpbuyRepository.findById(gpbuyIdx).orElseThrow(() -> new InvalidGroupBuyException(BaseResponseStatus.GROUPBUY_EMPTY));
            Bid bid = null;
            ProductImg thumbnailImg = null;
            List<String> productImgList = new ArrayList<>();


            //선정된 입찰 정보 가져오기
            for (Bid b : groupBuy.getBidList()) {
                if (b.getBidSelect()) {
                    bid = b;
                    break;
                }
            }
            //썸네일과 본문 이미지 분리
            for (ProductImg img : bid.getProduct().getProductImgList()) {
                thumbnailImg = extractThumbnailImg(bid.getProduct().getProductImgList());
                if (img.getProductImgSequence() != 0) {
                    productImgList.add(img.getProductImgUrl());
                }
            }
            //응답 DTO 빌딩
            GroupBuyDetailResponse response = GroupBuyDetailResponse.builder()
                    .gpbuyIdx(groupBuy.getIdx())
                    .userIdx(groupBuy.getUser().getIdx())
                    .productThumbnailImg(thumbnailImg.getProductImgUrl())
                    .productImgUrlList(productImgList)
                    .productName(bid.getProduct().getProductName())
                    .bidPrice(bid.getBidPrice())
                    .productContent(bid.getProduct().getProductContent())
                    .companyName(bid.getProduct().getCompany().getCompanyName())
                    .gpbuyRemainQuantity(groupBuy.getGpbuyRemainQuantity())
                    .gpbuyQuantity(groupBuy.getGpbuyQuantity())
                    .gpbuyStartedAt(groupBuy.getGpbuyStartedAt())
                    .gpbuyEndedAt(groupBuy.getGpbuyEndedAt())
                    .duration(calcDuration(groupBuy.getGpbuyEndedAt()))
                    .isLiked(isLiked)
                    .categoryIdx(groupBuy.getCategory().getIdx())
                    .build();

        return response;
    }

    public GroupBuyLikesResponse likesSave(Long gpbuyIdx, Long userIdx) {
        Optional<GroupBuy> groupBuy = gpbuyRepository.findById(gpbuyIdx);
        Optional<User> user = userRepository.findById(userIdx);
        if (groupBuy.isPresent() && user.isPresent()){
            Optional<Likes> likes = likesRepository.findByGpbuyIdxAndUserIdx(gpbuyIdx, userIdx);
            if (likes.isEmpty()){
                Likes newLikes = Likes.builder()
                        .groupBuy(groupBuy.get())
                        .user(user.get())
                        .build();
                likesRepository.save(newLikes);
                return GroupBuyLikesResponse.builder()
                        .isLiked(true)
                        .build();
            } else {
                likesRepository.deleteById(likes.get().getIdx());
                return GroupBuyLikesResponse.builder()
                        .isLiked(false)
                        .build();
            }
        } else if (groupBuy.isEmpty()) {
            throw new InvalidGroupBuyException(BaseResponseStatus.GROUPBUY_EMPTY);
        }else {
            throw  new InvalidGroupBuyException(BaseResponseStatus.USER_NOT_LOGIN);
        }
    }


    //관심 공구 목록 조회
    public List<GroupBuyLikesListResponse> likesList(Long userIdx) {
        List<Likes> likesList = likesRepository.findAllByIdx(userIdx).orElseThrow(
                () -> new InvalidGroupBuyException(BaseResponseStatus.GROUPBUY_LIKES_LIST_EMPTY)
        );
        List<GroupBuyLikesListResponse> responseList = new ArrayList<>();

        for (Likes l : likesList){
            Bid bid = null;
            for (Bid b : l.getGroupBuy().getBidList()) {
                if (isSelected(b)){
                    bid = b;
                    break;
                }
            }
            GroupBuyLikesListResponse response = GroupBuyLikesListResponse.builder()
                    .gpbuyIdx(l.getGroupBuy().getIdx())
                    .gpbuyQuantity(l.getGroupBuy().getGpbuyQuantity())
                    .gpbuyRemainQuantity(l.getGroupBuy().getGpbuyRemainQuantity())
                    .productThumbnailImg(extractThumbnailImg(bid.getProduct().getProductImgList()).getProductImgUrl())
                    .productName(bid.getProduct().getProductName())
                    .bidPrice(bid.getBidPrice())
                    .companyName(bid.getProduct().getCompany().getCompanyName())
                    .gpbuyStartedAt(l.getGroupBuy().getGpbuyStartedAt())
                    .gpbuyEndedAt(l.getGroupBuy().getGpbuyEndedAt())
                    .duration(calcDuration(l.getGroupBuy().getGpbuyEndedAt()))
                    .build();
            responseList.add(response);
        }
        return responseList;
    }

    public List<GroupBuyListResponse> search(GroupBuySearchRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.ASC, "gpbuy_remain_quantity"));
        Slice<GroupBuy> result = gpbuyRepository.searchList(pageable, request);
        List<GroupBuy> slicedResult = result.getContent();
        List<GroupBuyListResponse> responseList = new ArrayList<>();
        Product selectedProduct = null;
        ProductImg selectedProductThumbnailImg = null;
        Bid selectedBid = null;


        for (GroupBuy g : slicedResult){
            //선정된 상품 확인 (입찰 목록에서 선정 상태 확인)
            for (Bid b: g.getBidList()){
                if (isSelected(b)){
                    selectedBid = b;
                    selectedProduct = selectedBid.getProduct();
                    selectedProductThumbnailImg = extractThumbnailImg(selectedProduct.getProductImgList());
                    break;
                }
            }

            GroupBuyListResponse response = GroupBuyListResponse.builder()
                    .gpbuyIdx(g.getIdx())
                    .gpbuyQuantity(g.getGpbuyQuantity())
                    .gpbuyRemainQuantity(g.getGpbuyRemainQuantity())
                    .productThumbnailImg(selectedProductThumbnailImg.getProductImgUrl())
                    .productName(selectedProduct.getProductName())
                    .bidPrice(selectedBid.getBidPrice())
                    .companyName(selectedProduct.getCompany().getCompanyName())
                    .gpbuyStartedAt(g.getGpbuyStartedAt())
                    .gpbuyEndedAt(g.getGpbuyEndedAt())
                    .duration(calcDuration(g.getGpbuyEndedAt()))
                    .build();
            responseList.add(response);
        }

        return responseList;
    }

    //-- 유틸, 추출 메소드 --
    public Boolean isSelected(Bid b){
        return b.getBidSelect();
    }

    public ProductImg extractThumbnailImg(List<ProductImg> imgList){
        for (ProductImg img: imgList){
            if (img.getProductImgSequence() == 0){
                return img;
            }
        }
        return null;
    }

    @Transactional
    public Boolean cancle(Long userIdx, Long ordersIdx) throws IamportResponseException, IOException {
        User user = userRepository.findById(userIdx).orElseThrow(
                () -> new InvalidGroupBuyException(BaseResponseStatus.USER_INFO_DETAIL_FAIL)
        );
        Optional<Orders> optionalOrders = ordersRepository.findByIdxAndUserIdx(ordersIdx,userIdx);
        if (optionalOrders.isPresent()){
            //주문 테이블 상태수정, 취소시간 기록
            Orders orders = optionalOrders.get();
            orders.updateCancledAt(LocalDateTime.now());
            orders.updateOrderStatus("취소");
            ordersRepository.save(orders);

            //공구의 남은 수량 수정
            GroupBuy groupBuy = gpbuyRepository.findById(orders.getGroupBuy().getIdx()).orElseThrow();
            groupBuy.cancleRemainQuantity(orders.getOrderQuantity());
            gpbuyRepository.save(groupBuy);

            //사용자 쿠폰과 포인트 복구
            UserDetail userDetail = userDetailRepository.findByUserIdx(userIdx).orElseThrow();
            userDetail.restorePoint(orders.getUsePoint());
            userDetailRepository.save(userDetail);

            UserCoupon userCoupon = orders.getUserCoupon();
            if (userCoupon != null){
                userCoupon.cancleCoupon();
                userCouponRepository.save(userCoupon);
            }

            //결제 취소 요청
            Bid bid = null;
            for (Bid b: groupBuy.getBidList()){
                if (isSelected(b)){
                    bid = b;
                    break;
                }
            }
            BigDecimal amount = new BigDecimal(orders.getOrderQuantity()*bid.getBidPrice());
            ordersService.refund(orders.getImpUid(), amount);

            return true;
        }else {
            throw new InvalidGroupBuyException(BaseResponseStatus.GROUPBUY_CANCLE_FAIL_EMPTY_ORDER);
        }
    }


    public String calcDuration(LocalDateTime gpbuyEndedAt){
        //현재 시간과 마감기한의 차이를 구함
        Duration duration = Duration.between(LocalDateTime.now(), gpbuyEndedAt);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        //문자열 형태로 변환
        return String.format("%dT%02d:%02d:%02d",
                days, hours, minutes, seconds);
    }


}
