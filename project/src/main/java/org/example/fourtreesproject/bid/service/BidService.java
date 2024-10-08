package org.example.fourtreesproject.bid.service;

import lombok.RequiredArgsConstructor;
import org.example.fourtreesproject.bid.model.entity.Bid;
import org.example.fourtreesproject.bid.model.request.BidCancelRequest;
import org.example.fourtreesproject.bid.model.request.BidModifyRequest;
import org.example.fourtreesproject.bid.model.request.BidRegisterRequest;
import org.example.fourtreesproject.bid.model.response.BidMyListResponse;
import org.example.fourtreesproject.bid.model.response.BidRegisterResponse;
import org.example.fourtreesproject.bid.model.response.GpbuyWaitListResponse;
import org.example.fourtreesproject.bid.repository.BidRepository;
import org.example.fourtreesproject.exception.custom.InvalidBidException;
import org.example.fourtreesproject.groupbuy.model.entity.GroupBuy;
import org.example.fourtreesproject.groupbuy.repository.GroupBuyRepository;
import org.example.fourtreesproject.product.model.entity.Product;
import org.example.fourtreesproject.product.model.entity.ProductImg;
import org.example.fourtreesproject.product.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.example.fourtreesproject.common.BaseResponseStatus.*;

@Service
@RequiredArgsConstructor
public class BidService {
    private final BidRepository bidRepository;
    private final ProductRepository productRepository;
    private final GroupBuyRepository groupBuyRepository;

    public BidRegisterResponse register(Long userIdx, BidRegisterRequest bidRegisterRequest) {
        GroupBuy groupBuy = groupBuyRepository.findById(bidRegisterRequest.getGpbuyIdx())
                .orElseThrow(() -> new InvalidBidException((GROUPBUY_LIST_FAIL)));
        // 공구 상태가 대기일 경우에만 가능
        if(groupBuy.getGpbuyStatus().equals("대기")) {
            Product product = productRepository.findById(bidRegisterRequest.getProductIdx())
                    .orElseThrow(() -> new InvalidBidException((PRODUCT_INFO_FAIL)));

            if(product.getCompany().getUser().getIdx().equals(userIdx)) {
                Bid bid = Bid.builder()
                        .bidPrice(bidRegisterRequest.getBidPrice())
                        .product(product)
                        .groupBuy(groupBuy)
                        .build();
                bidRepository.save(bid);

                return BidRegisterResponse.builder()
                        .bidIdx(bid.getIdx())
                        .build();
            } else throw new InvalidBidException(PRODUCT_VERIFICATION_FAIL);
        } else throw new InvalidBidException(BID_REGISTER_FAIL);

    }

    @Transactional(readOnly = true)
    public List<BidMyListResponse> myList(Integer page, Integer size, Long userIdx, Boolean bidSelect) {
        Pageable pageable = PageRequest.of(page, size);
        Slice<Bid> result = bidRepository.findAllByUserIdAndBidSelect(pageable, userIdx, bidSelect);
        List<Bid> bidList = result.getContent();

        List<BidMyListResponse> bidMyListResponses = new ArrayList<>();
        for(Bid bid : bidList) {
            BidMyListResponse bidMyListResponse = BidMyListResponse.builder()
                    .bidIdx(bid.getIdx())
                    .gpbuyIdx(bid.getGroupBuy().getIdx())
                    .gpbuyTitle(bid.getGroupBuy().getGpbuyTitle())
                    .gpbuyStatus(bid.getGroupBuy().getGpbuyStatus())
                    .productName(bid.getProduct().getProductName())
                    .productImgUrl(bid.getProduct().getProductImgList().stream()
                            .filter(img -> img.getProductImgSequence() == 0)
                            .findFirst()
                            .map(ProductImg::getProductImgUrl)
                            .orElse(null))
                    .bidPrice(bid.getBidPrice())
                    .build();
            bidMyListResponses.add(bidMyListResponse);
        }
        return bidMyListResponses;
    }

    public List<GpbuyWaitListResponse> statusWaitList(Integer page, Integer size, Long categoryIdx, String gpbuyTitle) {
        Pageable pageable = PageRequest.of(page, size);
        Slice<GroupBuy> result = groupBuyRepository.searchWaitList(pageable,categoryIdx, gpbuyTitle);

        List<GroupBuy> groupBuyList = result.getContent();
        List<GpbuyWaitListResponse> gpbuyWaitListResponseList = new ArrayList<>();
        for(GroupBuy groupBuy : groupBuyList) {
            GpbuyWaitListResponse gpbuyWaitListResponse = GpbuyWaitListResponse.builder()
                    .gpbuyIdx(groupBuy.getIdx())
                    .gpbuyTitle(groupBuy.getGpbuyTitle())
                    .gpbuyQuantity(groupBuy.getGpbuyQuantity())
                    .build();
            gpbuyWaitListResponseList.add(gpbuyWaitListResponse);
        }
        return gpbuyWaitListResponseList;
    }

    public void modify(Long userIdx, BidModifyRequest bidModifyRequest) {
        Bid bid = bidRepository.findById(bidModifyRequest.getBidIdx())
                .orElseThrow(() -> new InvalidBidException(BID_INFO_FAIL));
        Product product = productRepository.findById(bidModifyRequest.getProductIdx())
                .orElseThrow(() -> new InvalidBidException(PRODUCT_INFO_FAIL));
        if(product.getCompany().getUser().getIdx().equals(userIdx)) {
            if(bid.getGroupBuy().getGpbuyStatus().equals("대기")) {
                bid.updateBid(product, bidModifyRequest.getBidPrice());
                bid.updataStatus("수정");
                bidRepository.save(bid);
            } else throw new InvalidBidException(BID_MODIFY_FAIL);
        } else throw new InvalidBidException(PRODUCT_VERIFICATION_FAIL);
    }

    public void cancel(Long userIdx, BidCancelRequest bidCancelRequest) {
        Bid bid = bidRepository.findById(bidCancelRequest.getBidIdx())
                .orElseThrow(() -> new InvalidBidException(BID_INFO_FAIL));
        if (bid.getProduct().getCompany().getUser().getIdx().equals(userIdx)) {
            if(bid.getGroupBuy().getGpbuyStatus().equals("대기")) {
                bid.updataStatus("삭제");
                bidRepository.save(bid);
            } else throw new InvalidBidException(BID_DELETE_FAIL);
        } else throw new InvalidBidException(BID_DELETE_FAIL);
    }
}
